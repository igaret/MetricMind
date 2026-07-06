package com.metricmind.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metricmind.domain.model.MetricType
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

@Composable
fun InsightsScreen(vm: InsightsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val series by vm.series.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Insights", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricType.entries.take(4).forEach { t ->
                FilterChip(
                    selected = state.type == t,
                    onClick = { vm.selectType(t) },
                    label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 30, 90).forEach { d ->
                FilterChip(
                    selected = state.rangeDays == d,
                    onClick = { vm.selectRange(d) },
                    label = { Text("${d}d") },
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("${state.type.name} trend (${state.rangeDays}d)")
                LineChart(
                    values = series.map { it.value },
                    modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 12.dp),
                )
                if (series.isEmpty()) Text("No data yet — log this metric on the Today tab.")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Correlation: ${state.type.name} vs ${state.correlationWith.name}")
                val c = state.correlation
                if (c != null && c.n >= 2) {
                    Text("Pearson r = ${"%.2f".format(c.pearsonR)} (n=${c.n})")
                } else {
                    Text("Need at least 2 overlapping days to correlate.")
                }
            }
        }
    }
}

/**
 * On-device line chart backed by Vico (Compose-native, see DESIGN.md §4).
 * Renders nothing until at least two points exist so empty states stay clean.
 */
@Composable
private fun LineChart(values: List<Float>, modifier: Modifier = Modifier) {
    if (values.size < 2) return
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        modelProducer.runTransaction {
            lineSeries { series(values) }
        }
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis(),
        ),
        modelProducer = modelProducer,
        modifier = modifier,
    )
}
