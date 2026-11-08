package com.dping.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.MyIpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyIpScreen(
    viewModel: MyIpViewModel,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Pre-extract theme colors in Composable scope
    val cyanColor = NeonCyan
    val tealColor = NeonTeal
    val textPrimary = TextPrimaryDark
    val textSecondary = TextSecondaryDark
    val surfaceColor = DarkSurface
    val surfaceVariant = DarkSurfaceVariant
    val borderColor = DarkBorder

    val isIpTab = state.selectedTab == "IP"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.myIpTitle,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (state.isLoading) "..." else if (state.info.publicIp != "Unavailable") "${state.info.publicIp} • ${state.info.country}" else "Offline / Local Only",
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // 1. Segmented Mode Toggle: [ My IP ] vs [ Geo Location ]
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
                        .background(if (isIpTab) cyanColor else Color.Transparent)
                        .clickable { viewModel.selectTab("IP") }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.drawerMyIp,
                        color = if (isIpTab) DarkBackground else textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isIpTab) cyanColor else Color.Transparent)
                        .clickable { viewModel.selectTab("GEO") }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.geoTelemetry,
                        color = if (!isIpTab) DarkBackground else textSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (isIpTab) {
                // ================= TAB 1: MY IP =================

                // 2. Local Network Card (LAN)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(tealColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lan,
                                        contentDescription = null,
                                        tint = tealColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = strings.localIp,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tealColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PRIVATE",
                                    color = tealColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // IPv4 Row
                        CopyableIpRow(
                            label = "IPv4 Address",
                            value = state.info.wifiIp,
                            copyTextLabel = strings.copyDetails,
                            onCopy = { copyText(context, "Local IPv4", state.info.wifiIp) }
                        )

                        // IPv6 Row
                        if (state.info.localIpv6.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            CopyableIpRow(
                                label = "IPv6 (Link-Local)",
                                value = state.info.localIpv6,
                                copyTextLabel = strings.copyDetails,
                                onCopy = { copyText(context, "Local IPv6", state.info.localIpv6) }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = borderColor
                        )

                        // Network Details Grid
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.subnetMask, color = textSecondary, fontSize = 11.sp)
                                Text(
                                    state.info.subnetMask,
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.gateway, color = textSecondary, fontSize = 11.sp)
                                Text(
                                    state.info.gatewayIp,
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.dns, color = textSecondary, fontSize = 11.sp)
                                Text(
                                    state.info.dnsServers.firstOrNull() ?: "1.1.1.1",
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Secondary DNS", color = textSecondary, fontSize = 11.sp)
                                Text(
                                    state.info.dnsServers.getOrNull(1) ?: "8.8.8.8",
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 3. Public Network Card (WAN)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cyanColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Public,
                                        contentDescription = null,
                                        tint = cyanColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = strings.publicIp,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PUBLIC",
                                    color = Color(0xFF10B981),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Public IP Row
                        CopyableIpRow(
                            label = "Public IPv4",
                            value = state.info.publicIp,
                            copyTextLabel = strings.copyDetails,
                            onCopy = { copyText(context, "Public IP", state.info.publicIp) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = borderColor
                        )

                        // Country & ISP Details
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.country, color = textSecondary, fontSize = 11.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (state.info.flagEmoji.isNotBlank()) {
                                        Text(state.info.flagEmoji, fontSize = 14.sp)
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = state.info.country,
                                        color = textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("ISP / Organization", color = textSecondary, fontSize = 11.sp)
                                Text(
                                    text = state.info.isp.ifBlank { "ISP Not Detected" },
                                    color = textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (state.info.asn.isNotBlank() || state.info.reverseDns.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (state.info.asn.isNotBlank()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Autonomous System", color = textSecondary, fontSize = 11.sp)
                                        Text(
                                            state.info.asn,
                                            color = cyanColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                if (state.info.reverseDns.isNotBlank()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Reverse DNS Host", color = textSecondary, fontSize = 11.sp)
                                        Text(
                                            state.info.reverseDns,
                                            color = textPrimary,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 4. Connection Interface Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFD2A8FF).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = Color(0xFFD2A8FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.networkType,
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${state.info.connectionType} • Active",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.info.isConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (state.info.isConnected) "ONLINE" else "OFFLINE",
                                color = if (state.info.isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            } else {
                // ================= TAB 2: GEOLOCATION =================

                // 2. Geolocation Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cyanColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = cyanColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = strings.geoTelemetry,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Estimated BGP routing coordinates",
                                    color = textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = borderColor
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.cityRegion, color = textSecondary, fontSize = 11.sp)
                                val cityRegion = when {
                                    state.info.city.isNotBlank() && state.info.region.isNotBlank() -> "${state.info.city}, ${state.info.region}"
                                    state.info.city.isNotBlank() -> state.info.city
                                    state.info.region.isNotBlank() -> state.info.region
                                    state.isLoading -> "Locating..."
                                    else -> "Unavailable"
                                }
                                Text(
                                    text = cityRegion,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.country, color = textSecondary, fontSize = 11.sp)
                                val countryText = when {
                                    state.info.country.isNotBlank() && state.info.country != "Unavailable" -> {
                                        if (state.info.flagEmoji.isNotBlank()) "${state.info.country} ${state.info.flagEmoji}" else state.info.country
                                    }
                                    state.isLoading -> "Locating..."
                                    else -> "Unavailable"
                                }
                                Text(
                                    text = countryText,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.timezone, color = textSecondary, fontSize = 11.sp)
                                val tzText = when {
                                    state.info.timezone.isNotBlank() -> state.info.timezone
                                    state.isLoading -> "Resolving..."
                                    else -> "Unavailable"
                                }
                                Text(
                                    text = tzText,
                                    color = textPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.coordinates, color = textSecondary, fontSize = 11.sp)
                                val hasCoords = state.info.latitude != 0.0 || state.info.longitude != 0.0
                                val coordsText = if (hasCoords) {
                                    String.format(java.util.Locale.US, "%.4f, %.4f", state.info.latitude, state.info.longitude)
                                } else if (state.isLoading) {
                                    "Resolving..."
                                } else {
                                    "Unavailable"
                                }
                                Text(
                                    text = coordsText,
                                    color = if (hasCoords) cyanColor else textSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 3. Open in Maps Option Card (In-app map removed as requested)
                val hasCoords = state.info.latitude != 0.0 || state.info.longitude != 0.0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cyanColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    tint = cyanColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.openInMaps,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (hasCoords) "${state.info.latitude}, ${state.info.longitude}" else "Awaiting GPS/BGP coordinates",
                                    color = if (hasCoords) cyanColor else textSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Open in Google Maps button
                        Button(
                            onClick = {
                                if (!hasCoords) {
                                    Toast.makeText(context, "Location coordinates unavailable", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val lat = state.info.latitude
                                val lon = state.info.longitude
                                try {
                                    val mapUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(My IP Location)")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {
                                    try {
                                        val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lon")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Unable to open maps", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = hasCoords,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cyanColor,
                                disabledContainerColor = surfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = if (hasCoords) DarkBackground else textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.openInMaps,
                                color = if (hasCoords) DarkBackground else textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 5. Bottom Refresh Action Button
            Button(
                onClick = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tealColor),
                shape = RoundedCornerShape(12.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Refreshing...",
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = strings.refresh,
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CopyableIpRow(
    label: String,
    value: String,
    copyTextLabel: String = "Copy",
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextSecondaryDark, fontSize = 10.sp)
            Text(
                text = value,
                color = TextPrimaryDark,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NeonCyan.copy(alpha = 0.12f))
                .clickable { onCopy() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = NeonCyan,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = copyTextLabel,
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Copied $label: $text", Toast.LENGTH_SHORT).show()
}
