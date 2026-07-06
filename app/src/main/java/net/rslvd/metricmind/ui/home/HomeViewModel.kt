package net.rslvd.metricmind.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.rslvd.metricmind.domain.model.HabitTask
import net.rslvd.metricmind.domain.model.MetricEntry
import net.rslvd.metricmind.domain.model.MetricType
import net.rslvd.metricmind.domain.model.TaskStatus
import net.rslvd.metricmind.domain.repository.HabitRepository
import net.rslvd.metricmind.domain.repository.MetricRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val metrics: MetricRepository,
    private val habits: HabitRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val todayTasks: StateFlow<List<HabitTask>> =
        habits.observeTasksFor(today)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logMetric(type: MetricType, value: Float, note: String? = null) {
        viewModelScope.launch {
            metrics.upsert(MetricEntry(type = type, value = value, note = note, day = today))
        }
    }

    fun setTask(task: HabitTask, status: TaskStatus) {
        viewModelScope.launch { habits.setTaskStatus(task.id, status) }
    }
}
