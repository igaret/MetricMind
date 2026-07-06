package com.metricmind.ui.vitals

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metricmind.domain.model.VitalType

/**
 * On-demand vitals: heart rate (BODY_SENSORS), ambient temperature, and an
 * offline awake/asleep estimate. Every reading asks the user to verify it so
 * the app can report its own measured accuracy per sensor.
 */
@Composable
fun VitalsCard(vm: VitalsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) vm.measureHeartRate() }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Vitals (beta)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Measured on demand with this device's sensors. Nothing runs in the background; nothing leaves your phone.",
                style = MaterialTheme.typography.bodySmall,
            )

            VitalRow(
                label = "Heart rate",
                value = state.heartRate?.let { "${it.toInt()} bpm" },
                buttonText = if (state.measuring == VitalType.HEART_RATE) "Measuring\u2026" else "Measure",
                enabled = vm.hasHeartRateSensor && state.measuring == null,
                unavailableText = if (!vm.hasHeartRateSensor) "No heart-rate sensor on this device" else null,
            ) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BODY_SENSORS,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) vm.measureHeartRate()
                else permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
            }

            VitalRow(
                label = "Temperature (ambient)",
                value = state.temperature?.let { "%.1f \u00b0C".format(it) },
                buttonText = if (state.measuring == VitalType.BODY_TEMP) "Reading\u2026" else "Read",
                enabled = vm.hasTemperatureSensor && state.measuring == null,
                unavailableText = if (!vm.hasTemperatureSensor) "No temperature sensor on this device" else null,
                onClick = vm::measureTemperature,
            )

            VitalRow(
                label = "Awake / asleep",
                value = state.sleepLabel,
                buttonText = if (state.measuring == VitalType.SLEEP_STATE) "Checking\u2026" else "Check now",
                enabled = state.measuring == null,
                unavailableText = null,
                onClick = vm::checkSleepState,
            )

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            val accuracyLines = state.accuracies.values.filter { it.verified > 0 }
            if (accuracyLines.isNotEmpty()) {
                Text("How accurate has this been?", style = MaterialTheme.typography.labelLarge)
                accuracyLines.forEach { acc ->
                    Text(
                        "${vitalName(acc.type)}: ${acc.percentCorrect}% correct (${acc.verified} of ${acc.total} readings verified)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    state.pendingVerification?.let { reading ->
        VerifyReadingDialog(
            typeName = vitalName(reading.type),
            valueLabel = readingLabel(reading.type, reading.value),
            isSleep = reading.type == VitalType.SLEEP_STATE,
            onConfirm = { vm.confirmReading(reading) },
            onCorrect = { vm.correctReading(reading, it) },
            onDismiss = vm::dismissVerification,
        )
    }
}

@Composable
private fun VitalRow(
    label: String,
    value: String?,
    buttonText: String,
    enabled: Boolean,
    unavailableText: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label)
            Text(
                unavailableText ?: value ?: "\u2014",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(onClick = onClick, enabled = enabled) { Text(buttonText) }
    }
}

@Composable
private fun VerifyReadingDialog(
    typeName: String,
    valueLabel: String,
    isSleep: Boolean,
    onConfirm: () -> Unit,
    onCorrect: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var correcting by remember { mutableStateOf(false) }
    var correctedText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Was this accurate?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$typeName: $valueLabel")
                if (correcting) {
                    if (isSleep) {
                        Text("What's the actual state?")
                    } else {
                        OutlinedTextField(
                            value = correctedText,
                            onValueChange = { correctedText = it },
                            label = { Text("Correct value") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!correcting) {
                Button(onClick = onConfirm) { Text("Looks right") }
            } else if (isSleep) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onCorrect(0f) }) { Text("Awake") }
                    Button(onClick = { onCorrect(1f) }) { Text("Asleep") }
                }
            } else {
                Button(
                    onClick = { correctedText.toFloatOrNull()?.let(onCorrect) },
                    enabled = correctedText.toFloatOrNull() != null,
                ) { Text("Save correction") }
            }
        },
        dismissButton = {
            if (!correcting) {
                TextButton(onClick = { correcting = true }) { Text("It's off") }
            } else {
                TextButton(onClick = onDismiss) { Text("Skip") }
            }
        },
    )
}

private fun vitalName(type: VitalType): String = when (type) {
    VitalType.HEART_RATE -> "Heart rate"
    VitalType.BODY_TEMP -> "Temperature"
    VitalType.SLEEP_STATE -> "Awake/asleep"
}

private fun readingLabel(type: VitalType, value: Float): String = when (type) {
    VitalType.HEART_RATE -> "${value.toInt()} bpm"
    VitalType.BODY_TEMP -> "%.1f \u00b0C".format(value)
    VitalType.SLEEP_STATE -> if (value >= 0.5f) "Asleep" else "Awake"
}
