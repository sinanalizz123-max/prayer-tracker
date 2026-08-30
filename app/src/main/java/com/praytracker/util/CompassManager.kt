package com.praytracker.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CompassManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val legacyOrientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private val _heading = MutableStateFlow(0f)
    val heading: StateFlow<Float> = _heading

    private val _accuracy = MutableStateFlow(SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
    val accuracy: StateFlow<Int> = _accuracy

    val isCompassAvailable: Boolean
        get() = rotationSensor != null || (gSensor != null && mSensor != null) || legacyOrientation != null

    private val rMat = FloatArray(9)
    private val iMat = FloatArray(9)
    private val orientation = FloatArray(3)
    private var lastGSensor = FloatArray(3)
    private var lastMSensor = FloatArray(3)
    private var hasGSensor = false
    private var hasMSensor = false

    fun start() {
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (gSensor != null && mSensor != null) {
            sensorManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (legacyOrientation != null) {
            sensorManager.registerListener(this, legacyOrientation, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        _accuracy.value = event.accuracy

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rMat, event.values)
                SensorManager.getOrientation(rMat, orientation)
                val azimuthInRadians = orientation[0]
                var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                if (azimuthInDegrees < 0) {
                    azimuthInDegrees += 360f
                }
                _heading.value = azimuthInDegrees
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastGSensor, 0, event.values.size)
                hasGSensor = true
                calculateOrientationFromMats()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMSensor, 0, event.values.size)
                hasMSensor = true
                calculateOrientationFromMats()
            }
            Sensor.TYPE_ORIENTATION -> {
                var azimuthInDegrees = event.values[0]
                if (azimuthInDegrees < 0) {
                    azimuthInDegrees += 360f
                }
                _heading.value = azimuthInDegrees
            }
        }
    }

    private fun calculateOrientationFromMats() {
        if (hasGSensor && hasMSensor) {
            if (SensorManager.getRotationMatrix(rMat, iMat, lastGSensor, lastMSensor)) {
                SensorManager.getOrientation(rMat, orientation)
                val azimuthInRadians = orientation[0]
                var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
                if (azimuthInDegrees < 0) {
                    azimuthInDegrees += 360f
                }
                _heading.value = azimuthInDegrees
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _accuracy.value = accuracy
    }
}
