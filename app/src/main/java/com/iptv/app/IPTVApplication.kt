package com.iptv.app

import android.app.Application
import android.content.Context

class IPTVApplication : Application() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext

        // Inicializa o provedor OkHttp com cache
        OkHttpProvider.init(this)
        
        // Inicializa o DNS Seguro logo no arranque da App
        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val isVpnEnabled = prefs.getBoolean("VPN_ENABLED", false)
        
        if (isVpnEnabled) {
            // Enable DNS over HTTPS para contornar bloqueios
            OkHttpProvider.enableDoH()
        } else {
            OkHttpProvider.disableDoH()
        }
    }
}
