package com.metricmind.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import com.metricmind.domain.model.SleepEstimate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * On-demand access to body sensors. Everything runs on-device; nothing is
 * sampled in the background and nothing leaves the device.
 */
@Singleton
class BodySensors @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager: SensorManager
        get() = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun hasHeartRateSensor(): Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null

    fun hasTemperatureSensor(): Boolean =
        sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null

    /**
     * Waits for a stable heart-rate reading (bpm), or null if no sensor /
     * no reading within [timeoutMs]. Requires BODY_SENSORS permission.
     */
    suspend fun readHeartRate(timeoutMs: Long = 30_000): Float? {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) ?: return null
        return withTimeoutOrNull(timeoutMs) {
            sensorSamples(sensor).first { it > 0f }
        }
    }

    /**
     * Reads ambient temperature in Celsius (phones rarely expose true body
     * temperature; this is labeled honestly in the UI), or null.
     */
    suspend fun readTemperature(timeoutMs: Long = 10_000): Float? {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) ?: return null
        return withTimeoutOrNull(timeoutMs) {
            sensorSamples(sensor).firstOrNull()
        }
    }

    /**
     * Estimates awake/asleep entirely offline: samples accelerometer stillness
     * for [sampleMs], then combines it with screen state and local time.
     */
    suspend fun estimateSleepState(sampleMs: Long = 3_000): SleepEstimate {
        val movement = sampleMovementVariance(sampleMs)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return SleepHeuristic.estimate(
            movementVariance = movement,
            screenInteractive = powerManager.isInteractive,
            hourOfDay = LocalTime.now().hour,
        )
    }

    /** Std-dev of accelerometer magnitude over the window; null if no sensor. */
    private suspend fun sampleMovementVariance(sampleMs: Long): Float? {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return null
        val samples = mutableListOf<Float>()
        try {
            withTimeout(sampleMs) {
                sensorMagnitudes(sensor).collect { magnitude ->
                    samples += magnitude
                }
            }
        } catch (_: TimeoutCancellationException) {
            // Expected: we sample for a fixed window.
        }
        if (samples.size < 2) return null
        val mean = samples.sum() / samples.size
        val variance = samples.sumOf { ((it - mean) * (it - mean)).toDouble() } / samples.size
        return sqrt(variance).toFloat()
    }

    private fun sensorSamples(sensor: Sensor) = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0])
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun sensorMagnitudes(sensor: Sensor) = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                trySend(sqrt(x * x + y * y + z * z))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}

/**
 * Pure scoring for the awake/asleep estimate so it is unit-testable.
 * Signals: device stillness (accelerometer), screen off, night hours.
 */
object SleepHeuristic {
    /** Below this accel-magnitude std-dev the device is considered still. */
    const val STILLNESS_THRESHOLD = 0.15f

    fun estimate(movementVariance: Float?, screenInteractive: Boolean, hourOfDay: Int): SleepEstimate {
        var score = 0f
        var weight = 0f

        if (movementVariance != null) {
            score += if (movementVariance < STILLNESS_THRESHOLD) 0.4f else 0f
            weight += 0.4f
        }

        score += if (!screenInteractive) 0.3f else 0f
        weight += 0.3f

        val night = hourOfDay >= 22 || hourOfDay < 7
        score += if (night) 0.3f else 0f
        weight += 0.3f

        val probability = if (weight == 0f) 0f else score / weight
        val asleep = probability >= 0.5f
        val confidence = if (asleep) probability else 1f - probability
        return SleepEstimate(asleep = asleep, confidence = confidence)
    }
}
