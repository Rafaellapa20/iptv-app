package com.iptv.app

import android.app.Application
import android.content.Context

class IPTVApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Inicializa o provedor OkHttp com cache
        OkHttpProvider.init(this)
        
        // Inicializa o DNS Seguro logo no arranque da App
        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val isVpnEnabled = prefs.getBoolean("VPN_ENABLED", true)
        
        if (isVpnEnabled) {
            // Enable DNS over HTTPS para contornar bloqueios
            OkHttpProvider.enableDoH()
        } else {
            OkHttpProvider.disableDoH()
        }
    }
}
