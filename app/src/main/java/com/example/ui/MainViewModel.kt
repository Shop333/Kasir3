package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.appDao)

    // === UI Tabs ===
    var currentTab by mutableStateOf(Tab.CASHIER)
    enum class Tab { CASHIER, STOCK, REPORTS, ADVANTAGES }

    // === Theme Mode ===
    var isDarkTheme by mutableStateOf(false)

    // === Cashier Shopping Cart State ===
    var cartItems = mutableStateOf<List<CartItem>>(emptyList())
    var searchQuery by mutableStateOf("")
    var barcodeQuery by mutableStateOf("")
    
    // Custom discount applied globally to the receipt (flat Rp)
    var globalDiscountAmount by mutableStateOf(0.0)

    // Active client selected for loyalty
    var selectedCustomer by mutableStateOf<Customer?>(null)
    var loyaltySearchPhone by mutableStateOf("")
    var loyaltySearchError by mutableStateOf("")

    // Active scale weighting simulation value (kg)
    var simulatedWeight by mutableStateOf(1.5)

    // Multi-Payment state
    var selectedPaymentMethod by mutableStateOf("Tunai") // "Tunai", "QRIS", "Transfer"
    var cashAmountPaidText by mutableStateOf("")
    var checkoutSuccessMessage by mutableStateOf<String?>(null)

    // === Sync State Simulator (Offline-First) ===
    var isOnlineState by mutableStateOf(true)
    var syncInProgress by mutableStateOf(false)
    var unsynchronizedCount by mutableStateOf(0)

    // === Database Flow Streams ===
    val productsList: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val batchesList: StateFlow<List<Batch>> = repository.allBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsList: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersList: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Seed database with fresh produce catalogue and historic analytics if clean
            repository.populateInitialDataIfEmpty()
        }
    }

    // === Cart Actions ===
    fun addToCart(product: Product, quantity: Double = 1.0) {
        val currentList = cartItems.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }

        if (existingIndex != -1) {
            val item = currentList[existingIndex]
            val nextQty = item.quantity + quantity
            // Check wholesale pricing trigger
            val isWholesale = nextQty >= product.wholesaleMinQty
            currentList[existingIndex] = item.copy(quantity = nextQty, isWholesale = isWholesale)
        } else {
            val isWholesale = quantity >= product.wholesaleMinQty
            currentList.add(CartItem(product = product, quantity = quantity, isWholesale = isWholesale))
        }
        cartItems.value = currentList
    }

    fun updateCartItemQty(productId: Int, nextQty: Double) {
        if (nextQty <= 0.0) {
            removeFromCart(productId)
            return
        }
        val currentList = cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = currentList[index]
            val isWholesale = nextQty >= item.product.wholesaleMinQty
            currentList[index] = item.copy(quantity = nextQty, isWholesale = isWholesale)
            cartItems.value = currentList
        }
    }

    fun applyCustomItemDiscount(productId: Int, rupiahDiscount: Double) {
        val currentList = cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            val item = currentList[index]
            currentList[index] = item.copy(customizedDiscount = rupiahDiscount)
            cartItems.value = currentList
        }
    }

    fun removeFromCart(productId: Int) {
        val currentList = cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index != -1) {
            currentList.removeAt(index)
            cartItems.value = currentList
        }
    }

    fun clearCart() {
        cartItems.value = emptyList()
        globalDiscountAmount = 0.0
        selectedCustomer = null
        loyaltySearchPhone = ""
        cashAmountPaidText = ""
        loyaltySearchError = ""
    }

    // === Weight Scale simulator ===
    fun triggerAutoWeigh(productId: Int) {
        // Simulates connection to Bluetooth retail scales
        // Generate a random weighted reading between 0.4kg and 3.5kg
        val scaleResult = (40 + (Math.random() * 300).toInt()) / 100.0 // e.g. 1.85 kg
        simulatedWeight = scaleResult

        // If the item is already in the cart, set its weight directly, otherwise add to cart with simulated weight
        val product = productsList.value.find { it.id == productId }
        if (product != null) {
            val currentList = cartItems.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.product.id == productId }
            if (existingIndex != -1) {
                val item = currentList[existingIndex]
                val isWholesale = scaleResult >= item.product.wholesaleMinQty
                currentList[existingIndex] = item.copy(quantity = scaleResult, isWholesale = isWholesale)
                cartItems.value = currentList
            } else {
                addToCart(product, scaleResult)
            }
        }
    }

    // === Barcode Simulator scanner ===
    fun scanBarcodeSimulated(barcode: String) {
        if (barcode.isBlank()) return
        viewModelScope.launch {
            val found = repository.getProductByBarcode(barcode)
            if (found != null) {
                // If it is unit 'buah', scan adds 1 unit. If kg, it triggers auto weigh automatically
                val qty = if (found.unit == "buah" || found.unit == "ikat") 1.0 else 1.25
                addToCart(found, qty)
                barcodeQuery = ""
            } else {
                barcodeQuery = "Kode Barcode Tidak Terdaftar!"
            }
        }
    }

    // === Loyalty Member Lookup ===
    fun searchLoyaltyCustomer() {
        if (loyaltySearchPhone.isBlank()) return
        viewModelScope.launch {
            val customer = repository.findCustomerByPhone(loyaltySearchPhone)
            if (customer != null) {
                selectedCustomer = customer
                loyaltySearchError = ""
            } else {
                selectedCustomer = null
                loyaltySearchError = "Member tidak ditemukan. Silakan tambahkan member baru!"
            }
        }
    }

    fun registerNewLoyaltyMember(name: String, phone: String) {
        if (name.isBlank() || phone.isBlank()) return
        viewModelScope.launch {
            val newCustomer = Customer(name = name, phone = phone, points = 10, tier = "Bronze")
            repository.saveCustomer(newCustomer)
            selectedCustomer = newCustomer
            loyaltySearchPhone = phone
            loyaltySearchError = ""
        }
    }

    // === Inventory & Product Operations ===
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun addIncomingBatch(productId: Int, initialQty: Double, shelfLifeDays: Int, origin: String, batchNum: String) {
        viewModelScope.launch {
            val p = repository.getProductById(productId)
            if (p != null) {
                val nextBatch = Batch(
                    productId = productId,
                    productReferenceName = p.name,
                    batchNumber = batchNum,
                    initialQty = initialQty,
                    currentQty = initialQty,
                    receivedDate = System.currentTimeMillis(),
                    shelfLifeDays = shelfLifeDays,
                    decayRiskFactor = when {
                        shelfLifeDays <= 3 -> 0.85
                        shelfLifeDays <= 6 -> 0.45
                        else -> 0.10
                    },
                    origin = origin
                )
                repository.addBatch(nextBatch)
                // Add to overall product catalogue level stock too
                val updatedStock = p.stock + initialQty
                repository.saveProduct(p.copy(stock = updatedStock))
            }
        }
    }

    // === Checkout Calculations ===
    fun getCartSalesTotalBeforeGlobalDiscount(): Double {
        return cartItems.value.sumOf { item ->
            val rate = if (item.isWholesale) item.product.wholesalePrice else item.product.retailPrice
            (item.quantity * rate) - item.customizedDiscount
        }
    }

    fun getFinalReceiptTotal(): Double {
        return (getCartSalesTotalBeforeGlobalDiscount() - globalDiscountAmount).coerceAtLeast(0.0)
    }

    fun checkoutActiveCart() {
        if (cartItems.value.isEmpty()) return

        val total = getFinalReceiptTotal()
        val cashPaid = if (selectedPaymentMethod == "Tunai") {
            cashAmountPaidText.toDoubleOrNull() ?: total
        } else {
            total
        }

        viewModelScope.launch {
            // Save locally always (offline-first design).
            // Sync status depends on the Online toggle state.
            val wasSynced = isOnlineState
            if (!wasSynced) {
                unsynchronizedCount += 1
            }

            val success = repository.checkout(
                items = cartItems.value,
                paymentMethod = selectedPaymentMethod,
                cashPaid = cashPaid,
                discountAmount = globalDiscountAmount,
                totalAmount = total,
                customerPhone = selectedCustomer?.phone,
                customerName = selectedCustomer?.name,
                isSynced = wasSynced
            )

            if (success) {
                checkoutSuccessMessage = "Transaksi berhasil disimpan! " + 
                        (if (wasSynced) "[SINKRON-ONLINE]" else "[OFFLINE-TERSIPAN]")
                clearCart()
            }
        }
    }

    // === Offline-First Synchronizer Simulator ===
    fun triggerForceSync() {
        viewModelScope.launch {
            syncInProgress = true
            // Simulate networking delays
            kotlinx.coroutines.delay(1200)
            unsynchronizedCount = 0
            syncInProgress = false
        }
    }
}
