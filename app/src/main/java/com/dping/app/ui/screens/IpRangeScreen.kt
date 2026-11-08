package com.dping.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.network.discovery.LanDeviceType
import com.dping.app.network.iprange.IpOccupancyStatus
import com.dping.app.network.iprange.IpScanItem
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.IpFilterOption
import com.dping.app.ui.viewmodels.IpRangeViewModel
import com.dping.app.utils.ExportManager
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpRangeScreen(
    viewModel: IpRangeViewModel,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToPing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var selectedItem by remember { mutableStateOf<IpScanItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var isConfigExpanded by remember { mutableStateOf(state.items.isEmpty()) }

    // Pre-extract theme colors in Composable scope
    val cyanColor = NeonCyan
    val tealColor = NeonTeal
    val textPrimary = TextPrimaryDark
    val textSecondary = TextSecondaryDark
    val textMuted = TextMutedDark
    val surfaceColor = DarkSurface
    val surfaceVariant = DarkSurfaceVariant
    val borderColor = DarkBorder

    val filteredItems = remember(state.items, state.filter, searchQuery) {
        val base = when (state.filter) {
            IpFilterOption.ALL -> state.items
            IpFilterOption.OCCUPIED -> state.items.filter { it.status == IpOccupancyStatus.OCCUPIED }
            IpFilterOption.FREE -> state.items.filter { it.status == IpOccupancyStatus.FREE }
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            val q = searchQuery.trim().lowercase()
            base.filter {
                it.ip.contains(q) ||
                it.hostname.lowercase().contains(q) ||
                (it.vendor?.lowercase()?.contains(q) == true) ||
                (it.macAddress?.lowercase()?.contains(q) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.ipRangeTitle,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${state.cidr} • CIDR Subnet Inspection",
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
            val cidrParts = state.cidr.trim().split("/")
            val baseIp = if (cidrParts.isNotEmpty()) cidrParts[0] else "192.168.1.0"
            val currentPrefix = cidrParts.getOrNull(1)?.toIntOrNull() ?: 24
            val totalHosts = if (currentPrefix == 0) 4294967296L else (1L shl (32 - currentPrefix))
            val usableHosts = when (currentPrefix) {
                32 -> 1L
                31 -> 2L
                else -> (totalHosts - 2).coerceAtLeast(0)
            }

            // Compact Subnet CIDR Config Card (Collapsible for max list view)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Header Row: Network Info Summary / Input + Quick Scan Button + Expand Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isConfigExpanded = !isConfigExpanded }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = cyanColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "$baseIp/$currentPrefix",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = cyanColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "$usableHosts hosts",
                                    color = cyanColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Primary Scan Subnet / Stop Button
                            Button(
                                onClick = {
                                    if (state.isScanning) viewModel.stopScan() else viewModel.startScan()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isScanning) Color(0xFFF59E0B) else cyanColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isScanning) Icons.Default.Stop else Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = DarkBackground,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (state.isScanning) strings.btnStop else strings.scanSubnet,
                                    color = DarkBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // Expand / Collapse Chevron
                            IconButton(
                                onClick = { isConfigExpanded = !isConfigExpanded },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isConfigExpanded) "Collapse" else "Expand",
                                    tint = cyanColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Expanded Controls (Network Input, Slider, Presets)
                    AnimatedVisibility(visible = isConfigExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Network Address input & Detect button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextField(
                                    value = baseIp,
                                    onValueChange = { viewModel.onBaseIpChange(it) },
                                    label = { Text(strings.networkAddress, fontSize = 11.sp, color = textSecondary) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = surfaceVariant,
                                        unfocusedContainerColor = surfaceVariant,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Button(
                                    onClick = { viewModel.detectSubnetRange() },
                                    colors = ButtonDefaults.buttonColors(containerColor = surfaceVariant),
                                    border = BorderStroke(1.dp, cyanColor.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = cyanColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = strings.detect,
                                        color = cyanColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Row 2: Slider with live badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CIDR Prefix",
                                    color = textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "/$currentPrefix • ${state.startIp} - ${state.endIp}",
                                    color = cyanColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Slider(
                                value = currentPrefix.toFloat(),
                                onValueChange = { viewModel.onPrefixChange(it.roundToInt()) },
                                valueRange = 1f..32f,
                                steps = 30,
                                colors = SliderDefaults.colors(
                                    thumbColor = cyanColor,
                                    activeTrackColor = cyanColor,
                                    inactiveTrackColor = surfaceVariant,
                                    activeTickColor = Color.Transparent,
                                    inactiveTickColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                            )

                            // Row 3: Preset chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(24, 26, 27, 28, 30).forEach { prefixOption ->
                                    val isSelected = currentPrefix == prefixOption
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) cyanColor.copy(alpha = 0.25f) else surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) cyanColor else Color.White.copy(alpha = 0.08f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.onPrefixChange(prefixOption) }
                                    ) {
                                        Text(
                                            text = "/$prefixOption",
                                            color = if (isSelected) cyanColor else textSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 3. Three Metric Telemetry Cards (Occupied | Available | Total Scanned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCounterCard(
                    title = strings.occupied,
                    count = state.occupiedCount.toString(),
                    indicatorColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )

                MetricCounterCard(
                    title = strings.available,
                    count = state.freeCount.toString(),
                    indicatorColor = Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                )

                MetricCounterCard(
                    title = strings.scannedLabel,
                    count = if (state.totalCount > 0) "${state.scannedCount}/${state.totalCount}" else "${state.items.size}",
                    indicatorColor = cyanColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Scanning Progress Bar
            if (state.isScanning) {
                Spacer(Modifier.height(8.dp))
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
                        val pct = if (state.totalCount > 0) ((state.scannedCount.toFloat() / state.totalCount) * 100).toInt() else 0
                        Text(
                            text = "$pct%",
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
                                .background(Brush.horizontalGradient(listOf(cyanColor, tealColor)))
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 4. Search & Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
                    placeholder = { Text("Filter by IP, hostname, MAC...", color = textSecondary, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = cyanColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
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
                    shape = RoundedCornerShape(10.dp)
                )

                // Export Button
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(surfaceColor)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Results",
                        tint = cyanColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    IpFilterOption.ALL to "${strings.filterAll} (${state.items.size})",
                    IpFilterOption.OCCUPIED to "${strings.filterOccupied} (${state.occupiedCount})",
                    IpFilterOption.FREE to "${strings.filterFree} (${state.freeCount})"
                )

                filters.forEach { (filter, label) ->
                    val isSelected = state.filter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) cyanColor else surfaceColor)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) cyanColor else borderColor,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { viewModel.onFilterChange(filter) }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DarkBackground else textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 5. Scanned IP Items List (Full height available, no bottom button obstructing)
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (state.isScanning) "Scanning subnet in parallel..." else "No addresses matching filter",
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
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredItems, key = { it.ip }) { item ->
                        IpScanItemCard(
                            item = item,
                            onClick = { selectedItem = item }
                        )
                    }
                }
            }
        }
    }

    // IP Item Details Dialog
    selectedItem?.let { item ->
        val isOccupied = item.status == IpOccupancyStatus.OCCUPIED
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOccupied) Color(0xFF10B981) else Color(0xFF64748B))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = item.ip,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("Occupancy Status", if (isOccupied) "OCCUPIED" else "FREE (Available)")
                    DetailRow("Hostname", item.hostname.ifBlank { "Unassigned" })
                    if (isOccupied) {
                        DetailRow("Device Type", when(item.deviceType) {
                            LanDeviceType.ROUTER -> "Router / Gateway"
                            LanDeviceType.COMPUTER -> "Computer / PC"
                            LanDeviceType.PHONE -> "Smartphone / Mobile"
                            LanDeviceType.PRINTER -> "Network Printer"
                            LanDeviceType.SMART_TV_IOT -> "Smart TV / IoT Node"
                            LanDeviceType.NAS_STORAGE -> "NAS Storage Server"
                            LanDeviceType.GENERIC -> "Network Device"
                        })
                        if (!item.macAddress.isNullOrBlank()) {
                            DetailRow("MAC Address", item.macAddress)
                        }
                        if (!item.vendor.isNullOrBlank()) {
                            DetailRow("Hardware Vendor", item.vendor)
                        }
                        if (item.latencyMs > 0) {
                            DetailRow("Response Latency", "${String.format(java.util.Locale.US, "%.1f", item.latencyMs)} ms")
                        }
                        if (item.openPort != null) {
                            DetailRow("Active Port", item.openPort.toString())
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = item.ip
                        selectedItem = null
                        onNavigateToPing(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = cyanColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ping IP", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("IP", item.ip))
                    Toast.makeText(context, "Copied IP: ${item.ip}", Toast.LENGTH_SHORT).show()
                    selectedItem = null
                }) {
                    Text("Copy IP", color = cyanColor)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Export Format Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text("Export IP Scan Results", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose export format for ${state.items.size} addresses:", color = textSecondary, fontSize = 13.sp)

                    // CSV Option
                    Button(
                        onClick = {
                            val sb = StringBuilder("IP,Status,Hostname,MAC,Vendor,Latency(ms)\n")
                            state.items.forEach {
                                sb.append("${it.ip},${it.status},\"${it.hostname}\",\"${it.macAddress ?: ""}\",\"${it.vendor ?: ""}\",${it.latencyMs}\n")
                            }
                            ExportManager.shareText(context, "IP Scan Results (CSV)", sb.toString())
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Export as CSV", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    // JSON Option
                    Button(
                        onClick = {
                            val json = buildString {
                                append("[\n")
                                state.items.forEachIndexed { idx, it ->
                                    append("  {\"ip\": \"${it.ip}\", \"status\": \"${it.status}\", \"hostname\": \"${it.hostname}\", \"mac\": \"${it.macAddress ?: ""}\", \"vendor\": \"${it.vendor ?: ""}\", \"latencyMs\": ${it.latencyMs}}")
                                    if (idx < state.items.size - 1) append(",")
                                    append("\n")
                                }
                                append("]")
                            }
                            ExportManager.shareText(context, "IP Scan Results (JSON)", json)
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Export as JSON", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    // Plain Text Option
                    Button(
                        onClick = {
                            val txt = buildString {
                                append("D Ping IP Range Scan Results\n")
                                append("Range: ${state.startIp} - ${state.endIp}\n")
                                append("Occupied: ${state.occupiedCount} | Free: ${state.freeCount}\n\n")
                                state.items.forEach {
                                    val stat = if (it.status == IpOccupancyStatus.OCCUPIED) "[OCCUPIED]" else "[FREE]"
                                    append("$stat ${it.ip} - ${it.hostname}\n")
                                }
                            }
                            ExportManager.shareText(context, "IP Scan Results (TXT)", txt)
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Export as Plain Text", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = textSecondary)
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
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
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
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = count,
                color = TextPrimaryDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IpScanItemCard(
    item: IpScanItem,
    onClick: () -> Unit
) {
    val isOccupied = item.status == IpOccupancyStatus.OCCUPIED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isOccupied) NeonCyan.copy(alpha = 0.4f) else DarkBorder,
                RoundedCornerShape(12.dp)
            )
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
            // Icon
            val (iconTint, bgTint) = if (isOccupied) getDeviceColors(item.deviceType) else TextMutedDark to DarkSurfaceVariant
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isOccupied) getDeviceIcon(item.deviceType) else Icons.Default.Circle,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(if (isOccupied) 20.dp else 10.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOccupied) item.hostname.ifBlank { item.ip } else "Available IP",
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isOccupied) {
                        val typeBadge = when (item.deviceType) {
                            LanDeviceType.ROUTER -> "ROUTER" to NeonCyan
                            LanDeviceType.COMPUTER -> "COMPUTER" to Color(0xFF79C0FF)
                            LanDeviceType.PHONE -> "PHONE" to NeonTeal
                            LanDeviceType.PRINTER -> "PRINTER" to Color(0xFFF59E0B)
                            LanDeviceType.SMART_TV_IOT -> "SMART / IOT" to Color(0xFFD2A8FF)
                            LanDeviceType.NAS_STORAGE -> "NAS" to Color(0xFF10B981)
                            LanDeviceType.GENERIC -> null
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
                }

                Spacer(Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.ip,
                        color = if (isOccupied) NeonTeal else TextSecondaryDark,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    if (isOccupied && !item.macAddress.isNullOrBlank()) {
                        Text(
                            text = " • ${item.macAddress}",
                            color = TextSecondaryDark,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isOccupied && (!item.vendor.isNullOrBlank() || item.openPort != null)) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!item.vendor.isNullOrBlank()) {
                            Text(
                                text = item.vendor,
                                color = Color(0xFFD2A8FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (item.openPort != null) {
                            Text(
                                text = " [Port ${item.openPort}]",
                                color = Color(0xFF79C0FF),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isOccupied) Color(0xFF10B981).copy(alpha = 0.15f) else DarkSurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isOccupied) "OCCUPIED" else "FREE",
                    color = if (isOccupied) Color(0xFF10B981) else TextMutedDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    softWrap = false,
                    maxLines = 1
                )
            }
        }
    }
}

private fun getDeviceIcon(type: LanDeviceType) = when (type) {
    LanDeviceType.ROUTER -> Icons.Default.Router
    LanDeviceType.COMPUTER -> Icons.Default.Computer
    LanDeviceType.PHONE -> Icons.Default.Smartphone
    LanDeviceType.PRINTER -> Icons.Default.Print
    LanDeviceType.SMART_TV_IOT -> Icons.Default.Tv
    LanDeviceType.NAS_STORAGE -> Icons.Default.Storage
    LanDeviceType.GENERIC -> Icons.Default.Lan
}

private fun getDeviceColors(type: LanDeviceType): Pair<Color, Color> = when (type) {
    LanDeviceType.ROUTER -> Color(0xFF00E5FF) to Color(0xFF00E5FF).copy(alpha = 0.15f)
    LanDeviceType.PHONE -> Color(0xFF00C2FF) to Color(0xFF00C2FF).copy(alpha = 0.15f)
    LanDeviceType.COMPUTER -> Color(0xFF79C0FF) to Color(0xFF1E3A5F)
    LanDeviceType.PRINTER -> Color(0xFFF59E0B) to Color(0xFFF59E0B).copy(alpha = 0.15f)
    LanDeviceType.SMART_TV_IOT -> Color(0xFFD2A8FF) to Color(0xFF3B2D54)
    LanDeviceType.NAS_STORAGE -> Color(0xFF10B981) to Color(0xFF10B981).copy(alpha = 0.15f)
    LanDeviceType.GENERIC -> Color(0xFF8B9EB5) to Color(0xFF14202E)
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondaryDark, fontSize = 12.sp)
        Text(text = value, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}
