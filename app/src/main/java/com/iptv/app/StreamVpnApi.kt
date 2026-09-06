package com.iptv.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente para o backend StreamVPN (streamvpn.faktio.ch:3000).
 * Segue o mesmo estilo do resto da app: OkHttp + JSONObject, sem Retrofit.
 * O token JWT fica guardado em IPTV_PREFS (STREAMVPN_TOKEN).
 */
object StreamVpnApi {

    private const val TAG = "StreamVpnApi"
    private const val PREF_TOKEN = "STREAMVPN_TOKEN"
    private const val PREF_BASE_URL = "STREAMVPN_BASE_URL"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun baseUrl(context: Context): String {
        val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        return prefs.getString(PREF_BASE_URL, Constants.STREAMVPN_BASE_URL) ?: Constants.STREAMVPN_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
            .edit().putString(PREF_BASE_URL, url.trimEnd('/')).apply()
    }

    fun token(context: Context): String? =
        context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
            .getString(PREF_TOKEN, null)

    fun isLoggedIn(context: Context) = !token(context).isNullOrBlank()

    fun logout(context: Context) {
        context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
            .edit().remove(PREF_TOKEN).apply()
    }

    data class VpnStatus(val status: String, val connectedSince: String?, val currentServer: String?)

    data class ConnectionInfo(
        val ipAddress: String, val localIp: String, val publicIp: String,
        val protocol: String, val bytesDownloaded: Long, val bytesUploaded: Long
    )

    data class SpeedTest(val download: String, val upload: String, val ping: String)

    data class VpnServer(val id: String, val name: String, val location: String?, val country: String?, val ping: Int)

    data class Quota(val monthlyGb: Long, val usedGb: Long, val remainingGb: Long, val status: String) {
        val percentUsed: Int get() = if (monthlyGb <= 0) 0 else ((usedGb * 100) / monthlyGb).toInt().coerceIn(0, 100)
    }

    // ---------- Auth ----------

    /** Login com o mesmo utilizador/password da app IPTV. */
    suspend fun login(context: Context, username: String, password: String): Result<Unit> = call {
        val body = JSONObject().put("username", username).put("password", password)
        val json = post(context, "/auth/login", body, auth = false)
        val token = json.optString("token")
        if (token.isBlank()) error("Resposta sem token")
        context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
            .edit().putString(PREF_TOKEN, token).apply()
    }

    /**
     * Ativação com o código dado pelo painel (ex.: SVPN-7K3M-9QX2). Faz-se uma
     * vez; o token de longa duração fica guardado e a partir daí é automático.
     */
    suspend fun activate(context: Context, code: String): Result<Unit> = call {
        val body = JSONObject().put("code", code.trim())
        val json = post(context, "/auth/activate", body, auth = false)
        val token = json.optString("token")
        if (token.isBlank()) error("Resposta sem token")
        context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
            .edit().putString(PREF_TOKEN, token).apply()
    }

    /**
     * Há sessão StreamVPN? A conta StreamVPN é independente do login IPTV, por isso
     * só existe sessão depois de o cliente ter metido o código uma vez. Falha
     * silenciosamente — a app IPTV nunca depende disto.
     */
    suspend fun ensureLoggedIn(context: Context): Result<Unit> =
        if (isLoggedIn(context)) Result.success(Unit)
        else Result.failure(IllegalStateException("StreamVPN ainda não ativada neste aparelho"))

    /**
     * O que o backend entrega a esta conta: a config WireGuard (rodada
     * automaticamente entre as ativas do servidor), a lista de DNS IPTV por
     * ordem de tentativa, e a validade.
     */
    /** Um servidor + WireGuard concreto. `fallbacks` em VpnConfig são os de reserva, pela ordem do painel. */
    data class VpnEntry(val serverName: String, val wireguardName: String, val endpoint: String?, val config: String, val country: String = "")

    data class VpnConfig(
        val primary: VpnEntry, val fallbacks: List<VpnEntry>,
        val dns: List<String>, val requireClientApp: Boolean, val expiresAt: String?,
        val forceWireguard: Boolean = false
    ) {
        /** Todos por ordem de tentativa: principal primeiro. */
        val all: List<VpnEntry> get() = listOf(primary) + fallbacks
    }

    private fun entry(j: JSONObject) = VpnEntry(
        serverName = j.optJSONObject("server")?.optString("name") ?: "",
        wireguardName = j.optJSONObject("wireguard")?.optString("name") ?: "",
        endpoint = j.optJSONObject("wireguard")?.optString("endpoint")?.ifBlank { null },
        config = j.optString("config"),
        country = j.optJSONObject("server")?.optString("country") ?: ""
    )

