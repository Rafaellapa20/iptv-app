package com.iptv.app

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Sincronização de favoritos, progresso e recentes via backend StreamVPN.
 * Chave: SHA-256(username + ':' + password) — calculado localmente, nunca
 * enviado em texto para o servidor. Só quem souber as credenciais consegue
 * calcular o hash certo para ler/escrever o documento.
 */
@OptIn(DelicateCoroutinesApi::class)
object SyncManager {

    // Endpoint: https://streamvpn.faktio.ch:8444/api/sync/<hash>
    private fun syncUrl(context: Context, hash: String): String {
        val base = StreamVpnApi.baseUrl(context) // ex: https://streamvpn.faktio.ch:8444/api
        return "$base/sync/$hash"
    }

    private fun syncHash(username: String, password: String): String {
        val input = "$username:$password"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun syncToCloud(context: Context) {
        val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        val password = prefs.getString("PASSWORD", "") ?: ""
        if (username.isEmpty() || password.isEmpty()) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val favPrefs = context.getSharedPreferences("IPTV_Favorites", Context.MODE_PRIVATE)
                val favArray = JSONArray(favPrefs.getString("favorites_list", "[]"))

                val progPrefs = context.getSharedPreferences("IPTV_Progress", Context.MODE_PRIVATE)
                val progArray = JSONArray(progPrefs.getString("recent_list", "[]"))

                val recPrefs = context.getSharedPreferences("IPTV_Recent", Context.MODE_PRIVATE)
                val recArray = JSONArray(recPrefs.getString("recent_list", "[]"))

                val json = JSONObject()
                    .put("favorites", favArray)
                    .put("progress", progArray)
                    .put("recent", recArray)

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url(syncUrl(context, syncHash(username, password)))
                    .post(body)
                    .build()
                OkHttpProvider.client.newCall(req).execute().close()
            } catch (_: Exception) {}
        }
    }

    fun syncFromCloud(context: Context, onComplete: () -> Unit = {}) {
        val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        val password = prefs.getString("PASSWORD", "") ?: ""
        if (username.isEmpty() || password.isEmpty()) { onComplete(); return }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(syncUrl(context, syncHash(username, password)))
                    .get()
                    .build()
                val response = OkHttpProvider.client.newCall(req).execute()

                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")

                    val favs = json.optJSONArray("favorites")
                    if (favs != null && favs.length() > 0)
                        context.getSharedPreferences("IPTV_Favorites", Context.MODE_PRIVATE)
                            .edit().putString("favorites_list", favs.toString()).apply()

                    val progs = json.optJSONArray("progress")
                    if (progs != null && progs.length() > 0)
                        context.getSharedPreferences("IPTV_Progress", Context.MODE_PRIVATE)
                            .edit().putString("recent_list", progs.toString()).apply()

                    val recents = json.optJSONArray("recent")
                    if (recents != null && recents.length() > 0)
                        context.getSharedPreferences("IPTV_Recent", Context.MODE_PRIVATE)
                            .edit().putString("recent_list", recents.toString()).apply()
                }
            } catch (_: Exception) {
            } finally {
                kotlinx.coroutines.withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }
}
