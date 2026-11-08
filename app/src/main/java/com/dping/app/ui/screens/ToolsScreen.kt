package com.dping.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.dping.app.ui.components.DPingFilledButton
import com.dping.app.ui.components.DPingInputField
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.ToolsViewModel

enum class ActiveToolDialog {
    NONE, TCP_PORT, HTTP_CHECK, TLS_CERT, SUBNET_CALC, GATEWAY
}

data class CyberToolItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNavigateToIpRange: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToLanDiscovery: () -> Unit,
    onNavigateToDnsLookup: () -> Unit,
    onNavigateToNetworkInterfaces: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    var activeDialog by remember { mutableStateOf(ActiveToolDialog.NONE) }

    // Pre-extract theme colors in Composable scope
    val cyanColor = NeonCyan
    val tealColor = NeonTeal
    val textPrimary = TextPrimaryDark
    val textSecondary = TextSecondaryDark
    val textMuted = TextMutedDark
    val surfaceColor = DarkSurface
    val surfaceVariant = DarkSurfaceVariant
    val borderColor = DarkBorder

    val toolItems = remember(strings) {
        listOf(
            CyberToolItem(
                title = strings.dnsLookupTitle,
                subtitle = strings.dnsLookupDesc,
                icon = Icons.Default.Public,
                accentColor = Color(0xFF00E5FF),
                onClick = onNavigateToDnsLookup
            ),
            CyberToolItem(
                title = strings.tcpPortTitle,
                subtitle = strings.tcpPortDesc,
                icon = Icons.Default.Lan,
                accentColor = Color(0xFF00C2FF),
                onClick = { activeDialog = ActiveToolDialog.TCP_PORT }
            ),
            CyberToolItem(
                title = strings.subnetCalcTitle,
                subtitle = strings.subnetCalcDesc,
                icon = Icons.Default.Calculate,
                accentColor = Color(0xFF79C0FF),
                onClick = { activeDialog = ActiveToolDialog.SUBNET_CALC }
            ),
            CyberToolItem(
                title = "Interfaces",
                subtitle = "Inspect wlan0, rmnet0, tun0 IP/MAC/MTU",
                icon = Icons.Default.SettingsEthernet,
                accentColor = Color(0xFF10B981),
                onClick = onNavigateToNetworkInterfaces
            ),
            CyberToolItem(
                title = "HTTP Checker",
                subtitle = "HTTP status codes, latency, and response headers",
                icon = Icons.Default.Http,
                accentColor = Color(0xFFD2A8FF),
                onClick = { activeDialog = ActiveToolDialog.HTTP_CHECK }
            ),
            CyberToolItem(
                title = "SSL / TLS",
                subtitle = "Inspect certificate chain, issuer & expiration",
                icon = Icons.Default.Lock,
                accentColor = Color(0xFFF59E0B),
                onClick = { activeDialog = ActiveToolDialog.TLS_CERT }
            ),
            CyberToolItem(
                title = "Gateway Diag",
                subtitle = "Default gateway reachability and DNS health",
                icon = Icons.Default.Router,
                accentColor = Color(0xFF00FFB2),
                onClick = {
                    viewModel.refreshGateway()
                    activeDialog = ActiveToolDialog.GATEWAY
                }
            ),
            CyberToolItem(
                title = strings.ipRangeTitle,
                subtitle = "Subnet CIDR scan: occupied vs free IP map with device types",
                icon = Icons.Default.Radar,
                accentColor = Color(0xFF00E5FF),
                onClick = onNavigateToIpRange
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.toolsTitle,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Diagnostic Utilities & Network Inspection",
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
            // 2x4 Cyber Diagnostic Grid (Screen 9 format)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(toolItems) { item ->
                    CyberToolCard(item = item)
                }
            }
        }
    }

    // Modal Tool Dialogs
    when (activeDialog) {
        ActiveToolDialog.TCP_PORT -> {
            TcpPortDialog(
                viewModel = viewModel,
                onDismiss = { activeDialog = ActiveToolDialog.NONE }
            )
        }
        ActiveToolDialog.HTTP_CHECK -> {
            HttpCheckDialog(
                viewModel = viewModel,
                onDismiss = { activeDialog = ActiveToolDialog.NONE }
            )
        }
        ActiveToolDialog.TLS_CERT -> {
            TlsCertDialog(
                viewModel = viewModel,
                onDismiss = { activeDialog = ActiveToolDialog.NONE }
            )
        }
        ActiveToolDialog.SUBNET_CALC -> {
            SubnetCalcDialog(
                viewModel = viewModel,
                onDismiss = { activeDialog = ActiveToolDialog.NONE }
            )
        }
        ActiveToolDialog.GATEWAY -> {
            GatewayDialog(
                viewModel = viewModel,
                onDismiss = { activeDialog = ActiveToolDialog.NONE }
            )
        }
        else -> {}
    }
}

