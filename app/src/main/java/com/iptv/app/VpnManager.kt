package com.iptv.app

import android.app.Activity
import android.content.Context
import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.Request
import java.io.IOException

/**
 * Single entry-point for all VPN/protection logic.
 *
 * Modes (persisted in IPTV_PREFS as STREAMVPN_MODE):
 *   AUTO         — try ByeDPI first; fall back to WireGuard; else nothing
 *   LOCAL_ONLY   — ByeDPI only
 *   WIREGUARD    — WireGuard only
 *   OFF          — nothing
 *
 * Only one VpnService may be active at a time on Android.
 */
object VpnManager {

    private const val TAG = "VpnManager"
    private const val PREF_MODE = "STREAMVPN_MODE"

    enum class Mode { AUTO, LOCAL_ONLY, WIREGUARD, OFF }

    // Combined state visible to UI
    sealed class TunnelState {
        object Off : TunnelState()
        object Connecting : TunnelState()
        data class ByeDpi(val ok: Boolean = true) : TunnelState()
        data class WireGuard(val serverName: String?, val serverCountry: String? = null) : TunnelState()
        object Error : TunnelState()
    }

    interface Listener { fun onTunnelStateChanged(state: TunnelState) }

    @Volatile var currentState: TunnelState = TunnelState.Off
        private set

    private val listeners = mutableListOf<Listener>()
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var retryJob: Job? = null

    fun addListener(l: Listener)    { synchronized(listeners) { listeners.add(l) } }
    fun removeListener(l: Listener) { synchronized(listeners) { listeners.remove(l) } }

    private fun notify(state: TunnelState) {
        currentState = state
        synchronized(listeners) { listeners.toList() }.forEach { it.onTunnelStateChanged(state) }
    }

    // ── Mode persistence ─────────────────────────────────────────────────────

    fun getMode(ctx: Context): Mode = try {
        Mode.valueOf(
            ctx.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
                .getString(PREF_MODE, Mode.AUTO.name) ?: Mode.AUTO.name
        )
    } catch (e: Exception) { Mode.AUTO }

    fun setMode(ctx: Context, mode: Mode) {
        ctx.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
            .edit().putString(PREF_MODE, mode.name).apply()
        // Apply immediately
        ensure(ctx)
    }

    // ── Main entry-point ─────────────────────────────────────────────────────

    /**
     * Call from MainActivity.onResume (and after permission grant).
     * Checks if a tunnel should be running and starts/stops as needed.
     */
    fun ensure(ctx: Context) {
        val mode = getMode(ctx)
        if (mode == Mode.OFF) { stopAll(ctx); return }
        if (!StreamVpnApi.isLoggedIn(ctx)) { stopAll(ctx); return }

        // Check VPN permission
        val permIntent = VpnService.prepare(ctx)
        if (permIntent != null) {
            // No permission yet — caller must request it
            Log.d(TAG, "VPN permission needed")
            return
        }

        retryJob?.cancel()
        retryJob = managerScope.launch { runCascade(ctx, mode) }
    }

    /**
     * Call when VPN permission is granted (Activity.onActivityResult).
     */
    fun onPermissionGranted(ctx: Context) = ensure(ctx)

    /**
     * Request VPN permission if needed. Returns true if permission was already
     * granted (caller can proceed); false if a dialog was launched.
     */
    fun ensurePermission(activity: Activity, requestCode: Int): Boolean {
        val intent = VpnService.prepare(activity) ?: return true
        activity.startActivityForResult(intent, requestCode)
        return false
    }

    fun stopAll(ctx: Context) {
        retryJob?.cancel()
        LocalProtection.stop(ctx)
        StreamVpnTunnel.disconnect()
        notify(TunnelState.Off)
    }

    // ── Cascade logic ────────────────────────────────────────────────────────

