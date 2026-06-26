package com.metricmind.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metricmind.domain.model.CorrelationResult
import com.metricmind.domain.model.MetricEntry
import com.metricmind.domain.model.MetricType
import com.metricmind.domain.repository.MetricRepository
import com.metricmind.domain.usecase.ComputeCorrelation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class InsightsState(
    val type: MetricType = MetricType.MOOD,
    val rangeDays: Int = 30,
    val correlationWith: MetricType = MetricType.SLEEP,
    val correlation: CorrelationResult? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val metrics: MetricRepository,
    private val computeCorrelation: ComputeCorrelation,
) : ViewModel() {

    private val _state = MutableStateFlow(InsightsState())
    val state: StateFlow<InsightsState> = _state

    val series: StateFlow<List<MetricEntry>> =
        _state.flatMapLatest { s ->
            val to = LocalDate.now()
            metrics.observe(s.type, to.minusDays(s.rangeDays.toLong()), to)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectType(type: MetricType) { _state.value = _state.value.copy(type = type); recompute() }
    fun selectRange(days: Int) { _state.value = _state.value.copy(rangeDays = days); recompute() }
    fun selectCorrelation(type: MetricType) { _state.value = _state.value.copy(correlationWith = type); recompute() }

    private fun recompute() {
        viewModelScope.launch {
            val s = _state.value
            val to = LocalDate.now()
            val result = computeCorrelation(s.type, s.correlationWith, to.minusDays(s.rangeDays.toLong()), to)
            _state.value = _state.value.copy(correlation = result)
        }
    }

    init { recompute() }
}
