package com.dping.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.network.speedtest.SpeedTestPhase
import com.dping.app.network.speedtest.SpeedTestProvider
import com.dping.app.network.speedtest.SpeedTestProviders
import com.dping.app.ui.components.DPingFilledButton
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.SpeedTestViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val isRunning = state.phase in listOf(
        SpeedTestPhase.PING,
        SpeedTestPhase.DOWNLOAD,
        SpeedTestPhase.UPLOAD
    )

    var showProviderDialog by remember { mutableStateOf(false) }

    val animatedSpeed by animateFloatAsState(
        targetValue = state.currentSpeedMbps.toFloat(),
        animationSpec = tween(durationMillis = 200),
        label = "SpeedGaugeAnimation"
    )

    val scrollState = rememberScrollState()

    // Pre-extract colors in Composable scope for use inside Canvas DrawScopes
    val cyanColor = NeonCyan
    val tealColor = NeonTeal
    val textPrimary = TextPrimaryDark
    val textSecondary = TextSecondaryDark
    val textMuted = TextMutedDark
    val surfaceColor = DarkSurface
    val surfaceVariant = DarkSurfaceVariant
    val borderColor = DarkBorder

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.speedTestTitle,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Multi-Provider CDN Throughput Engine",
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
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Ookla-Style Server Provider Selector Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isRunning) { showProviderDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cyanColor.copy(alpha = 0.15f))
                                .border(1.dp, cyanColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = cyanColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = state.selectedProvider.name,
                                    color = textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = cyanColor.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = state.selectedProvider.badge,
                                        color = cyanColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = state.selectedProvider.location,
                                color = textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = surfaceVariant,
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Change", color = cyanColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = cyanColor, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2. Circular Speed Gauge (Center Dial)
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(surfaceVariant.copy(alpha = 0.35f))
                    .border(1.dp, borderColor.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = Color(0xFF132030)
                val activeBrush = Brush.sweepGradient(
                    colors = listOf(
                        cyanColor,
                        tealColor,
                        Color(0xFF00FFB2),
                        cyanColor
                    )
                )

                Canvas(modifier = Modifier.size(200.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val radius = diameter / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw 240-degree background track
                    drawArc(
                        color = trackColor,
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2),
                        size = androidx.compose.ui.geometry.Size(diameter, diameter),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Draw active speed progress arc (capped at 500 Mbps max scale)
                    val speedCap = 500f
                    val sweepFraction = (animatedSpeed / speedCap).coerceIn(0f, 1f)
                    val activeSweep = sweepFraction * 240f

                    if (activeSweep > 0f) {
                        drawArc(
                            brush = activeBrush,
                            startAngle = 150f,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2),
                            size = androidx.compose.ui.geometry.Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Tick marks around dial
                    for (i in 0..10) {
                        val angleDeg = 150f + (i * 24f)
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val innerR = radius - 18.dp.toPx()
                        val outerR = radius - 10.dp.toPx()

                        val startP = Offset(
                            center.x + innerR * cos(angleRad).toFloat(),
                            center.y + innerR * sin(angleRad).toFloat()
                        )
                        val endP = Offset(
                            center.x + outerR * cos(angleRad).toFloat(),
                            center.y + outerR * sin(angleRad).toFloat()
                        )

                        drawLine(
                            color = if (angleDeg <= (150f + activeSweep)) cyanColor else Color(0xFF2A3D52),
                            start = startP,
                            end = endP,
                            strokeWidth = if (i % 2 == 0) 2.dp.toPx() else 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Needle tip glowing dot
                    val currentAngleRad = Math.toRadians((150f + activeSweep).toDouble())
                    val tipX = center.x + radius * cos(currentAngleRad).toFloat()
                    val tipY = center.y + radius * sin(currentAngleRad).toFloat()
                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = Offset(tipX, tipY)
                    )
                    drawCircle(
                        color = cyanColor,
                        radius = 3.dp.toPx(),
                        center = Offset(tipX, tipY)
                    )
                }

                // Digital readout in center: returns to 0.0 upon test completion!
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (state.phase == SpeedTestPhase.IDLE || state.phase == SpeedTestPhase.COMPLETE) {
                            String.format(java.util.Locale.US, "%.1f", animatedSpeed)
                        } else {
                            String.format(java.util.Locale.US, "%.1f", animatedSpeed)
                        },
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Mbps",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = cyanColor
                    )
                    Spacer(Modifier.height(6.dp))

                    // Phase Badge Pill
                    val (badgeText, badgeBg, badgeTextColor) = when (state.phase) {
                        SpeedTestPhase.DOWNLOAD -> Triple("DOWNLOADING", cyanColor.copy(alpha = 0.15f), cyanColor)
                        SpeedTestPhase.UPLOAD -> Triple("UPLOADING", Color(0xFFD2A8FF).copy(alpha = 0.15f), Color(0xFFD2A8FF))
                        SpeedTestPhase.PING -> Triple("LATENCY PROBE", tealColor.copy(alpha = 0.15f), tealColor)
                        SpeedTestPhase.COMPLETE -> Triple("TEST COMPLETE", Color(0xFF10B981).copy(alpha = 0.18f), Color(0xFF10B981))
                        else -> Triple("READY", surfaceVariant, textSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeTextColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 3. Primary Results Grid: SHOWS BOTH DOWNLOAD AND UPLOAD PROMINENTLY!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // DOWNLOAD CARD
                TelemetryMetricCard(
                    title = strings.downloadSpeed,
                    value = if (state.downloadSpeedMbps > 0) "${String.format(java.util.Locale.US, "%.1f", state.downloadSpeedMbps)} Mbps" else "--",
                    icon = Icons.Default.ArrowDownward,
                    accentColor = cyanColor,
                    isHighlight = true,
                    modifier = Modifier.weight(1f)
                )

                // UPLOAD CARD
                TelemetryMetricCard(
                    title = strings.uploadSpeed,
                    value = if (state.uploadSpeedMbps > 0) "${String.format(java.util.Locale.US, "%.1f", state.uploadSpeedMbps)} Mbps" else "--",
                    icon = Icons.Default.ArrowUpward,
                    accentColor = Color(0xFFD2A8FF),
                    isHighlight = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // PING CARD
                TelemetryMetricCard(
                    title = strings.pingLatency,
                    value = if (state.pingMs > 0) "${String.format(java.util.Locale.US, "%.0f", state.pingMs)} ms" else "--",
                    icon = Icons.Default.Bolt,
                    accentColor = tealColor,
                    isHighlight = false,
                    modifier = Modifier.weight(1f)
                )

                // JITTER CARD
                TelemetryMetricCard(
                    title = strings.jitter,
                    value = if (state.jitterMs > 0) "${String.format(java.util.Locale.US, "%.1f", state.jitterMs)} ms" else "--",
                    icon = Icons.Default.Refresh,
                    accentColor = WarningOrange,
                    isHighlight = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // 4. Real-time Live Throughput Waveform Canvas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "THROUGHPUT GRAPH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (state.peakSpeedMbps > 0) "Peak: ${String.format(java.util.Locale.US, "%.1f", state.peakSpeedMbps)} Mbps" else "Live Stream",
                            fontSize = 10.sp,
                            color = cyanColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val history = state.speedHistory
                        if (history.size >= 2) {
                            val maxVal = (history.maxOrNull() ?: 100f).coerceAtLeast(50f)
                            val stepX = size.width / (history.size - 1)
                            val path = Path()

                            history.forEachIndexed { index, value ->
                                val x = index * stepX
                                val y = size.height - (value / maxVal) * size.height
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = cyanColor,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        } else {
                            // Baseline idle grid
                            drawLine(
                                color = borderColor,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 5. Main Action Button: START / STOP SPEED TEST
            DPingFilledButton(
                text = if (isRunning) strings.btnStop else strings.startSpeedTest,
                onClick = {
                    if (isRunning) viewModel.stopTest() else viewModel.startTest()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))
        }
    }

    // Ookla-Style Provider Selection Dialog
    if (showProviderDialog) {
        AlertDialog(
            onDismissRequest = { showProviderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = cyanColor)
                    Text("Select Speed Test Server", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                val dialogScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(dialogScrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choose an edge provider cluster or auto-select the nearest fastest node:",
                        color = textSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(2.dp))

                    SpeedTestProviders.ALL_PROVIDERS.forEach { prov ->
                        val isSelected = state.selectedProvider.id == prov.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) cyanColor.copy(alpha = 0.14f) else surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) cyanColor else borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectProvider(prov)
                                    showProviderDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = prov.name,
                                            color = textPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) cyanColor.copy(alpha = 0.25f) else surfaceColor
                                        ) {
                                            Text(
                                                text = prov.badge,
                                                color = if (isSelected) cyanColor else textSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                softWrap = false,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = prov.location,
                                        color = textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.selectProvider(prov)
                                        showProviderDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = cyanColor,
                                        unselectedColor = textMuted
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProviderDialog = false }) {
                    Text("Close", color = cyanColor)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TelemetryMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(
            width = if (isHighlight) 1.2.dp else 1.dp,
            color = if (isHighlight) accentColor.copy(alpha = 0.45f) else DarkBorder,
            shape = RoundedCornerShape(12.dp)
        ),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = title,
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = value,
                color = if (value != "--") TextPrimaryDark else TextMutedDark,
                fontWeight = FontWeight.Bold,
                fontSize = if (isHighlight) 17.sp else 15.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
