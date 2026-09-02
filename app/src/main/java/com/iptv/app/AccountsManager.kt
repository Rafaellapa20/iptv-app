package com.iptv.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedAccount(
    val username: String,
    val password: String,
    val vencimento: String = "Ilimitado",
    val lastLogin: Long = System.currentTimeMillis()
)

object AccountsManager {
    private const val PREFS_NAME = "IPTV_SAVED_ACCOUNTS"
    private const val KEY_ACCOUNTS = "ACCOUNTS_JSON"

    fun getAccounts(context: Context): List<SavedAccount> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ACCOUNTS, "[]") ?: "[]"
        val list = mutableListOf<SavedAccount>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SavedAccount(
                        username = obj.getString("username"),
                        password = obj.getString("password"),
                        vencimento = obj.optString("vencimento", "Ilimitado"),
                        lastLogin = obj.optLong("lastLogin", 0L)
                    )
                )
            }
        } catch (e: Exception) {}
        return list.sortedByDescending { it.lastLogin }
    }

    fun saveAccount(context: Context, account: SavedAccount) {
        val current = getAccounts(context).toMutableList()
        current.removeAll { it.username.equals(account.username, ignoreCase = true) }
        current.add(0, account)

        val array = JSONArray()
        for (acc in current) {
            val obj = JSONObject().apply {
                put("username", acc.username)
                put("password", acc.password)
                put("vencimento", acc.vencimento)
                put("lastLogin", acc.lastLogin)
            }
            array.put(obj)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCOUNTS, array.toString())
            .apply()
    }

    fun removeAccount(context: Context, username: String) {
        val current = getAccounts(context).toMutableList()
        current.removeAll { it.username.equals(username, ignoreCase = true) }

        val array = JSONArray()
        for (acc in current) {
            val obj = JSONObject().apply {
                put("username", acc.username)
                put("password", acc.password)
                put("vencimento", acc.vencimento)
                put("lastLogin", acc.lastLogin)
            }
            array.put(obj)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCOUNTS, array.toString())
            .apply()
    }
}
