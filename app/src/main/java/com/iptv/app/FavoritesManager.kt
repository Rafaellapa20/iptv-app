package com.iptv.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object FavoritesManager {

    private const val PREFS_NAME   = "IPTV_Favorites"
    private const val FAVORITES_KEY = "favorites_list"
    private const val REMOVED_KEY   = "favorites_removed"

    data class FavoriteItem(
        val streamId: String,
        val title: String,
        val coverUrl: String,
        val type: String
    ) {
        fun toStream() = Stream(streamId, title, coverUrl, type, "")

        val stream_id: String get() = streamId
        val name: String get() = title
        val stream_icon: String get() = coverUrl
        val stream_type: String get() = type
        val extension: String get() = ""
    }

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /* ─── consulta ──────────────────────────────────────────────────── */

    fun isFavorite(context: Context, streamId: String): Boolean {
        if (streamId.isEmpty()) return false
        return getFavorites(context).any { it.streamId == streamId }
    }

    fun isFavorite(context: Context, stream: Stream): Boolean = isFavorite(context, stream.stream_id)

    fun getFavorites(context: Context): List<FavoriteItem> {
        val jsonString = getPrefs(context).getString(FAVORITES_KEY, "[]") ?: "[]"
        val list = mutableListOf<FavoriteItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("streamId", obj.optString("stream_id", ""))
                val title = obj.optString("title", obj.optString("name", "Sem Título"))
                val cover = obj.optString("coverUrl", obj.optString("stream_icon", ""))
                val type = obj.optString("type", obj.optString("stream_type", "live"))
                if (id.isNotEmpty()) list.add(FavoriteItem(id, title, cover, type))
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoritesManager", "Erro ao carregar favoritos: ${e.message}")
        }
        return list
    }

    /* ─── toggle ────────────────────────────────────────────────────── */

    /**
     * Adiciona ou remove um favorito. Ao adicionar, guarda "at" (timestamp)
     * para que a junção por data funcione correctamente. Ao remover, escreve
     * uma lápide em REMOVED_KEY para que a remoção se propague entre aparelhos.
     */
    fun toggleFavorite(context: Context, streamId: String, title: String, coverUrl: String, type: String): Boolean {
        if (streamId.isEmpty()) return false
        val prefs = getPrefs(context)
        val array = try { JSONArray(prefs.getString(FAVORITES_KEY, "[]")) } catch (e: Exception) { JSONArray() }

        // Procura o item existente
        var found = false
        val kept  = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optString("streamId") == streamId || obj.optString("stream_id") == streamId) {
                found = true // omite da lista (= remoção)
            } else {
                kept.put(obj)
            }
        }

        val now = System.currentTimeMillis()

        if (found) {
            // Gravação da lápide para que a remoção se propague
            val tombstones = try { JSONArray(prefs.getString(REMOVED_KEY, "[]")) } catch (e: Exception) { JSONArray() }
            tombstones.put(JSONObject().put("streamId", streamId).put("at", now))
            prefs.edit()
                .putString(FAVORITES_KEY, kept.toString())
                .putString(REMOVED_KEY, tombstones.toString())
                .apply()
            SyncManager.syncToCloud(context)
            return false
        }

        // Adição: coloca no topo com o timestamp
        val newItem = JSONObject()
            .put("streamId", streamId)
            .put("title",    title)
            .put("coverUrl", coverUrl)
            .put("type",     type)
            .put("at",       now)
        val finalArray = JSONArray()
        finalArray.put(newItem)
        for (i in 0 until array.length()) finalArray.put(array.get(i))
        prefs.edit().putString(FAVORITES_KEY, finalArray.toString()).apply()
        SyncManager.syncToCloud(context)
        return true
    }

    fun toggleFavorite(context: Context, stream: Stream): Boolean =
        toggleFavorite(context, stream.stream_id, stream.name, stream.stream_icon, stream.stream_type)

    /* ─── export / import (usados pelo SyncManager) ─────────────────── */

    /** Devolve o blob tal como está guardado. O SyncManager usa isto. */
    fun exportJson(context: Context): JSONArray =
        try { JSONArray(getPrefs(context).getString(FAVORITES_KEY, "[]")) }
        catch (e: Exception) { JSONArray() }

    /** Lápides: favoritos removidos, com data. Sem isto, remover nunca propaga. */
    fun exportRemovedJson(context: Context): JSONArray =
        try { JSONArray(getPrefs(context).getString(REMOVED_KEY, "[]")) }
        catch (e: Exception) { JSONArray() }

    /**
     * Guarda a lista juntada vinda do servidor. Substitui directamente sem
     * passar pelo SyncManager (evitar loop).
     */
    fun importJson(context: Context, favorites: JSONArray, removed: JSONArray) {
        getPrefs(context).edit()
            .putString(FAVORITES_KEY, favorites.toString())
            .putString(REMOVED_KEY,   removed.toString())
            .apply()
    }
}