    private suspend fun runCascade(ctx: Context, mode: Mode) {
        notify(TunnelState.Connecting)

        when (mode) {
            Mode.LOCAL_ONLY -> startByeDpi(ctx)
            Mode.WIREGUARD  -> startWireGuard(ctx)
            Mode.OFF        -> stopAll(ctx)
            Mode.AUTO       -> {
                // Check if backend forces WireGuard for this user
                val forceWg = try {
                    StreamVpnApi.vpnConfig(ctx).getOrNull()?.forceWireguard == true
                } catch (e: Exception) { false }

                if (forceWg) {
                    Log.d(TAG, "AUTO: forceWireguard=true, skipping ByeDPI")
                    startWireGuard(ctx)
                    return
                }

                // 1. Try ByeDPI
                Log.d(TAG, "AUTO: trying ByeDPI")
                startByeDpi(ctx)

                // Wait up to 5s for ByeDPI to come up
                val deadline = System.currentTimeMillis() + 5_000
                while (LocalProtection.state != LocalProtection.State.ACTIVE) {
                    if (System.currentTimeMillis() > deadline) break
                    delay(200)
                }

                if (LocalProtection.state == LocalProtection.State.ACTIVE) {
                    // 2. Test connectivity through ByeDPI
                    Log.d(TAG, "AUTO: ByeDPI up, testing connectivity...")
                    val ok = testConnectivity(ctx)
                    if (ok) {
                        Log.d(TAG, "AUTO: ByeDPI works")
                        notify(TunnelState.ByeDpi())
                        return
                    }
                    Log.d(TAG, "AUTO: ByeDPI failed connectivity, switching to WireGuard")
                    LocalProtection.stop(ctx)
                    delay(800) // let the TUN tear down
                }

                // 3. Try WireGuard
                startWireGuard(ctx)

                // 4. If WireGuard also fails, schedule retry
                val wgDeadline = System.currentTimeMillis() + 15_000
                while (StreamVpnTunnel.state != StreamVpnTunnel.State.ON) {
                    if (System.currentTimeMillis() > wgDeadline) break
                    if (StreamVpnTunnel.state == StreamVpnTunnel.State.OFF ||
                        StreamVpnTunnel.state == StreamVpnTunnel.State.ERROR) break
                    delay(300)
                }

                if (StreamVpnTunnel.state != StreamVpnTunnel.State.ON) {
                    Log.w(TAG, "AUTO: both tunnels failed, retry in 2 min")
                    notify(TunnelState.Error)
                    retryJob = managerScope.launch {
                        delay(120_000)
                        runCascade(ctx, mode)
                    }
                }
            }
        }
    }

    private suspend fun startByeDpi(ctx: Context) {
        withContext(Dispatchers.Main) { LocalProtection.start(ctx) }
    }

    private suspend fun startWireGuard(ctx: Context) {
        // StreamVpnTunnel.connect() runs on IO internally
        withContext(Dispatchers.Main) { StreamVpnTunnel.connect(ctx) }
    }

    private suspend fun testConnectivity(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(Constants.STREAMVPN_BASE_URL.replace("/api", "") + "/api/health")
                .head()
                .build()
            val call = OkHttpProvider.client.newCall(req)
            try {
                val response = call.execute()
                response.close()
                true
            } catch (e: IOException) { false }
        } catch (e: Exception) { false }
    }

    // ── Wire up state listeners (call once, from Application) ────────────────

    fun init(ctx: Context) {
        // Mirror LocalProtection states to VpnManager
        LocalProtection.addListener(object : LocalProtection.Listener {
            override fun onStateChanged(state: LocalProtection.State) {
                when (state) {
                    LocalProtection.State.ACTIVE     -> notify(TunnelState.ByeDpi())
                    LocalProtection.State.CONNECTING -> if (currentState !is TunnelState.WireGuard) notify(TunnelState.Connecting)
                    LocalProtection.State.ERROR      -> if (currentState !is TunnelState.WireGuard) notify(TunnelState.Error)
                    LocalProtection.State.OFF        -> if (currentState is TunnelState.ByeDpi) notify(TunnelState.Off)
                }
            }
        })
        // Mirror StreamVpnTunnel states
        StreamVpnTunnel.addListener(object : StreamVpnTunnel.Listener {
            override fun onVpnStateChanged(state: StreamVpnTunnel.State, serverName: String?, serverCountry: String?) {
                when (state) {
                    StreamVpnTunnel.State.ON         -> notify(TunnelState.WireGuard(serverName, serverCountry))
                    StreamVpnTunnel.State.CONNECTING -> if (currentState !is TunnelState.ByeDpi) notify(TunnelState.Connecting)
                    StreamVpnTunnel.State.ERROR,
                    StreamVpnTunnel.State.OFF        -> if (currentState is TunnelState.WireGuard) notify(TunnelState.Off)
                }
            }
        })
    }
}
