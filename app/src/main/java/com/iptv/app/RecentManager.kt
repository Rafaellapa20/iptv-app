package com.iptv.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object RecentManager {
    private const val PREFS_NAME = "IPTV_RECENT"
    private const val KEY_RECENT = "RECENT_LIST"
    private const val MAX_RECENT = 15

    fun addRecent(context: Context, stream: Stream) {
        val list = getRecent(context).toMutableList()
        list.removeAll { it.stream_id == stream.stream_id }
        list.add(0, stream)
        if (list.size > MAX_RECENT) {
            list.removeAt(list.size - 1)
        }
        saveRecent(context, list)
    }

    fun getRecent(context: Context): List<Stream> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_RECENT, "[]") ?: "[]"
        val list = mutableListOf<Stream>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Stream(
                        obj.getString("stream_id"),
                        obj.getString("name"),
                        obj.optString("stream_icon", ""),
                        obj.optString("stream_type", "live"),
                        obj.optString("extension", "ts")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveRecent(context: Context, list: List<Stream>) {
        try {
            val array = JSONArray()
            for (stream in list) {
                val obj = JSONObject()
                obj.put("stream_id", stream.stream_id)
                obj.put("name", stream.name)
                obj.put("stream_icon", stream.stream_icon)
                obj.put("stream_type", stream.stream_type)
                obj.put("extension", stream.extension)
                array.put(obj)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_RECENT, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
