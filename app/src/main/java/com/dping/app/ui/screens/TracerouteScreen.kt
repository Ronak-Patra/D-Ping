package com.dping.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.network.traceroute.HopInfo
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.PingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracerouteScreen(
    viewModel: PingViewModel,
    onBackClick: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val destinationReached = remember(state.hops) {
        state.hops.any { it.isDestinationReached }
    }

    val bestRtt = remember(state.hops) {
        state.hops.filter { !it.isTimeout && it.rttMs > 0 }.minOfOrNull { it.rttMs } ?: 0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Traceroute",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = TextPrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            // Target Input Bar & Action Button (Screen 4 exact layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.target,
                    onValueChange = { viewModel.onTargetChange(it) },
                    placeholder = { Text("google.com", color = TextSecondaryDark, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
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
                        if (!state.isTracerouteRunning) viewModel.startTraceroute()
                    }),
                    trailingIcon = {
                        if (state.target.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onTargetChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )

                if (state.isTracerouteRunning) {
                    Button(
                        onClick = { viewModel.stopAllTests() },
                        colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Stop", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.startTraceroute()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Start", color = Color(0xFF070D15), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hops Table Container (Screen 4)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Table Column Headers: Hop | IP Address / Hostname | RTT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hop",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp)
                        )
                        Text(
                            text = "IP Address / Hostname",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "RTT",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(55.dp)
                        )
                    }

                    HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))

                    if (state.hops.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (state.isTracerouteRunning) "Probing network route to ${state.target}..." else "Enter a target and press Start",
                                color = TextSecondaryDark,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.hops) { hop ->
                                HopTableRow(hop = hop)
                            }
                        }
                    }

                    // Green Destination Reached Banner (Screen 4)
                    if (destinationReached) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonTeal.copy(alpha = 0.15f))
                                .border(1.dp, NeonTeal, RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DESTINATION REACHED",
                                    color = NeonTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.1.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route Information Summary Card (Screen 4 exact layout)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Route Information",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    RouteInfoRow("Total Hops", "${state.hops.size}")
                    RouteInfoRow("Destination", state.target)
                    RouteInfoRow(
                        "Status",
                        if (destinationReached) "Reached" else if (state.isTracerouteRunning) "In Progress..." else "Idle",
                        if (destinationReached) NeonTeal else if (state.isTracerouteRunning) NeonCyan else TextSecondaryDark
                    )
                    RouteInfoRow(
                        "Best RTT",
                        if (bestRtt > 0) "${String.format(java.util.Locale.US, "%.0f", bestRtt)} ms" else "--",
                        NeonCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun HopTableRow(hop: HopInfo) {
    val isDest = hop.isDestinationReached
    val textColor = if (isDest) NeonTeal else if (hop.isTimeout) TextSecondaryDark else TextPrimaryDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDest) NeonTeal.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hop Number
        Text(
            text = "${hop.hopNumber}",
            color = if (isDest) NeonTeal else NeonCyan,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp)
        )

        // Hostname / IP
        val hostDisplay = if (hop.isTimeout) {
            "* Request timed out"
        } else if (hop.hostname.isNotBlank() && hop.hostname != hop.ip) {
            "${hop.ip} (${hop.hostname})"
        } else {
            hop.ip
        }

        Text(
            text = hostDisplay,
            color = textColor,
            fontWeight = if (isDest) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        // RTT
        val rttDisplay = if (hop.isTimeout) "--" else "${String.format(java.util.Locale.US, "%.0f", hop.rttMs)} ms"
        Text(
            text = rttDisplay,
            color = if (isDest) NeonTeal else TextSecondaryDark,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(55.dp)
        )
    }
}

@Composable
private fun RouteInfoRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimaryDark
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondaryDark, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
