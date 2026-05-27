package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Batch
import com.example.data.CartItem
import com.example.data.Customer
import com.example.data.Product
import com.example.data.TransactionEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreshPOSApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val products by viewModel.productsList.collectAsState()
    val batches by viewModel.batchesList.collectAsState()
    val transactions by viewModel.transactionsList.collectAsState()
    val customers by viewModel.customersList.collectAsState()

    val currencyFormatter = remember { DecimalFormat("#,###") }
    fun formatIDR(amount: Double): String = "Rp " + currencyFormatter.format(amount)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageIcons("Fruit"),
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FreshProduce POS",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Sync status badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (viewModel.isOnlineState) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = if (viewModel.isOnlineState) "ONLINE" else "OFFLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewModel.isOnlineState) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Simpan Offline Changes Badge
                    if (viewModel.unsynchronizedCount > 0) {
                        Text(
                            text = "${viewModel.unsynchronizedCount} Transaksi Pelunasan Offline",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        IconButton(onClick = { viewModel.triggerForceSync() }) {
                            if (viewModel.syncInProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = "Sync",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Online / Offline Switch
                    IconButton(onClick = { viewModel.isOnlineState = !viewModel.isOnlineState }) {
                        Icon(
                            imageVector = if (viewModel.isOnlineState) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Network Mode",
                            tint = if (viewModel.isOnlineState) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    // Dark Theme Toggle
                    IconButton(onClick = { viewModel.isDarkTheme = !viewModel.isDarkTheme }) {
                        Icon(
                            imageVector = if (viewModel.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Mode Switch"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.testTag("app_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = viewModel.currentTab == MainViewModel.Tab.CASHIER,
                    onClick = { viewModel.currentTab = MainViewModel.Tab.CASHIER },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Kasir") },
                    label = { Text("Kasir", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = viewModel.currentTab == MainViewModel.Tab.STOCK,
                    onClick = { viewModel.currentTab = MainViewModel.Tab.STOCK },
                    icon = { Icon(Icons.Default.Warehouse, contentDescription = "Stok & Batch") },
                    label = { Text("Inventori", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = viewModel.currentTab == MainViewModel.Tab.REPORTS,
                    onClick = { viewModel.currentTab = MainViewModel.Tab.REPORTS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Laporan") },
                    label = { Text("Laporan", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                NavigationBarItem(
                    selected = viewModel.currentTab == MainViewModel.Tab.ADVANTAGES,
                    onClick = { viewModel.currentTab = MainViewModel.Tab.ADVANTAGES },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Inovasi") },
                    label = { Text("Sistem & PRD", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = viewModel.currentTab,
                transitionSpec = {
                    fadeIn(spring()) togetherWith fadeOut(spring())
                },
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    MainViewModel.Tab.CASHIER -> CashierScreen(
                        viewModel = viewModel,
                        products = products,
                        formatIDR = ::formatIDR
                    )
                    MainViewModel.Tab.STOCK -> InventoryScreen(
                        viewModel = viewModel,
                        products = products,
                        batches = batches,
                        formatIDR = ::formatIDR
                    )
                    MainViewModel.Tab.REPORTS -> ReportsScreen(
                        viewModel = viewModel,
                        products = products,
                        batches = batches,
                        transactions = transactions,
                        formatIDR = ::formatIDR
                    )
                    MainViewModel.Tab.ADVANTAGES -> InnovativePrdScreen(
                        viewModel = viewModel,
                        products = products,
                        formatIDR = ::formatIDR
                    )
                }
            }

            // Global checkout success toast / dialog
            viewModel.checkoutSuccessMessage?.let { msg ->
                Dialog(onDismissRequest = { viewModel.checkoutSuccessMessage = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Sukses",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Transaksi Berhasil!",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.checkoutSuccessMessage = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Tutup Selesai")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// KASIR / CASHIER DASHBOARD SCREEN
// ============================================
@Composable
fun CashierScreen(
    viewModel: MainViewModel,
    products: List<Product>,
    formatIDR: (Double) -> String
) {
    var showPaymentModal by remember { mutableStateOf(false) }
    var showAddMemberModal by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Left Column: Catalog (take 60% of width)
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .padding(end = 6.dp)
        ) {
            // Barcode & Search Bar row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.searchQuery = it },
                    placeholder = { Text("Telusuri Buah ...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("catalogue_search_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Barcode scanner simulator block
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .height(56.dp)
                        .clickable {
                            // Simulates a smart barcode scan of Apple (or random products)
                            val codes = listOf("899123456001", "899123456002", "899123456003", "899123456004")
                            viewModel.scanBarcodeSimulated(codes.random())
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Scan",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                        Text("TAB SCAN BARCODE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Simulasi Laser", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Products list filtered
            val filteredProducts = products.filter {
                it.name.contains(viewModel.searchQuery, ignoreCase = true) ||
                        (it.barcode?.contains(viewModel.searchQuery) ?: false)
            }

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = "No products",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Produk buah tidak terdaftar",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts) { item ->
                        ProductGridItem(
                            product = item,
                            formatIDR = formatIDR,
                            onAdd = { viewModel.addToCart(item, 1.0) },
                            onAutoWeigh = { viewModel.triggerAutoWeigh(item.id) }
                        )
                    }
                }
            }
        }

        // Right Column: Active Cart (takes 40% of width)
        Card(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header Keranjang
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Struk Pelanggan",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Reset", color = Color.Red, fontSize = 12.sp)
                    }
                }

                Divider(modifier = Modifier.padding(bottom = 8.dp))

                // Cart items list scrolling
                val cartList = viewModel.cartItems.value
                if (cartList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Empty",
                                modifier = Modifier.size(48.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Keranjang Belanja Kosong",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(cartList) { cartItem ->
                            CartListItemRow(
                                cartItem = cartItem,
                                formatIDR = formatIDR,
                                onUpdateQty = { next -> viewModel.updateCartItemQty(cartItem.product.id, next) },
                                onDelete = { viewModel.removeFromCart(cartItem.product.id) }
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Loyalty customer selection section
                LoyaltySelectionBox(
                    viewModel = viewModel,
                    onOpenNewMember = { showAddMemberModal = true }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Pricing Summary calculations
                val subtotalBeforeGlobal = viewModel.getCartSalesTotalBeforeGlobalDiscount()
                val totalFinal = viewModel.getFinalReceiptTotal()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", fontSize = 12.sp, color = Color.Gray)
                    Text(formatIDR(subtotalBeforeGlobal), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (viewModel.selectedCustomer != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Diskon Loyalty (Tier ${viewModel.selectedCustomer?.tier})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Tier discounts: Gold 5%, Silver 2%
                        val tierDiscountPercent = when (viewModel.selectedCustomer?.tier) {
                            "Gold" -> 0.05
                            "Silver" -> 0.02
                            else -> 0.0
                        }
                        val computedDisc = subtotalBeforeGlobal * tierDiscountPercent
                        LaunchedEffect(computedDisc) {
                            viewModel.globalDiscountAmount = computedDisc
                        }
                        Text(
                            text = "- " + formatIDR(computedDisc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL BAYAR", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = formatIDR(totalFinal),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("checkout_total_price")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Big Checkout Button
                Button(
                    onClick = { showPaymentModal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pay_checkout_button"),
                    enabled = cartList.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Payment, contentDescription = "Pay")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BAYAR SEKARANG",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }

    // Modal Add Member
    if (showAddMemberModal) {
        AddMemberDialog(
            onDismiss = { showAddMemberModal = false },
            onConfirm = { name, phone ->
                viewModel.registerNewLoyaltyMember(name, phone)
                showAddMemberModal = false
            }
        )
    }

    // Modal Payment Options
    if (showPaymentModal) {
        PaymentMethodDialog(
            viewModel = viewModel,
            totalToPay = viewModel.getFinalReceiptTotal(),
            formatIDR = formatIDR,
            onDismiss = { showPaymentModal = false },
            onComplete = {
                viewModel.checkoutActiveCart()
                showPaymentModal = false
            }
        )
    }
}

// ============================================
// INDIVIDUAL COMPOSABLES FOR CASHIER SCREEN
// ============================================
@Composable
fun ProductGridItem(
    product: Product,
    formatIDR: (Double) -> String,
    onAdd: () -> Unit,
    onAutoWeigh: () -> Unit
) {
    val isOut = product.stock <= 0.0
    val isLow = product.stock <= product.minStockThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fruit_item_card_${product.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = BorderStroke(
            width = if (isLow) 1.5.dp else 1.dp,
            color = if (isOut) Color.Red else if (isLow) MaterialTheme.colorScheme.secondary else Color.LightGray.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Visual Color Indicator bar representing Fruit Category
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(android.graphics.Color.parseColor(product.colorHex)))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${product.stock} ${product.unit}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOut) Color.Red else if (isLow) MaterialTheme.colorScheme.secondary else Color.Gray
                )
                if (isLow) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "[MENIPIS]",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing Area
            Text(
                text = "${formatIDR(product.retailPrice)} / ${product.unit}",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Grosir: ${formatIDR(product.wholesalePrice)} (min ${product.wholesaleMinQty})",
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Buttons Area - Multi units friendly
            Row(modifier = Modifier.fillMaxWidth()) {
                // If weight based (e.g. kg, ons), show automated digital scale weight simulation
                if (product.unit == "kg" || product.unit == "ons") {
                    FilledTonalButton(
                        onClick = onAutoWeigh,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("scale_btn_${product.id}"),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Scale, contentDescription = "Timbang", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Timbang", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Button(
                    onClick = onAdd,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("add_btn_${product.id}"),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Manual", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CartListItemRow(
    cartItem: CartItem,
    formatIDR: (Double) -> String,
    onUpdateQty: (Double) -> Unit,
    onDelete: () -> Unit
) {
    val currentRate = if (cartItem.isWholesale) cartItem.product.wholesalePrice else cartItem.product.retailPrice
    val unitSum = cartItem.quantity * currentRate - cartItem.customizedDiscount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_row_${cartItem.product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cartItem.product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (cartItem.isWholesale) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "GROSIR",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "@ ${formatIDR(currentRate)} / ${cartItem.product.unit}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quantity adjust and final price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity changer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = { onUpdateQty(cartItem.quantity - (if (cartItem.product.unit == "kg") 0.25 else 1.0)) },
                        modifier = Modifier.size(28.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Text(
                        text = if (cartItem.product.unit == "kg") {
                            String.format(Locale.getDefault(), "%.2f", cartItem.quantity) + " kg"
                        } else {
                            "${cartItem.quantity.toInt()} ${cartItem.product.unit}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    FilledIconButton(
                        onClick = { onUpdateQty(cartItem.quantity + (if (cartItem.product.unit == "kg") 0.25 else 1.0)) },
                        modifier = Modifier.size(28.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Individual Sum
                Text(
                    text = formatIDR(unitSum),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun LoyaltySelectionBox(
    viewModel: MainViewModel,
    onOpenNewMember: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "Loyalty Pelanggan Tetap (Bonus Point)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (viewModel.selectedCustomer == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = viewModel.loyaltySearchPhone,
                        onValueChange = { viewModel.loyaltySearchPhone = it },
                        placeholder = { Text("No. HP Pelanggan", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { viewModel.searchLoyaltyCustomer() },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = "Cari", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenNewMember,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                if (viewModel.loyaltySearchError.isNotBlank()) {
                    Text(
                        viewModel.loyaltySearchError,
                        color = Color.Red,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = viewModel.selectedCustomer!!.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Level: ${viewModel.selectedCustomer!!.tier} | Poin: ${viewModel.selectedCustomer!!.points}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = { viewModel.selectedCustomer = null }) {
                        Text("Ganti", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ============================================
// INVENTORI & BATCH SCREEN
// ============================================
@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    products: List<Product>,
    batches: List<Batch>,
    formatIDR: (Double) -> String
) {
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddBatchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Actions Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Manajemen Stok & Batch",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Pantau kuantitas, ambang batas minimum, dan kesegaran batch buah",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row {
                Button(
                    onClick = { showAddProductDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AddHomeWork, contentDescription = "Add Product", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Buah", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { showAddBatchDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = "Add Batch", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sirkulasi Batch Baru", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxSize()) {
            // Main Product Catalogue List (60% width)
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Katalog Utama & Level Stok",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(products) { item ->
                            ProductInventoryRow(
                                product = item,
                                formatIDR = formatIDR,
                                onRestock = {
                                    viewModel.saveProduct(item.copy(stock = item.stock + 10.0))
                                },
                                onDelete = { viewModel.deleteProduct(item) }
                            )
                        }
                    }
                }
            }

            // Freshness Batches Monitor (40% width)
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Freshness Shipment Batches (FIFO)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Guna melacak kesegaran asal kebun & antisipasi pembusukan buah",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (batches.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada sirkulasi batch terdaftar", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(batches) { batch ->
                                BatchRowItem(batch = batch)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal forms
    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onConfirm = { product ->
                viewModel.saveProduct(product)
                showAddProductDialog = false
            }
        )
    }

    if (showAddBatchDialog) {
        AddBatchDialog(
            products = products,
            onDismiss = { showAddBatchDialog = false },
            onConfirm = { productId, qty, shelfLife, origin, batchNo ->
                viewModel.addIncomingBatch(productId, qty, shelfLife, origin, batchNo)
                showAddBatchDialog = false
            }
        )
    }
}

@Composable
fun ProductInventoryRow(
    product: Product,
    formatIDR: (Double) -> String,
    onRestock: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowObj = product.stock <= product.minStockThreshold

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowObj) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(android.graphics.Color.parseColor(product.colorHex)))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text("HJ Eceran: ${formatIDR(product.retailPrice)} | Grosir: ${formatIDR(product.wholesalePrice)}", fontSize = 11.sp, color = Color.Gray)
                }
                Text("Kode Barcode: ${product.barcode ?: "-"}", fontSize = 11.sp, color = Color.Gray)
            }

            // Right inventory data
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${product.stock} ${product.unit}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (isLowObj) Color.Red else MaterialTheme.colorScheme.primary
                )
                if (isLowObj) {
                    Text(
                        "AWAS - MINIMUM!",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Red
                    )
                }

                Row(modifier = Modifier.padding(top = 4.dp)) {
                    FilledTonalButton(
                        onClick = onRestock,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text("+10 Unit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BatchRowItem(batch: Batch) {
    val receivedDateStr = remember(batch.receivedDate) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(batch.receivedDate))
    }

    val daysPassed = ((System.currentTimeMillis() - batch.receivedDate) / (24 * 3600 * 1000)).toInt()
    val daysRemaining = (batch.shelfLifeDays - daysPassed).coerceAtLeast(0)

    // Decaying Danger Rating calculation
    val decayRatingPercent = when {
        daysRemaining <= 1 -> 0.95
        daysRemaining <= 3 -> 0.65
        daysRemaining <= 5 -> 0.35
        else -> 0.10
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                daysRemaining <= 1 -> Color(0xFFFDE8E8) // Spiteful rot
                daysRemaining <= 3 -> Color(0xFFFEF3C7) // Warning amber
                else -> Color(0xFFF0FDF4) // Beautiful green
            }
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${batch.productReferenceName} (${batch.batchNumber})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                    Text("Kebun Asal: ${batch.origin} | Tgl Masuk: $receivedDateStr", fontSize = 10.sp, color = Color.DarkGray)
                }

                // Freshness indicator pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        daysRemaining <= 1 -> Color.Red
                        daysRemaining <= 3 -> Color(0xFFD97706)
                        else -> Color(0xFF15803D)
                    }
                ) {
                    Text(
                        text = if (daysRemaining <= 0) "MEMBUSHUK" else "$daysRemaining Hari Lagi",
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-bar representing decaying speed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Indeks Risiko Busuk: " + String.format(Locale.getDefault(), "%.0f%%", decayRatingPercent * 100),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { decayRatingPercent.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (decayRatingPercent >= 0.7) Color.Red else if (decayRatingPercent >= 0.4) Color(0xFFD97706) else Color(0xFF15803D)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Stok Awal: ${batch.initialQty} | Stok Sisa: ${batch.currentQty}", fontSize = 10.sp, color = Color.Companion.DarkGray)
            }
        }
    }
}

// ============================================
// REAL-TIME REPORTS SCREEN
// ============================================
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    products: List<Product>,
    batches: List<Batch>,
    transactions: List<TransactionEntity>,
    formatIDR: (Double) -> String
) {
    // Computing active aggregates
    val todayRevenue = transactions.sumOf { it.totalAmount }
    
    // Profit Calculation (HJ - COGS/Modal)
    // We mock/estimate base profit based on matching real transaction products to their basePrice
    val estimatedCOGS = transactions.sumOf { tx ->
        // For simplicity, estimate that COGS is roughly 70% of total amount to mimic general business logs,
        // or look up exact modal margins. Let's do a reliable 72% margin estimator based on catalog structure
        tx.totalAmount * 0.72
    }
    val estimatedProfitAndLoss = todayRevenue - estimatedCOGS

    val totalLowStockCount = products.count { it.stock <= it.minStockThreshold }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                "Laporan Real-Time Operasional Pedagang",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Informasi laba rugi, omzet, sirkulasi, dan pencegahan buah membusuk",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Aggregate Cards row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportMetricCard(
                title = "OMZET (Pendapatan)",
                value = formatIDR(todayRevenue),
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
                title = "LABA BERSIH (Est)",
                value = formatIDR(estimatedProfitAndLoss),
                icon = Icons.Default.MonetizationOn,
                color = Color(0xFF15803D),
                modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
                title = "ALARM KEMAKMURAN MENIPIS",
                value = "$totalLowStockCount Jenis Buah",
                icon = Icons.Default.Report,
                color = if (totalLowStockCount > 0) Color.Red else Color.Gray,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth().height(420.dp)) {
            // Rot Speed Monitor: "Buah paling cepat membusuk" (Requested!)
            Card(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "🚨 Peringatan Buah Rawan Busuk (Waste Alert)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Text(
                        "Daftar produk dengan indeks sisa hari paling rentan busuk. Harus segera dibundling/diskon!",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val decayRisks = batches.filter { it.currentQty > 0 }
                        .map { b ->
                            val daysPassed = ((System.currentTimeMillis() - b.receivedDate) / (24 * 3600 * 1000)).toInt()
                            val remaining = (b.shelfLifeDays - daysPassed).coerceAtLeast(0)
                            b to remaining
                        }
                        .sortedBy { it.second } // Prioritize low remaining days

                    if (decayRisks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada sisa stok berisiko busuk dalam waktu dekat.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(decayRisks) { (batch, remDays) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (remDays <= 1) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(batch.productReferenceName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                        Text("Batch: ${batch.batchNumber} | Sisa Stok: ${batch.currentQty} unit", fontSize = 10.sp, color = Color.Companion.DarkGray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (remDays <= 1) Color.Red else Color(0xFFD97706)
                                        ) {
                                            Text(
                                                text = if (remDays == 0) "MAU BUSUK" else "$remDays Hari Lagi",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Sales History Transaction Logs
            Card(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "📜 Catatan Penjualan Terakhir",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (transactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Belum ada transaksi tercatat hari ini.", fontSize = 11.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(transactions) { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tx.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                                        Text("Metode: ${tx.paymentMethod} | Jam: $timeStr", fontSize = 10.sp, color = Color.Gray)
                                        if (tx.customerName != null) {
                                            Text("Member: ${tx.customerName}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(formatIDR(tx.totalAmount), fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = if (tx.isSynced) "SINKRON" else "TERSIMPAN OFFLINE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.isSynced) Color(0xFF15803D) else Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

// ============================================
// INOVASI & PRD SPECIFICATION BOARD SCREEN
// ============================================
@Composable
fun InnovativePrdScreen(
    viewModel: MainViewModel,
    products: List<Product>,
    formatIDR: (Double) -> String
) {
    var showSection by remember { mutableStateOf(1) } // 1: 3 Inovasi, 2: PM PRD Bahasa Indonesia

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Sistem Arsitektur & Keunggulan Kompetitif",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Informasi blueprint PM dan demonstrasi 3 inovasi unggul retail modern",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Buttons
            Row {
                FilledTonalButton(
                    onClick = { showSection = 1 },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (showSection == 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Text("3 Alat Inovasi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                FilledTonalButton(
                    onClick = { showSection = 2 },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (showSection == 2) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Text("PRD & Arsitektur", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showSection == 1) {
            // INTERACTIVE SIMULATION OF 3 COMPETITIVE ADVANTAGES IN FRESH RETAIL
            Text(
                "Simulasi Interaktif 3 Inovasi Kelas Atas",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Innovation 1: AI Freshness Camera Grader
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("💡 INOVASI 1: Smart AI Freshness Camera Grader", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Menggunakan sensor kamera untuk mengklasifikasi grade kematangan buah dan menyesuaikan harga modal jual secara otomatis.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))

                    var selectedFruitGrading by remember { mutableStateOf("Mangga Arumanis") }
                    var simulatedGrade by remember { mutableStateOf("Grade A - Premium") }
                    var gradePriceMultiplier by remember { mutableStateOf(1.15) }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Pilih Target Buah:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Row {
                            listOf("Mangga Arumanis", "Pisang Raja", "Apel Malang").forEach { name ->
                                SuggestionChip(
                                    onClick = {
                                        selectedFruitGrading = name
                                        // Randomize grading
                                        val grades = listOf("Grade A - Super Terpilih", "Grade B - Standar Segar", "Grade C - Diskon Cepat")
                                        val multi = listOf(1.20, 1.0, 0.70)
                                        val randIdx = (0..2).random()
                                        simulatedGrade = grades[randIdx]
                                        gradePriceMultiplier = multi[randIdx]
                                    },
                                    label = { Text(name, fontSize = 10.sp) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("🔍 Hasil Analisis Lens AI Freshness:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Deteksi Fisik: $selectedFruitGrading -> [$simulatedGrade]", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Implikasi Harga Otomatis: Multiplier harga x${gradePriceMultiplier} (Menghasilkan penyesuaian pasar instan)", fontSize = 11.sp, color = Color.Companion.DarkGray)
                        }
                    }
                }
            }

            // Innovation 2: Predictive Spoilage Auto-Markdown Engine
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("💡 INOVASI 2: Predictive Spoilage Auto-Markdown Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Meramal masa layu buah sebelum terjadi sisa pembusukan, mengusulkan program diskon bertahap agar pedagang tidak mengalami kerugian total (Zero-Waste Retail).", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))

                    var simulateHoursRemaining by remember { mutableStateOf(36) }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sisa Jam Estimasi Kesegaran:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = simulateHoursRemaining.toFloat(),
                            onValueChange = { simulateHoursRemaining = it.toInt() },
                            valueRange = 12f..120f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text("${simulateHoursRemaining} Jam", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val (discountOffer, descriptionOffer) = when {
                        simulateHoursRemaining <= 24 -> "Diskon 50% Obral Kilat" to "Obral sore hari untuk habiskan stok sebelum pembusukan mutlak."
                        simulateHoursRemaining <= 48 -> "Diskon 25% Clearance Promo" to "Tampilkan stiker 'Manis Siap Makan' dengan margin bersahabat."
                        else -> "Diskon 0% (Harga Normal Berjaya)" to "Buah dalam kondisi prima segar terbaik."
                    }

                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PriceCheck, contentDescription = "Saran", tint = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Hasil Simulasi Markdown: $discountOffer", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                Text(descriptionOffer, fontSize = 10.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }

            // Innovation 3: Dynamic Group-Buying / Es Buah Bundle Generator
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("💡 INOVASI 3: Smart 'Es Buah' Bundle Suggestion Wizard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Merekomendasikan paket bundle unik otomatis guna melepaskan sisa produk buah yang berlebih / menipis dalam satu kupon belanja.", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = "Es", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rekomendasi Paket Bundle Hari Ini:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("🍨 Paket Es Buah Campur Meriah", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Text("Isi Paket: 1.0 kg Alpukat Mentega + 1 Buah Pepaya California + 1 Buah Semangka Tanpa Biji", fontSize = 11.sp, color = Color.DarkGray)
                            Text("Harga Total Gabungan Normal: Rp 62,000", fontSize = 11.sp, color = Color.Gray)
                            Text("Harga Spesial Es Buah Bundle: Rp 52,000 (Potongan Instan Rp 10.000)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    // Instantly inject the 3 target products of the bundle into cashier cart
                                    val alpukat = products.find { it.name.contains("Alpukat") }
                                    val pepaya = products.find { it.name.contains("Pepaya") }
                                    val semangka = products.find { it.name.contains("Semangka") }

                                    alpukat?.let { viewModel.addToCart(it, 1.0) }
                                    pepaya?.let { viewModel.addToCart(it, 1.0) }
                                    semangka?.let { viewModel.addToCart(it, 1.0) }

                                    // Apply flat discounts
                                    viewModel.globalDiscountAmount = 10000.0
                                    viewModel.currentTab = MainViewModel.Tab.CASHIER
                                },
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Masukkan Bundle Masuk Struk Kasir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

        } else {
            // COMPLETE PM SPECIFICATION & ARCHITECTURE (PRD) INDONESIAN
            Text(
                "Product Requirement Document (PRD) & Desain Arsitektur",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. CORES FEATURES (FITUR UTAMA RETAIL)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "• Manajemen Stok Multi-Unit: Sistem kami menyimpan entri stok dengan dukungan penuh desimal untuk kg & ons (contoh: 12.25 kg mangga) serta unit bulat seperti ikat (pisang) atau buah (semangka).\n" +
                                "• Manajemen Produk Berbasis Batch (FIFO): Setiap pengiriman buah dicatat per batch dengan shelfLifeDays (masa simpan) masing-masing. Alur transaksi kasir akan memotong kuantitas batch tertua terlebih dahulu untuk memastikan kesegaran maksimum.\n" +
                                "• Timbang Otomatis Digital: Dashboard kasir terintegrasi via API timbangan elektronik Bluetooth/USB. Di dalam aplikasi ini, klik tombol 'Timbang' mendeteksi berat akurat dari sensor untuk dimasukkan langsung ke struk belanja.\n" +
                                "• Barcode Scanner Terintegrasi: Simulasi input laser barcode mendeteksi buah kemasan siap saji secara ekspres.\n" +
                                "• Diskon Dinamis & Loyalty: Perhitungan harga grosir otomatis saat kuantitas pesanan melampaui batas minimum eceran. Sistem membership mengumpulkan poin dan mengelompokkan pelanggan ke level Bronze, Silver, dan Gold untuk diskon otomatis.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "2. SISTEM OPERASIONAL (OFFLINE-FIRST DESIGN)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "• Pelaporan Real-Time Laba Rugi: Analisis keuangan menghitung sisa stok, omzet penjualan secara langsung, dikurangi Modal COGS (HPP) untuk menyajikan Laba Bersih riil.\n" +
                                "• Low-Stock Alerts otomatis menyala merah saat kuantitas buah menipis di bawah ambang batas aman produk.\n" +
                                "• Arsitektur Offline-First: Apabila jaringan internet terputus, transaksi tetap bisa diselesaikan secara penuh karena data tersimpan aman secara luring di SQLite via Room Database. Saat internet aktif, klik 'Sinkronisasi' untuk sinkronisasi batch terjadwal ke server utama.",
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "3. REKOMENDASI TEKNOLOGI & DATABASE SCHEMA",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "• Mobile Stack: Cocok dengan Kotlin Jetpack Compose untuk aplikasi Android Native berkepantasan tinggi, atau Flutter jika menginginkan rilis multi-platform.\n" +
                                "• Database Relasional Lokasi Kasir: Room Database dengan index relasi asing yang diperlihatkan di bawah:\n" +
                                "  - Table products (id PK, name, unit, basePrice, retailPrice, wholesalePrice, stock, minStockThreshold)\n" +
                                "  - Table batches (id PK, productId FK, batchNumber, currentQty, receivedDate, shelfLifeDays, decayRiskFactor)\n" +
                                "  - Table transactions (id PK, invoiceNumber, timestamp, totalAmount, paymentMethod, customerPhone, isSynced)\n" +
                                "  - Table customers (id PK, name, phone, points, tier)",
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// Dialog helper to register loyalty member
@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrasi Member Baru", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Lengkap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("No. HP / Kontak") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone) },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Daftarkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// Dialog helper for checkout payments
@Composable
fun PaymentMethodDialog(
    viewModel: MainViewModel,
    totalToPay: Double,
    formatIDR: (Double) -> String,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf("Tunai") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Pembayaran Multi-Payment",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.testTag("payment_modal_title")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Total Jual Akhir: ${formatIDR(totalToPay)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text("Pilih Kanal Pembayaran:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                // Method selector row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Tunai", "QRIS", "Transfer").forEach { method ->
                        val isSel = selectedMethod == method
                        FilledTonalButton(
                            onClick = {
                                selectedMethod = method
                                viewModel.selectedPaymentMethod = method
                            },
                            modifier = Modifier.weight(1f).height(40.dp).testTag("payment_btn_$method"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(method, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (selectedMethod) {
                    "Tunai" -> {
                        OutlinedTextField(
                            value = viewModel.cashAmountPaidText,
                            onValueChange = { viewModel.cashAmountPaidText = it },
                            label = { Text("Uang Tunai Diterima (Rp)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("cash_received_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Quick cash shortcuts
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(20000.0, 50000.0, 100000.0).forEach { cash ->
                                FilledTonalButton(
                                    onClick = { viewModel.cashAmountPaidText = cash.toInt().toString() },
                                    modifier = Modifier.weight(1f).height(32.dp).testTag("quick_cash_${cash.toInt()}"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(formatIDR(cash), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        val paidAmount = viewModel.cashAmountPaidText.toDoubleOrNull() ?: 0.0
                        val refund = (paidAmount - totalToPay).coerceAtLeast(0.0)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Uang Kembalian Pelanggan:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatIDR(refund),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (refund > 0.0) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.testTag("refund_amount_text")
                                )
                            }
                        }
                    }
                    "QRIS" -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color.LightGray),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Draw a simplistic clean visual QR mesh layout using icons
                                Icon(Icons.Default.QrCode2, contentDescription = "QRIS Code", modifier = Modifier.size(90.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("PIN KONFIRMASI QRIS DINAMIS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                                Text("Otomatis Terverifikasi Sistem Lunas", fontSize = 10.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "Transfer" -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Bank Penerima Transfer:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("💰 Bank Central Asia (BCA)", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                Text("No. Rekening: 8872-1029-338a a/n FreshProduce Utama", fontSize = 10.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Pastikan merchant menerima konfirmasi transfer m-banking di panel kasir.", fontSize = 9.sp, color = Color.Magenta, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onComplete,
                modifier = Modifier.testTag("submit_payment_modal")
            ) {
                Text("Selesaikan Transaksi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// Dialog helper to add product
@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var basePrice by remember { mutableStateOf("") }
    var retailPrice by remember { mutableStateOf("") }
    var wholesalePrice by remember { mutableStateOf("") }
    var wholesaleMinQty by remember { mutableStateOf("5") }
    var initialStock by remember { mutableStateOf("15") }
    var minStockThreshold by remember { mutableStateOf("5") }
    var barcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambahkan Buah Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Buah") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                // Unit selection segment
                Text("Satuan Unit:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("kg", "ons", "ikat", "buah").forEach { item ->
                        val isSel = unit == item
                        ElevatedFilterChip(
                            selected = isSel,
                            onClick = { unit = item },
                            label = { Text(item) }
                        )
                    }
                }

                OutlinedTextField(value = basePrice, onValueChange = { basePrice = it }, label = { Text("Harga Modal / Beli (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = retailPrice, onValueChange = { retailPrice = it }, label = { Text("Harga Jual Eceran (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = wholesalePrice, onValueChange = { wholesalePrice = it }, label = { Text("Harga Jual Grosir (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = wholesaleMinQty, onValueChange = { wholesaleMinQty = it }, label = { Text("Min Qty Syarat Grosir") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = initialStock, onValueChange = { initialStock = it }, label = { Text("Stok Awal") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minStockThreshold, onValueChange = { minStockThreshold = it }, label = { Text("Ambang Batas Minimum") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Nomor Barcode (Opsional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = Product(
                        name = name,
                        unit = unit,
                        basePrice = basePrice.toDoubleOrNull() ?: 10000.0,
                        retailPrice = retailPrice.toDoubleOrNull() ?: 15000.0,
                        wholesalePrice = wholesalePrice.toDoubleOrNull() ?: 13000.0,
                        wholesaleMinQty = wholesaleMinQty.toDoubleOrNull() ?: 5.0,
                        stock = initialStock.toDoubleOrNull() ?: 10.0,
                        minStockThreshold = minStockThreshold.toDoubleOrNull() ?: 3.0,
                        barcode = barcode.ifBlank { null },
                        colorHex = listOf("#E91E63", "#9C27B0", "#E1BEE7", "#FFF9C4", "#FF5722", "#FFC107").random()
                    )
                    onConfirm(p)
                },
                enabled = name.isNotBlank() && retailPrice.isNotBlank() && initialStock.isNotBlank()
            ) {
                Text("Simpan Buah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// Dialog helper to add batch
@Composable
fun AddBatchDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (productId: Int, qty: Double, shelfLifeDays: Int, origin: String, batchNo: String) -> Unit
) {
    if (products.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Data kosong") },
            text = { Text("Daftarkan produk buah terlebih dahulu sebelum menginput batch baru.") },
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedProductIdx by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf("15") }
    var shelfLife by remember { mutableStateOf("7") }
    var origin by remember { mutableStateOf("Kebun Probolinggo") }
    var batchNo by remember { mutableStateOf("BATCH-S-" + (System.currentTimeMillis() / 1000 % 10000)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambahkan Sirkulasi Batch Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Pilih Produk Buah:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                // Simple Dropdown spinner container using custom list of suggestions
                products.forEachIndexed { index, product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (selectedProductIdx == index) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedProductIdx = index }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedProductIdx == index, onClick = { selectedProductIdx = index })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(product.name + " (${product.unit})", fontSize = 13.sp)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Kuantitas Batch Masuk") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = shelfLife, onValueChange = { shelfLife = it }, label = { Text("Masa Simpan Layu (Hari)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = origin, onValueChange = { origin = it }, label = { Text("Asal Daerah Kebun") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = batchNo, onValueChange = { batchNo = it }, label = { Text("Nomor Kode Batch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        products[selectedProductIdx].id,
                        qty.toDoubleOrNull() ?: 15.0,
                        shelfLife.toIntOrNull() ?: 7,
                        origin,
                        batchNo
                    )
                },
                enabled = qty.isNotBlank() && shelfLife.isNotBlank()
            ) {
                Text("Input Batch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// Custom parser to map categorical icons easily without imports
@Composable
fun imageIcons(key: String): ImageVector {
    return when (key) {
        "Fruit" -> Icons.Default.Eco
        else -> Icons.Default.Add
    }
}


