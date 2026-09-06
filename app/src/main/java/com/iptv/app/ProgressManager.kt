package com.iptv.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object ProgressManager {

    private const val PREFS_NAME     = "IPTV_Progress"
    private const val SEEN_PREFS     = "IPTV_Seen"
    private const val RECENT_LIST_KEY = "recent_progress_list"

    data class ProgressItem(
        val streamId: String,
        val title: String,
        val coverUrl: String,
        val type: String,
        val position: Long,
        val duration: Long,
        val timestamp: Long,
        val episodeIndex: Int = 0
    )

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getSeenPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(SEEN_PREFS, Context.MODE_PRIVATE)

    // Compatibilidade com o Player Antigo (devolve apenas a posição)
    fun getProgress(context: Context, streamId: String): Long {
        val jsonString = getPrefs(context).getString(RECENT_LIST_KEY, "[]")
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("streamId") == streamId) return obj.getLong("position")
            }
        } catch (e: Exception) {}
        return 0L
    }

    fun saveProgressFull(
        context: Context,
        streamId: String, title: String, coverUrl: String, type: String,
        position: Long, duration: Long, episodeIndex: Int = 0
    ) {
        if (position <= 0) { removeProgress(context, streamId); return }

        val prefs      = getPrefs(context)
        val jsonString = prefs.getString(RECENT_LIST_KEY, "[]")
        val list       = mutableListOf<ProgressItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(ProgressItem(
                    obj.getString("streamId"),
                    obj.getString("title"),
                    obj.getString("coverUrl"),
                    obj.getString("type"),
                    obj.getLong("position"),
                    obj.getLong("duration"),
                    obj.getLong("timestamp"),
                    obj.optInt("episodeIndex", 0)
                ))
            }
        } catch (e: Exception) {}

        list.removeAll { it.streamId == streamId }
        list.add(0, ProgressItem(streamId, title, coverUrl, type, position, duration, System.currentTimeMillis(), episodeIndex))
        if (list.size > 20) list.removeAt(list.size - 1)

        val newArray = JSONArray()
        for (item in list) {
            newArray.put(JSONObject()
                .put("streamId",     item.streamId)
                .put("title",        item.title)
                .put("coverUrl",     item.coverUrl)
                .put("type",         item.type)
                .put("position",     item.position)
                .put("duration",     item.duration)
                .put("timestamp",    item.timestamp)
                .put("episodeIndex", item.episodeIndex))
        }
        prefs.edit().putString(RECENT_LIST_KEY, newArray.toString()).apply()
        SyncManager.syncToCloud(context)
    }

    private fun removeProgress(context: Context, streamId: String) {
        val prefs      = getPrefs(context)
        val jsonString = prefs.getString(RECENT_LIST_KEY, "[]")
        try {
            val array    = JSONArray(jsonString)
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("streamId") != streamId) newArray.put(obj)
            }
            prefs.edit().putString(RECENT_LIST_KEY, newArray.toString()).apply()
            SyncManager.syncToCloud(context)
        } catch (e: Exception) {}
    }

    fun getRecentProgressList(context: Context): List<ProgressItem> {
        val jsonString = getPrefs(context).getString(RECENT_LIST_KEY, "[]")
        val list       = mutableListOf<ProgressItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(ProgressItem(
                    obj.getString("streamId"),
                    obj.getString("title"),
                    obj.getString("coverUrl"),
                    obj.getString("type"),
                    obj.getLong("position"),
                    obj.getLong("duration"),
                    obj.getLong("timestamp"),
                    obj.optInt("episodeIndex", 0)
                ))
            }
        } catch (e: Exception) {}
        return list
    }

    fun markAsSeen(context: Context, streamId: String) {
        getSeenPrefs(context).edit().putBoolean(streamId, true).apply()
        removeProgress(context, streamId)
    }

    fun isSeen(context: Context, streamId: String): Boolean =
        getSeenPrefs(context).getBoolean(streamId, false)

    fun saveProgress(context: Context, streamId: String, position: Long) {
        if (position <= 0) removeProgress(context, streamId)
    }

    /* ─── export / import (usados pelo SyncManager) ─────────────────── */

    /**
     * RECENT_LIST_KEY era privado — e é por isso que o SyncManager antigo
     * errava a chave ("recent_list" em vez de "recent_progress_list").
     */
    fun exportJson(context: Context): JSONArray =
        try { JSONArray(getPrefs(context).getString(RECENT_LIST_KEY, "[]")) }
        catch (e: Exception) { JSONArray() }

    fun importJson(context: Context, items: JSONArray) {
        getPrefs(context).edit().putString(RECENT_LIST_KEY, items.toString()).apply()
    }
}
