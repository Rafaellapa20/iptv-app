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

@OptIn(DelicateCoroutinesApi::class)
object SyncManager {
    // GlobalScope é usado deliberadamente aqui: o sync deve continuar mesmo que
    // a Activity que o disparou (ex: LoginActivity) seja destruída logo a seguir.

    // DESATIVADO (2026-09-04): este servidor corria na VPS 176.111.109.14, que
    // deixou de estar sob o nosso controlo. Continuar a chamar este endpoint
    // enviaria username + favoritos + progresso + histórico de visualização de
    // utilizadores reais para um servidor de terceiros desconhecido. As funções
    // abaixo ficam como no-ops (favoritos/progresso/histórico continuam a
    // funcionar normalmente, só deixam de sincronizar entre dispositivos) até
    // existir um servidor de sync próprio e de confiança para apontar aqui.
    private const val SYNC_ENABLED = false
    private const val SYNC_URL = "http://176.111.109.14:5000/sync/"

    fun syncToCloud(context: Context) {
        if (!SYNC_ENABLED) return
        val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        if (username.isEmpty()) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Read local data
                val favPrefs = context.getSharedPreferences("IPTV_Favorites", Context.MODE_PRIVATE)
                val favArray = JSONArray(favPrefs.getString("favorites_list", "[]"))
                
                val progPrefs = context.getSharedPreferences("IPTV_Progress", Context.MODE_PRIVATE)
                val progArray = JSONArray(progPrefs.getString("recent_list", "[]"))
                
                val recPrefs = context.getSharedPreferences("IPTV_Recent", Context.MODE_PRIVATE)
                val recArray = JSONArray(recPrefs.getString("recent_list", "[]"))

                val json = JSONObject()
                json.put("favorites", favArray)
                json.put("progress", progArray)
                json.put("recent", recArray)

                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$SYNC_URL$username")
                    .post(body)
                    .build()
                
                val directClient = okhttp3.OkHttpClient.Builder().build()
                directClient.newCall(request).execute().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncFromCloud(context: Context, onComplete: () -> Unit = {}) {
        if (!SYNC_ENABLED) {
            onComplete()
            return
        }
        val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        if (username.isEmpty()) {
            onComplete()
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$SYNC_URL$username")
                    .get()
                    .build()

                val directClient = okhttp3.OkHttpClient.Builder().build()
                val response = directClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: "{}"
                    val json = JSONObject(bodyString)
                    
                    val favs = json.optJSONArray("favorites")
                    if (favs != null && favs.length() > 0) {
                        context.getSharedPreferences("IPTV_Favorites", Context.MODE_PRIVATE)
                            .edit().putString("favorites_list", favs.toString()).apply()
                    }
                    
                    val progs = json.optJSONArray("progress")
                    if (progs != null && progs.length() > 0) {
                        context.getSharedPreferences("IPTV_Progress", Context.MODE_PRIVATE)
                            .edit().putString("recent_list", progs.toString()).apply()
                    }
                    
                    val recents = json.optJSONArray("recent")
                    if (recents != null && recents.length() > 0) {
                        context.getSharedPreferences("IPTV_Recent", Context.MODE_PRIVATE)
                            .edit().putString("recent_list", recents.toString()).apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}
