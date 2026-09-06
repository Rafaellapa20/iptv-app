package com.iptv.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

/*
 * Ponto único de acesso às credenciais e ao syncId.
 *
 * PORQUÊ: a password estava em texto simples em IPTV_PREFS, lida em
 * 20 ficheiros. Este objecto:
 *   1. cria um ficheiro CIFRADO (IPTV_SECURE) ao lado do antigo
 *   2. migra username+password na primeira execução e apaga a password do antigo
 *   3. dá uma API curta para os 20 sítios usarem à medida que forem tocados
 *
 * O syncId NÃO é gerado localmente. Está a null até SyncManager o pedir
 * ao servidor (POST /api/sync/id { credHash }) e guardar o resultado aqui.
 *
 * Dependência (app/build.gradle):
 *   implementation 'androidx.security:security-crypto:1.1.0-alpha06'
 */
object Prefs {

    private const val LEGACY  = "IPTV_PREFS"
    private const val SECURE  = "IPTV_SECURE"
    private const val MIGRATED = "MIGRATED_TO_SECURE"

    private const val K_USER    = "USERNAME"
    private const val K_PASS    = "PASSWORD"
    private const val K_SYNC_ID = "SYNC_ID"

    @Volatile private var secure: SharedPreferences? = null

    private fun secure(ctx: Context): SharedPreferences {
        secure?.let { return it }
        return synchronized(this) {
            secure ?: build(ctx).also { secure = it; migrateIfNeeded(ctx, it) }
        }
    }

    private fun build(ctx: Context): SharedPreferences = try {
        val key = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx, SECURE, key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Algumas TV boxes têm o Keystore incompleto. Melhor degradar para
        // texto simples do que a app não arrancar.
        Log.w("Prefs", "Keystore indisponível, a usar prefs normais", e)
        ctx.getSharedPreferences(SECURE, Context.MODE_PRIVATE)
    }

    private fun legacy(ctx: Context) =
        ctx.getSharedPreferences(LEGACY, Context.MODE_PRIVATE)

    /** Migra credenciais do ficheiro antigo e limpa a password do original. Corre uma vez. */
    private fun migrateIfNeeded(ctx: Context, target: SharedPreferences) {
        if (target.getBoolean(MIGRATED, false)) return
        val old = legacy(ctx)
        target.edit().apply {
            old.getString(K_USER, null)?.let { putString(K_USER, it) }
            old.getString(K_PASS, null)?.let { putString(K_PASS, it) }
            putBoolean(MIGRATED, true)
        }.apply()
        // a password deixa de existir em texto simples
        old.edit().remove(K_PASS).apply()
        Log.i("Prefs", "Credenciais migradas para armazenamento cifrado")
    }

    fun username(ctx: Context): String = secure(ctx).getString(K_USER, "").orEmpty()
    fun password(ctx: Context): String = secure(ctx).getString(K_PASS, "").orEmpty()

    fun hasCredentials(ctx: Context): Boolean =
        username(ctx).isNotEmpty() && password(ctx).isNotEmpty()

    /**
     * Guarda as credenciais. Se mudaram, invalida o syncId em cache para que o
     * próximo syncFromCloud/syncToCloud obtenha o correto para esta conta.
     */
    fun saveCredentials(ctx: Context, user: String, pass: String) {
        val p = secure(ctx)
        val oldUser = p.getString(K_USER, "")
        val oldPass = p.getString(K_PASS, "")
        val editor = p.edit().putString(K_USER, user).putString(K_PASS, pass)
        if (oldUser != user || oldPass != pass) editor.remove(K_SYNC_ID) // conta diferente
        editor.apply()
        // o resto da app ainda lê USERNAME daqui; a PASSWORD já não.
        legacy(ctx).edit().putString(K_USER, user).remove(K_PASS).apply()
    }

    fun clearCredentials(ctx: Context) {
        secure(ctx).edit().remove(K_USER).remove(K_PASS).remove(K_SYNC_ID).apply()
        legacy(ctx).edit().remove(K_USER).remove(K_PASS).apply()
    }

    /* ── sync id ───────────────────────────────────────────────────── */

    /**
     * Devolve o syncId em cache, ou null se ainda não foi obtido do servidor.
     * SyncManager é responsável por chamar POST /api/sync/id e guardar o resultado.
     */
    fun syncId(ctx: Context): String? = secure(ctx).getString(K_SYNC_ID, null)

    /** Guarda o syncId recebido do servidor. */
    fun setSyncId(ctx: Context, id: String) {
        secure(ctx).edit().putString(K_SYNC_ID, id).apply()
    }

    /** Apaga o syncId em cache (ex: logout, mudança de conta). */
    fun clearSyncId(ctx: Context) {
        secure(ctx).edit().remove(K_SYNC_ID).apply()
    }

    /**
     * SHA-256(username:password) — usado como credHash para obter o syncId
     * do servidor, e para a migração de dados da versão anterior.
     */
    fun credHash(ctx: Context): String {
        val u = username(ctx)
        val p = password(ctx)
        if (u.isEmpty() && p.isEmpty()) return ""
        return MessageDigest.getInstance("SHA-256")
            .digest("$u:$p".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
