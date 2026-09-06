package com.iptv.app.meta

import android.content.Context
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONObject
import java.io.File

/**
 * Le os metadados que o servidor preparou (ver servidor/enrich.js).
 * Offline primeiro: o ecra pinta sempre da cache local, a sincronizacao
 * acontece em fundo. A chave da API do TMDB nunca esta na app.
 *
 * Armazenamento: ficheiro JSON em filesDir (sem Room, sem annotation processor).
 * Para ~3500 titulos o ficheiro tem ~3 MB e a leitura inicial fica em <30ms.
 */
class MetaRepository(
    private val ctx: Context,
    private val baseUrl: String = com.iptv.app.Constants.SERVER_URL
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheFile get() = File(ctx.filesDir, "meta_cache.json")

    // cache em memoria: carregada uma vez, usada para sempre
    @Volatile private var memCache: Map<String, Meta> = emptyMap()
    @Volatile private var updatedAt: Long = 0L
    @Volatile private var loaded = false

    private fun ensureLoaded() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            loadFromDisk()
            loaded = true
        }
    }

    private fun loadFromDisk() {
        try {
            if (!cacheFile.exists()) return
            val json = JSONObject(cacheFile.readText())
            updatedAt = json.optLong("updated_at", 0L)
            val items = json.optJSONObject("items") ?: return
            val map = HashMap<String, Meta>(items.length())
            items.keys().forEach { k ->
                val o = items.getJSONObject(k)
                if (!o.optBoolean("failed", false)) {
                    map[k] = Meta(
                        key = k,
                        tmdbId = o.optInt("tmdbId").takeIf { it != 0 },
                        kind = o.optString("kind", null),
                        title = o.optString("title", null),
                        year = o.optInt("year").takeIf { it != 0 },
                        overview = o.optString("overview", null)?.takeIf { it.isNotBlank() },
                        posterPath = o.optString("posterPath", null),
                        backdropPath = o.optString("backdropPath", null),
                        runtime = o.optInt("runtime").takeIf { it != 0 },
                        rating = o.optDouble("rating").takeIf { !it.isNaN() && it > 0 },
                        certification = o.optString("certification", null),
                        fetchedAt = o.optLong("fetchedAt", 0L),
                        failed = false
                    )
                }
            }
            memCache = map
        } catch (e: Exception) {
            android.util.Log.w("MetaRepository", "Erro ao ler cache: ${e.message}")
        }
    }

    /** O que o adapter chama. Nunca faz rede: so le a cache. */
    suspend fun of(rawTitle: String): Meta? = withContext(Dispatchers.IO) {
        ensureLoaded()
        memCache[TitleCleaner.parse(rawTitle).key]
    }

    /** Em bloco, para uma fila inteira de uma vez. */
    suspend fun of(rawTitles: List<String>): Map<String, Meta> = withContext(Dispatchers.IO) {
        ensureLoaded()
        rawTitles.mapNotNull { raw ->
            memCache[TitleCleaner.parse(raw).key]?.let { raw to it }
        }.toMap()
    }

    /**
     * Sincroniza incrementalmente. Chamar DEPOIS de a Home estar desenhada —
     * nunca no arranque, nunca a bloquear o ecra.
     */
    fun syncInBackground() {
        scope.launch {
            runCatching {
                ensureLoaded()
                val req = Request.Builder()
                    .url("$baseUrl/v1/meta?since=$updatedAt")
                    .header("Accept-Encoding", "gzip")
                    .build()

                com.iptv.app.OkHttpProvider.client.newCall(req).execute().use { res ->
                    if (res.code == 304) return@use
                    if (!res.isSuccessful) return@use
                    val body = res.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val newUpdatedAt = json.optLong("updated_at", 0L)
                    val items = json.optJSONObject("items") ?: return@use

                    // merge com o ficheiro em disco
                    val disk = try {
                        if (cacheFile.exists()) JSONObject(cacheFile.readText())
                        else JSONObject().put("items", JSONObject())
                    } catch (e: Exception) { JSONObject().put("items", JSONObject()) }

                    val diskItems = disk.optJSONObject("items") ?: JSONObject()
                    items.keys().forEach { k -> diskItems.put(k, items.getJSONObject(k)) }
                    disk.put("items", diskItems)
                    disk.put("updated_at", newUpdatedAt)

                    cacheFile.writeText(disk.toString())
                    updatedAt = newUpdatedAt

                    // recarregar mem cache
                    loaded = false
                    loadFromDisk()
                    loaded = true
                }
            }.onFailure {
                android.util.Log.w("MetaRepository", "Sync falhou: ${it.message}")
            }
        }
    }

    /** Para o ecra de diagnostico. */
    fun cachedCount(): Int { ensureLoaded(); return memCache.size }
}
