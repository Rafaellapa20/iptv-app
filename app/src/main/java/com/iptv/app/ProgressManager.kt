package com.iptv.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object ProgressManager {

    private const val PREFS_NAME = "IPTV_Progress"
    private const val SEEN_PREFS = "IPTV_Seen"
    private const val RECENT_LIST_KEY = "recent_progress_list"

    data class ProgressItem(
        val streamId: String,
        val title: String,
        val coverUrl: String,
        val type: String,
        val position: Long,
        val duration: Long,
        val timestamp: Long
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getSeenPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(SEEN_PREFS, Context.MODE_PRIVATE)
    }

    // Compatibilidade com o Player Antigo (Retorna apenas a posição)
    fun getProgress(context: Context, streamId: String): Long {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(RECENT_LIST_KEY, "[]")
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("streamId") == streamId) {
                    return obj.getLong("position")
                }
            }
        } catch (e: Exception) {}
        return 0L
    }

    // Salvar Progresso (Novo Sistema Completo)
    fun saveProgressFull(context: Context, streamId: String, title: String, coverUrl: String, type: String, position: Long, duration: Long) {
        if (position <= 0) {
            removeProgress(context, streamId)
            return
        }

        val prefs = getPrefs(context)
        val jsonString = prefs.getString(RECENT_LIST_KEY, "[]")
        
        val list = mutableListOf<ProgressItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProgressItem(
                        obj.getString("streamId"),
                        obj.getString("title"),
                        obj.getString("coverUrl"),
                        obj.getString("type"),
                        obj.getLong("position"),
                        obj.getLong("duration"),
                        obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {}

        // Remove se já existir para colocar no topo
        list.removeAll { it.streamId == streamId }

        // Adiciona no topo
        list.add(0, ProgressItem(streamId, title, coverUrl, type, position, duration, System.currentTimeMillis()))

        // Manter apenas os 20 mais recentes
        if (list.size > 20) {
            list.removeAt(list.size - 1)
        }

        // Salvar de volta
        val newArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("streamId", item.streamId)
            obj.put("title", item.title)
            obj.put("coverUrl", item.coverUrl)
            obj.put("type", item.type)
            obj.put("position", item.position)
            obj.put("duration", item.duration)
            obj.put("timestamp", item.timestamp)
            newArray.put(obj)
        }

        prefs.edit().putString(RECENT_LIST_KEY, newArray.toString()).apply()
    }

    private fun removeProgress(context: Context, streamId: String) {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(RECENT_LIST_KEY, "[]")
        try {
            val array = JSONArray(jsonString)
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("streamId") != streamId) {
                    newArray.put(obj)
                }
            }
            prefs.edit().putString(RECENT_LIST_KEY, newArray.toString()).apply()
        } catch (e: Exception) {}
    }

    fun getRecentProgressList(context: Context): List<ProgressItem> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(RECENT_LIST_KEY, "[]")
        val list = mutableListOf<ProgressItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ProgressItem(
                        obj.getString("streamId"),
                        obj.getString("title"),
                        obj.getString("coverUrl"),
                        obj.getString("type"),
                        obj.getLong("position"),
                        obj.getLong("duration"),
                        obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {}
        return list
    }

    fun markAsSeen(context: Context, streamId: String) {
        getSeenPrefs(context).edit().putBoolean(streamId, true).apply()
        removeProgress(context, streamId) // Se já assistiu até o final, tira do Continuar Assistindo
    }

    fun isSeen(context: Context, streamId: String): Boolean {
        return getSeenPrefs(context).getBoolean(streamId, false)
    }

    // Método antigo compatível (Para PlayerActivity não quebrar se não mandar os dados completos ainda)
    fun saveProgress(context: Context, streamId: String, position: Long) {
        // Será substituído pelo saveProgressFull, se chamado diretamente apenas remove o antigo
        if (position <= 0) removeProgress(context, streamId)
    }
}
