package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val unit: String, // kg, ons, ikat, buah
    val basePrice: Double, // Modal cost
    val retailPrice: Double, // Price per unit
    val wholesalePrice: Double, // Bulk discount price
    val wholesaleMinQty: Double, // Qty needed for wholesale (e.g. 5.0 kg)
    val stock: Double, // Current inventory (decimal supported for Kg)
    val minStockThreshold: Double, // Threshold for alert
    val barcode: String? = null,
    val colorHex: String = "#FF9800" // Custom visual indicator for fruits
)

@Entity(tableName = "batches")
data class Batch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productReferenceName: String,
    val batchNumber: String,
    val initialQty: Double,
    val currentQty: Double,
    val receivedDate: Long, // timestamp
    val shelfLifeDays: Int, // e.g. 5 days till rot
    val decayRiskFactor: Double = 0.1, // 0.0 to 1.0 risk of decay (spoiling speed)
    val origin: String = "Medan"
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val timestamp: Long,
    val totalAmount: Double,
    val discountAmount: Double,
    val paymentMethod: String, // "Tunai", "QRIS", "Transfer"
    val cashPaid: Double,
    val changeAmount: Double,
    val customerPhone: String? = null, // linked customer
    val customerName: String? = null,
    val isSynced: Boolean = false // for offline-first visualization
)

@Entity(tableName = "transaction_items")
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transactionId: Int, // Links to TransactionEntity.id
    val productId: Int,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val pricePerUnit: Double,
    val subtotal: Double
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String, // Unique identifier
    val points: Int = 0,
    val tier: String = "Bronze" // Bronze, Silver, Gold
)
