package com.dping.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.R
import com.dping.app.ui.i18n.AppStrings
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DPingTopBar(
    title: String = "D Ping",
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                // Vector mini logo brand icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF000000))
                        .border(1.dp, NeonTeal.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dping_icon),
                        contentDescription = "D Ping Logo",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "D Ping",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = NeonTeal
                        )
                    )
                    Text(
                        text = "NETWORK TOOLKIT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.2.sp,
                            color = TextSecondaryDark
                        )
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Menu",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground
        )
    )
}

enum class MainTab(val title: String, val icon: ImageVector) {
    PING("Diagnostic", Icons.Default.Bolt),
    TOOLS("Tools Hub", Icons.Default.Handyman),
    MY_IP("My Network", Icons.Default.Public),
    HISTORY("History", Icons.Default.History);

    fun getLocalizedTitle(strings: AppStrings): String {
        return when (this) {
            PING -> strings.tabDiagnostic
            TOOLS -> strings.tabTools
            MY_IP -> strings.tabMyNetwork
            HISTORY -> strings.tabHistory
        }
    }
}

@Composable
fun DPingBottomNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) NeonCyan else TextSecondaryDark,
                    label = "iconTint"
                )
                val textTint by animateColorAsState(
                    targetValue = if (isSelected) NeonCyan else TextSecondaryDark,
                    label = "textTint"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) NeonTeal.copy(alpha = 0.4f) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = tab.getLocalizedTitle(LocalAppStrings.current),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = textTint,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedTabBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MainTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) DarkSurfaceVariant else Color.Transparent
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.getLocalizedTitle(LocalAppStrings.current),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NeonTeal else TextSecondaryDark,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedToggleSwitch(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onOptionSelected(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) NeonCyan else TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticOutputToggleButton(
    isChart: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onToggle(!isChart) },
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // CMD Console side
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (!isChart) DarkSurfaceVariant else Color.Transparent)
                    .border(
                        width = if (!isChart) 1.dp else 0.dp,
                        color = if (!isChart) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onToggle(false) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "CMD Console",
                        tint = if (!isChart) NeonCyan else TextSecondaryDark,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LocalAppStrings.current.toggleConsole,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (!isChart) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isChart) NeonCyan else TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Live Chart side
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isChart) DarkSurfaceVariant else Color.Transparent)
                    .border(
                        width = if (isChart) 1.dp else 0.dp,
                        color = if (isChart) NeonTeal.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onToggle(true) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Live Chart",
                        tint = if (isChart) NeonTeal else TextSecondaryDark,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = LocalAppStrings.current.toggleChart,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isChart) FontWeight.Bold else FontWeight.Medium,
                            color = if (isChart) NeonTeal else TextSecondaryDark,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun DPingFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) NeonGradient else androidx.compose.ui.graphics.SolidColor(DarkSurfaceVariant))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    if (LocalDPingPalette.current.isDark) Color(0xFF070D15) else Color.White
                } else TextMutedDark,
                fontSize = 15.sp
            )
        )
    }
}

@Composable
fun DPingOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = NeonTeal,
    textColor: Color = NeonTeal
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                fontSize = 15.sp
            )
        )
    }
}

@Composable
fun DPingInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = NeonTeal,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark
            ),
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            singleLine = true
        )
    }
}
