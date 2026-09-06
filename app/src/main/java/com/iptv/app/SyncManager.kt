package com.iptv.app

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sincronização de favoritos, progresso e recentes via backend StreamVPN.
 *
 * O que mudou em relação à versão anterior:
 *
 *  1. As chaves das SharedPreferences deixam de ser literais aqui — passam
 *     pelos próprios gestores. Antes divergiam em três sítios e o progresso
 *     e os recentes NUNCA sincronizaram (liam chaves que não existem).
 *  2. Junção por item pela data mais recente, em vez de substituir a lista
 *     local inteira. Antes, abrir a TV apagava favoritos feitos no telemóvel.
 *  3. GlobalScope substituído por um scope próprio, cancelável.
 *  4. Os erros vão para o Log em vez de serem engolidos por catch vazio.
 *  5. Não envia quando nada mudou.
 *  6. O sync_id é atribuído pelo servidor (POST /api/sync/id { credHash }),
 *     não gerado no aparelho. Dois aparelhos com as mesmas credenciais
 *     obtêm sempre o mesmo sync_id.
 */
object SyncManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private const val TAG           = "SyncManager"
    private const val PREF_FP       = "SYNC_FINGERPRINT"
    private const val PREF_LAST_OK  = "SYNC_LAST_OK"
    private const val PREF_FAILS    = "SYNC_FAILS"
    private const val PREF_MIGRATED = "SYNC_ID_MIGRATED"

    /** Chamar no onDestroy da última Activity, ou no onTerminate. */
    fun cancel() = scope.coroutineContext.cancelChildren()

    private fun meta(ctx: Context) =
        ctx.getSharedPreferences("IPTV_SYNC_META", Context.MODE_PRIVATE)

    private fun dataUrl(ctx: Context, id: String) =
        "${StreamVpnApi.baseUrl(ctx)}/sync/$id"

    /* ──────────────────────────────────────────────────────────────────
     * Resolução do sync_id
     *
     * O syncId é obtido do servidor UMA VEZ por conta e guardado cifrado
     * em Prefs. Se já estiver em cache, devolve imediatamente. Bloqueia
     * (okhttp síncrono) — chamar sempre a partir de Dispatchers.IO.
     * ────────────────────────────────────────────────────────────────── */

    private fun ensureSyncId(ctx: Context): String? {
        Prefs.syncId(ctx)?.let { return it }

        val credHash = Prefs.credHash(ctx)
        if (credHash.isEmpty()) return null

        val body = JSONObject().put("credHash", credHash).toString()
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("${StreamVpnApi.baseUrl(ctx)}/sync/id")
            .post(body)
            .build()

        return try {
            OkHttpProvider.client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    noteFailure(ctx, "syncId: HTTP ${res.code}")
                    return null
                }
                val text = res.body?.string()?.takeIf { it.isNotBlank() } ?: return null
                val id = JSONObject(text).optString("syncId").takeIf { it.isNotEmpty() } ?: return null
                Prefs.setSyncId(ctx, id)
                Log.i(TAG, "syncId obtido do servidor: $id")
                id
            }
        } catch (e: Exception) {
            noteFailure(ctx, "syncId: ${e.message}")
            null
        }
    }

    /* ──────────────────────────────────────────────────────────────────
     * Migração da chave antiga (SHA-256 direto) para o novo syncId
     *
     * Na primeira execução lê-se uma última vez pela chave antiga para não
     * perder o histórico de quem já usava a app.
     * ────────────────────────────────────────────────────────────────── */

    private fun migrateFromLegacyKey(ctx: Context): JSONObject? {
        if (meta(ctx).getBoolean(PREF_MIGRATED, false)) return null
        meta(ctx).edit().putBoolean(PREF_MIGRATED, true).apply()

        val legacyHash = Prefs.credHash(ctx)
        if (legacyHash.isEmpty()) return null

        val legacy = fetchRemote(ctx, legacyHash)
        if (legacy != null) {
            Log.i(TAG, "Dados recuperados da chave legada; a reescrever no syncId novo")
            // força um envio para o id novo
            meta(ctx).edit().putInt(PREF_FP, 0).apply()
        }
        return legacy
    }

    /* ──────────────────────────────────────────────────────────────────
     * Enviar
     * ────────────────────────────────────────────────────────────────── */

    fun syncToCloud(ctx: Context) {
        if (!Prefs.hasCredentials(ctx)) return

        scope.launch {
            val id = ensureSyncId(ctx) ?: return@launch

            try {
                val favs    = FavoritesManager.exportJson(ctx)
                val removed = FavoritesManager.exportRemovedJson(ctx)
                val prog    = ProgressManager.exportJson(ctx)
                val recent  = RecentManager.exportJson(ctx)

                // nada mudou desde o último envio bem-sucedido
                val fp = SyncPayload.fingerprint(favs, removed, prog, recent)
                if (meta(ctx).getInt(PREF_FP, 0) == fp) return@launch

                val body = JSONObject()
                    .put("favorites",         favs)
                    .put("favorites_removed", removed)
                    .put("progress",          prog)
                    .put("recent",            recent)
                    .put("updated_at",        System.currentTimeMillis())
                    .toString()
                    .toRequestBody("application/json".toMediaType())

                val req = Request.Builder()
                    .url(dataUrl(ctx, id))
                    .post(body)
                    .build()

                OkHttpProvider.client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) {
                        meta(ctx).edit()
                            .putInt(PREF_FP, fp)
                            .putLong(PREF_LAST_OK, System.currentTimeMillis())
                            .putInt(PREF_FAILS, 0)
                            .apply()
                    } else {
                        noteFailure(ctx, "envio: HTTP ${res.code}")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                noteFailure(ctx, "envio: ${e.message}")
            }
        }
    }

    /* ──────────────────────────────────────────────────────────────────
     * Receber
     * ────────────────────────────────────────────────────────────────── */

    fun syncFromCloud(ctx: Context, onComplete: () -> Unit = {}) {
        if (!Prefs.hasCredentials(ctx)) { onComplete(); return }

        scope.launch {
            try {
                val id     = ensureSyncId(ctx)
                val remote = if (id != null) fetchRemote(ctx, id) else null
                    ?: migrateFromLegacyKey(ctx)   // primeira execução após a mudança de chave

                if (remote != null) applyMerged(ctx, remote)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                noteFailure(ctx, "leitura: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) { onComplete() }
            }
        }
    }

    private fun fetchRemote(ctx: Context, id: String): JSONObject? = try {
        val req = Request.Builder().url(dataUrl(ctx, id)).get().build()
        OkHttpProvider.client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) {
                if (res.code != 404) noteFailure(ctx, "leitura: HTTP ${res.code}")
                return null
            }
            val text = res.body?.string().orEmpty()
            if (text.isBlank()) null else JSONObject(text)
        }
    } catch (e: Exception) {
        noteFailure(ctx, "leitura: ${e.message}")
        null
    }

    /* ──────────────────────────────────────────────────────────────────
     * Junção
     * ────────────────────────────────────────────────────────────────── */

    private fun applyMerged(ctx: Context, remote: JSONObject) {
        val rFav    = remote.optJSONArray("favorites")         ?: JSONArray()
        val rFavRem = remote.optJSONArray("favorites_removed") ?: JSONArray()
        val rProg   = remote.optJSONArray("progress")          ?: JSONArray()
        val rRecent = remote.optJSONArray("recent")            ?: JSONArray()

        val (mergedFav, mergedRem) = SyncPayload.mergeFavorites(
            local         = FavoritesManager.exportJson(ctx),
            remote        = rFav,
            localRemoved  = FavoritesManager.exportRemovedJson(ctx),
            remoteRemoved = rFavRem
        )
        FavoritesManager.importJson(ctx, mergedFav, mergedRem)

        ProgressManager.importJson(
            ctx,
            SyncPayload.mergeByTimestamp(ProgressManager.exportJson(ctx), rProg)
        )

        RecentManager.importJson(
            ctx,
            SyncPayload.mergeByTimestamp(
                RecentManager.exportJson(ctx), rRecent,
                idField = "stream_id",   // RecentManager usa stream_id, não streamId
                limit   = 30
            )
        )

        // o estado local mudou: a próxima subida tem de acontecer
        meta(ctx).edit()
            .putInt(PREF_FP, 0)
            .putLong(PREF_LAST_OK, System.currentTimeMillis())
            .apply()
    }

    /* ──────────────────────────────────────────────────────────────────
     * Diagnóstico
     * ────────────────────────────────────────────────────────────────── */

    private fun noteFailure(ctx: Context, msg: String) {
        Log.w(TAG, msg)
        val m = meta(ctx)
        m.edit().putInt(PREF_FAILS, m.getInt(PREF_FAILS, 0) + 1).apply()
    }

    /** Para o ecrã de diagnóstico: "Sincronizado há 2 min" / "3 falhas". */
    fun status(ctx: Context): String {
        val m     = meta(ctx)
        val last  = m.getLong(PREF_LAST_OK, 0L)
        val fails = m.getInt(PREF_FAILS, 0)
        if (last == 0L) return if (fails > 0) "Ainda não sincronizou ($fails falhas)" else "—"
        val mins = ((System.currentTimeMillis() - last) / 60_000L).toInt()
        val when_ = when {
            mins < 1    -> "agora mesmo"
            mins < 60   -> "há $mins min"
            mins < 1440 -> "há ${mins / 60} h"
            else        -> "há ${mins / 1440} dias"
        }
        return if (fails > 0) "Sincronizado $when_ · $fails falhas" else "Sincronizado $when_"
    }
}
