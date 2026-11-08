package com.dping.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.network.ping.PingPacket
import com.dping.app.network.ping.PingPacketStatus
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.PingUiState
import com.dping.app.ui.viewmodels.PingViewModel
import com.dping.app.utils.ExportManager
import java.util.Locale

@Composable
fun PingScreen(
    viewModel: PingViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var resultMode by remember { mutableStateOf("CMD") } // "CMD" for Console, "CHART" for Chart
    var showQuickSettingsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 1. Subtitle & Status Row (Screens 2 & 3 in reference image)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (resultMode == "CMD") "Ping – Continuous Mode" else "Ping – Chart Mode",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            )

            // Running / Idle Status Pill
            if (state.isRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DestructiveRed.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(DestructiveRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Running",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DestructiveRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(TextSecondaryDark)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ready",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Target Input Bar with Search Icon (Screens 2 & 3)
        OutlinedTextField(
            value = state.target,
            onValueChange = { viewModel.onTargetChange(it) },
            placeholder = { Text(strings.targetHint, color = TextSecondaryDark, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = {
                keyboardController?.hide()
                if (!state.isRunning) viewModel.startPing(presetCount = 0)
            }),
            trailingIcon = {
                IconButton(onClick = {
                    keyboardController?.hide()
                    if (state.isRunning) viewModel.stopAllTests() else viewModel.startPing(presetCount = 0)
                }) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Stop else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (state.isRunning) DestructiveRed else NeonCyan
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Segmented Toggle Switch: [ Console ] vs [ Chart ] (Screens 2 & 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                .padding(4.dp)
        ) {
            // Console Button
            val isConsole = resultMode == "CMD"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isConsole) DarkSurfaceVariant else Color.Transparent)
                    .border(
                        width = if (isConsole) 1.dp else 0.dp,
                        color = if (isConsole) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { resultMode = "CMD" }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.toggleConsole,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isConsole) FontWeight.Bold else FontWeight.Medium,
                        color = if (isConsole) NeonCyan else TextSecondaryDark,
                        fontSize = 13.sp
                    )
                )
            }

            // Chart Button
            val isChart = resultMode == "CHART"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isChart) NeonCyan else Color.Transparent)
                    .clickable { resultMode = "CHART" }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.toggleChart,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isChart) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isChart) Color(0xFF070D15) else TextSecondaryDark,
                        fontSize = 13.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Main Body: Console View (Screen 2) OR Chart Mode (Screen 3)
        Box(modifier = Modifier.weight(1f)) {
            if (resultMode == "CMD") {
                PingConsoleView(state = state, onCopy = {
                    val log = state.packets.joinToString("\n") { p ->
                        if (p.status == PingPacketStatus.SUCCESS) {
                            "64 bytes from ${p.ip}: icmp_seq=${p.sequence} time=${p.rttMs} ms TTL=${p.ttl}"
                        } else {
                            "Request timed out for icmp_seq ${p.sequence}"
                        }
                    }
                    ExportManager.copyToClipboard(context, "Ping Output", log)
                })
            } else {
                PingChartView(state = state)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Bottom Action Buttons Bar (Screens 2 & 3 in reference image)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left Button: Stop (Red) or Start (Cyan)
            if (state.isRunning) {
                Button(
                    onClick = { viewModel.stopAllTests() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = strings.btnStop,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.startPing(presetCount = 0) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = strings.btnPing,
                        color = Color(0xFF070D15),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            // Right Button: Settings (Screen 2 & 3: [ ⚙ Settings ])
            OutlinedButton(
                onClick = { showQuickSettingsDialog = true },
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurface),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimaryDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.settingsTitle,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Quick Settings Dialog
    if (showQuickSettingsDialog) {
        QuickSettingsDialog(
            state = state,
            onPacketSizeChange = { viewModel.onPacketSizeChange(it) },
            onIntervalChange = { viewModel.onIntervalChange(it) },
            onResolveHostnameToggle = { viewModel.onResolveHostnameToggle(it) },
            onKeepRunningBgToggle = { viewModel.onKeepRunningBgToggle(it) },
            onDismiss = { showQuickSettingsDialog = false }
        )
    }
}

// ----------------- SCREEN 2: CONSOLE VIEW -----------------
@Composable
private fun PingConsoleView(
    state: PingUiState,
    onCopy: () -> Unit
) {
    val listState = rememberLazyListState()
    val strings = LocalAppStrings.current

    // Auto-scroll to latest response line
    LaunchedEffect(state.packets.size) {
        if (state.packets.isNotEmpty()) {
            listState.animateScrollToItem(state.packets.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Authentic Terminal Box (Screen 2 exact representation)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF132030), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF03070D))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Terminal Top Mini Bar with Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                    }
                    if (state.packets.isNotEmpty()) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF8B9EB5), modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (state.packets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.isRunning) "Pinging ${state.target}..." else "Press Start to begin ping test",
                            color = Color(0xFF8B9EB5),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.packets) { packet ->
                            if (packet.status == PingPacketStatus.SUCCESS) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "64 bytes from ${packet.ip}: icmp_seq=${packet.sequence} ",
                                        color = Color(0xFF8B9EB5),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "time=${packet.rttMs} ms ",
                                        color = NeonTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "TTL=${packet.ttl}",
                                        color = Color(0xFF8B9EB5),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = "Request timed out for icmp_seq ${packet.sequence}",
                                    color = DestructiveRed,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Packets Summary Card (Screen 2 bottom metrics)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sent / Received / Lost / Loss %
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricColumn(strings.statsSent, "${state.stats.sent}")
                    MetricColumn(strings.statsReceived, "${state.stats.received}")
                    MetricColumn("Lost", "${state.stats.lost}")
                    MetricColumn(strings.statsLoss, "${String.format(Locale.US, "%.0f", state.stats.lossPercentage)}%", if (state.stats.lossPercentage > 0) DestructiveRed else NeonTeal)
                }

                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 8.dp))

                // Min / Avg / Max Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricRowItem(strings.statsMin, "${String.format(Locale.US, "%.1f", state.stats.minRtt)} ms")
                    MetricRowItem(strings.statsAvg, "${String.format(Locale.US, "%.1f", state.stats.avgRtt)} ms", NeonCyan)
                    MetricRowItem(strings.statsMax, "${String.format(Locale.US, "%.1f", state.stats.maxRtt)} ms")
                }
            }
        }
    }
}

