package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class AppRepository(private val appDao: AppDao) {

    val allProducts: Flow<List<Product>> = appDao.getAllProducts()
    val allBatches: Flow<List<Batch>> = appDao.getAllBatches()
    val allTransactions: Flow<List<TransactionEntity>> = appDao.getAllTransactions()
    val allCustomers: Flow<List<Customer>> = appDao.getAllCustomers()

    suspend fun getProductById(id: Int): Product? = appDao.getProductById(id)
    suspend fun getProductByBarcode(barcode: String): Product? = appDao.getProductByBarcode(barcode)

    suspend fun saveProduct(product: Product) {
        if (product.id == 0) {
            val generatedId = appDao.insertProduct(product)
            // Create a default batch automatically when a new product is added
            val defaultBatch = Batch(
                productId = generatedId.toInt(),
                productReferenceName = product.name,
                batchNumber = "BATCH-${System.currentTimeMillis() / 1000}",
                initialQty = product.stock,
                currentQty = product.stock,
                receivedDate = System.currentTimeMillis(),
                shelfLifeDays = 7, // Default 7 days shelf life for new additions
                decayRiskFactor = 0.15,
                origin = "Kebun Mitra Lokal"
            )
            appDao.insertBatch(defaultBatch)
        } else {
            appDao.updateProduct(product)
        }
    }

    suspend fun updateProductStockDirectly(productId: Int, newStock: Double) {
        appDao.updateProductStock(productId, newStock)
    }

    suspend fun deleteProduct(product: Product) {
        appDao.deleteProduct(product)
    }

    // === Batch Operations ===
    suspend fun addBatch(batch: Batch) {
        appDao.insertBatch(batch)
    }

    suspend fun updateBatch(batch: Batch) {
        appDao.updateBatch(batch)
    }

    suspend fun deleteBatch(batch: Batch) {
        appDao.deleteBatch(batch)
    }

    // === Customer Operations ===
    suspend fun findCustomerByPhone(phone: String): Customer? = appDao.getCustomerByPhone(phone)
    suspend fun saveCustomer(customer: Customer) {
        if (customer.id == 0) {
            appDao.insertCustomer(customer)
        } else {
            appDao.updateCustomer(customer)
        }
    }

    // === Transaction details ===
    suspend fun getTransactionItems(transactionId: Int): List<TransactionItemEntity> {
        return appDao.getItemsForTransaction(transactionId)
    }

    // === Checkout Implementation ===
    suspend fun checkout(
        items: List<CartItem>,
        paymentMethod: String,
        cashPaid: Double,
        discountAmount: Double,
        totalAmount: Double,
        customerPhone: String?,
        customerName: String?,
        isSynced: Boolean = true
    ): Boolean {
        // 1. Create Transaction entity
        val invoice = "TRX-${System.currentTimeMillis() / 1000}"
        val transaction = TransactionEntity(
            invoiceNumber = invoice,
            timestamp = System.currentTimeMillis(),
            totalAmount = totalAmount,
            discountAmount = discountAmount,
            paymentMethod = paymentMethod,
            cashPaid = cashPaid,
            changeAmount = if (cashPaid > 0) (cashPaid - totalAmount).coerceAtLeast(0.0) else 0.0,
            customerPhone = customerPhone,
            customerName = customerName,
            isSynced = isSynced
        )

        val txId = appDao.insertTransaction(transaction).toInt()

        // 2. Insert items and update product catalog stock & batches
        for (cartItem in items) {
            val salePrice = if (cartItem.isWholesale) cartItem.product.wholesalePrice else cartItem.product.retailPrice
            val itemEntity = TransactionItemEntity(
                transactionId = txId,
                productId = cartItem.product.id,
                productName = cartItem.product.name,
                quantity = cartItem.quantity,
                unit = cartItem.product.unit,
                pricePerUnit = salePrice,
                subtotal = cartItem.quantity * salePrice
            )
            appDao.insertTransactionItem(itemEntity)

            // Reduce main stock
            val currentProduct = appDao.getProductById(cartItem.product.id)
            if (currentProduct != null) {
                val nextStock = (currentProduct.stock - cartItem.quantity).coerceAtLeast(0.0)
                appDao.updateProductStock(currentProduct.id, nextStock)
            }

            // Reduce FIFO Batches
            val batches = appDao.getActiveBatchesForProduct(cartItem.product.id)
            var qtyToDeduct = cartItem.quantity
            for (batch in batches) {
                if (qtyToDeduct <= 0.0) break
                val dec = minOf(batch.currentQty, qtyToDeduct)
                val updatedBatch = batch.copy(currentQty = batch.currentQty - dec)
                appDao.updateBatch(updatedBatch)
                qtyToDeduct -= dec
            }
        }

        // 3. Update customer points
        if (!customerPhone.isNullOrBlank()) {
            val customer = appDao.getCustomerByPhone(customerPhone)
            if (customer != null) {
                // Earn points: 1 point for every Rp 10.000 spent
                val pointsEarned = (totalAmount / 10000.0).toInt()
                val updatedPoints = customer.points + pointsEarned
                val nextTier = when {
                    updatedPoints >= 500 -> "Gold"
                    updatedPoints >= 200 -> "Silver"
                    else -> "Bronze"
                }
                appDao.updateCustomer(customer.copy(points = updatedPoints, tier = nextTier))
            }
        }

        return true
    }

    // === Prepulate Mock Data for first-time usage ===
    suspend fun populateInitialDataIfEmpty() {
        // Expose first block list
        val currentProducts = appDao.getAllProducts().firstOrNull() ?: emptyList()
        if (currentProducts.isEmpty()) {
            // 1. Initial Products
            val p1 = Product(name = "Mangga Arumanis", unit = "kg", basePrice = 18000.0, retailPrice = 25000.0, wholesalePrice = 22000.0, wholesaleMinQty = 5.0, stock = 45.0, minStockThreshold = 10.0, barcode = "899123456001", colorHex = "#FFC107")
            val p2 = Product(name = "Pisang Raja", unit = "ikat", basePrice = 12000.0, retailPrice = 18000.0, wholesalePrice = 15000.0, wholesaleMinQty = 3.0, stock = 8.0, minStockThreshold = 10.0, barcode = "899123456002", colorHex = "#FFEB3B")
            val p3 = Product(name = "Apel Malang Fuyu", unit = "kg", basePrice = 22000.0, retailPrice = 30000.0, wholesalePrice = 27000.0, wholesaleMinQty = 5.0, stock = 30.0, minStockThreshold = 10.0, barcode = "899123456003", colorHex = "#F44336")
            val p4 = Product(name = "Alpukat Mentega Super", unit = "kg", basePrice = 20000.0, retailPrice = 28000.0, wholesalePrice = 25000.0, wholesaleMinQty = 4.0, stock = 15.0, minStockThreshold = 5.0, barcode = "899123456004", colorHex = "#4CAF50")
            val p5 = Product(name = "Semangka Merah Tanpa Biji", unit = "buah", basePrice = 14000.0, retailPrice = 22000.0, wholesalePrice = 19000.0, wholesaleMinQty = 3.0, stock = 25.0, minStockThreshold = 8.0, barcode = "899123456005", colorHex = "#8BC34A")
            val p6 = Product(name = "Pepaya California", unit = "buah", basePrice = 7000.0, retailPrice = 12000.0, wholesalePrice = 10000.0, wholesaleMinQty = 3.0, stock = 4.0, minStockThreshold = 5.0, barcode = "899123456006", colorHex = "#FF9800")
            val p7 = Product(name = "Jeruk Sunkist Fresh", unit = "kg", basePrice = 28000.0, retailPrice = 38000.0, wholesalePrice = 34000.0, wholesaleMinQty = 5.0, stock = 50.0, minStockThreshold = 10.0, barcode = "899123456007", colorHex = "#FF5722")

            val id1 = appDao.insertProduct(p1).toInt()
            val id2 = appDao.insertProduct(p2).toInt()
            val id3 = appDao.insertProduct(p3).toInt()
            val id4 = appDao.insertProduct(p4).toInt()
            val id5 = appDao.insertProduct(p5).toInt()
            val id6 = appDao.insertProduct(p6).toInt()
            val id7 = appDao.insertProduct(p7).toInt()

            // 2. Populate Freshness Batches
            val nowTime = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L

            // Pisang (High decay risk, expires in 2-3 days, very fast rotted)
            appDao.insertBatch(Batch(productId = id2, productReferenceName = "Pisang Raja", batchNumber = "B-PSG-01", initialQty = 8.0, currentQty = 8.0, receivedDate = nowTime - (1 * dayMs), shelfLifeDays = 3, decayRiskFactor = 0.90, origin = "Sukabumi"))
            
            // Pepaya (Expires in 4 days)
            appDao.insertBatch(Batch(productId = id6, productReferenceName = "Pepaya California", batchNumber = "B-PEPAYA-01", initialQty = 4.0, currentQty = 4.0, receivedDate = nowTime - (1.5 * dayMs).toLong(), shelfLifeDays = 4, decayRiskFactor = 0.65, origin = "Lampung"))
            
            // Mangga (Semi-risk, expires in 6 days)
            appDao.insertBatch(Batch(productId = id1, productReferenceName = "Mangga Arumanis", batchNumber = "B-MNG-01", initialQty = 25.0, currentQty = 25.0, receivedDate = nowTime - (2 * dayMs), shelfLifeDays = 7, decayRiskFactor = 0.35, origin = "Probolinggo"))
            appDao.insertBatch(Batch(productId = id1, productReferenceName = "Mangga Arumanis", batchNumber = "B-MNG-02", initialQty = 20.0, currentQty = 20.0, receivedDate = nowTime, shelfLifeDays = 8, decayRiskFactor = 0.25, origin = "Indramayu"))

            // Apel (Very stable, expires in 14 days)
            appDao.insertBatch(Batch(productId = id3, productReferenceName = "Apel Malang Fuyu", batchNumber = "B-APL-01", initialQty = 30.0, currentQty = 30.0, receivedDate = nowTime - (3 * dayMs), shelfLifeDays = 14, decayRiskFactor = 0.10, origin = "Batu Malang"))

            // Alpukat (Expires in 5 days)
            appDao.insertBatch(Batch(productId = id4, productReferenceName = "Alpukat Mentega Super", batchNumber = "B-AVO-01", initialQty = 15.0, currentQty = 15.0, receivedDate = nowTime - (1 * dayMs), shelfLifeDays = 5, decayRiskFactor = 0.40, origin = "Garut"))

            // Semangka
            appDao.insertBatch(Batch(productId = id5, productReferenceName = "Semangka Merah Tanpa Biji", batchNumber = "B-SMG-01", initialQty = 25.0, currentQty = 25.0, receivedDate = nowTime - (1 * dayMs), shelfLifeDays = 9, decayRiskFactor = 0.15, origin = "Banyuwangi"))

            // Jeruk (Stable, expires in 12 days)
            appDao.insertBatch(Batch(productId = id7, productReferenceName = "Jeruk Sunkist Fresh", batchNumber = "B-JRK-01", initialQty = 50.0, currentQty = 50.0, receivedDate = nowTime, shelfLifeDays = 12, decayRiskFactor = 0.08, origin = "Import"))

            // 3. Customers (Loyalty points)
            val c1 = Customer(name = "Budi Santoso", phone = "081234567890", points = 240, tier = "Silver")
            val c2 = Customer(name = "Siti Rahma", phone = "085678901234", points = 510, tier = "Gold")
            val c3 = Customer(name = "Andi Wijaya", phone = "087789012345", points = 45, tier = "Bronze")
            
            appDao.insertCustomer(c1)
            appDao.insertCustomer(c2)
            appDao.insertCustomer(c3)

            // 4. Past historical transactions for analytics charts/reports
            // Trx 1 (Rp 88,000)
            val t1Id = appDao.insertTransaction(TransactionEntity(
                invoiceNumber = "TRX-HIST-01",
                timestamp = nowTime - (4 * 3600 * 1000), // 4 hours ago
                totalAmount = 88000.0,
                discountAmount = 5000.0,
                paymentMethod = "QRIS",
                cashPaid = 88000.0,
                changeAmount = 0.0,
                customerPhone = "081234567890",
                customerName = "Budi Santoso",
                isSynced = true
            )).toInt()
            appDao.insertTransactionItem(TransactionItemEntity(transactionId = t1Id, productId = id1, productName = "Mangga Arumanis", quantity = 2.0, unit = "kg", pricePerUnit = 25000.0, subtotal = 50000.0))
            appDao.insertTransactionItem(TransactionItemEntity(transactionId = t1Id, productId = id7, productName = "Jeruk Sunkist Fresh", quantity = 1.0, unit = "kg", pricePerUnit = 38000.0, subtotal = 38000.0))

            // Trx 2 (Rp 36,000)
            val t2Id = appDao.insertTransaction(TransactionEntity(
                invoiceNumber = "TRX-HIST-02",
                timestamp = nowTime - (12 * 3600 * 1000), // 12 hours ago
                totalAmount = 36000.0,
                discountAmount = 0.0,
                paymentMethod = "Tunai",
                cashPaid = 50000.0,
                changeAmount = 14000.0,
                isSynced = true
            )).toInt()
            appDao.insertTransactionItem(TransactionItemEntity(transactionId = t2Id, productId = id2, productName = "Pisang Raja", quantity = 2.0, unit = "ikat", pricePerUnit = 18000.0, subtotal = 36000.0))

            // Trx 3 (Rp 220,000)
            val t3Id = appDao.insertTransaction(TransactionEntity(
                invoiceNumber = "TRX-HIST-03",
                timestamp = nowTime - (27 * 3600 * 1000), // Yesterday
                totalAmount = 220000.0,
                discountAmount = 15000.0,
                paymentMethod = "Transfer",
                cashPaid = 220000.0,
                changeAmount = 0.0,
                customerPhone = "085678901234",
                customerName = "Siti Rahma",
                isSynced = true
            )).toInt()
            appDao.insertTransactionItem(TransactionItemEntity(transactionId = t3Id, productId = id3, productName = "Apel Malang Fuyu", quantity = 5.0, unit = "kg", pricePerUnit = 27000.0, subtotal = 135000.0)) // wholesale
            appDao.insertTransactionItem(TransactionItemEntity(transactionId = t3Id, productId = id7, productName = "Jeruk Sunkist Fresh", quantity = 2.5, unit = "kg", pricePerUnit = 38000.0, subtotal = 95000.0)) // retail
        }
    }
}

// Helper POJOs for Shopping Cart State
data class CartItem(
    val product: Product,
    var quantity: Double,
    val isWholesale: Boolean = false,
    val customizedDiscount: Double = 0.0 // Custom manual rupiah discount per item
)
