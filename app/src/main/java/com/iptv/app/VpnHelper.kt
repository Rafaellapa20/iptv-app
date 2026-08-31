package com.iptv.app

import android.content.Context
import android.util.Log

/**
 * Anti-Bloqueio Helper - Gere a ativação e desativação do DNS-over-HTTPS (DoH)
 * de forma nativa e transparente na aplicação.
 */
object VpnHelper {

    private const val TAG = "VpnHelper"

    fun setEnabled(context: Context, enabled: Boolean) {
        if (enabled) {
            OkHttpProvider.enableDoH()
            Log.i(TAG, "Anti-Bloqueio (DNS Seguro) ativado")
        } else {
            OkHttpProvider.disableDoH()
            Log.i(TAG, "Anti-Bloqueio (DNS Seguro) desativado")
        }
    }
}
