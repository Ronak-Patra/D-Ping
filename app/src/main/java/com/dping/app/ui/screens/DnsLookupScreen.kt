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
import com.dping.app.network.dns.DnsRecordItem
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.ToolsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsLookupScreen(
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

    val recordTypes = listOf("A", "AAAA", "CNAME", "MX", "TXT", "NS", "PTR")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DNS Lookup Tool",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${state.dnsTarget} • ${state.dnsRecordType} Record",
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
            // 1. Target Domain Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = state.dnsTarget,
                    onValueChange = { viewModel.onDnsTargetChange(it) },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                    placeholder = { Text("e.g. google.com or 8.8.8.8", color = textSecondary, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = cyanColor, modifier = Modifier.size(18.dp))
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

                Button(
                    onClick = { viewModel.runDnsQuery(isReverse = state.dnsRecordType == "PTR") },
                    colors = ButtonDefaults.buttonColors(containerColor = cyanColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    enabled = !state.isDnsLoading
                ) {
                    if (state.isDnsLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DarkBackground, strokeWidth = 2.dp)
                    } else {
                        Text("Query", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 2. Record Type Filter Chips (Screen 11 format)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recordTypes) { rType ->
                    val isSelected = state.dnsRecordType == rType
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) cyanColor else surfaceColor)
                            .border(1.dp, if (isSelected) cyanColor else borderColor, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.onDnsTypeChange(rType)
                                viewModel.runDnsQuery(isReverse = rType == "PTR")
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = rType,
                            color = if (isSelected) DarkBackground else textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 3. Telemetry Status Card
            state.dnsResult?.let { res ->
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
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = res.resolver,
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (res.isSuccess) "Status: NOERROR (${res.records.size} records)" else "Status: ${res.errorMessage.ifBlank { "ERROR" }}",
                                color = if (res.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tealColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", res.durationMs)} ms",
                                color = tealColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            }

            // 4. DNS Records List
            val records = state.dnsResult?.records.orEmpty()
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (state.isDnsLoading) "Resolving DNS records via DoH..." else "Enter a domain and tap Query",
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
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(records) { item ->
                        DnsRecordCard(
                            item = item,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("DNS Record", item.value))
                                Toast.makeText(context, "Copied: ${item.value}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DnsRecordCard(
    item: DnsRecordItem,
    onCopy: () -> Unit
) {
    val (typeColor, typeBg) = when (item.type.uppercase()) {
        "A" -> Color(0xFF00E5FF) to Color(0xFF00E5FF).copy(alpha = 0.15f)
        "AAAA" -> Color(0xFF00C2FF) to Color(0xFF00C2FF).copy(alpha = 0.15f)
        "MX" -> Color(0xFFF59E0B) to Color(0xFFF59E0B).copy(alpha = 0.15f)
        "TXT" -> Color(0xFFD2A8FF) to Color(0xFFD2A8FF).copy(alpha = 0.15f)
        "CNAME" -> Color(0xFF10B981) to Color(0xFF10B981).copy(alpha = 0.15f)
        "NS" -> Color(0xFF79C0FF) to Color(0xFF79C0FF).copy(alpha = 0.15f)
        else -> TextSecondaryDark to DarkSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.type,
                        color = typeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.value,
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    if (item.ttl > 0) {
                        Text(
                            text = "TTL: ${item.ttl}s",
                            color = TextSecondaryDark,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Copy button
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy Record",
                    tint = NeonCyan,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
