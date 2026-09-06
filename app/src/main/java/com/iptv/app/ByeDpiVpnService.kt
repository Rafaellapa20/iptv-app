package com.iptv.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import io.github.dovecoteescapee.byedpi.core.TProxyService
import kotlinx.coroutines.*
import java.io.File

class ByeDpiVpnService : VpnService() {

    companion object {
        const val TAG = "ByeDpiVpnService"
        const val ACTION_START = "com.iptv.app.byedpi.START"
        const val ACTION_STOP  = "com.iptv.app.byedpi.STOP"
        const val NOTIFICATION_ID = 42
        const val CHANNEL_ID = "byedpi_protection"

        // Default args: disorder at split position 1 for HTTPS
        val DEFAULT_ARGS = arrayOf("ciadpi", "--disorder", "1")
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunPfd: android.os.ParcelFileDescriptor? = null
    private var proxyFd = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopVpn(); return START_NOT_STICKY }
            else        -> startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        startForeground(NOTIFICATION_ID, buildNotification())
        LocalProtection.setState(LocalProtection.State.CONNECTING)

        serviceScope.launch {
            try {
                // 1. Start byedpi SOCKS5 proxy on 127.0.0.1:1080
                val fd = ByeDpiProxy.jniCreateSocketWithCommandLine(DEFAULT_ARGS)
                if (fd < 0) throw IllegalStateException("byedpi socket creation failed")
                proxyFd = fd

                // Run event loop in background (blocks until stopped)
                launch { ByeDpiProxy.jniStartProxy(fd) }

                // Give byedpi 300ms to bind
                delay(300)

                // 2. Write hev-socks5-tunnel YAML config
                val configFile = File.createTempFile("hst_config", ".yml", cacheDir)
                configFile.writeText("""
                    misc:
                      task-stack-size: 81920
                    socks5:
                      mtu: 8500
                      address: 127.0.0.1
                      port: 1080
                      udp: udp
                """.trimIndent())

                // 3. Build TUN via VpnService.Builder
                val pfd = Builder()
                    .setSession("StreamVPN · Proteção ativa")
                    .addAddress("10.10.10.10", 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("1.1.1.1")
                    .addDnsServer("8.8.8.8")
                    .setMtu(8500)
                    .setBlocking(false)
                    .addDisallowedApplication(packageName)   // own app bypasses VPN
                    .setMetered(false)
                    .establish()
                    ?: throw IllegalStateException("VpnService.Builder.establish() returned null")

                tunPfd = pfd

                // 4. Hand TUN fd to hev-socks5-tunnel
                val ok = TProxyService.TProxyStartService(configFile.absolutePath, pfd.fd)
                if (!ok) throw IllegalStateException("TProxyStartService failed")

                LocalProtection.setState(LocalProtection.State.ACTIVE)
                Log.i(TAG, "ByeDPI VPN active, TUN fd=${pfd.fd}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ByeDPI VPN", e)
                LocalProtection.setState(LocalProtection.State.ERROR)
                stopVpn()
            }
        }
    }

    private fun stopVpn() {
        serviceScope.launch {
            try {
                TProxyService.TProxyStopService()
            } catch (e: Exception) { Log.w(TAG, "TProxyStopService error", e) }

            try {
                if (proxyFd >= 0) ByeDpiProxy.jniStopProxy(proxyFd)
            } catch (e: Exception) { Log.w(TAG, "jniStopProxy error", e) }

            proxyFd = -1
            tunPfd?.close()
            tunPfd = null
            LocalProtection.setState(LocalProtection.State.OFF)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVpn()
        LocalProtection.setState(LocalProtection.State.OFF)
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Proteção Anti-DPI",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "StreamVPN ByeDPI ativo" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ByeDpiVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("StreamVPN")
            .setContentText("Proteção ativa")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .addAction(android.R.drawable.ic_media_pause, "Parar", stopIntent)
            .setOngoing(true)
            .build()
    }
}
