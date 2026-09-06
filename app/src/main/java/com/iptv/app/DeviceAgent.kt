package com.iptv.app

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.PixelCopy
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Agente de controlo remoto (StreamVPN).
 *
 * Mantém um WebSocket com o backend, envia heartbeat a cada 30 s com o estado
 * da app (ecrã atual, canal, VPN) e executa comandos do painel:
 *
 *   KEY        { key: "UP|DOWN|LEFT|RIGHT|OK|BACK|HOME|MENU" }  → tecla na activity atual
 *   MESSAGE    { text, title? }                                → diálogo no ecrã
 *   RESTART                                                     → reinicia a app
 *   SCREENSHOT                                                  → envia captura do ecrã
 *   LIVE_ON / LIVE_OFF                                          → frames a cada 1,5 s
 *   RELOAD                                                      → volta ao ecrã principal
 *
 * Nunca interfere com a app IPTV: se o backend estiver em baixo, fica em
 * silêncio a tentar religar com back-off. Chama-se uma vez em
 * IPTVApplication.onCreate(): DeviceAgent.start(this)
 */
object DeviceAgent : Application.ActivityLifecycleCallbacks {

    private const val TAG = "DeviceAgent"
    private const val HEARTBEAT_MS = 30_000L
    private const val FRAME_MS = 1_500L
    private const val PREF_DEVICE_ID = "STREAMVPN_DEVICE_ID"

