package com.praytracker.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.praytracker.PrayerTrackerApp
import com.praytracker.R
import com.praytracker.qibla.QiblaCalculator
import com.praytracker.ui.nav.ScreenScaffold
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as PrayerTrackerApp

    var latitude by remember { mutableStateOf(21.4225) }
    var longitude by remember { mutableStateOf(39.8262) }
    LaunchedEffect(Unit) {
        val s = app.container.settingsRepository.snapshot()
        latitude = s.latitude
        longitude = s.longitude
    }

    var currentAzimuth by remember { mutableFloatStateOf(-1f) }
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotation = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotation, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotation, orientation)
                    currentAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(
            listener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            SensorManager.SENSOR_DELAY_UI,
        )
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val qiblaBearing = QiblaCalculator.bearing(latitude, longitude)
    val relative = if (currentAzimuth >= 0) qiblaBearing - currentAzimuth else qiblaBearing

    ScreenScaffold(title = stringResource(R.string.more_qibla), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.size(16.dp))

            CompassCircle(relativeDegrees = relative, qiblaBearing = qiblaBearing)

            Spacer(Modifier.size(24.dp))

            Text(
                text = stringResource(R.string.qibla_heading, qiblaBearing),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = stringResource(R.string.qibla_accuracy_low),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (currentAzimuth < 0) {
                Spacer(Modifier.size(8.dp))
                Text(
                    "Moving your device in a slight figure of eight helps calibrate the compass.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompassCircle(relativeDegrees: Float, qiblaBearing: Double) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(200.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension / 2f - 10.dp.toPx()
            val stroke = 4.dp.toPx()

            drawCircle(
                color = MaterialTheme.colorScheme.surfaceVariant,
                radius = r,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = MaterialTheme.colorScheme.primary,
                radius = r - 2.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = stroke),
            )

            // North tick
            drawLine(
                color = Color.Red,
                start = Offset(cx, cy - r + 8.dp.toPx()),
                end = Offset(cx, cy + 4.dp.toPx()),
                strokeWidth = stroke,
            )
            drawLine(
                color = Color(0xFF2E7D56),
                start = Offset(cx, cy + r - 8.dp.toPx()),
                end = Offset(cx, cy - 4.dp.toPx()),
                strokeWidth = stroke,
            )

            // Qibla needle rotated by relative degrees.
            // Screen +y points down; bearing θ from north (up) towards east (+x):
            //   x = cx + n*sin(θ), y = cy - n*cos(θ)
            val angleRad = Math.toRadians(relativeDegrees.toDouble())
            val needleLength = r - 14.dp.toPx()
            val sinA = sin(angleRad).toFloat()
            val cosA = cos(angleRad).toFloat()
            drawLine(
                color = MaterialTheme.colorScheme.primary,
                start = Offset(cx, cy),
                end = Offset(cx + needleLength * sinA, cy - needleLength * cosA),
                strokeWidth = 8.dp.toPx(),
            )
            drawLine(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                start = Offset(cx, cy),
                end = Offset(cx - needleLength * sinA, cy + needleLength * cosA),
                strokeWidth = 4.dp.toPx(),
            )
        }
    }

    Text(
        text = "Qibla",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp),
    )
}