@Composable
private fun CyberToolCard(
    item: CyberToolItem
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .clickable { item.onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon in tinted circular pill
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = item.title,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.subtitle,
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    maxLines = 2,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

// Dialog: TCP Port Checker
@Composable
private fun TcpPortDialog(
    viewModel: ToolsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("TCP Port Connectivity", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DPingInputField(
                    label = "Target Host / IP",
                    value = state.tcpHost,
                    onValueChange = { viewModel.onTcpHostChange(it) }
                )
                DPingInputField(
                    label = "Port Number (e.g. 80, 443, 22)",
                    value = state.tcpPort,
                    onValueChange = { viewModel.onTcpPortChange(it) }
                )

                Button(
                    onClick = { viewModel.runTcpCheck() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !state.isTcpLoading
                ) {
                    if (state.isTcpLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkBackground, strokeWidth = 2.dp)
                    } else {
                        Text("Check Port", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                state.tcpResult?.let { res ->
                    val isOpen = res.status == com.dping.app.network.tcp.TcpPortStatus.ESTABLISHED
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isOpen) "Port ${res.port} is OPEN" else "Port ${res.port} is ${res.status.name}",
                                color = if (isOpen) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (res.durationMs > 0) {
                                Text("Handshake RTT: ${String.format(java.util.Locale.US, "%.1f", res.durationMs)} ms", color = TextSecondaryDark, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = NeonCyan) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

// Dialog: HTTP Web Checker
@Composable
private fun HttpCheckDialog(
    viewModel: ToolsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("HTTP / HTTPS Endpoint Checker", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DPingInputField(
                    label = "Target URL",
                    value = state.httpUrl,
                    onValueChange = { viewModel.onHttpUrlChange(it) }
                )

                Button(
                    onClick = { viewModel.runHttpCheck() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !state.isHttpLoading
                ) {
                    if (state.isHttpLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkBackground, strokeWidth = 2.dp)
                    } else {
                        Text("Send Request", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                state.httpResult?.let { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Status Code: ${res.statusCode} ${res.statusMessage}",
                                color = if (res.statusCode in 200..299) Color(0xFF10B981) else WarningOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text("Response Latency: ${String.format(java.util.Locale.US, "%.1f", res.durationMs)} ms", color = TextSecondaryDark, fontSize = 11.sp)
                            val serverHeader = res.headers["Server"] ?: res.headers["server"] ?: ""
                            if (serverHeader.isNotBlank()) Text("Server: $serverHeader", color = TextSecondaryDark, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = NeonCyan) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

// Dialog: TLS Certificate Inspector
@Composable
private fun TlsCertDialog(
    viewModel: ToolsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SSL / TLS Certificate Inspector", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DPingInputField(
                    label = "Domain / Host",
                    value = state.tlsHost,
                    onValueChange = { viewModel.onTlsHostChange(it) }
                )

                Button(
                    onClick = { viewModel.runTlsCheck() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !state.isTlsLoading
                ) {
                    if (state.isTlsLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkBackground, strokeWidth = 2.dp)
                    } else {
                        Text("Inspect Certificate", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                state.tlsResult?.let { cert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val isValid = cert.isSuccess && !cert.isExpired && !cert.isNotYetValid
                            Text(
                                text = if (isValid) "Certificate Valid" else "Invalid / Expired",
                                color = if (isValid) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text("Subject: ${cert.subject}", color = TextPrimaryDark, fontSize = 11.sp)
                            Text("Issuer: ${cert.issuer}", color = TextSecondaryDark, fontSize = 11.sp)
                            Text("Expires: ${cert.validUntil}", color = TextSecondaryDark, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = NeonCyan) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

// Dialog: Subnet Calculator
@Composable
private fun SubnetCalcDialog(
    viewModel: ToolsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subnet Calculator", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DPingInputField(
                    label = "IP Address",
                    value = state.subnetIp,
                    onValueChange = { viewModel.onSubnetIpChange(it) }
                )

                // Interactive CIDR Prefix Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CIDR Prefix",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeonCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "/${state.subnetPrefix}",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Slider(
                        value = state.subnetPrefix.toFloat(),
                        onValueChange = { viewModel.onSubnetPrefixChange(it.roundToInt()) },
                        valueRange = 1f..32f,
                        steps = 30,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurface,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Jump Subnet Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(8, 16, 24, 27, 30).forEach { prefixOption ->
                            val isSelected = state.subnetPrefix == prefixOption
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.25f) else DarkSurface.copy(alpha = 0.7f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.onSubnetPrefixChange(prefixOption) }
                            ) {
                                Text(
                                    text = "/$prefixOption",
                                    color = if (isSelected) NeonCyan else TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                state.subnetResult?.let { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Network:", color = TextSecondaryDark, fontSize = 11.sp)
                                Text(res.networkAddress, color = NeonTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Netmask:", color = TextSecondaryDark, fontSize = 11.sp)
                                Text(res.subnetMask, color = TextPrimaryDark, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Usable Hosts:", color = TextSecondaryDark, fontSize = 11.sp)
                                Text("${res.usableHosts}", color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Range:", color = TextSecondaryDark, fontSize = 11.sp)
                                Text("${res.firstUsableHost} - ${res.lastUsableHost}", color = TextSecondaryDark, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = NeonCyan) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

// Dialog: Gateway Diagnostics
@Composable
private fun GatewayDialog(
    viewModel: ToolsViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default Gateway Diagnostics", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.gatewayInfo?.let { gw ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Gateway IP: ${gw.gatewayIp}", color = NeonTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Local Interface IP: ${gw.localIp}", color = TextPrimaryDark, fontSize = 12.sp)
                            Text("DNS Servers: ${gw.dnsServers.joinToString(", ")}", color = TextSecondaryDark, fontSize = 11.sp)
                            Text(
                                text = if (gw.isReachable) "Reachable (${String.format(java.util.Locale.US, "%.1f", gw.latencyMs)} ms)" else "Status: Probing",
                                color = if (gw.isReachable) Color(0xFF10B981) else WarningOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = NeonCyan) }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
