package com.vibelauncher.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Belt-and-suspenders alongside the theme's windowShowWallpaper - some OEM skins
        // only honor one or the other.
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        setContent {
            val controller = rememberNavController()
            navController = controller
            VibeLauncherApp(navController = controller)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            val controller = navController ?: return
            // The home key doesn't skip past setup - leaving it that way saves nothing, and a
            // first-run setup would only reappear on the next launch.
            if (controller.currentDestination?.route == ROUTE_SETUP) return
            controller.popBackStack(ROUTE_HOME, inclusive = false)
        }
    }
}