    suspend fun vpnConfig(context: Context): Result<VpnConfig> = call {
        val j = get(context, "/vpn/config")
        val dnsArr = j.optJSONArray("dns")
        val fbArr = j.optJSONArray("fallbacks")
        VpnConfig(
            primary = entry(j),
            fallbacks = if (fbArr == null) emptyList() else (0 until fbArr.length()).map { entry(fbArr.getJSONObject(it)) },
            dns = if (dnsArr == null) emptyList() else (0 until dnsArr.length()).map { dnsArr.getString(it) },
            requireClientApp = j.optBoolean("requireClientApp"),
            expiresAt = j.optString("expiresAt").ifBlank { null },
            forceWireguard = j.optBoolean("forceWireguard", false)
        )
    }

    suspend fun health(context: Context): Result<Boolean> = call {
        get(context, "/health", auth = false).optString("status") == "online"
    }

    suspend fun status(context: Context): Result<VpnStatus> = call {
        val j = get(context, "/vpn/status")
        VpnStatus(
            status = j.optString("status", "disconnected"),
            connectedSince = j.optString("connectedSince").ifBlank { null },
            currentServer = j.optString("currentServer").ifBlank { null }
        )
    }

    suspend fun connectionInfo(context: Context): Result<ConnectionInfo> = call {
        val j = get(context, "/vpn/connection-info")
        ConnectionInfo(
            ipAddress = j.optString("ipAddress", "N/A"),
            localIp = j.optString("localIp", "N/A"),
            publicIp = j.optString("publicIp", "N/A"),
            protocol = j.optString("protocol", "WireGuard"),
            bytesDownloaded = j.optLong("bytesDownloaded"),
            bytesUploaded = j.optLong("bytesUploaded")
        )
    }

    suspend fun speedTest(context: Context): Result<SpeedTest> = call {
        val j = post(context, "/vpn/speed-test", JSONObject())
        SpeedTest(j.optString("downloadSpeed"), j.optString("uploadSpeed"), j.optString("ping"))
    }

    suspend fun reconnect(context: Context): Result<String> = call {
        post(context, "/vpn/reconnect", JSONObject()).optString("message", "OK")
    }

    suspend fun availableServers(context: Context): Result<List<VpnServer>> = call {
        val arr = getArray(context, "/vpn/available-servers")
        (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            VpnServer(
                id = s.optString("id"),
                name = s.optString("name"),
                location = s.optString("location").ifBlank { null },
                country = s.optString("country").ifBlank { null },
                ping = s.optInt("ping", -1)
            )
        }
    }

    suspend fun changeServer(context: Context, serverId: String): Result<String> = call {
        post(context, "/vpn/change-server/$serverId", JSONObject()).optString("message", "OK")
    }

    suspend fun quota(context: Context): Result<Quota> = call {
        val j = get(context, "/vpn/quota")
        Quota(
            monthlyGb = j.optLong("monthlyBandwidth"),
            usedGb = j.optLong("usedBandwidth"),
            remainingGb = j.optLong("remainingBandwidth"),
            status = j.optString("status", "good")
        )
    }

    suspend fun logs(context: Context): Result<JSONArray> = call { getArray(context, "/vpn/logs") }

    suspend fun usageAnalytics(context: Context): Result<JSONObject> = call { get(context, "/vpn/usage-analytics") }

    private suspend inline fun <T> call(crossinline block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            try { Result.success(block()) }
            catch (e: Exception) {
                Log.w(TAG, "Falha no pedido: ${e.message}")
                Result.failure(e)
            }
        }

    private fun request(context: Context, path: String, auth: Boolean): Request.Builder {
        val b = Request.Builder().url(baseUrl(context) + path)
        if (auth) {
            val t = token(context) ?: error("Sem sessão StreamVPN — faz login primeiro")
            b.header("Authorization", "Bearer $t")
        }
        return b
    }

    private fun execute(req: Request): String {
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (resp.code == 401) error("Sessão expirada (401)")
            if (!resp.isSuccessful) {
                val msg = try { JSONObject(body).optString("error") } catch (_: Exception) { "" }
                error(msg.ifBlank { "HTTP ${resp.code}" })
            }
            return body
        }
    }

    private fun get(context: Context, path: String, auth: Boolean = true): JSONObject =
        JSONObject(execute(request(context, path, auth).get().build()))

    private fun getArray(context: Context, path: String, auth: Boolean = true): JSONArray =
        JSONArray(execute(request(context, path, auth).get().build()))

    private fun post(context: Context, path: String, body: JSONObject, auth: Boolean = true): JSONObject =
        JSONObject(execute(request(context, path, auth).post(body.toString().toRequestBody(JSON)).build()))
}
