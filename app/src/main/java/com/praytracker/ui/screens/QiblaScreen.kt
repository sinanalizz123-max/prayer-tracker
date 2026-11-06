package com.praytracker.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praytracker.ui.MainViewModel
import com.praytracker.ui.components.LocationPickerBottomSheet
import com.praytracker.ui.components.rememberLocationPermissionDetector
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QiblaScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val accuracy by viewModel.compassAccuracy.collectAsState()
    val qiblaAngle by viewModel.qiblaDirection.collectAsState()
    val isAvailable = viewModel.compassManager.isCompassAvailable
    val locationResolved = viewModel.locationResolved

    var showLocationPickerSheet by remember { mutableStateOf(false) }

    val requestLocation = rememberLocationPermissionDetector(
        onGranted = { viewModel.detectLocation() }
    )

    DisposableEffect(Unit) {
        viewModel.compassManager.start()
        onDispose {
            viewModel.compassManager.stop()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Qibla Direction",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (locationResolved) {
                    "${viewModel.settings.locationName} • Qibla: ${qiblaAngle.toInt()}° from North"
                } else {
                    "${viewModel.settings.locationName} • Set your location to get the Qibla direction"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            if (locationResolved) {
                Spacer(modifier = Modifier.height(8.dp))
                AlignmentStatusPill(viewModel)
            }
        }

        // Compass Visual
        if (locationResolved) {
            CompassDial(viewModel)
        } else {
            LocationNeededCard(
                onUseGps = requestLocation,
                onChooseCity = { showLocationPickerSheet = true }
            )
        }

        // Accuracy & Sensor Calibration Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (!isAvailable) {
                            "Compass sensor not detected on this device. Point device according to angle."
                        } else if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                            "Low compass accuracy. Wave device in a figure-8 pattern to calibrate."
                        } else {
                            "Hold your phone flat and away from magnetic objects for highest accuracy."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }
        }

        if (showLocationPickerSheet) {
            LocationPickerBottomSheet(
                viewModel = viewModel,
                onDismiss = { showLocationPickerSheet = false }
            )
        }
    }
}

@Composable
private fun AlignmentStatusPill(viewModel: MainViewModel) {
    // Heading + Qibla angle are read here so per-sensor-event updates recompose
    // only this small pill, not the whole screen.
    val heading by viewModel.trueHeading.collectAsState()
    val qiblaAngle by viewModel.qiblaDirection.collectAsState()

    // Difference between device heading and Qibla
    val diff = normalizeAngle(qiblaAngle - heading)
    val isAligned = abs(diff) <= 3f || abs(diff - 360f) <= 3f

    // Alignment Status Pill
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isAligned) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("qibla_alignment_status")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAligned) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Facing the Kaaba",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                val turnDirection = if (diff in 0f..180f || diff < -180f) "Turn Right" else "Turn Left"
                Text(
                    text = turnDirection,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}
