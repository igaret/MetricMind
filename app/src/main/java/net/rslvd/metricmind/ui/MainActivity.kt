package net.rslvd.metricmind.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.rslvd.metricmind.ui.navigation.MetricMindApp
import net.rslvd.metricmind.ui.theme.MetricMindTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MetricMindTheme {
                MetricMindApp()
            }
        }
    }
}
