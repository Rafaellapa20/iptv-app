package com.iptv.app

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build

enum class Surface { TV, TABLET, PHONE }

/**
 * Substitui o DeviceUtils.kt actual.
 * A decisao e tomada uma vez, guardada, e pode ser forcada pelo utilizador
 * em Definicoes -> Aparencia -> "Forcar layout" (para boxes que se
 * identificam mal como telemovel).
 */
object DeviceUtils {

    private const val PREFS = "device"
    private const val KEY_FORCE = "force_surface"
    private const val KEY_CACHE = "surface_cache"

    fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun surface(ctx: Context): Surface {
        prefs(ctx).getString(KEY_FORCE, null)?.let { forced ->
            runCatching { return Surface.valueOf(forced) }
        }
        prefs(ctx).getString(KEY_CACHE, null)?.let { cached ->
            runCatching { return Surface.valueOf(cached) }
        }
        val detected = detect(ctx)
        prefs(ctx).edit().putString(KEY_CACHE, detected.name).apply()
        return detected
    }

    fun force(ctx: Context, surface: Surface?) {
        prefs(ctx).edit().apply {
            if (surface == null) remove(KEY_FORCE) else putString(KEY_FORCE, surface.name)
            remove(KEY_CACHE)
        }.apply()
    }

    /** Convenience — keeps callers that only need TV vs non-TV working. */
    fun isTv(ctx: Context): Boolean = surface(ctx) == Surface.TV

    private fun detect(ctx: Context): Surface {
        val pm = ctx.packageManager
        val uiMode = (ctx.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
            .currentModeType

        val isTv = uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) ||
            (Build.MANUFACTURER.equals("Amazon", true) && Build.MODEL.startsWith("AFT"))

        if (isTv) return Surface.TV

        val sw = ctx.resources.configuration.smallestScreenWidthDp
        return if (sw >= 600) Surface.TABLET else Surface.PHONE
    }
}
