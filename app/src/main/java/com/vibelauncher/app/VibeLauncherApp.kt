package com.vibelauncher.app

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
import com.vibelauncher.app.ui.settings.AppearanceScreen
import com.vibelauncher.app.ui.settings.AppearanceViewModel
import com.vibelauncher.app.ui.settings.CardColorScreen
import com.vibelauncher.app.ui.settings.CardColorViewModel
import com.vibelauncher.app.ui.settings.CustomAccentScreen
import com.vibelauncher.app.ui.settings.HomeAppsScreen
import com.vibelauncher.app.ui.settings.HomeAppsViewModel
import com.vibelauncher.app.ui.settings.IconThemeScreen
import com.vibelauncher.app.ui.settings.SettingsScreen
import com.vibelauncher.app.ui.settings.SettingsViewModel
import com.vibelauncher.app.ui.theme.VibeLauncherTheme
import com.vibelauncher.app.ui.todos.TodoScreen
import com.vibelauncher.app.ui.todos.TodoViewModel

const val ROUTE_HOME = "home"
const val ROUTE_DRAWER = "drawer"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_HOME_APPS = "home_apps"
const val ROUTE_CARD_COLOR = "card_color"
const val ROUTE_TODOS = "todos"
const val ROUTE_APPEARANCE = "appearance"
const val ROUTE_CUSTOM_ACCENT = "custom_accent"
const val ROUTE_ICON_THEME = "icon_theme"

@Composable
fun VibeLauncherApp(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val container = (context.applicationContext as VibeLauncherApplication).container
    val accentArgb by container.settingsRepository.accentColor.collectAsState(initial = 0xFFF97316.toInt())
    val fontScale by container.settingsRepository.fontScale.collectAsState(initial = 1.0f)

    VibeLauncherTheme(accentColor = Color(accentArgb)) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, fontScale = density.fontScale * fontScale)
        ) {
        NavHost(navController = navController, startDestination = ROUTE_HOME) {
            composable(ROUTE_HOME) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(
                        context.applicationContext,
                        container.calendarRepository,
                        container.weatherRepository,
                        container.tileRepository,
                        container.settingsRepository,
                        container.iconThemeRepository,
                        container.todoRepository,
                        container.installedAppsRepository
                    )
                )
                val pickerFactory = AppPickerViewModel.Factory(container.installedAppsRepository)
                HomeScreen(
                    viewModel = homeViewModel,
                    pickerViewModelFactory = pickerFactory,
                    onOpenDrawer = { navController.navigate(ROUTE_DRAWER) },
                    onOpenTodos = { navController.navigate(ROUTE_TODOS) }
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
                    onOpenAppearance = { navController.navigate(ROUTE_APPEARANCE) },
                    onOpenHomeApps = { navController.navigate(ROUTE_HOME_APPS) },
                    onOpenCardColor = { navController.navigate(ROUTE_CARD_COLOR) },
                    onOpenIconTheme = { navController.navigate(ROUTE_ICON_THEME) }
                )
            }
            composable(ROUTE_APPEARANCE) {
                val appearanceViewModel: AppearanceViewModel = viewModel(
                    factory = AppearanceViewModel.Factory(container.settingsRepository)
                )
                AppearanceScreen(
                    viewModel = appearanceViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenCustomAccent = { navController.navigate(ROUTE_CUSTOM_ACCENT) }
                )
            }
            composable(ROUTE_CUSTOM_ACCENT) {
                val appearanceViewModel: AppearanceViewModel = viewModel(
                    factory = AppearanceViewModel.Factory(container.settingsRepository)
                )
                CustomAccentScreen(viewModel = appearanceViewModel, onBack = { navController.popBackStack() })
            }
            composable(ROUTE_ICON_THEME) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(container.settingsRepository, container.iconThemeRepository)
                )
                IconThemeScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(ROUTE_HOME_APPS) {
                val homeAppsViewModel: HomeAppsViewModel = viewModel(
                    factory = HomeAppsViewModel.Factory(container.tileRepository, container.installedAppsRepository)
                )
                HomeAppsScreen(
                    viewModel = homeAppsViewModel,
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
            composable(ROUTE_TODOS) {
                val todoViewModel: TodoViewModel = viewModel(
                    factory = TodoViewModel.Factory(container.todoRepository)
                )
                TodoScreen(viewModel = todoViewModel, onBack = { navController.popBackStack() })
            }
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
