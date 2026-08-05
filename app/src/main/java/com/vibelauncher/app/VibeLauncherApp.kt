package com.vibelauncher.app

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibelauncher.app.data.apps.AppInfo
import com.vibelauncher.app.ui.drawer.AppDrawerScreen
import com.vibelauncher.app.ui.drawer.AppDrawerViewModel
import com.vibelauncher.app.ui.home.HomeScreen
import com.vibelauncher.app.ui.home.HomeViewModel
import com.vibelauncher.app.ui.picker.AppPickerViewModel
import com.vibelauncher.app.ui.settings.CardColorScreen
import com.vibelauncher.app.ui.settings.CardColorViewModel
import com.vibelauncher.app.ui.settings.HomeAppsScreen
import com.vibelauncher.app.ui.settings.HomeAppsViewModel
import com.vibelauncher.app.ui.settings.SettingsScreen
import com.vibelauncher.app.ui.settings.SettingsViewModel
import com.vibelauncher.app.ui.theme.VibeLauncherTheme

const val ROUTE_HOME = "home"
const val ROUTE_DRAWER = "drawer"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_HOME_APPS = "home_apps"
const val ROUTE_CARD_COLOR = "card_color"

@Composable
fun VibeLauncherApp(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val container = (context.applicationContext as VibeLauncherApplication).container

    VibeLauncherTheme {
        NavHost(navController = navController, startDestination = ROUTE_HOME) {
            composable(ROUTE_HOME) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(
                        context.applicationContext,
                        container.calendarRepository,
                        container.weatherRepository,
                        container.tileRepository,
                        container.settingsRepository,
                        container.iconThemeRepository
                    )
                )
                val pickerFactory = AppPickerViewModel.Factory(container.installedAppsRepository)
                HomeScreen(
                    viewModel = homeViewModel,
                    pickerViewModelFactory = pickerFactory,
                    onOpenDrawer = { navController.navigate(ROUTE_DRAWER) }
                )
            }
            composable(
                ROUTE_DRAWER,
                enterTransition = { slideInVertically(initialOffsetY = { it }) },
                exitTransition = { slideOutVertically(targetOffsetY = { it }) },
                popEnterTransition = { slideInVertically(initialOffsetY = { it }) },
                popExitTransition = { slideOutVertically(targetOffsetY = { it }) }
            ) {
                val drawerViewModel: AppDrawerViewModel = viewModel(
                    factory = AppDrawerViewModel.Factory(
                        container.installedAppsRepository,
                        container.settingsRepository,
                        container.iconThemeRepository
                    )
                )
                AppDrawerScreen(
                    viewModel = drawerViewModel,
                    onLaunchApp = { app: AppInfo -> launchApp(context, app) },
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(ROUTE_SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(container.settingsRepository, container.iconThemeRepository)
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenHomeApps = { navController.navigate(ROUTE_HOME_APPS) },
                    onOpenCardColor = { navController.navigate(ROUTE_CARD_COLOR) }
                )
            }
            composable(ROUTE_HOME_APPS) {
                val homeAppsViewModel: HomeAppsViewModel = viewModel(
                    factory = HomeAppsViewModel.Factory(container.tileRepository)
                )
                val pickerFactory = AppPickerViewModel.Factory(container.installedAppsRepository)
                HomeAppsScreen(
                    viewModel = homeAppsViewModel,
                    pickerViewModelFactory = pickerFactory,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(ROUTE_CARD_COLOR) {
                val cardColorViewModel: CardColorViewModel = viewModel(
                    factory = CardColorViewModel.Factory(container.settingsRepository)
                )
                CardColorScreen(
                    viewModel = cardColorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun launchApp(context: android.content.Context, app: AppInfo) {
    val intent = Intent().apply {
        component = ComponentName(app.packageName, app.className)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    // Never let a launch failure (uninstalled app, permission-gated component, etc.)
    // crash this process - it's the Home launcher, so a crash takes down the home screen.
    val started = runCatching { context.startActivity(intent) }.isSuccess
    if (!started) {
        Toast.makeText(context, "Couldn't open ${app.label}", Toast.LENGTH_SHORT).show()
    }
}
