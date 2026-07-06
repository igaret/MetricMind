package com.metricmind.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metricmind.domain.model.MetricType
import com.metricmind.domain.model.TaskStatus
import com.metricmind.ui.vitals.VitalsCard

@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val tasks by vm.todayTasks.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Log today", style = androidx.compose.material3.MaterialTheme.typography.titleLarge) }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mood")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { score ->
                            OutlinedButton(onClick = { vm.logMetric(MetricType.MOOD, score.toFloat()) }) {
                                Text("$score")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sleep (hours)")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5f, 6f, 7f, 8f, 9f).forEach { h ->
                            OutlinedButton(onClick = { vm.logMetric(MetricType.SLEEP, h) }) {
                                Text("${h.toInt()}h")
                            }
                        }
                    }
                }
            }
        }

        item { VitalsCard() }

        item { Text("Today's habits", style = androidx.compose.material3.MaterialTheme.typography.titleLarge) }

        if (tasks.isEmpty()) {
            item { Text("No habit tasks yet. Add a habit in the Habits tab.") }
        }
        items(tasks) { task ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        task.prompt,
                        textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else null,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.setTask(task, TaskStatus.DONE) }) { Text("Done") }
                        OutlinedButton(onClick = { vm.setTask(task, TaskStatus.SKIPPED) }) { Text("Skip") }
                    }
                }
            }
        }
    }
}
