package com.iptv.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/*
 * ATENÇÃO: PREFS_NAME = "IPTV_RECENT" (maiúsculas) e KEY = "RECENT_LIST".
 * O SyncManager antigo usava "IPTV_Recent" + "recent_list" — os nomes de
 * ficheiro de SharedPreferences são sensíveis a maiúsculas, por isso estava
 * a sincronizar um ficheiro vazio que ele próprio criou.
 */
object RecentManager {
    private const val PREFS_NAME = "IPTV_RECENT"
    private const val KEY_RECENT = "RECENT_LIST"
    private const val MAX_RECENT = 15

    fun addRecent(context: Context, stream: Stream) {
        val list = getRecent(context).toMutableList()
        list.removeAll { it.stream_id == stream.stream_id }
        list.add(0, stream)
        if (list.size > MAX_RECENT) list.removeAt(list.size - 1)
        saveRecent(context, list)
    }

    fun getRecent(context: Context): List<Stream> {
        val prefs      = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_RECENT, "[]") ?: "[]"
        val list       = mutableListOf<Stream>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(Stream(
                    obj.getString("stream_id"),
                    obj.getString("name"),
                    obj.optString("stream_icon", ""),
                    obj.optString("stream_type", "live"),
                    obj.optString("extension", "ts")
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveRecent(context: Context, list: List<Stream>) {
        try {
            val now   = System.currentTimeMillis()
            val array = JSONArray()
            for (stream in list) {
                array.put(JSONObject()
                    .put("stream_id",   stream.stream_id)
                    .put("name",        stream.name)
                    .put("stream_icon", stream.stream_icon)
                    .put("stream_type", stream.stream_type)
                    .put("extension",   stream.extension)
                    .put("timestamp",   now))   // necessário para a junção por data no SyncPayload
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_RECENT, array.toString()).apply()
            SyncManager.syncToCloud(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /* ─── export / import (usados pelo SyncManager) ─────────────────── */

    fun exportJson(context: Context): JSONArray =
        try {
            JSONArray(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(KEY_RECENT, "[]")
            )
        } catch (e: Exception) { JSONArray() }

    fun importJson(context: Context, items: JSONArray) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_RECENT, items.toString()).apply()
    }
}
