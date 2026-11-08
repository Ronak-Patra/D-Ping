package com.dping.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.R
import com.dping.app.ui.i18n.LocalAppStrings
import com.dping.app.ui.theme.*

data class HomeToolItem(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPing: () -> Unit,
    onNavigateToTraceroute: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToLanDiscovery: () -> Unit,
    onNavigateToIpRange: () -> Unit,
    onNavigateToMyIp: () -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val strings = LocalAppStrings.current

    val tools = listOf(
        HomeToolItem(strings.btnPing, Icons.Default.Bolt, NeonCyan, onNavigateToPing),
        HomeToolItem(strings.btnTrace, Icons.Default.Route, NeonTeal, onNavigateToTraceroute),
        HomeToolItem(strings.speedTestTitle, Icons.Default.Speed, NeonCyan, onNavigateToSpeedTest),
        HomeToolItem(strings.lanDiscoveryTitle, Icons.Default.Devices, NeonTeal, onNavigateToLanDiscovery),
        HomeToolItem(strings.ipRangeTitle, Icons.Default.Radar, NeonCyan, onNavigateToIpRange),
        HomeToolItem(strings.drawerMyIp, Icons.Default.Public, NeonTeal, onNavigateToMyIp),
        HomeToolItem(strings.tabTools, Icons.Default.Handyman, NeonCyan, onNavigateToTools),
        HomeToolItem(strings.tabHistory, Icons.Default.History, NeonTeal, onNavigateToHistory),
        HomeToolItem(strings.settingsTitle, Icons.Default.Settings, TextSecondaryDark, onNavigateToSettings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_dping_icon),
                            contentDescription = "Logo",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "D Ping",
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = strings.settingsTitle,
                            tint = TextPrimaryDark
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
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Glowing Authentic Brand Logo
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF000000))
                    .border(1.5.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_dping_icon),
                    contentDescription = "D Ping Brand Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Title & Tagline
            Text(
                text = "D Ping",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimaryDark,
                    fontSize = 32.sp,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ping  •  Trace  •  Test  •  Discover",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NeonTeal,
                    fontSize = 13.sp,
                    letterSpacing = 1.1.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Your Complete Network Utility Toolkit",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 3x3 Tool Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (rowIndex in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (colIndex in 0 until 3) {
                            val itemIndex = rowIndex * 3 + colIndex
                            val tool = tools[itemIndex]

                            HomeToolCard(
                                item = tool,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cyber Wireframe Globe Graphic
            val globeColor = NeonCyan.copy(alpha = 0.25f)
            val dotColor = NeonTeal.copy(alpha = 0.6f)
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                val cx = size.width / 2
                val cy = size.height + 40.dp.toPx()
                val r = size.width * 0.48f

                // Concentric wireframe arcs
                drawCircle(
                    color = globeColor,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawCircle(
                    color = globeColor.copy(alpha = 0.15f),
                    radius = r * 0.75f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawCircle(
                    color = globeColor.copy(alpha = 0.1f),
                    radius = r * 0.5f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Network node dots
                val dotPositions = listOf(
                    Offset(cx - 80.dp.toPx(), 20.dp.toPx()),
                    Offset(cx - 30.dp.toPx(), 10.dp.toPx()),
                    Offset(cx + 35.dp.toPx(), 12.dp.toPx()),
                    Offset(cx + 85.dp.toPx(), 22.dp.toPx()),
                    Offset(cx, 35.dp.toPx()),
                    Offset(cx - 45.dp.toPx(), 45.dp.toPx()),
                    Offset(cx + 50.dp.toPx(), 42.dp.toPx())
                )

                for (pos in dotPositions) {
                    drawCircle(color = dotColor, radius = 2.5.dp.toPx(), center = pos)
                }

                // Connecting network lines
                for (i in 0 until dotPositions.size - 1) {
                    drawLine(
                        color = globeColor.copy(alpha = 0.3f),
                        start = dotPositions[i],
                        end = dotPositions[i + 1],
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeToolCard(
    item: HomeToolItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .clickable { item.onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Pill
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, item.accentColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = item.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    fontSize = 11.sp
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
