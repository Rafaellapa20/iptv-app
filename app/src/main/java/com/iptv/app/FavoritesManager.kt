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
        // Allow FavoriteItem to be treated as a Stream for adapters
        fun toStream() = Stream(streamId, title, coverUrl, type, "")
        
        // Expose Stream-compatible field names so existing code keeps compiling
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
        val list = getFavorites(context)
        return list.any { it.streamId == streamId }
    }
    
    // Stream overload
    fun isFavorite(context: Context, stream: Stream): Boolean = isFavorite(context, stream.stream_id)

    fun toggleFavorite(context: Context, streamId: String, title: String, coverUrl: String, type: String): Boolean {
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
    
    // Stream overload — used by StreamsActivity, LiveTvActivity, RecentMoviesAdapter
    fun toggleFavorite(context: Context, stream: Stream): Boolean =
        toggleFavorite(context, stream.stream_id, stream.name, stream.stream_icon, stream.stream_type)

    fun getFavorites(context: Context): List<FavoriteItem> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(FAVORITES_KEY, "[]")
        val list = mutableListOf<FavoriteItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FavoriteItem(
                        obj.getString("streamId"),
                        obj.getString("title"),
                        obj.getString("coverUrl"),
                        obj.getString("type")
                    )
                )
            }
        } catch (e: Exception) {}
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
    }
}
