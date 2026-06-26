package com.metricmind.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metricmind.billing.BillingManager
import com.metricmind.domain.model.MetricType
import com.metricmind.domain.repository.MetricRepository
import com.metricmind.domain.repository.ScreenTimeRepository
import com.metricmind.export.CsvExporter
import com.metricmind.export.JsonExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val metrics: MetricRepository,
    private val screenTime: ScreenTimeRepository,
    private val csvExporter: CsvExporter,
    private val jsonExporter: JsonExporter,
    billingManager: BillingManager,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = billingManager.isPro

    fun hasUsageAccess(): Boolean = screenTime.hasUsageAccess()

    fun exportCsv(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = allMetrics()
            csvExporter.export(uri, all)
        }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val all = allMetrics()
            jsonExporter.export(uri, all, habits = emptyList())
        }
    }

    private suspend fun allMetrics() = withContext(Dispatchers.IO) {
        val to = LocalDate.now()
        val from = to.minusYears(5)
        MetricType.entries.flatMap { metrics.range(it, from, to) }.sortedBy { it.day }
    }
}
