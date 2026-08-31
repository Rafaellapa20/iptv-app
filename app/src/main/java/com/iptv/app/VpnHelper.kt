package com.iptv.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * VPN Helper - Integra com a app Cloudflare WARP (1.1.1.1) do utilizador.
 * Quando o utilizador ativa "VPN/Anti-Bloqueio", ativa DoH internamente
 * e sugere instalar a app WARP para proteção VPN completa.
 */
object VpnHelper {

    private const val TAG = "VpnHelper"
    private const val WARP_PACKAGE = "com.cloudflare.onedotonedotonedotone"

    fun setEnabled(context: Context, enabled: Boolean) {
        if (enabled) {
            OkHttpProvider.enableDoH()
            Log.i(TAG, "DNS-over-HTTPS ativado")
        } else {
            OkHttpProvider.disableDoH()
            Log.i(TAG, "DNS-over-HTTPS desativado")
        }
    }

    /**
     * Verifica se a app Cloudflare WARP (1.1.1.1) está instalada.
     */
    fun isWarpInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(WARP_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Abre a app WARP se instalada, ou abre a Play Store para instalar.
     */
    fun openWarpApp(context: Context) {
        if (isWarpInstalled(context)) {
            val intent = context.packageManager.getLaunchIntentForPackage(WARP_PACKAGE)
            if (intent != null) {
                context.startActivity(intent)
            }
        } else {
            // Abrir Play Store para instalar
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$WARP_PACKAGE"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$WARP_PACKAGE"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }
}
