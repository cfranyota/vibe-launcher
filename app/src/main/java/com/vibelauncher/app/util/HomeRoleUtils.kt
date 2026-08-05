package com.vibelauncher.app.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build

object HomeRoleUtils {

    /** Returns an Intent to launch for the "set as default launcher" flow, or null if
     *  the RoleManager API isn't available (pre-API 29) - caller should fall back to
     *  telling the user to pick it from the system Home-app chooser / Settings instead. */
    fun requestHomeRoleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return null
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return null
        if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) return null
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
    }

    fun isDefaultHome(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return false
        return roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && roleManager.isRoleHeld(RoleManager.ROLE_HOME)
    }
}
