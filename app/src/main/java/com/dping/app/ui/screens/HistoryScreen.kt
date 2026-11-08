package com.dping.app.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.data.model.DiagnosticRecord
import com.dping.app.ui.components.DPingFilledButton
import com.dping.app.ui.components.DPingInputField
import com.dping.app.ui.components.DPingOutlinedButton
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.HistoryViewModel
import com.dping.app.utils.ExportManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    var showClearDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<DiagnosticRecord?>(null) }

    // Pre-extract theme colors in Composable scope
    val cyanColor = NeonCyan
    val tealColor = NeonTeal
    val textPrimary = TextPrimaryDark
    val textSecondary = TextSecondaryDark
    val textMuted = TextMutedDark
    val surfaceColor = DarkSurface
    val surfaceVariant = DarkSurfaceVariant
    val borderColor = DarkBorder

    val totalRecords = state.records.size
    val pingCount = remember(state.records) { state.records.count { it.testType.contains("PING", ignoreCase = true) } }
    val speedCount = remember(state.records) { state.records.count { it.testType.contains("SPEED", ignoreCase = true) } }
    val traceCount = remember(state.records) { state.records.count { it.testType.contains("TRACE", ignoreCase = true) } }

    val filterOptions = listOf(
        Pair("ALL", "All ($totalRecords)"),
        Pair("PING", "Ping ($pingCount)"),
        Pair("SPEED", "Speed ($speedCount)"),
        Pair("TRACEROUTE", "Traceroute ($traceCount)")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.historyTitle,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Diagnostic Logs & Export Vault",
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
                    IconButton(onClick = { showExportDialog = true }, enabled = state.records.isNotEmpty()) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = "Export Data",
                            tint = if (state.records.isNotEmpty()) cyanColor else textMuted
                        )
                    }
                    IconButton(onClick = { showClearDialog = true }, enabled = state.records.isNotEmpty()) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = if (state.records.isNotEmpty()) DestructiveRed else textMuted
                        )
                    }
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Telemetry Counters Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryCounterCard(
                    label = "Total Logs",
                    count = totalRecords.toString(),
                    accentColor = cyanColor,
                    modifier = Modifier.weight(1f)
                )
                TelemetryCounterCard(
                    label = "Ping Tests",
                    count = pingCount.toString(),
                    accentColor = tealColor,
                    modifier = Modifier.weight(1f)
                )
                TelemetryCounterCard(
                    label = "Speed Tests",
                    count = speedCount.toString(),
                    accentColor = Color(0xFFFFB800),
                    modifier = Modifier.weight(1f)
                )
                TelemetryCounterCard(
                    label = "Traceroutes",
                    count = traceCount.toString(),
                    accentColor = Color(0xFFFF6D00),
                    modifier = Modifier.weight(1f)
                )
            }

            // Cyber Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Filter by host, IP, or diagnostic summary...", color = textMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = cyanColor) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = textSecondary)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor,
                    focusedBorderColor = cyanColor,
                    unfocusedBorderColor = borderColor,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                singleLine = true
            )

            // Category Filter Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { (key, title) ->
                    val isSelected = state.selectedFilter == key
                    val filterColor = when (key) {
                        "PING" -> tealColor
                        "SPEED" -> Color(0xFFFFB800)
                        "TRACEROUTE" -> Color(0xFFFF6D00)
                        else -> cyanColor
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) filterColor.copy(alpha = 0.2f) else surfaceColor,
                        border = BorderStroke(1.dp, if (isSelected) filterColor else borderColor),
                        modifier = Modifier.clickable { viewModel.onFilterChange(key) }
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) filterColor else textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // History Records List
            val displayRecords = state.filteredRecords
            if (displayRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (state.searchQuery.isBlank()) strings.noHistory else "No matching diagnostic logs",
                            color = textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • HH:mm:ss", Locale.US) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayRecords, key = { it.id }) { record ->
                        CyberHistoryCard(
                            record = record,
                            dateStr = dateFormat.format(Date(record.timestamp)),
                            onClick = { selectedRecord = record },
                            onDelete = { viewModel.deleteRecord(record.id) }
                        )
                    }
                }
            }
        }
    }

    // Export Options Modal Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = cyanColor)
                    Text("Export Diagnostic Vault", color = textPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Choose format to export ${state.filteredRecords.size} current records:",
                        color = textSecondary,
                        fontSize = 13.sp
                    )

                    DPingFilledButton(
                        text = "Export as CSV (.csv)",
                        onClick = {
                            viewModel.exportToCsv(context)
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DPingOutlinedButton(
                        text = "Export as JSON (.json)",
                        onClick = {
                            viewModel.exportToJson(context)
                            showExportDialog = false
                        },
                        borderColor = tealColor,
                        textColor = tealColor,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DPingOutlinedButton(
                        text = "Copy All to Clipboard",
                        onClick = {
                            val csv = ExportManager.exportHistoryToCsv(state.filteredRecords)
                            ExportManager.copyToClipboard(context, "D Ping Diagnostic Log", csv)
                            showExportDialog = false
                        },
                        borderColor = cyanColor,
                        textColor = cyanColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Clear All Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text("Clear All Diagnostic Logs?", color = textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This action cannot be undone. All saved ping, speed test, and traceroute logs will be permanently deleted from device storage.",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Everything", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Detailed Log Inspection Dialog
    selectedRecord?.let { rec ->
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
        AlertDialog(
            onDismissRequest = { selectedRecord = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (rec.testType.uppercase()) {
                            "PING" -> tealColor.copy(alpha = 0.2f)
                            "SPEED TEST" -> Color(0xFFFFB800).copy(alpha = 0.2f)
                            "TRACEROUTE" -> Color(0xFFFF6D00).copy(alpha = 0.2f)
                            else -> cyanColor.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = rec.testType,
                            color = when (rec.testType.uppercase()) {
                                "PING" -> tealColor
                                "SPEED TEST" -> Color(0xFFFFB800)
                                "TRACEROUTE" -> Color(0xFFFF6D00)
                                else -> cyanColor
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(rec.target, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Timestamp: ${dateFormat.format(Date(rec.timestamp))}", color = textSecondary, fontSize = 11.sp)
                    Text("Duration: ${rec.durationMs} ms", color = textSecondary, fontSize = 11.sp)
                    if (!rec.countryName.isNullOrBlank()) {
                        Text("Geo: ${rec.countryName}", color = textSecondary, fontSize = 11.sp)
                    }
                    HorizontalDivider(color = borderColor, thickness = 1.dp)
                    Text("Result Summary:", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(rec.summary, color = cyanColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                    if (rec.detailsJson.isNotBlank()) {
                        HorizontalDivider(color = borderColor, thickness = 1.dp)
                        Text("Raw Trace Output:", color = textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                        ) {
                            Text(
                                text = rec.detailsJson,
                                color = textPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ExportManager.copyToClipboard(context, "Diagnostic Log", "${rec.testType} -> ${rec.target}\n${rec.summary}\n${rec.detailsJson}")
                    selectedRecord = null
                }) {
                    Text("Copy Details", color = cyanColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecord = null }) {
                    Text("Close", color = textSecondary)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TelemetryCounterCard(
    label: String,
    count: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.border(1.dp, DarkBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = count, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
            Text(text = label, color = TextSecondaryDark, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CyberHistoryCard(
    record: DiagnosticRecord,
    dateStr: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when (record.testType.uppercase()) {
        "PING" -> NeonTeal
        "SPEED TEST" -> Color(0xFFFFB800)
        "TRACEROUTE" -> Color(0xFFFF6D00)
        else -> NeonCyan
    }

    val icon = when (record.testType.uppercase()) {
        "PING" -> Icons.Default.Bolt
        "SPEED TEST" -> Icons.Default.Speed
        "TRACEROUTE" -> Icons.Default.Route
        else -> Icons.Default.NetworkCheck
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Neon Type Icon Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = record.testType,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.target,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!record.countryName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${record.countryName})",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = record.summary,
                    color = badgeColor,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateStr,
                    color = TextSecondaryDark,
                    fontSize = 10.sp
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = TextMutedDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
