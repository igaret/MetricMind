package net.rslvd.metricmind.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.rslvd.metricmind.R

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isPro by vm.isPro.collectAsStateWithLifecycle()

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(vm::exportCsv) }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(vm::exportJson) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                Text(stringRes(R.string.privacy_promise))
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Screen time", style = MaterialTheme.typography.titleMedium)
                Text(stringRes(R.string.screen_time_rationale))
                val granted = vm.hasUsageAccess()
                Text(if (granted) "Access: granted" else "Access: not granted")
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }) { Text(if (granted) "Manage access" else "Grant usage access") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Export (local only)", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { csvLauncher.launch("metricmind-export.csv") }) { Text("Export CSV") }
                Button(onClick = { jsonLauncher.launch("metricmind-export.json") }) { Text("Export JSON") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("MetricMind Pro", style = MaterialTheme.typography.titleMedium)
                Text(if (isPro) "Active — thank you!" else "Premium charts, themes, PDF export, custom habits.")
                if (!isPro) Button(onClick = { /* launchPurchaseFlow via BillingManager */ }) { Text("Unlock Pro") }
            }
        }
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)
