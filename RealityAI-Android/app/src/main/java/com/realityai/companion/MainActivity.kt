package com.realityai.companion

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.realityai.companion.ui.MainViewModel
import com.realityai.companion.ui.screens.AiAssistantScreen
import com.realityai.companion.ui.screens.GameLibraryScreen
import com.realityai.companion.ui.screens.GraphicsProfilesScreen
import com.realityai.companion.ui.screens.HomeDashboardScreen
import com.realityai.companion.ui.screens.PerformanceScreen
import com.realityai.companion.ui.screens.SettingsScreen
import com.realityai.companion.ui.screens.VisualAnalysisScreen
import com.realityai.companion.ui.theme.CharcoalSurface
import com.realityai.companion.ui.theme.NeonGreen
import com.realityai.companion.ui.theme.ObsidianBg
import com.realityai.companion.ui.theme.RealityAiTheme
import com.realityai.companion.ui.theme.TextSecondary

enum class RealityRoute(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Default.Dashboard),
    GAMES("games", "Games", Icons.Default.SportsEsports),
    PROFILES("profiles", "Profiles", Icons.Default.Tune),
    VISUAL("visual", "Visual", Icons.Default.Compare),
    ASSISTANT("assistant", "AI", Icons.Default.AutoAwesome),
    PERFORMANCE("performance", "Performance", Icons.Default.Speed),
    SETTINGS("settings", "Settings", Icons.Default.Settings)
}

private val PrimaryBottomRoutes = listOf(
    RealityRoute.HOME,
    RealityRoute.GAMES,
    RealityRoute.PROFILES,
    RealityRoute.VISUAL,
    RealityRoute.ASSISTANT
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RealityAiTheme {
                RealityAiRoot(viewModel = viewModel, activity = this)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RealityAiRoot(viewModel: MainViewModel, activity: ComponentActivity) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val projectionManager = remember {
        activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            viewModel.startMediaProjectionCapture(result.resultCode, data)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importImageFromUri(uri)
        }
    }

    val requestStartCapture: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    LaunchedEffect(state.uiNotice) {
        val notice = state.uiNotice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(notice)
        viewModel.clearNotice()
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRouteName = currentDestination?.route

    Scaffold(
        containerColor = ObsidianBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        text = when (currentRouteName) {
                            RealityRoute.PERFORMANCE.route -> "Performance & Thermal"
                            RealityRoute.SETTINGS.route -> "Settings & Privacy"
                            else -> "REALITY AI"
                        }
                    )
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = CharcoalSurface,
                    titleContentColor = com.realityai.companion.ui.theme.TextPrimary
                ),
                actions = {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            navController.navigate(RealityRoute.PERFORMANCE.route) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Performance",
                            tint = if (currentRouteName == RealityRoute.PERFORMANCE.route) NeonGreen else TextSecondary
                        )
                    }
                    androidx.compose.material3.IconButton(
                        onClick = {
                            navController.navigate(RealityRoute.SETTINGS.route) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentRouteName == RealityRoute.SETTINGS.route) NeonGreen else TextSecondary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = CharcoalSurface) {
                PrimaryBottomRoutes.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonGreen,
                            selectedTextColor = NeonGreen,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = ObsidianBg
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = RealityRoute.HOME.route
            ) {
                composable(RealityRoute.HOME.route) {
                    HomeDashboardScreen(
                        state = state,
                        onRequestStartCapture = requestStartCapture,
                        onStopCapture = viewModel::stopMediaProjectionCapture,
                        onNavigateToGames = { navController.navigate(RealityRoute.GAMES.route) },
                        onNavigateToProfiles = { navController.navigate(RealityRoute.PROFILES.route) },
                        onNavigateToVisualAnalysis = { navController.navigate(RealityRoute.VISUAL.route) },
                        onNavigateToAssistant = { navController.navigate(RealityRoute.ASSISTANT.route) },
                        onAcceptDetectedGame = {
                            state.detectedForegroundGame?.let(viewModel::acceptDetectedGame)
                        },
                        onDismissDetectedGame = viewModel::dismissDetectedGame
                    )
                }
                composable(RealityRoute.GAMES.route) {
                    GameLibraryScreen(
                        state = state,
                        onSelectGame = viewModel::selectGame,
                        onAssignProfile = viewModel::setProfileForGame,
                        onRefreshGames = viewModel::refreshInstalledGames
                    )
                }
                composable(RealityRoute.PROFILES.route) {
                    GraphicsProfilesScreen(
                        state = state,
                        onSelectProfile = viewModel::selectActiveProfile
                    )
                }
                composable(RealityRoute.VISUAL.route) {
                    VisualAnalysisScreen(
                        state = state,
                        onUpdateAdjustments = viewModel::updateAdjustments,
                        onResetToProfile = viewModel::resetAdjustmentsToActiveProfile,
                        onRequestStartCapture = requestStartCapture,
                        onImportImage = { imagePickerLauncher.launch("image/*") }
                    )
                }
                composable(RealityRoute.ASSISTANT.route) {
                    AiAssistantScreen(
                        state = state,
                        onAnalyzeNow = viewModel::analyzeCurrentFrameOnDemand,
                        onApplyRecommendation = viewModel::applyAssistantRecommendation,
                        onImportImage = { imagePickerLauncher.launch("image/*") }
                    )
                }
                composable(RealityRoute.PERFORMANCE.route) {
                    PerformanceScreen(
                        state = state,
                        onToggleThermalDownscale = viewModel::setThermalAutoDownscale
                    )
                }
                composable(RealityRoute.SETTINGS.route) {
                    SettingsScreen(
                        state = state,
                        onSelectResolution = viewModel::setCaptureResolution,
                        onSelectQuality = viewModel::setProcessingQuality,
                        onToggleBatterySaver = viewModel::setBatterySavingMode,
                        onToggleAutoDetect = viewModel::setAutoDetectGames,
                        onOpenUsageAccessSettings = {
                            activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        onRemoveSavedProfile = viewModel::removeSavedGameProfile
                    )
                }
            }
        }
    }
}
