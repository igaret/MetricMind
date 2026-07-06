package net.rslvd.metricmind.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.rslvd.metricmind.domain.model.HabitTemplate

@Composable
fun HabitsScreen(vm: HabitsViewModel = hiltViewModel()) {
    val habits by vm.habitList.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New habit") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Your habits", style = MaterialTheme.typography.titleLarge) }
            if (habits.isEmpty()) item { Text("No habits yet. Tap “New habit”.") }
            items(habits) { habit ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(habit.title, style = MaterialTheme.typography.titleMedium)
                            Text(habit.template.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { vm.deleteHabit(habit.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        NewHabitDialog(
            onDismiss = { showDialog = false },
            onCreate = { title, template -> vm.addHabit(title, template); showDialog = false },
        )
    }
}

@Composable
private fun NewHabitDialog(onDismiss: () -> Unit, onCreate: (String, HabitTemplate) -> Unit) {
    var title by remember { mutableStateOf("") }
    var template by remember { mutableStateOf(HabitTemplate.WATER) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New micro-habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                Text("Template")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HabitTemplate.entries.filter { it != HabitTemplate.CUSTOM }.forEach { t ->
                        FilterChip(
                            selected = template == t,
                            onClick = { template = t },
                            label = { Text(t.displayName) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = { onCreate(title.trim(), template) },
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