// ----------------- SCREEN 3: CHART MODE VIEW -----------------
@Composable
private fun PingChartView(state: PingUiState) {
    val rttPoints = remember(state.packets) {
        state.packets.filter { it.status == PingPacketStatus.SUCCESS }.map { it.rttMs.toFloat() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Real-time Latency Line Wave Graph with Grid (Screen 3 representation)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF060E18))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Y-Axis Markers column (100, 80, 60, 40, 20, 00)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(28.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("100", "80", "60", "40", "20", "00").forEach { label ->
                        Text(
                            text = label,
                            color = TextSecondaryDark,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }

                // Canvas Area for Grid Lines + Wave Curve + Peak Pill
                val lineColor = NeonCyan
                val maxPoint = rttPoints.maxOrNull() ?: 0f
                val maxPointIndex = if (rttPoints.isNotEmpty()) rttPoints.indexOf(maxPoint) else -1

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // Draw 5 horizontal faint grid lines
                    for (i in 0..5) {
                        val y = h * (i / 5f)
                        drawLine(
                            color = Color(0xFF13263D),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (rttPoints.size >= 2) {
                        val maxScale = 100f
                        val stepX = w / (rttPoints.size - 1).coerceAtLeast(1)

                        val strokePath = Path()
                        val fillPath = Path()

                        fillPath.moveTo(0f, h)

                        rttPoints.forEachIndexed { idx, rtt ->
                            val cx = idx * stepX
                            val cy = (h - (rtt.coerceIn(0f, maxScale) / maxScale) * h).coerceIn(4f, h - 4f)
                            if (idx == 0) {
                                strokePath.moveTo(cx, cy)
                                fillPath.lineTo(cx, cy)
                            } else {
                                strokePath.lineTo(cx, cy)
                                fillPath.lineTo(cx, cy)
                            }
                        }

                        fillPath.lineTo(w, h)
                        fillPath.close()

                        // Gradient fill underneath
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )

                        // Glowing stroke line
                        drawPath(
                            path = strokePath,
                            color = lineColor,
                            style = Stroke(width = 2.5.dp.toPx())
                        )

                        // Peak dot
                        if (maxPointIndex >= 0) {
                            val peakX = maxPointIndex * stepX
                            val peakY = (h - (maxPoint.coerceIn(0f, maxScale) / maxScale) * h).coerceIn(4f, h - 4f)
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(peakX, peakY))
                            drawCircle(color = lineColor, radius = 2.dp.toPx(), center = Offset(peakX, peakY))
                        }
                    }
                }

                // Peak Indicator Pill (e.g. "Max 42.7 ms") over highest point
                if (maxPoint > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Max ${String.format(Locale.US, "%.1f", maxPoint)} ms",
                            color = Color(0xFF070D15),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // X-Axis Markers at bottom (0, 10, 20, 30, 40, 50)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomEnd)
                        .padding(start = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("0", "10", "20", "30", "40", "50").forEach { step ->
                        Text(
                            text = step,
                            color = TextSecondaryDark,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4-Column Metrics Row: Min, Avg, Max, Jitter (Screen 3)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricColumn("Min", "${String.format(Locale.US, "%.1f", state.stats.minRtt)} ms")
                MetricColumn("Avg", "${String.format(Locale.US, "%.1f", state.stats.avgRtt)} ms", NeonCyan)
                MetricColumn("Max", "${String.format(Locale.US, "%.1f", state.stats.maxRtt)} ms")
                MetricColumn("Jitter", "${String.format(Locale.US, "%.1f", state.stats.mdevRtt)} ms", WarningOrange)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Packets Summary Card (Sent, Received, Lost, Loss %)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricColumn("Sent", "${state.stats.sent}")
                MetricColumn("Received", "${state.stats.received}")
                MetricColumn("Lost", "${state.stats.lost}")
                MetricColumn("Loss", "${String.format(Locale.US, "%.2f", state.stats.lossPercentage)}%", if (state.stats.lossPercentage > 0) DestructiveRed else NeonTeal)
            }
        }
    }
}

// ----------------- SUB-COMPONENTS -----------------
@Composable
private fun MetricColumn(
    label: String,
    value: String,
    color: Color = TextPrimaryDark
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = TextSecondaryDark, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun MetricRowItem(
    label: String,
    value: String,
    color: Color = TextPrimaryDark
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label ", color = TextSecondaryDark, fontSize = 11.sp)
        Text(text = value, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

// ----------------- QUICK SETTINGS DIALOG -----------------
@Composable
private fun QuickSettingsDialog(
    state: PingUiState,
    onPacketSizeChange: (String) -> Unit,
    onIntervalChange: (String) -> Unit,
    onResolveHostnameToggle: (Boolean) -> Unit,
    onKeepRunningBgToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ping Parameters", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Packet Size
                Column {
                    Text("Packet Size (bytes)", color = TextSecondaryDark, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("32", "64", "128", "512").forEach { sz ->
                            val sel = state.packetSize == sz
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) NeonCyan else DarkSurfaceVariant)
                                    .clickable { onPacketSizeChange(sz) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = sz,
                                    color = if (sel) Color(0xFF070D15) else TextPrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Ping Interval
                Column {
                    Text("Interval", color = TextSecondaryDark, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("500" to "0.5s", "1000" to "1.0s", "2000" to "2.0s").forEach { (ms, lbl) ->
                            val sel = state.intervalMs == ms
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) NeonCyan else DarkSurfaceVariant)
                                    .clickable { onIntervalChange(ms) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = lbl,
                                    color = if (sel) Color(0xFF070D15) else TextPrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Resolve Hostname Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reverse DNS Lookup", color = TextPrimaryDark, fontSize = 13.sp)
                    Switch(
                        checked = state.resolveHostname,
                        onCheckedChange = onResolveHostnameToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = NeonTeal)
                    )
                }

                // Background Execution Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Background Execution", color = TextPrimaryDark, fontSize = 13.sp)
                    Switch(
                        checked = state.keepRunningBg,
                        onCheckedChange = onKeepRunningBgToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = NeonTeal)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Done", color = Color(0xFF070D15), fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}
