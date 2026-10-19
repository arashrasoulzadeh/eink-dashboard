package com.arashrasoulzadeh.einkdash.settings

import android.app.Activity
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/** Checks and requests "default Home app" across Android 4.2 … 15. */
object DefaultHome {

    fun isDefault(ctx: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return ctx.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == ctx.packageName
    }

    fun request(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = activity.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                activity.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 77)
                return
            }
        }
        if (Build.VERSION.SDK_INT >= 21 && runCatching {
                activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }.isSuccess) return
        // Android 4.x: briefly enabling a second Home activity clears the saved choice,
        // so pressing Home shows the "Complete action using" chooser with "Always".
        val fake = ComponentName(activity, FakeHome::class.java)
        val pm = activity.packageManager
        pm.setComponentEnabledSetting(fake, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        activity.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        pm.setComponentEnabledSetting(fake, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }

    /** Disabled placeholder Home activity; only exists to reset the launcher choice on old Android. */
    class FakeHome : Activity()
}
