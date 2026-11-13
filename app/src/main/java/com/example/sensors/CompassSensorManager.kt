package com.example.sensors

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CompassReading(
    val azimuthDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val magneticFieldMicroTesla: Float = 45f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasSensors: Boolean = true,
    val sensorModeLabel: String = "مقياس التسارع + المغناطيسية"
)

class CompassSensorManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun observeAzimuth(latitude: Double, longitude: Double): Flow<CompassReading> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val useAccelMag = accelerometer != null && magnetometer != null
        if (!useAccelMag && rotationSensor == null) {
            trySend(
                CompassReading(
                    azimuthDegrees = 0f,
                    accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE,
                    hasSensors = false,
                    sensorModeLabel = "لا توجد مستشعرات"
                )
            )
            awaitClose { }
            return@callbackFlow
        }

        val geomagneticField = GeomagneticField(
            latitude.toFloat(),
            longitude.toFloat(),
            0f,
            System.currentTimeMillis()
        )
        val declination = geomagneticField.declination

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnetic = false
        var magneticStrengthUt = 45f

        var smoothedSin = 0.0
        var smoothedCos = 1.0
        var isFirstAzimuth = true
        var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

        val sensorMode = if (useAccelMag) {
            "مقياس التسارع والمغناطيسية"
        } else {
            "مستشعر الدوران المدمج"
        }

        fun emitOrientation(orientationRad: FloatArray) {
            val rawAzimuth = Math.toDegrees(orientationRad[0].toDouble()).toFloat()
            val pitch = Math.toDegrees(orientationRad[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientationRad[2].toDouble()).toFloat()

            val trueNorth = ((rawAzimuth + declination) % 360f + 360f) % 360f
            val rad = Math.toRadians(trueNorth.toDouble())
            if (isFirstAzimuth) {
                smoothedSin = sin(rad)
                smoothedCos = cos(rad)
                isFirstAzimuth = false
            } else {
                val alpha = 0.16
                smoothedSin = smoothedSin * (1.0 - alpha) + sin(rad) * alpha
                smoothedCos = smoothedCos * (1.0 - alpha) + cos(rad) * alpha
            }
            val smoothDeg = ((Math.toDegrees(atan2(smoothedSin, smoothedCos)) % 360.0) + 360.0) % 360.0
            trySend(
                CompassReading(
                    azimuthDegrees = smoothDeg.toFloat(),
                    pitchDegrees = pitch,
                    rollDegrees = roll,
                    magneticFieldMicroTesla = magneticStrengthUt,
                    accuracy = currentAccuracy,
                    hasSensors = true,
                    sensorModeLabel = sensorMode
                )
            )
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        if (!hasGravity) {
                            System.arraycopy(event.values, 0, gravity, 0, 3)
                            hasGravity = true
                        } else {
                            val alpha = 0.18f
                            gravity[0] += alpha * (event.values[0] - gravity[0])
                            gravity[1] += alpha * (event.values[1] - gravity[1])
                            gravity[2] += alpha * (event.values[2] - gravity[2])
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        if (!hasMagnetic) {
                            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                            hasMagnetic = true
                        } else {
                            val alpha = 0.20f
                            geomagnetic[0] += alpha * (event.values[0] - geomagnetic[0])
                            geomagnetic[1] += alpha * (event.values[1] - geomagnetic[1])
                            geomagnetic[2] += alpha * (event.values[2] - geomagnetic[2])
                        }
                        magneticStrengthUt = sqrt(
                            event.values[0] * event.values[0] +
                                event.values[1] * event.values[1] +
                                event.values[2] * event.values[2]
                        )
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        if (!useAccelMag) {
                            val rotationMatrix = FloatArray(9)
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                            val orientation = FloatArray(3)
                            SensorManager.getOrientation(rotationMatrix, orientation)
                            emitOrientation(orientation)
                        }
                    }
                }

                if (useAccelMag && hasGravity && hasMagnetic) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        emitOrientation(orientation)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    currentAccuracy = accuracy
                }
            }
        }

        if (useAccelMag) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        } else if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
