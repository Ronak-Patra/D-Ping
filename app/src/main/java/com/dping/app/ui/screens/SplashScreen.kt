package com.dping.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dping.app.R
import com.dping.app.ui.theme.NeonCyan
import com.dping.app.ui.theme.NeonTeal
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 2.5 seconds splash display timer
    LaunchedEffect(Unit) {
        delay(2500L)
        onTimeout()
    }

    // Intro entrance animation (scale + alpha)
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val entranceAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "entranceAlpha"
    )

    val entranceScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "entranceScale"
    )

    // Continuous subtle pulsing neon aura
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Beam travel animation for the bottom progress indicator
    val beamOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beamOffset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle cyber radar background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val gridColor = Color(0xFF00E5FF).copy(alpha = 0.04f)

            // Faint concentric circles
            drawCircle(gridColor, radius = 80.dp.toPx(), center = center, style = Stroke(width = 1f))
            drawCircle(gridColor, radius = 160.dp.toPx(), center = center, style = Stroke(width = 1f))
            drawCircle(gridColor, radius = 240.dp.toPx(), center = center, style = Stroke(width = 1f))

            // Crosshairs
            drawLine(gridColor, Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 1f)
        }

        // Center Brand Identity
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(entranceAlpha)
                .scale(entranceScale * pulseScale)
                .padding(horizontal = 24.dp)
        ) {
            // Glowing Backdrop aura
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(260.dp)
            ) {
                Canvas(modifier = Modifier.size(240.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.25f * pulseGlow),
                                Color(0xFF00C2FF).copy(alpha = 0.08f * pulseGlow),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2f
                    )
                }

                // Official D Ping High-Res Logo
                Image(
                    painter = painterResource(id = R.drawable.ic_dping_logo),
                    contentDescription = "D Ping Logo",
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(32.dp))
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Brand Title
            Text(
                text = "D Ping",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline
            Text(
                text = "NEXT-GEN NETWORK INTELLIGENCE",
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.5.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Sleek Cyber Progress Indicator
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(Color(0xFF0A1522))
            ) {
                // Traveling glowing pulse beam
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(60.dp)
                        .offset(x = (180.dp - 60.dp) * beamOffset)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF00E5FF),
                                    Color.White,
                                    Color(0xFF00E5FF),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}
