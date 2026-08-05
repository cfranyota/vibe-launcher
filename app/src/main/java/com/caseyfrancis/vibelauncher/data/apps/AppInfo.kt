package com.caseyfrancis.vibelauncher.data.apps

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Drawable
)
