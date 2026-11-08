package com.dping.app.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.ui.components.DPingOutlinedButton
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*
import com.dping.app.ui.viewmodels.SettingsViewModel

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val category: String // "INDIAN" or "GLOBAL"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    var showResetDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("ALL") } // "ALL", "INDIAN", "GLOBAL"

    // Pre-extract theme colors in Composable scope
    val cyanColor = NeonCyan
    val tealColor = NeonTeal
    val textPrimary = TextPrimaryDark
    val textSecondary = TextSecondaryDark
    val surfaceColor = DarkSurface
    val surfaceVariant = DarkSurfaceVariant
    val borderColor = DarkBorder

    val languages = remember {
        listOf(
            // Indian Languages (14)
            LanguageOption("hi", "Hindi", "हिन्दी", "🇮🇳", "INDIAN"),
            LanguageOption("bn", "Bengali", "বাংলা", "🇮🇳", "INDIAN"),
            LanguageOption("pa", "Punjabi", "ਪੰਜਾਬੀ", "🇮🇳", "INDIAN"),
            LanguageOption("mr", "Marathi", "मराठी", "🇮🇳", "INDIAN"),
            LanguageOption("gu", "Gujarati", "ગુજરાતી", "🇮🇳", "INDIAN"),
            LanguageOption("ta", "Tamil", "தமிழ்", "🇮🇳", "INDIAN"),
            LanguageOption("te", "Telugu", "తెలుగు", "🇮🇳", "INDIAN"),
            LanguageOption("kn", "Kannada", "ಕನ್ನಡ", "🇮🇳", "INDIAN"),
            LanguageOption("ml", "Malayalam", "മലയാളം", "🇮🇳", "INDIAN"),
            LanguageOption("or", "Odia", "ଓଡ଼ିଆ", "🇮🇳", "INDIAN"),
            LanguageOption("as", "Assamese", "অসমীয়া", "🇮🇳", "INDIAN"),
            LanguageOption("ur", "Urdu", "اردو", "🇮🇳", "INDIAN"),
            LanguageOption("ne", "Nepali", "नेपाली", "🇳🇵", "INDIAN"),
            LanguageOption("sa", "Sanskrit", "संस्कृतम्", "🇮🇳", "INDIAN"),

            // Global Languages (9)
            LanguageOption("en", "English", "English", "🇺🇸", "GLOBAL"),
            LanguageOption("es", "Spanish", "Español", "🇪🇸", "GLOBAL"),
            LanguageOption("zh", "Chinese", "中文", "🇨🇳", "GLOBAL"),
            LanguageOption("de", "German", "Deutsch", "🇩🇪", "GLOBAL"),
            LanguageOption("fr", "French", "Français", "🇫🇷", "GLOBAL"),
            LanguageOption("ru", "Russian", "Русский", "🇷🇺", "GLOBAL"),
            LanguageOption("he", "Hebrew", "עברית", "🇮🇱", "GLOBAL"),
            LanguageOption("tr", "Turkish", "Türkçe", "🇹🇷", "GLOBAL"),
            LanguageOption("ko", "Korean", "한국어", "🇰🇷", "GLOBAL")
        )
    }

    val filteredLanguages = remember(selectedCategory, languages) {
        when (selectedCategory) {
            "INDIAN" -> languages.filter { it.category == "INDIAN" }
            "GLOBAL" -> languages.filter { it.category == "GLOBAL" }
            else -> languages
        }
    }

    val packetSizes = listOf(32, 56, 64, 128, 256, 512, 1024)
    val intervalOptions = listOf(
        Pair(200L, "200ms"),
        Pair(500L, "500ms"),
        Pair(1000L, "1.0s"),
        Pair(2000L, "2.0s"),
        Pair(5000L, "5.0s")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.settingsTitle,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Diagnostic Configuration & Regional Vault",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Section 1: Ping Engine Diagnostic Tuning
            item {
                CyberSectionCard(
                    title = "PING DIAGNOSTIC ENGINE",
                    icon = Icons.Default.Tune,
                    accentColor = cyanColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Packet Size Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Payload Packet Size", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${state.packetSize} Bytes", color = cyanColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(packetSizes) { size ->
                                    val isSelected = state.packetSize == size
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) cyanColor.copy(alpha = 0.2f) else surfaceVariant,
                                        border = BorderStroke(1.dp, if (isSelected) cyanColor else borderColor),
                                        modifier = Modifier.clickable { viewModel.setPacketSize(size) }
                                    ) {
                                        Text(
                                            text = "${size}B",
                                            color = if (isSelected) cyanColor else textSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Ping Interval Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ping Interval Delay", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("${state.intervalMs} ms", color = tealColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(intervalOptions) { (ms, label) ->
                                    val isSelected = state.intervalMs == ms
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) tealColor.copy(alpha = 0.2f) else surfaceVariant,
                                        border = BorderStroke(1.dp, if (isSelected) tealColor else borderColor),
                                        modifier = Modifier.clickable { viewModel.setIntervalMs(ms) }
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) tealColor else textSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        // Toggle: DNS Resolve Hostname
                        CyberSwitchRow(
                            title = "DNS Reverse Hostname Resolution",
                            description = "Resolve target IP addresses to human-readable domain names",
                            checked = state.resolveHostname,
                            onCheckedChange = { viewModel.setResolveHostname(it) },
                            accentColor = cyanColor
                        )

                        // Toggle: Background Ping
                        CyberSwitchRow(
                            title = "Background Execution",
                            description = "Keep ping diagnostic running while app is minimized",
                            checked = state.keepRunningBg,
                            onCheckedChange = { viewModel.setKeepRunningBg(it) },
                            accentColor = tealColor
                        )

                        // Toggle: Newest First
                        CyberSwitchRow(
                            title = strings.newestFirst,
                            description = "Show latest probe responses at the top of console",
                            checked = state.newestFirst,
                            onCheckedChange = { viewModel.setNewestFirst(it) },
                            accentColor = cyanColor
                        )
                    }
                }
            }

            // Section 2: UI & Theme Appearance
            item {
                CyberSectionCard(
                    title = strings.themeSection.uppercase(),
                    icon = Icons.Default.Palette,
                    accentColor = Color(0xFFFFB800)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CyberThemeOption(
                            title = "Dark Cyberpunk (${strings.themeDark})",
                            subtitle = "Deep void background, high-contrast neon cyan & teal",
                            isSelected = state.themeMode == AppThemeMode.DARK,
                            onClick = { viewModel.setTheme(AppThemeMode.DARK) },
                            accentColor = cyanColor
                        )

                        CyberThemeOption(
                            title = "Light Minimal (${strings.themeLight})",
                            subtitle = "Clean high-visibility daylight interface",
                            isSelected = state.themeMode == AppThemeMode.LIGHT,
                            onClick = { viewModel.setTheme(AppThemeMode.LIGHT) },
                            accentColor = Color(0xFF3B82F6)
                        )

                        CyberThemeOption(
                            title = "System Default (${strings.themeAuto})",
                            subtitle = "Automatically align with Android OS day/night mode",
                            isSelected = state.themeMode == AppThemeMode.AUTO,
                            onClick = { viewModel.setTheme(AppThemeMode.AUTO) },
                            accentColor = textSecondary
                        )
                    }
                }
            }

            // Section 3: Multi-Language & Regional (Indian Languages & Global Languages)
            item {
                CyberSectionCard(
                    title = "${strings.languageSection.uppercase()} & REGIONAL (I18N)",
                    icon = Icons.Default.Language,
                    accentColor = Color(0xFF10B981)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Category Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkBackground)
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("ALL", "All (${languages.size})", Color(0xFF10B981)),
                                Triple("INDIAN", "Indian (14)", cyanColor),
                                Triple("GLOBAL", "Global (9)", tealColor)
                            ).forEach { (cat, label, accent) ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) accent.copy(alpha = 0.25f) else Color.Transparent)
                                        .clickable { selectedCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) accent else textSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Languages List
                        filteredLanguages.forEach { lang ->
                            val isSelected = state.selectedLanguage.equals(lang.code, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.12f) else surfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF10B981) else borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setLanguage(lang.code) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(text = lang.flagEmoji, fontSize = 22.sp)
                                        Column {
                                            Text(
                                                text = lang.nativeName,
                                                color = textPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${lang.name} • ${if (lang.category == "INDIAN") "Indian" else "Global"}",
                                                color = textSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = DarkBackground,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: About & System Diagnostics
            item {
                CyberSectionCard(
                    title = "ABOUT D PING",
                    icon = Icons.Default.Info,
                    accentColor = textSecondary
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Application Version", color = textSecondary, fontSize = 12.sp)
                            Text("v1.0.0 (Cyber Release)", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Diagnostic Engine", color = textSecondary, fontSize = 12.sp)
                            Text("Native ICMP & DoH", color = cyanColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("UI Architecture", color = textSecondary, fontSize = 12.sp)
                            Text("Jetpack Compose M3", color = tealColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        HorizontalDivider(color = borderColor, thickness = 1.dp)

                        DPingOutlinedButton(
                            text = "Reset All Preferences to Defaults",
                            onClick = { showResetDialog = true },
                            borderColor = DestructiveRed,
                            textColor = DestructiveRed,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Preferences?", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "All diagnostic settings (packet size, interval, theme, and switches) will be restored to their factory defaults.",
                    color = textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reset Defaults", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = surfaceColor,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun CyberSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = title,
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
            content()
        }
    }
}

@Composable
private fun CyberSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = TextSecondaryDark, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkBackground,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = TextMutedDark,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
private fun CyberThemeOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else DarkSurfaceVariant,
        border = BorderStroke(1.dp, if (isSelected) accentColor else DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimaryDark,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accentColor,
                    unselectedColor = TextMutedDark
                )
            )
        }
    }
}
