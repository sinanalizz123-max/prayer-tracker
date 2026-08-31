package com.praytracker.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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

    // Track the dial rotation continuously (unwrapped) and always animate the
    // shortest angular path so crossing 359° -> 1° rotates by -2° instead of ~358°.
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
            .size(300.dp)
            .testTag("compass_dial")
    ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceColor = MaterialTheme.colorScheme.surface
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface

            // Outer dial with North marker and degree ticks
            Canvas(
                modifier = Modifier
                    .size(280.dp)
                    .rotate(rotationState.value)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 12.dp.toPx()

                // Background Circle
                drawCircle(
                    color = surfaceColor,
                    radius = radius
                )

                // Dial Ring
                drawCircle(
                    color = onSurfaceColor.copy(alpha = 0.15f),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 36 Degree Ticks
                for (i in 0 until 36) {
                    val angleDeg = i * 10f
                    val angleRad = Math.toRadians(angleDeg.toDouble() - 90)
                    val isMajor = i % 9 == 0 // N, E, S, W

                    val tickLength = if (isMajor) 14.dp.toPx() else 6.dp.toPx()
                    val startX = (center.x + (radius - tickLength) * cos(angleRad)).toFloat()
                    val startY = (center.y + (radius - tickLength) * sin(angleRad)).toFloat()
                    val endX = (center.x + radius * cos(angleRad)).toFloat()
                    val endY = (center.y + radius * sin(angleRad)).toFloat()

                    drawLine(
                        color = if (i == 0) Color.Red else onSurfaceColor.copy(alpha = if (isMajor) 0.8f else 0.3f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()
                    )
                }

                // Qibla Indicator Line & Kaaba Badge
                rotate(qiblaAngle, pivot = center) {
                    val qiblaRad = Math.toRadians(-90.0)
                    val qiblaX = (center.x + (radius - 24.dp.toPx()) * cos(qiblaRad)).toFloat()
                    val qiblaY = (center.y + (radius - 24.dp.toPx()) * sin(qiblaRad)).toFloat()

                    // Gold / Primary Kaaba Needle
                    val path = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(qiblaX - 8.dp.toPx(), qiblaY + 16.dp.toPx())
                        lineTo(qiblaX, qiblaY)
                        lineTo(qiblaX + 8.dp.toPx(), qiblaY + 16.dp.toPx())
                        close()
                    }
                    drawPath(path, color = primaryColor)

                    // Kaaba symbol circle at tip
                    drawCircle(
                        color = primaryColor,
                        radius = 8.dp.toPx(),
                        center = Offset(qiblaX, qiblaY)
                    )
                }
            }

            // Center Heading Badge (Fixed device orientation)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${heading.toInt()}°",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = alignmentColor
                    )
                }
            }

            // Fixed Top Device Pointer (North alignment marker)
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = null,
                tint = alignmentColor,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.TopCenter)
            )
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
