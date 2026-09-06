package com.iptv.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Emparelhamento real TV <-> telemóvel via backend StreamVPN (sem Firestore).
 *
 * A TV faz POST /api/pairing → recebe código de 6 dígitos.
 * O telemóvel introduz o código → GET /api/pairing/:code → recebe credenciais.
 * Código de uso único, TTL 10 minutos, gerido pelo servidor.
 */
object PairingManager {

    private val JSON_MT = "application/json; charset=utf-8".toMediaType()

    data class PairedCredentials(val username: String, val password: String)

    private fun baseUrl(context: Context): String {
        // Usa o mesmo servidor que o StreamVpnApi mas sem o /api no fim
        val apiUrl = StreamVpnApi.baseUrl(context)              // ex: https://streamvpn.faktio.ch:8444/api
        return apiUrl.removeSuffix("/api").removeSuffix("/")    // → https://streamvpn.faktio.ch:8444
    }

    /**
     * Gera um código de 6 dígitos no servidor e devolve-o.
     * Devolve null se houver erro de rede ou o servidor falhar.
     */
    suspend fun generateSelfCode(context: Context, username: String, password: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("username", username)
                    .put("password", password)
                    .toString()
                    .toRequestBody(JSON_MT)
                val req = Request.Builder()
                    .url("${baseUrl(context)}/api/pairing")
                    .post(body)
                    .build()
                val resp = OkHttpProvider.client.newCall(req).execute()
                if (!resp.isSuccessful) return@withContext null
                JSONObject(resp.body?.string() ?: "").optString("code").ifBlank { null }
            } catch (e: Exception) {
                null
            }
        }

    data class PollResult(val pending: Boolean, val credentials: PairedCredentials?, val expired: Boolean)

    /**
     * Tenta consumir o código. Uso único — o servidor apaga-o na primeira leitura com sucesso.
     * - pending=true  → erro temporário de rede, tentar novamente
     * - credentials != null → sucesso, emparelhamento concluído
     * - expired=true  → código inválido ou expirado, parar polling
     */
    suspend fun pollOnce(context: Context, code: String): PollResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${baseUrl(context)}/api/pairing/$code")
                .get()
                .build()
            val resp = OkHttpProvider.client.newCall(req).execute()
            when (resp.code) {
                200 -> {
                    val j = JSONObject(resp.body?.string() ?: "")
                    val u = j.optString("username")
                    val p = j.optString("password")
                    if (u.isNotBlank() && p.isNotBlank())
                        PollResult(false, PairedCredentials(u, p), false)
                    else
                        PollResult(false, null, true)
                }
                404 -> PollResult(false, null, true)   // expirado ou inválido
                else -> PollResult(true, null, false)  // erro temporário
            }
        } catch (e: Exception) {
            PollResult(true, null, false) // sem rede — tentar novamente
        }
    }
}
