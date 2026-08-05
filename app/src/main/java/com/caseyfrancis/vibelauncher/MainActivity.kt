package com.caseyfrancis.vibelauncher

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
            navController?.popBackStack(ROUTE_HOME, inclusive = false)
        }
    }
}
