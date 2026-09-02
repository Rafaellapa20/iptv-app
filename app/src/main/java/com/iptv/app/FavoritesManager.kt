package com.iptv.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object FavoritesManager {

    private const val PREFS_NAME = "IPTV_Favorites"
    private const val FAVORITES_KEY = "favorites_list"

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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFavorite(context: Context, streamId: String): Boolean {
        if (streamId.isEmpty()) return false
        val list = getFavorites(context)
        return list.any { it.streamId == streamId }
    }
    
    fun isFavorite(context: Context, stream: Stream): Boolean = isFavorite(context, stream.stream_id)

    fun toggleFavorite(context: Context, streamId: String, title: String, coverUrl: String, type: String): Boolean {
        if (streamId.isEmpty()) return false
        val list = getFavorites(context).toMutableList()
        val existing = list.find { it.streamId == streamId }
        
        val isNowFavorite = if (existing != null) {
            list.remove(existing)
            false
        } else {
            list.add(0, FavoriteItem(streamId, title, coverUrl, type))
            true
        }
        
        saveFavorites(context, list)
        return isNowFavorite
    }
    
    fun toggleFavorite(context: Context, stream: Stream): Boolean =
        toggleFavorite(context, stream.stream_id, stream.name, stream.stream_icon, stream.stream_type)

    fun getFavorites(context: Context): List<FavoriteItem> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(FAVORITES_KEY, "[]") ?: "[]"
        val list = mutableListOf<FavoriteItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("streamId", obj.optString("stream_id", ""))
                val title = obj.optString("title", obj.optString("name", "Sem Título"))
                val cover = obj.optString("coverUrl", obj.optString("stream_icon", ""))
                val type = obj.optString("type", obj.optString("stream_type", "live"))
                
                if (id.isNotEmpty()) {
                    list.add(FavoriteItem(id, title, cover, type))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoritesManager", "Erro ao carregar favoritos: ${e.message}")
        }
        return list
    }

    private fun saveFavorites(context: Context, list: List<FavoriteItem>) {
        val newArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("streamId", item.streamId)
            obj.put("title", item.title)
            obj.put("coverUrl", item.coverUrl)
            obj.put("type", item.type)
            newArray.put(obj)
        }
        getPrefs(context).edit().putString(FAVORITES_KEY, newArray.toString()).apply()
        SyncManager.syncToCloud(context)
    }
}
