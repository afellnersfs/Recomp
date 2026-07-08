package com.arie.recomp.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arie.recomp.R
import com.arie.recomp.data.Graph
import com.arie.recomp.health.HealthConnectManager
import com.arie.recomp.ui.body.BodyScreen
import com.arie.recomp.ui.detail.HeartDetailScreen
import com.arie.recomp.ui.detail.SleepDetailScreen
import com.arie.recomp.ui.detail.StepsDetailScreen
import com.arie.recomp.ui.detail.WeightDetailScreen
import com.arie.recomp.ui.home.HomeScreen
import com.arie.recomp.ui.nutrition.NutritionScreen
import com.arie.recomp.ui.profile.ProfileScreen
import com.arie.recomp.ui.settings.SettingsScreen
import com.arie.recomp.ui.theme.Accent
import com.arie.recomp.ui.theme.Surface1
import com.arie.recomp.ui.trends.TrendsScreen
import com.arie.recomp.ui.workout.RoutineScreen
import com.arie.recomp.ui.workout.SummaryScreen
import com.arie.recomp.ui.workout.WorkoutScreen
import com.arie.recomp.ui.workouts.WorkoutsScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch

private data class NavItem(val route: String, val icon: ImageVector, val label: String)

private val navItems = listOf(
    NavItem("home", Icons.Filled.Home, "Home"),
    NavItem("trends", Icons.Filled.ShowChart, "Trends"),
    NavItem("workouts", Icons.Filled.FitnessCenter, "Workouts"),
    NavItem("profile", Icons.Filled.Person, "Profile")
)

@Composable
fun AppRoot(showPrivacyInitially: Boolean = false) {
    val nav: NavHostController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    var showPrivacy by remember { mutableStateOf(showPrivacyInitially) }
    var runOnboarding by remember { mutableStateOf(false) }

    val hc = remember { HealthConnectManager(context) }
    val hcLauncher = rememberLauncherForActivityResult(hc.requestPermissionContract()) { }
    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        val s = Graph.settings.current()
        if (!s.onboarded) {
            runOnboarding = true
            showPrivacy = true
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = navItems.any { it.route == currentRoute }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(Modifier.fillMaxSize()) {
            // Everything the glass refracts lives on this layer.
            MeshBackground(
                Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
            )

            NavHost(nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") { HomeScreen(nav) }
                composable("trends") { TrendsScreen() }
                composable("workouts") { WorkoutsScreen(nav) }
                composable("profile") { ProfileScreen(nav) }

                composable("sleep") { SleepDetailScreen() }
                composable("heart") { HeartDetailScreen() }
                composable("steps") { StepsDetailScreen() }
                composable("weight") { WeightDetailScreen() }

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

            if (showBottomBar) {
                GlassPillNav(
                    current = currentRoute,
                    onSelect = { route ->
                        if (currentRoute != route) {
                            nav.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 14.dp)
                )
            }
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
}

/** The signature Liquid Glass element: a floating frosted pill, heaviest blur on screen. */
@Composable
private fun GlassPillNav(
    current: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haze = LocalHazeState.current
    val shape = RoundedCornerShape(32.dp)
    var m = modifier.clip(shape)
    m = if (haze != null) m.hazeEffect(state = haze, style = glassStyle(tintAlpha = 0.10f))
    else m.background(Surface1.copy(alpha = 0.96f))
    m = m.border(1.dp, glassBorder(), shape)

    Row(m.padding(horizontal = 8.dp, vertical = 6.dp)) {
        navItems.forEach { item ->
            val selected = item.route == current
            val interaction = remember { MutableInteractionSource() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(interactionSource = interaction, indication = null) {
                        onSelect(item.route)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = if (selected) Accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
