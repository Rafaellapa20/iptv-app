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

        // Limpeza única de ficheiros de SharedPreferences criados pelo SyncManager
        // antigo com nomes errados — nunca foram lidos por nada.
        getSharedPreferences("IPTV_Recent", Context.MODE_PRIVATE).edit().clear().apply()   // nome errado (devia ser IPTV_RECENT)
        getSharedPreferences("IPTV_Progress", Context.MODE_PRIVATE).edit().remove("recent_list").apply() // chave errada

        // Agente StreamVPN: heartbeat + controlo remoto pelo painel (silencioso se offline)
        DeviceAgent.start(this)
        StreamVpnTunnel.init(this)
        VpnManager.init(this)
        
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
