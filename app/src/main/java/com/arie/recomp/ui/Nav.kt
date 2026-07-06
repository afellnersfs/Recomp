package com.arie.recomp.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arie.recomp.R
import com.arie.recomp.data.Graph
import com.arie.recomp.health.HealthConnectManager
import com.arie.recomp.ui.body.BodyScreen
import com.arie.recomp.ui.home.HomeScreen
import com.arie.recomp.ui.nutrition.NutritionScreen
import com.arie.recomp.ui.progress.ProgressScreen
import com.arie.recomp.ui.settings.SettingsScreen
import com.arie.recomp.ui.workout.RoutineScreen
import com.arie.recomp.ui.workout.SummaryScreen
import com.arie.recomp.ui.workout.WorkoutScreen
import kotlinx.coroutines.launch

private data class NavItem(val route: String, val icon: ImageVector, val label: String)

private val navItems = listOf(
    NavItem("home", Icons.Filled.Home, "Home"),
    NavItem("progress", Icons.Filled.ShowChart, "Progress"),
    NavItem("body", Icons.Filled.Scale, "Body"),
    NavItem("fuel", Icons.Filled.Restaurant, "Fuel"),
    NavItem("settings", Icons.Filled.Settings, "Settings")
)

@Composable
fun AppRoot(showPrivacyInitially: Boolean = false) {
    val nav: NavHostController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showPrivacy by remember { mutableStateOf(showPrivacyInitially) }
    var runOnboarding by remember { mutableStateOf(false) }

    val hc = remember { HealthConnectManager(context) }
    val hcLauncher = rememberLauncherForActivityResult(hc.requestPermissionContract()) { }
    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // First launch: privacy dialog, then notification + Health Connect permissions.
    LaunchedEffect(Unit) {
        val s = Graph.settings.current()
        if (!s.onboarded) {
            runOnboarding = true
            showPrivacy = true
        }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Your data stays on your phone") },
            text = { Text(stringResource(R.string.health_privacy)) },
            confirmButton = {
                TextButton(onClick = {
                    showPrivacy = false
                    if (runOnboarding) {
                        runOnboarding = false
                        if (Build.VERSION.SDK_INT >= 33) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        if (hc.isAvailable()) hcLauncher.launch(hc.permissions)
                        scope.launch { Graph.settings.update { it.copy(onboarded = true) } }
                    }
                }) { Text("Got it") }
            }
        )
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = navItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    nav.navigate(item.route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(nav) }
            composable("progress") { ProgressScreen() }
            composable("body") { BodyScreen() }
            composable("fuel") { NutritionScreen() }
            composable("settings") { SettingsScreen() }
            composable("warmup") { RoutineScreen(nav, warmup = true) }
            composable("cooldown") { RoutineScreen(nav, warmup = false) }
            composable("workout") { WorkoutScreen(nav) }
            composable("summary/{id}") { entry ->
                SummaryScreen(nav, entry.arguments?.getString("id")?.toLongOrNull() ?: 0L)
            }
        }
    }
}
