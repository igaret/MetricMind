package net.rslvd.metricmind.ui.vitals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.rslvd.metricmind.domain.model.VitalAccuracy
import net.rslvd.metricmind.domain.model.VitalReading
import net.rslvd.metricmind.domain.model.VitalType
import net.rslvd.metricmind.domain.model.VitalVerification
import net.rslvd.metricmind.domain.repository.VitalsRepository
import net.rslvd.metricmind.sensors.BodySensors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VitalsUiState(
    val heartRate: Float? = null,
    val temperature: Float? = null,
    val sleepLabel: String? = null,
    val measuring: VitalType? = null,
    /** Most recent reading awaiting user verification. */
    val pendingVerification: VitalReading? = null,
    val accuracies: Map<VitalType, VitalAccuracy> = emptyMap(),
    val error: String? = null,
)

@HiltViewModel
class VitalsViewModel @Inject constructor(
    private val sensors: BodySensors,
    private val vitals: VitalsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VitalsUiState())
    val state: StateFlow<VitalsUiState> = _state.asStateFlow()

    val hasHeartRateSensor: Boolean get() = sensors.hasHeartRateSensor()
    val hasTemperatureSensor: Boolean get() = sensors.hasTemperatureSensor()

    init {
        refreshAccuracies()
    }

    fun measureHeartRate() {
        if (_state.value.measuring != null) return
        _state.value = _state.value.copy(measuring = VitalType.HEART_RATE, error = null)
        viewModelScope.launch {
            val bpm = sensors.readHeartRate()
            if (bpm == null) {
                _state.value = _state.value.copy(
                    measuring = null,
                    error = "No heart-rate reading. Keep your finger on the sensor and try again.",
                )
            } else {
                val reading = vitals.record(VitalType.HEART_RATE, bpm)
                _state.value = _state.value.copy(
                    measuring = null,
                    heartRate = bpm,
                    pendingVerification = reading,
                )
                refreshAccuracies()
            }
        }
    }

    fun measureTemperature() {
        if (_state.value.measuring != null) return
        _state.value = _state.value.copy(measuring = VitalType.BODY_TEMP, error = null)
        viewModelScope.launch {
            val celsius = sensors.readTemperature()
            if (celsius == null) {
                _state.value = _state.value.copy(
                    measuring = null,
                    error = "No temperature reading available on this device.",
                )
            } else {
                val reading = vitals.record(VitalType.BODY_TEMP, celsius)
                _state.value = _state.value.copy(
                    measuring = null,
                    temperature = celsius,
                    pendingVerification = reading,
                )
                refreshAccuracies()
            }
        }
    }

    fun checkSleepState() {
        if (_state.value.measuring != null) return
        _state.value = _state.value.copy(measuring = VitalType.SLEEP_STATE, error = null)
        viewModelScope.launch {
            val estimate = sensors.estimateSleepState()
            val label = (if (estimate.asleep) "Asleep" else "Awake") +
                " (${(estimate.confidence * 100).toInt()}% confidence)"
            val reading = vitals.record(VitalType.SLEEP_STATE, if (estimate.asleep) 1f else 0f)
            _state.value = _state.value.copy(
                measuring = null,
                sleepLabel = label,
                pendingVerification = reading,
            )
            refreshAccuracies()
        }
    }

    fun confirmReading(reading: VitalReading) {
        viewModelScope.launch {
            vitals.verify(reading.id, VitalVerification.CONFIRMED)
            _state.value = _state.value.copy(pendingVerification = null)
            refreshAccuracies()
        }
    }

    fun correctReading(reading: VitalReading, correctedValue: Float) {
        viewModelScope.launch {
            vitals.verify(reading.id, VitalVerification.CORRECTED, correctedValue)
            _state.value = _state.value.copy(pendingVerification = null)
            refreshAccuracies()
        }
    }

    fun dismissVerification() {
        _state.value = _state.value.copy(pendingVerification = null)
    }

    private fun refreshAccuracies() {
        viewModelScope.launch {
            val map = VitalType.entries.associateWith { vitals.accuracy(it) }
            _state.value = _state.value.copy(accuracies = map)
        }
    }
}
