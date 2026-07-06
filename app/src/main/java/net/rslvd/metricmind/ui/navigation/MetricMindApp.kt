package net.rslvd.metricmind.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import net.rslvd.metricmind.ui.habits.HabitsScreen
import net.rslvd.metricmind.ui.home.HomeScreen
import net.rslvd.metricmind.ui.insights.InsightsScreen
import net.rslvd.metricmind.ui.settings.SettingsScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Today", Icons.Filled.Home),
    INSIGHTS("insights", "Insights", Icons.Filled.BarChart),
    HABITS("habits", "Habits", Icons.Filled.CheckCircle),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

@Composable
fun MetricMindApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                Dest.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.HOME.route,
            modifier = androidx.compose.ui.Modifier.padding(padding),
        ) {
            composable(Dest.HOME.route) { HomeScreen() }
            composable(Dest.INSIGHTS.route) { InsightsScreen() }
            composable(Dest.HABITS.route) { HabitsScreen() }
            composable(Dest.SETTINGS.route) { SettingsScreen() }
        }
    }
}
