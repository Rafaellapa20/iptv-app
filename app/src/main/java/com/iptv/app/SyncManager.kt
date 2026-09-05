package com.iptv.app

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

@OptIn(DelicateCoroutinesApi::class)
object SyncManager {
    // GlobalScope é usado deliberadamente aqui: o sync deve continuar mesmo que
    // a Activity que o disparou (ex: LoginActivity) seja destruída logo a seguir.

    // ATIVAR SÓ DEPOIS DE:
    // 1. Criar o projeto em https://console.firebase.google.com
    // 2. Adicionar uma app Android com o package "com.iptv.app"
    // 3. Colocar o google-services.json descarregado em app/google-services.json
    // 4. Descomentar "apply plugin: 'com.google.gms.google-services'" no fim
    //    de app/build.gradle
    // 5. Ativar o Firestore Database no projeto (modo produção)
    // 6. Aplicar as regras de segurança em firestore.rules (ver esse ficheiro)
    // Sem isto, FirebaseFirestore.getInstance() rebenta com
    // "Default FirebaseApp is not initialized" — por isso o try/catch abaixo
    // e esta flag continuam a proteger a app até o setup estar completo.
    private const val FIREBASE_READY = false

    // Não há um sistema de contas próprio (a "conta" é o username/password do
    // Xtream, que vem do fornecedor de IPTV, não é nosso). Sem um backend de
    // autenticação, usamos um ID de documento derivado de
    // SHA-256(username + ":" + password) como substituto de um token —
    // qualquer dispositivo que souber a conta consegue calcular o mesmo ID
    // (é assim que dois aparelhos com a mesma conta encontram os mesmos
    // dados), mas ninguém consegue adivinhar o ID de outra conta a partir de
    // só o username (ao contrário do sistema antigo, que usava só o username
    // em bruto como chave, sem password nenhuma).
    private fun syncDocId(username: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$username:$password".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun syncToCloud(context: Context) {
        if (!FIREBASE_READY) return
        val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        val password = prefs.getString("PASSWORD", "") ?: ""
        if (username.isEmpty()) return

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val favPrefs = context.getSharedPreferences("IPTV_Favorites", Context.MODE_PRIVATE)
                val favArray = JSONArray(favPrefs.getString("favorites_list", "[]"))

                val progPrefs = context.getSharedPreferences("IPTV_Progress", Context.MODE_PRIVATE)
                val progArray = JSONArray(progPrefs.getString("recent_list", "[]"))

                val recPrefs = context.getSharedPreferences("IPTV_Recent", Context.MODE_PRIVATE)
                val recArray = JSONArray(recPrefs.getString("recent_list", "[]"))

                val docId = syncDocId(username, password)
                val data = hashMapOf<String, Any>(
                    "favorites" to favArray.toString(),
                    "progress" to progArray.toString(),
                    "recent" to recArray.toString(),
                    "updatedAt" to System.currentTimeMillis()
                )

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("sync")
                    .document(docId)
                    .set(data)
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun syncFromCloud(context: Context, onComplete: () -> Unit = {}) {
        if (!FIREBASE_READY) {
            onComplete()
            return
        }
        val prefs = context.getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        val password = prefs.getString("PASSWORD", "") ?: ""
        if (username.isEmpty()) {
            onComplete()
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val docId = syncDocId(username, password)
                val snapshot = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("sync")
                    .document(docId)
                    .get()
                    .await()

                if (snapshot.exists()) {
                    val favs = JSONArray(snapshot.getString("favorites") ?: "[]")
                    if (favs.length() > 0) {
                        context.getSharedPreferences("IPTV_Favorites", Context.MODE_PRIVATE)
                            .edit().putString("favorites_list", favs.toString()).apply()
                    }

                    val progs = JSONArray(snapshot.getString("progress") ?: "[]")
                    if (progs.length() > 0) {
                        context.getSharedPreferences("IPTV_Progress", Context.MODE_PRIVATE)
                            .edit().putString("recent_list", progs.toString()).apply()
                    }

                    val recents = JSONArray(snapshot.getString("recent") ?: "[]")
                    if (recents.length() > 0) {
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
