package com.vibelauncher.app

import android.app.Application

class VibeLauncherApplication : Application() {
    val container by lazy { AppContainer(this) }
}