    private lateinit var app: Application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS).build()

    private var ws: WebSocket? = null
    private var connected = false
    private var reconnectDelay = 5_000L
    private var liveJob: Job? = null
    private var currentActivity: Activity? = null
    private var playingTitle: String? = null
    private var started = false

    // ---------------- Arranque ----------------

    fun start(application: Application) {
        if (started) return
        started = true
        app = application
        app.registerActivityLifecycleCallbacks(this)
        scope.launch { connectLoop() }
    }

    /** A app chama isto quando começa a reproduzir algo, para aparecer no painel. */
    fun setPlaying(title: String?) { playingTitle = title }

    private fun deviceId(): String {
        val prefs = app.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        return prefs.getString(PREF_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(PREF_DEVICE_ID, it).apply()
        }
    }

    private suspend fun connectLoop() {
        while (scope.isActive) {
            if (!connected) {
                val logged = StreamVpnApi.ensureLoggedIn(app).isSuccess
                if (logged) connect() else Log.d(TAG, "Sem sessão StreamVPN; tento mais tarde")
            }
            delay(if (connected) HEARTBEAT_MS else reconnectDelay)
            if (connected) send(heartbeat("heartbeat"))
            else reconnectDelay = (reconnectDelay * 2).coerceAtMost(120_000L)
        }
    }

    private fun connect() {
        val token = StreamVpnApi.token(app) ?: return
        val base = StreamVpnApi.baseUrl(app)                       // http(s)://host[:porta]/api
            .replaceFirst("http", "ws").removeSuffix("/api")
        val req = Request.Builder().url("$base/ws/device?token=$token").build()
        ws = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connected = true; reconnectDelay = 5_000L
                Log.i(TAG, "Ligado ao hub")
                webSocket.send(heartbeat("hello").toString())
            }
            override fun onMessage(webSocket: WebSocket, text: String) { handle(text) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connected = false; stopLive()
                if (response?.code == 401) StreamVpnApi.logout(app)   // token expirou → novo login no próximo ciclo
                Log.w(TAG, "WS falhou: ${t.message}")
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { connected = false; stopLive() }
        })
    }

    private fun send(obj: JSONObject) { ws?.send(obj.toString()) }

    // ---------------- Heartbeat ----------------

    private fun heartbeat(type: String): JSONObject {
        val prefs = app.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val device = JSONObject()
            .put("deviceId", deviceId())
            .put("mac", macAddress() ?: JSONObject.NULL)
            .put("model", Build.MODEL)
            .put("brand", Build.MANUFACTURER)
            .put("androidVersion", Build.VERSION.RELEASE)
            .put("appVersion", try { app.packageManager.getPackageInfo(app.packageName, 0).versionName } catch (_: Exception) { "?" })
            .put("screen", currentActivity?.javaClass?.simpleName ?: JSONObject.NULL)
            .put("playing", playingTitle ?: JSONObject.NULL)
            .put("vpnOn", StreamVpnTunnel.state == StreamVpnTunnel.State.ON)
            .put("vpnServer", StreamVpnTunnel.serverName ?: JSONObject.NULL)
            .put("vpnMode", if (StreamVpnTunnel.state == StreamVpnTunnel.State.ON) "wireguard" else JSONObject.NULL)
        return JSONObject().put("type", type).put("device", device)
    }

    private fun macAddress(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .firstOrNull { it.name.equals("wlan0", true) || it.name.equals("eth0", true) }
            ?.hardwareAddress?.joinToString(":") { "%02X".format(it) }
            ?.takeIf { it != "02:00:00:00:00:00" }
    } catch (_: Exception) { null }

    // ---------------- Comandos ----------------

    private fun handle(text: String) {
        val msg = try { JSONObject(text) } catch (_: Exception) { return }
        if (msg.optString("type") != "command") return
        val id = msg.optString("id")
        val command = msg.optString("command")
        val payload = msg.optJSONObject("payload") ?: JSONObject()

        when (command) {
            "KEY" -> main.post { result(id, command, sendKey(payload.optString("key"))) }
            "MESSAGE" -> main.post { showMessage(payload.optString("title", "Mensagem do suporte"), payload.optString("text")); result(id, command, true) }
            "SCREENSHOT" -> captureFrame(snapshot = true) { ok -> result(id, command, ok, if (ok) "Captura enviada" else "Sem ecrã visível") }
            "LIVE_ON" -> startLive()
            "LIVE_OFF" -> stopLive()
            "RELOAD" -> main.post { goHome(); result(id, command, true) }
            "RESTART" -> { result(id, command, true, "A reiniciar"); main.postDelayed({ restartApp() }, 500) }
            else -> result(id, command, false, "Comando desconhecido")
        }
    }

    private fun result(id: String, command: String, ok: Boolean, detail: String? = null) {
        send(JSONObject().put("type", "result").put("id", id).put("command", command).put("ok", ok).put("detail", detail ?: JSONObject.NULL))
    }

    private fun sendKey(key: String): Boolean {
        val act = currentActivity ?: return false
        val code = when (key.uppercase()) {
            "UP" -> KeyEvent.KEYCODE_DPAD_UP
            "DOWN" -> KeyEvent.KEYCODE_DPAD_DOWN
            "LEFT" -> KeyEvent.KEYCODE_DPAD_LEFT
            "RIGHT" -> KeyEvent.KEYCODE_DPAD_RIGHT
            "OK", "ENTER" -> KeyEvent.KEYCODE_DPAD_CENTER
            "BACK" -> KeyEvent.KEYCODE_BACK
            "MENU" -> KeyEvent.KEYCODE_MENU
            "HOME" -> { goHome(); return true }
            else -> return false
        }
        act.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        act.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        return true
    }

    private fun showMessage(title: String, text: String) {
        val act = currentActivity
        if (act == null || act.isFinishing) { Toast.makeText(app, text, Toast.LENGTH_LONG).show(); return }
        AlertDialog.Builder(act).setTitle(title).setMessage(text).setPositiveButton("OK", null).show()
    }

    private fun goHome() {
        val i = Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        app.startActivity(i)
    }

    private fun restartApp() {
        val i = app.packageManager.getLaunchIntentForPackage(app.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) ?: return
        app.startActivity(i)
        Runtime.getRuntime().exit(0)
    }

    // ---------------- Ecrã ----------------

    private fun startLive() {
        if (liveJob?.isActive == true) return
        liveJob = scope.launch {
            while (isActive && connected) { captureFrame(snapshot = false) {}; delay(FRAME_MS) }
        }
    }

    private fun stopLive() { liveJob?.cancel(); liveJob = null }

    /** Captura a janela da activity atual (menus e listas; o vídeo em SurfaceView sai preto — limitação Android). */
    private fun captureFrame(snapshot: Boolean, done: (Boolean) -> Unit) {
        main.post {
            val act = currentActivity
            val view = act?.window?.decorView
            if (act == null || view == null || view.width == 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { done(false); return@post }
            // reduz para ~640 px de largura: leve o suficiente para 1 frame/1,5 s
            val scale = 640f / view.width
            val bmp = Bitmap.createBitmap((view.width * scale).toInt(), (view.height * scale).toInt(), Bitmap.Config.RGB_565)
            try {
                PixelCopy.request(act.window, bmp, { res ->
                    if (res == PixelCopy.SUCCESS) {
                        scope.launch {
                            val out = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.JPEG, if (snapshot) 70 else 45, out)
                            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                            send(JSONObject().put("type", "frame").put("jpeg", b64).put("snapshot", snapshot))
                            done(true)
                        }
                    } else done(false)
                }, main)
            } catch (e: Exception) { Log.w(TAG, "PixelCopy: ${e.message}"); done(false) }
        }
    }

    // ---------------- Lifecycle ----------------

    override fun onActivityResumed(activity: Activity) { currentActivity = activity; if (connected) send(heartbeat("heartbeat")) }
    override fun onActivityPaused(activity: Activity) { if (currentActivity === activity) currentActivity = null }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
