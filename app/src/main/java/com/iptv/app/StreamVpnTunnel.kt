package com.iptv.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
import java.util.concurrent.TimeUnit

/**
 * Túnel StreamVPN (WireGuard) — a peça que faz o tráfego do IPTV passar pelo
 * servidor atribuído no painel.
 *
 * Fluxo:
 *   1. [ensurePermission] pede a autorização de VPN ao Android (uma vez)
 *   2. [connect] pede ao backend a config atribuída (/vpn/config), tenta a
 *      principal e depois as reservas até uma responder
 *   3. Se cair, [reconnect] tenta de novo com back-off (só se autoStart ligado)
 *
 * Nunca bloqueia a app IPTV: qualquer falha fica em [state]/[lastError] e a app
 * continua sem túnel.
 */
object StreamVpnTunnel {

    private const val TAG = "StreamVpnTunnel"
    private const val PREF_AUTO = "STREAMVPN_AUTO"
    private const val PREF_LAST_SERVER = "STREAMVPN_LAST_SERVER"
    const val REQUEST_VPN_PERMISSION = 7741

    enum class State { OFF, CONNECTING, ON, ERROR }

    interface Listener { fun onVpnStateChanged(state: State, serverName: String?) }

    private lateinit var app: Context
    private var backend: GoBackend? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectJob: Job? = null
    private var manualDisconnect = false
    private val listeners = mutableSetOf<Listener>()
    private val probe = OkHttpClient.Builder().connectTimeout(6, TimeUnit.SECONDS).readTimeout(6, TimeUnit.SECONDS).build()

    @Volatile var state: State = State.OFF; private set
    @Volatile var serverName: String? = null; private set
    @Volatile var lastError: String? = null; private set
    @Volatile var expiresAt: String? = null; private set

    private val tunnel = object : Tunnel {
        override fun getName() = "streamvpn"
        override fun onStateChange(newState: Tunnel.State) {
            Log.i(TAG, "Estado do túnel: $newState")
            if (newState == Tunnel.State.DOWN && state == State.ON) {
                // caiu sem sermos nós — tenta religar
                setState(State.OFF)
                if (!manualDisconnect && autoStart(app)) scheduleReconnect()
            }
        }
    }

    // ---------------- API pública ----------------

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        backend = GoBackend(app)
    }

    fun autoStart(context: Context) =
        context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).getBoolean(PREF_AUTO, true)

    fun setAutoStart(context: Context, on: Boolean) =
        context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).edit().putBoolean(PREF_AUTO, on).apply()

    fun addListener(l: Listener) { listeners += l; l.onVpnStateChanged(state, serverName) }
    fun removeListener(l: Listener) { listeners -= l }

    /**
     * Garante a autorização de VPN. Devolve true se já está dada; se não,
     * abre o diálogo do sistema e devolve false — chama [connect] em
     * onActivityResult quando REQUEST_VPN_PERMISSION der RESULT_OK.
     */
    fun ensurePermission(activity: Activity): Boolean {
        val intent = GoBackend.VpnService.prepare(activity) ?: return true
        activity.startActivityForResult(intent, REQUEST_VPN_PERMISSION)
        return false
    }

    fun hasPermission(context: Context) = GoBackend.VpnService.prepare(context) == null

    /** Liga (ou religa) ao servidor atribuído. Seguro chamar várias vezes. */
    fun connect(context: Context) {
        init(context)
        if (state == State.CONNECTING || state == State.ON) return
        if (!StreamVpnApi.isLoggedIn(app)) { fail("StreamVPN não ativada neste aparelho"); return }
        if (!hasPermission(app)) { fail("Sem autorização de VPN"); return }
        manualDisconnect = false
        reconnectJob?.cancel()
        setState(State.CONNECTING)
        scope.launch { doConnect() }
    }

    /** Desliga e não volta a ligar sozinho até [connect] ou reinício da app. */
    fun disconnect() {
        manualDisconnect = true
        reconnectJob?.cancel()
        scope.launch {
            try { backend?.setState(tunnel, Tunnel.State.DOWN, null) } catch (e: Exception) { Log.w(TAG, "down: ${e.message}") }
            serverName = null
            setState(State.OFF)
        }
    }

    /** Bytes recebidos/enviados pelo túnel (para o ecrã e para o heartbeat). */
    fun stats(): Pair<Long, Long> = try {
        val s = backend?.getStatistics(tunnel)
        Pair(s?.totalRx() ?: 0L, s?.totalTx() ?: 0L)
    } catch (_: Exception) { Pair(0L, 0L) }

    // ---------------- Interno ----------------

    private suspend fun doConnect() {
        val cfg = StreamVpnApi.vpnConfig(app).getOrElse { fail(it.message ?: "Erro ao obter configuração"); return }
        expiresAt = cfg.expiresAt
        val candidates = cfg.all
        if (candidates.isEmpty()) { fail("Nenhum servidor VPN atribuído"); return }

        for ((i, entry) in candidates.withIndex()) {
            Log.i(TAG, "A tentar ${entry.serverName} / ${entry.wireguardName} (${i + 1}/${candidates.size})")
            try {
                val parsed = Config.parse(BufferedReader(StringReader(entry.config)))
                backend!!.setState(tunnel, Tunnel.State.UP, parsed)
            } catch (e: Exception) {
                Log.w(TAG, "Falha a levantar túnel: ${e.message}")
                continue
            }
            // O túnel está UP; confirma que passa tráfego (handshake + resposta do backend pelo túnel)
            if (probeThroughTunnel()) {
                serverName = entry.serverName
                app.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).edit()
                    .putString(PREF_LAST_SERVER, entry.serverName).putBoolean("VPN_ENABLED", true).apply()
                setState(State.ON)
                return
            }
            Log.w(TAG, "${entry.serverName}: túnel sem resposta, passa ao seguinte")
            try { backend?.setState(tunnel, Tunnel.State.DOWN, null) } catch (_: Exception) {}
        }
        fail("Nenhum servidor VPN respondeu")
        if (autoStart(app)) scheduleReconnect()
    }

    /** Dá até ~8 s ao handshake e depois testa um pedido real pelo túnel. */
    private suspend fun probeThroughTunnel(): Boolean {
        repeat(4) { attempt ->
            delay(2000)
            val ok = withContext(Dispatchers.IO) {
                try {
                    probe.newCall(Request.Builder().url(StreamVpnApi.baseUrl(app) + "/health").build()).execute().use { it.isSuccessful }
                } catch (_: Exception) { false }
            }
            if (ok) return true
            Log.d(TAG, "probe ${attempt + 1}/4 falhou")
        }
        return false
    }

    private var backoff = 10_000L
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(120_000L)
            if (!manualDisconnect && state != State.ON) { setState(State.CONNECTING); doConnect() }
        }
    }

    private fun fail(msg: String) {
        Log.w(TAG, msg)
        lastError = msg
        app.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE).edit().putBoolean("VPN_ENABLED", false).apply()
        setState(State.ERROR)
    }

    private fun setState(s: State) {
        state = s
        if (s == State.ON) { backoff = 10_000L; lastError = null }
        val name = serverName
        val snapshot = listeners.toList()
        scope.launch(Dispatchers.Main) { snapshot.forEach { it.onVpnStateChanged(s, name) } }
    }
}