@Composable
private fun CompassDial(viewModel: MainViewModel) {
    val heading by viewModel.trueHeading.collectAsState()
    val qiblaAngle by viewModel.qiblaDirection.collectAsState()

    val diff = normalizeAngle(qiblaAngle - heading)
    val isAligned = abs(diff) <= 3f || abs(diff - 360f) <= 3f

    val rotationState = remember { Animatable(0f) }
    LaunchedEffect(heading) {
        val current = rotationState.value
        val delta = shortestAngleDelta(current, -heading)
        rotationState.animateTo(current + delta, animationSpec = tween(durationMillis = 200))
    }

    val alignmentColor by animateColorAsState(
        targetValue = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        label = "AlignmentColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(320.dp)
            .testTag("compass_dial")
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        val surfaceColor = MaterialTheme.colorScheme.surface
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        // Outer dial with North marker and degree ticks - Enhanced Material 3 Expressive
        Canvas(
            modifier = Modifier
                .size(300.dp)
                .rotate(rotationState.value)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width / 2 - 16.dp.toPx()
            val innerRadius = size.width / 2 - 80.dp.toPx()

            // Outer Ring - Gradient for Expressive look
            val outerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    primaryColor.copy(alpha = 0.1f),
                    primaryColor.copy(alpha = 0.2f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            )
            
            drawCircle(
                brush = outerBrush,
                radius = outerRadius,
                style = Stroke(width = 16.dp.toPx())
            )

            // Main Dial Background with subtle pattern
            drawCircle(
                color = surfaceColor,
                radius = innerRadius
            )

            // Concentric circles for depth
            drawCircle(
                color = onSurfaceColor.copy(alpha = 0.05f),
                radius = innerRadius - 20.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = onSurfaceColor.copy(alpha = 0.05f),
                radius = innerRadius - 40.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            // Cardinal Directions with labels
            val directions = listOf(
                Pair(0f, "N"),
                Pair(90f, "E"),
                Pair(180f, "S"),
                Pair(270f, "W")
            )
            
            for ((angleDeg, label) in directions) {
                val angleRad = Math.toRadians(angleDeg.toDouble() - 90)
                val textRadius = innerRadius - 24.dp.toPx()
                val x = (center.x + textRadius * cos(angleRad)).toFloat()
                val y = (center.y + textRadius * sin(angleRad)).toFloat()
                
                drawContext.canvas.drawText(
                    label,
                    x - 6.dp.toPx(),
                    y + 6.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 24.sp.toPx()
                        color = primaryColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
                    }
                )
            }

            // Degree Ticks - More elegant
            for (i in 0 until 36) {
                val angleDeg = i * 10f
                val angleRad = Math.toRadians(angleDeg.toDouble() - 90)
                val isMajor = i % 6 == 0

                val tickLength = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                val startRadius = innerRadius - tickLength
                val startX = (center.x + startRadius * cos(angleRad)).toFloat()
                val startY = (center.y + startRadius * sin(angleRad)).toFloat()
                val endX = (center.x + innerRadius * cos(angleRad)).toFloat()
                val endY = (center.y + innerRadius * sin(angleRad)).toFloat()

                drawLine(
                    color = if (i == 0) primaryColor else onSurfaceColor.copy(alpha = if (isMajor) 0.6f else 0.2f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.5.dp.toPx()
                )
            }

            // Qibla Indicator - Enhanced design with Kaaba symbol
            rotate(qiblaAngle, pivot = center) {
                val qiblaRad = Math.toRadians(-90.0)
                val needleLength = innerRadius - 30.dp.toPx()
                val qiblaX = (center.x + needleLength * cos(qiblaRad)).toFloat()
                val qiblaY = (center.y + needleLength * sin(qiblaRad)).toFloat()

                // Enhanced Kaaba Needle with gradient
                val needlePath = Path().apply {
                    moveTo(center.x, center.y)
                    lineTo(qiblaX - 12.dp.toPx(), qiblaY)
                    lineTo(qiblaX, qiblaY + 20.dp.toPx())
                    lineTo(qiblaX + 12.dp.toPx(), qiblaY)
                    close()
                }
                
                drawPath(
                    path = needlePath,
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor),
                        start = Offset(center.x, center.y),
                        end = Offset(qiblaX, qiblaY)
                    )
                )

                // Kaaba symbol - Cube shape
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(qiblaX - 12.dp.toPx(), qiblaY - 12.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(24.dp.toPx(), 24.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
                
                // Inner cube highlight
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f),
                    topLeft = Offset(qiblaX - 8.dp.toPx(), qiblaY - 8.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(16.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                )
            }
        }

        // Center Heading Badge - Enhanced with Material 3 Expressive
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                )
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${heading.toInt()}°",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isAligned) primaryColor else alignmentColor
                )
                Text(
                    text = "Qibla",
                    style = MaterialTheme.typography.labelSmall,
                    color = alignmentColor.copy(alpha = 0.7f)
                )
            }
        }

        // Fixed Top Device Pointer - Enhanced
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(28.dp)
            )
        }

        // Qibla Direction Text at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Qibla: ${qiblaAngle.toInt()}°",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = primaryColor
            )
        }
    }
}


private fun shortestAngleDelta(from: Float, to: Float): Float {
    var diff = (to - from) % 360f
    if (diff > 180f) diff -= 360f
    if (diff < -180f) diff += 360f
    return diff
}

private fun normalizeAngle(angle: Float): Float {
    var a = angle % 360f
    if (a < 0f) a += 360f
    return a
}

@Composable
private fun LocationNeededCard(
    onUseGps: () -> Unit,
    onChooseCity: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Location required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enable GPS or choose a city to compute the exact Qibla direction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onUseGps,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Current Location")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onChooseCity,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose a City")
            }
        }
    }
}
