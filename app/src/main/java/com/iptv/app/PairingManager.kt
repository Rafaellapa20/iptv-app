package com.iptv.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Emparelhamento real TV <-> telemóvel.
 * A TV pede um código de 6 dígitos, o telemóvel (já com sessão iniciada)
 * submete as credenciais guardadas para esse código, e a TV recebe-as
 * automaticamente por polling, sem necessidade de escrever nada com o comando.
 *
 * Usa um cliente HTTP dedicado (ligação direta ao servidor de emparelhamento,
 * que é independente do relay/DoH usado para o conteúdo IPTV).
 */
object PairingManager {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    data class PairedCredentials(val username: String, val password: String)

    /**
     * Gera um código próprio: cria o código no servidor e submete de imediato
     * as credenciais deste dispositivo, ficando o código "pronto a usar" para
     * que um dispositivo novo (ainda sem sessão) o introduza e entre na conta
     * automaticamente, sem escrever utilizador/senha.
     * Devolve o código ou null em caso de erro.
     */
    suspend fun generateSelfCode(username: String, password: String): String? {
        val code = createCode() ?: return null
        val ok = submitCredentials(code, username, password)
        return if (ok) code else null
    }

    /** Pede um novo código de emparelhamento ao servidor. Devolve null em caso de erro. */
    suspend fun createCode(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${Constants.PAIR_API_URL}/pair/create")
                .post("".toRequestBody(null))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                JSONObject(body).optString("code", "").ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Submete as credenciais do utilizador atual para o código mostrado na TV. */
    suspend fun submitCredentials(code: String, username: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject()
                json.put("code", code)
                json.put("username", username)
                json.put("password", password)
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${Constants.PAIR_API_URL}/pair/submit")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val respBody = response.body?.string() ?: return@withContext false
                    JSONObject(respBody).optBoolean("ok", false)
                }
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Consulta uma vez se já chegaram credenciais para este código.
     * Devolve Triple(pending, credentials, erroFatal):
     * - pending = true -> continuar a fazer polling
     * - credentials != null -> emparelhamento concluído com sucesso
     * - erroFatal = true -> código inválido/expirado, parar polling
     */
    suspend fun pollOnce(code: String): PollResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${Constants.PAIR_API_URL}/pair/poll?code=$code")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext PollResult(pending = false, credentials = null, expired = true)
                }
                if (!response.isSuccessful) {
                    return@withContext PollResult(pending = true, credentials = null, expired = false)
                }
                val body = response.body?.string() ?: return@withContext PollResult(true, null, false)
                val json = JSONObject(body)
                if (json.optBoolean("pending", true)) {
                    PollResult(pending = true, credentials = null, expired = false)
                } else {
                    val user = json.optString("username", "")
                    val pass = json.optString("password", "")
                    PollResult(pending = false, credentials = PairedCredentials(user, pass), expired = false)
                }
            }
        } catch (e: Exception) {
            PollResult(pending = true, credentials = null, expired = false)
        }
    }

    data class PollResult(val pending: Boolean, val credentials: PairedCredentials?, val expired: Boolean)
}
