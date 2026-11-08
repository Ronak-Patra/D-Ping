package com.dping.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.network.discovery.LanDevice
import com.dping.app.network.discovery.LanDeviceFilter
import com.dping.app.network.discovery.LanDeviceType
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.LanDiscoveryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanDiscoveryScreen(
    viewModel: LanDiscoveryViewModel,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var selectedDevice by remember { mutableStateOf<LanDevice?>(null) }

    // Pre-extract Composable theme colors
    val cyanColor = NeonCyan
    val tealColor = NeonTeal
    val textPrimary = TextPrimaryDark
    val textSecondary = TextSecondaryDark
    val surfaceColor = DarkSurface
    val surfaceVariant = DarkSurfaceVariant
    val borderColor = DarkBorder

    val onlineCount = remember(state.devices) { state.devices.count { it.isOnline } }
    val offlineCount = remember(state.devices) { state.devices.count { !it.isOnline } }

    val filteredDevices = remember(state.devices, state.filter, state.searchQuery) {
        val byFilter = when (state.filter) {
            LanDeviceFilter.ALL -> state.devices
            LanDeviceFilter.ONLINE -> state.devices.filter { it.isOnline }
            LanDeviceFilter.OFFLINE -> state.devices.filter { !it.isOnline }
            LanDeviceFilter.GATEWAYS -> state.devices.filter { it.isGateway || it.deviceType == LanDeviceType.ROUTER }
            LanDeviceFilter.COMPUTERS -> state.devices.filter {
                it.deviceType == LanDeviceType.COMPUTER || it.deviceType == LanDeviceType.NAS_STORAGE
            }
            LanDeviceFilter.PHONES -> state.devices.filter { it.deviceType == LanDeviceType.PHONE }
            LanDeviceFilter.PRINTERS -> state.devices.filter { it.deviceType == LanDeviceType.PRINTER }
            LanDeviceFilter.IOT -> state.devices.filter { it.deviceType == LanDeviceType.SMART_TV_IOT }
        }

        if (state.searchQuery.isBlank()) {
            byFilter
        } else {
            val q = state.searchQuery.trim().lowercase()
            byFilter.filter {
                it.ip.contains(q) ||
                it.hostname.lowercase().contains(q) ||
                (it.macAddress?.lowercase()?.contains(q) == true) ||
                (it.vendor?.lowercase()?.contains(q) == true)
            }
        }
    }

    val subnetPrefix = remember(state.networkInfo.localIp) {
        val parts = state.networkInfo.localIp.split(".")
        if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}.0/24" else "192.168.1.0/24"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.lanDiscoveryTitle,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (state.isRangeMode) "${state.startIp} - ${state.endIp} • Custom Range" else "$subnetPrefix • ${state.networkInfo.ssid}",
                            color = cyanColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Home",
                            tint = textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Mode Segmented Switch: [ Local LAN Scan ] vs [ Custom IP Range ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(surfaceColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!state.isRangeMode) cyanColor else Color.Transparent)
                        .clickable { viewModel.setScanMode(false) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Local LAN Scan",
                        color = if (!state.isRangeMode) DarkBackground else textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (state.isRangeMode) cyanColor else Color.Transparent)
                        .clickable { viewModel.setScanMode(true) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom IP Range",
                        color = if (state.isRangeMode) DarkBackground else textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Scan Mode Controls Section
            if (state.isRangeMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextField(
                        value = state.startIp,
                        onValueChange = { viewModel.onStartIpChange(it) },
                        label = { Text("Start IP", fontSize = 11.sp, color = textSecondary) },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    TextField(
                        value = state.endIp,
                        onValueChange = { viewModel.onEndIpChange(it) },
                        label = { Text("End IP", fontSize = 11.sp, color = textSecondary) },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Quick range presets and action button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val parts = state.networkInfo.localIp.split(".")
                        val base = if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}" else "192.168.1"
                        listOf(
                            "1 - 50" to ("$base.1" to "$base.50"),
                            "1 - 100" to ("$base.1" to "$base.100"),
                            "1 - 254" to ("$base.1" to "$base.254")
                        ).forEach { (label, range) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = surfaceVariant,
                                border = BorderStroke(1.dp, borderColor),
                                modifier = Modifier.clickable {
                                    viewModel.onStartIpChange(range.first)
                                    viewModel.onEndIpChange(range.second)
                                }
                            ) {
                                Text(
                                    text = label,
                                    color = cyanColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (state.isScanning) viewModel.stopScan() else viewModel.startScan()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isScanning) Color(0xFFF59E0B) else cyanColor
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (state.isScanning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (state.isScanning) "Stop" else "Scan Range",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${state.networkInfo.ssid} • Subnet $subnetPrefix",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Gateway: ${state.networkInfo.gatewayIp} • My IP: ${state.networkInfo.localIp}",
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (state.isScanning) viewModel.stopScan() else viewModel.startScan()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isScanning) Color(0xFFF59E0B) else cyanColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                if (state.isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (state.isScanning) "Stop" else "Rescan LAN",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 1. Three Metric Counters Row (Devices | Online | Offline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Total Devices
                MetricCounterCard(
                    title = "Devices",
                    count = state.devices.size.toString(),
                    indicatorColor = cyanColor,
                    modifier = Modifier.weight(1f)
                )

                // Card 2: Online Devices
                MetricCounterCard(
                    title = "Online",
                    count = onlineCount.toString(),
                    indicatorColor = Color(0xFF10B981), // Emerald Green
                    modifier = Modifier.weight(1f)
                )

                // Card 3: Offline Devices
                MetricCounterCard(
                    title = "Offline",
                    count = offlineCount.toString(),
                    indicatorColor = Color(0xFF64748B), // Muted Slate
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // 2. Search Input Field
            TextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                placeholder = {
                    Text(
                        text = "Search by hostname, IP, MAC or vendor...",
                        color = textSecondary,
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = cyanColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchChange("") }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(10.dp))

            // 3. Category Filter Chips (Screen 6 horizontal pills)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    LanDeviceFilter.ALL to "All (${state.devices.size})",
                    LanDeviceFilter.ONLINE to "Online ($onlineCount)",
                    LanDeviceFilter.OFFLINE to "Offline ($offlineCount)",
                    LanDeviceFilter.GATEWAYS to "Routers",
                    LanDeviceFilter.COMPUTERS to "Computers / NAS",
                    LanDeviceFilter.PHONES to "Phones",
                    LanDeviceFilter.PRINTERS to "Printers",
                    LanDeviceFilter.IOT to "Smart / TV"
                )

                items(filters) { (filter, label) ->
                    val isSelected = state.filter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) cyanColor else surfaceColor)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) cyanColor else borderColor,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.onFilterChange(filter) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBackground else textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Active Scanning Progress Bar
            if (state.isScanning) {
                Spacer(Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.statusMessage,
                            color = textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${state.scannedCount}/${state.totalCount}",
                            color = cyanColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val progress = if (state.totalCount > 0) state.scannedCount.toFloat() / state.totalCount else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF132030))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(listOf(cyanColor, tealColor))
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 4. Categorized Device Cards List
            if (filteredDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DevicesOther,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (state.isScanning) "Discovering network devices..." else "No devices matching filter",
                            color = textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(filteredDevices, key = { it.ip }) { device ->
                        LanDeviceCard(
                            device = device,
                            onClick = { selectedDevice = device }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 5. Bottom Action Button: Scan Network / Stop Scan
            Button(
                onClick = {
                    if (state.isScanning) {
                        viewModel.stopScan()
                    } else {
                        viewModel.startScan()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isScanning) Color(0xFFE53935) else cyanColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (state.isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                        contentDescription = null,
                        tint = if (state.isScanning) Color.White else DarkBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.isScanning) "Stop Discovery" else "Scan Network",
                        color = if (state.isScanning) Color.White else DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Device Details Dialog
    selectedDevice?.let { dev ->
        AlertDialog(
            onDismissRequest = { selectedDevice = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getDeviceIcon(dev.deviceType),
                        contentDescription = null,
                        tint = cyanColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = dev.hostname.ifBlank { dev.ip },
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("IP Address", dev.ip)
                    DetailRow("Device Type", when(dev.deviceType) {
                        LanDeviceType.ROUTER -> "Router / Gateway"
                        LanDeviceType.COMPUTER -> "Computer / PC"
                        LanDeviceType.PHONE -> "Smartphone / Mobile"
                        LanDeviceType.PRINTER -> "Network Printer"
                        LanDeviceType.SMART_TV_IOT -> "Smart TV / IoT Node"
                        LanDeviceType.NAS_STORAGE -> "NAS Storage Server"
                        LanDeviceType.GENERIC -> "Generic Network Node"
                    })
                    if (!dev.macAddress.isNullOrBlank()) {
                        DetailRow("MAC Address", dev.macAddress)
                    }
                    if (!dev.vendor.isNullOrBlank()) {
                        DetailRow("Vendor", dev.vendor)
                    }
                    DetailRow("Status", if (dev.isOnline) "Online" else "Offline")
                    if (dev.isOnline && dev.latencyMs > 0) {
                        DetailRow("Latency", "${String.format(java.util.Locale.US, "%.1f", dev.latencyMs)} ms")
                    }
                    if (dev.openServices.isNotEmpty()) {
                        DetailRow("Open Services", dev.openServices.joinToString(", "))
                    }
                    if (dev.isGateway) {
                        DetailRow("Role", "Default Gateway / Router")
                    } else if (dev.isCurrentDevice) {
                        DetailRow("Role", "This Device")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = dev.ip
                        selectedDevice = null
                        onNavigateToPing(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cyanColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ping Device", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("IP", dev.ip))
                        Toast.makeText(context, "Copied IP: ${dev.ip}", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy IP", color = cyanColor)
                    }
                    if (!dev.macAddress.isNullOrBlank()) {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MAC", dev.macAddress))
                            Toast.makeText(context, "Copied MAC: ${dev.macAddress}", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Copy MAC", color = cyanColor)
                        }
                    }
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun MetricCounterCard(
    title: String,
    count: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = count,
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun LanDeviceCard(
    device: LanDevice,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with tinted container
            val (iconTint, bgTint) = getDeviceColors(device.deviceType, device.isGateway)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.deviceType),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = device.hostname.ifBlank { device.ip },
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    val typeBadge = when {
                        device.isGateway -> "ROUTER" to NeonCyan
                        device.isCurrentDevice -> "THIS DEVICE" to NeonTeal
                        device.deviceType == LanDeviceType.ROUTER -> "ROUTER" to NeonCyan
                        device.deviceType == LanDeviceType.COMPUTER -> "COMPUTER" to Color(0xFF79C0FF)
                        device.deviceType == LanDeviceType.PHONE -> "PHONE" to NeonTeal
                        device.deviceType == LanDeviceType.PRINTER -> "PRINTER" to Color(0xFFF59E0B)
                        device.deviceType == LanDeviceType.SMART_TV_IOT -> "SMART / IOT" to Color(0xFFD2A8FF)
                        device.deviceType == LanDeviceType.NAS_STORAGE -> "NAS" to Color(0xFF10B981)
                        else -> null
                    }
                    if (typeBadge != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(typeBadge.second.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = typeBadge.first,
                                color = typeBadge.second,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.ip,
                        color = NeonTeal,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    if (!device.macAddress.isNullOrBlank()) {
                        Text(
                            text = " • ${device.macAddress}",
                            color = TextSecondaryDark,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!device.vendor.isNullOrBlank() || device.openServices.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!device.vendor.isNullOrBlank()) {
                            Text(
                                text = device.vendor,
                                color = Color(0xFFD2A8FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (device.openServices.isNotEmpty()) {
                            Text(
                                text = " [${device.openServices.joinToString(", ")}]",
                                color = Color(0xFF79C0FF),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Trailing Latency / Status Badge
            Column(horizontalAlignment = Alignment.End) {
                if (device.isOnline) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", device.latencyMs)} ms",
                            color = if (device.latencyMs > 100) WarningOrange else NeonTeal,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF202632))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Offline",
                            color = TextSecondaryDark,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondaryDark,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = TextPrimaryDark,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

private fun getDeviceIcon(type: LanDeviceType): ImageVector = when (type) {
    LanDeviceType.ROUTER -> Icons.Default.Router
    LanDeviceType.PHONE -> Icons.Default.Smartphone
    LanDeviceType.COMPUTER -> Icons.Default.Laptop
    LanDeviceType.NAS_STORAGE -> Icons.Default.Storage
    LanDeviceType.PRINTER -> Icons.Default.Print
    LanDeviceType.SMART_TV_IOT -> Icons.Default.Tv
    LanDeviceType.GENERIC -> Icons.Default.Devices
}

private fun getDeviceColors(type: LanDeviceType, isGateway: Boolean): Pair<Color, Color> {
    if (isGateway) return Color(0xFF00E5FF) to Color(0xFF00E5FF).copy(alpha = 0.15f)
    return when (type) {
        LanDeviceType.ROUTER -> Color(0xFF00E5FF) to Color(0xFF00E5FF).copy(alpha = 0.15f)
        LanDeviceType.PHONE -> Color(0xFF00C2FF) to Color(0xFF00C2FF).copy(alpha = 0.15f)
        LanDeviceType.COMPUTER -> Color(0xFF79C0FF) to Color(0xFF1E3A5F)
        LanDeviceType.NAS_STORAGE -> Color(0xFF10B981) to Color(0xFF10B981).copy(alpha = 0.15f)
        LanDeviceType.PRINTER -> Color(0xFFF59E0B) to Color(0xFFF59E0B).copy(alpha = 0.15f)
        LanDeviceType.SMART_TV_IOT -> Color(0xFFD2A8FF) to Color(0xFF3B2D54)
        LanDeviceType.GENERIC -> Color(0xFF8B9EB5) to Color(0xFF14202E)
    }
}
