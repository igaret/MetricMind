package net.rslvd.metricmind.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.rslvd.metricmind.domain.model.Habit
import net.rslvd.metricmind.domain.model.HabitTemplate
import net.rslvd.metricmind.domain.model.ReminderMode
import net.rslvd.metricmind.domain.repository.HabitRepository
import net.rslvd.metricmind.habit.HabitEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habits: HabitRepository,
    private val engine: HabitEngine,
) : ViewModel() {

    val habitList: StateFlow<List<Habit>> =
        habits.observeHabits()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addHabit(title: String, template: HabitTemplate) {
        viewModelScope.launch {
            habits.upsertHabit(
                Habit(title = title, template = template, reminderMode = ReminderMode.SMART),
            )
            // Generate today's task immediately so the user sees it on the Today tab.
            engine.generateFor(LocalDate.now())
        }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch { habits.deleteHabit(id) }
    }
}
