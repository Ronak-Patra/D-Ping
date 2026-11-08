package com.dping.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.network.discovery.InterfaceDetails
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.ToolsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkInterfacesScreen(
    viewModel: ToolsViewModel,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Pre-extract theme colors in Composable scope
    val cyanColor = NeonCyan
    val tealColor = NeonTeal
    val textPrimary = TextPrimaryDark
    val textSecondary = TextSecondaryDark
    val textMuted = TextMutedDark
    val surfaceColor = DarkSurface
    val surfaceVariant = DarkSurfaceVariant
    val borderColor = DarkBorder

    LaunchedEffect(Unit) {
        viewModel.refreshInterfaces()
    }

    val connectedCount = remember(state.interfaces) {
        state.interfaces.count { iface ->
            iface.isUp && iface.ipv4Addresses.any {
                it != "127.0.0.1" && !it.startsWith("169.254.") && !it.startsWith("0.")
            }
        }
    }
    val vpnCount = remember(state.interfaces) { state.interfaces.count { it.isVpn } }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredInterfaces = remember(state.interfaces, selectedFilter) {
        when (selectedFilter) {
            "CONNECTED" -> state.interfaces.filter { iface ->
                iface.isUp && iface.ipv4Addresses.any {
                    it != "127.0.0.1" && !it.startsWith("169.254.") && !it.startsWith("0.")
                }
            }
            "PHYSICAL" -> state.interfaces.filter { iface ->
                val n = iface.name.lowercase()
                n.startsWith("wlan") || n.startsWith("wifi") || n.startsWith("eth") ||
                n.startsWith("rmnet") || n.startsWith("ccmni") || n.startsWith("pdp") ||
                n.startsWith("rndis") || n.startsWith("usb") || n.startsWith("p2p") ||
                n.startsWith("bt") || n.startsWith("bnep")
            }
            "SYSTEM" -> state.interfaces.filter { iface ->
                val n = iface.name.lowercase()
                iface.isLoopback || n == "lo" || n.startsWith("dummy") ||
                n.startsWith("sit") || n.startsWith("ip6tnl") || n.startsWith("vnet")
            }
            else -> state.interfaces
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Network Interfaces",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (connectedCount > 0) "$connectedCount Connected • ${state.interfaces.size} Total Adapters" else "${state.interfaces.size} Total Adapters",
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
            // 1. Overview Telemetry Cards (Screen 12 format)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InterfaceMetricCard(
                    title = "Connected",
                    count = connectedCount.toString(),
                    indicatorColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )

                InterfaceMetricCard(
                    title = "Adapters",
                    count = state.interfaces.size.toString(),
                    indicatorColor = cyanColor,
                    modifier = Modifier.weight(1f)
                )

                InterfaceMetricCard(
                    title = "VPN / Tun",
                    count = vpnCount.toString(),
                    indicatorColor = Color(0xFFD2A8FF),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Category Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All (${state.interfaces.size})",
                    "CONNECTED" to "Connected ($connectedCount)",
                    "PHYSICAL" to "Physical / Wi-Fi",
                    "SYSTEM" to "System / Virtual"
                )

                filters.forEach { (filterKey, label) ->
                    val isSelected = selectedFilter == filterKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) cyanColor else surfaceColor)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) cyanColor else borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilter = filterKey }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
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

            Spacer(Modifier.height(12.dp))

            // 2. Interfaces List
            if (filteredInterfaces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SettingsEthernet,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (state.interfaces.isEmpty()) "Reading system network interfaces..." else "No interfaces matching filter",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredInterfaces, key = { it.name }) { iface ->
                        NetworkInterfaceCard(
                            iface = iface,
                            onCopy = { label, valStr ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(label, valStr))
                                Toast.makeText(context, "Copied $label: $valStr", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 3. Refresh Action Button
            Button(
                onClick = { viewModel.refreshInterfaces() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cyanColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = DarkBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Refresh Network Interfaces",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InterfaceMetricCard(
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun NetworkInterfaceCard(
    iface: InterfaceDetails,
    onCopy: (String, String) -> Unit
) {
    val isUp = iface.isUp
    val isConnected = isUp && iface.ipv4Addresses.any {
        it != "127.0.0.1" && !it.startsWith("169.254.") && !it.startsWith("0.")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isConnected) 1.5.dp else 1.dp,
                color = if (isConnected) NeonCyan.copy(alpha = 0.7f) else if (isUp) NeonTeal.copy(alpha = 0.35f) else DarkBorder,
                shape = RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Adapter name, role, badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isConnected) NeonCyan.copy(alpha = 0.2f)
                                else if (isUp) NeonTeal.copy(alpha = 0.15f)
                                else DarkSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when {
                            iface.name.startsWith("wlan") || iface.name.startsWith("wifi") -> Icons.Default.Wifi
                            iface.name.startsWith("eth") || iface.name.startsWith("en") -> Icons.Default.SettingsEthernet
                            iface.name.startsWith("rmnet") || iface.name.startsWith("ccmni") || iface.name.startsWith("pdp") -> Icons.Default.SignalCellularAlt
                            iface.isVpn || iface.name.startsWith("tun") || iface.name.startsWith("ppp") || iface.name.startsWith("wg") -> Icons.Default.VpnKey
                            iface.isLoopback || iface.name == "lo" -> Icons.Default.Loop
                            iface.name.startsWith("rndis") || iface.name.startsWith("usb") -> Icons.Default.Usb
                            iface.name.startsWith("bt") || iface.name.startsWith("bnep") -> Icons.Default.Bluetooth
                            iface.name.startsWith("p2p") -> Icons.Default.Devices
                            iface.name.startsWith("dummy") || iface.name.startsWith("sit") || iface.name.startsWith("ip6tnl") -> Icons.Default.Layers
                            else -> Icons.Default.SettingsEthernet
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isConnected) NeonCyan else if (isUp) NeonTeal else TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            text = iface.name,
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                        val role = when {
                            iface.name.startsWith("wlan") || iface.name.startsWith("wifi") -> "Wi-Fi Adapter"
                            iface.name.startsWith("eth") || iface.name.startsWith("en") -> "Ethernet Adapter"
                            iface.name.startsWith("rmnet") || iface.name.startsWith("ccmni") || iface.name.startsWith("pdp") -> "Cellular Modem"
                            iface.isVpn || iface.name.startsWith("tun") || iface.name.startsWith("ppp") || iface.name.startsWith("wg") -> "VPN Tunnel"
                            iface.isLoopback || iface.name == "lo" -> "Local Loopback"
                            iface.name.startsWith("p2p") -> "Wi-Fi Direct (P2P)"
                            iface.name.startsWith("dummy") -> "Virtual Dummy Interface"
                            iface.name.startsWith("sit") || iface.name.startsWith("ip6tnl") -> "IPv6 Tunnel Interface"
                            iface.name.startsWith("vnet") || iface.name.startsWith("virbr") || iface.name.startsWith("bridge") -> "Virtual Bridge"
                            iface.name.startsWith("rndis") || iface.name.startsWith("usb") -> "USB Tethering"
                            iface.name.startsWith("bt") || iface.name.startsWith("bnep") -> "Bluetooth Tethering"
                            else -> "Network Interface"
                        }
                        Text(
                            text = role,
                            color = if (isConnected) NeonTeal else TextSecondaryDark,
                            fontSize = 10.sp,
                            fontWeight = if (isConnected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                // Status Badges & MTU
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isConnected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.18f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    if (iface.mtu > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MTU ${iface.mtu}",
                                color = TextSecondaryDark,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isUp) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF64748B).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isUp) "UP" else "DOWN",
                            color = if (isUp) Color(0xFF10B981) else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Addresses section
            if (iface.ipv4Addresses.isNotEmpty() || iface.ipv6Addresses.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = DarkBorder
                )

                if (iface.ipv4Addresses.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("IPv4 Address", color = TextSecondaryDark, fontSize = 10.sp)
                            Text(
                                text = iface.ipv4Addresses.joinToString(", "),
                                color = NeonTeal,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = { onCopy("IPv4", iface.ipv4Addresses.first()) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy IPv4", tint = NeonCyan, modifier = Modifier.size(13.dp))
                        }
                    }
                }

                if (iface.ipv6Addresses.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("IPv6 Address", color = TextSecondaryDark, fontSize = 10.sp)
                            Text(
                                text = iface.ipv6Addresses.first().split("%")[0],
                                color = TextPrimaryDark,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = { onCopy("IPv6", iface.ipv6Addresses.first()) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy IPv6", tint = NeonCyan, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }
        }
    }
}
