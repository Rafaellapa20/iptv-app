package com.iptv.app

import org.json.JSONArray
import org.json.JSONObject

/*
 * Junção por item, não por objecto.
 *
 * O SyncManager antigo substituía a lista local inteira quando o servidor
 * tinha qualquer coisa. Marcar favoritos no telemóvel e depois abrir a TV
 * apagava-os. A regra correcta é "ganha o item com data mais recente" —
 * e é o que está aqui.
 */
object SyncPayload {

    /* ─── favoritos ──────────────────────────────────────────────────── */

    /*
     * Os favoritos não tinham data, e sem data não há como distinguir
     * "ainda não chegou aqui" de "foi removido lá". Por isso:
     *   · ao adicionar, grava-se "at"
     *   · ao remover, fica uma lápide em "removed" com "at"
     *   · na junção, por streamId ganha o mais recente dos dois
     */

    fun mergeFavorites(
        local: JSONArray, remote: JSONArray,
        localRemoved: JSONArray, remoteRemoved: JSONArray
    ): Pair<JSONArray, JSONArray> {

        val added   = HashMap<String, JSONObject>()
        val removed = HashMap<String, Long>()

        fun collectAdded(arr: JSONArray) {
            for (i in 0 until arr.length()) {
                val o  = arr.optJSONObject(i) ?: continue
                val id = o.optString("streamId").ifEmpty { continue }
                val at = o.optLong("at", 0L)
                val prev = added[id]
                if (prev == null || at >= prev.optLong("at", 0L)) added[id] = o
            }
        }
        fun collectRemoved(arr: JSONArray) {
            for (i in 0 until arr.length()) {
                val o  = arr.optJSONObject(i) ?: continue
                val id = o.optString("streamId").ifEmpty { continue }
                val at = o.optLong("at", 0L)
                if (at >= (removed[id] ?: 0L)) removed[id] = at
            }
        }

        collectAdded(local);   collectAdded(remote)
        collectRemoved(localRemoved); collectRemoved(remoteRemoved)

        val outAdded = JSONArray()
        for ((_, obj) in added) {
            val addedAt   = obj.optLong("at", 0L)
            val removedAt = removed[obj.optString("streamId")] ?: 0L
            if (addedAt >= removedAt) outAdded.put(obj)   // adicionado depois de removido (ou nunca removido)
        }

        // as lápides expiram: não vale a pena carregá-las para sempre
        val cutoff     = System.currentTimeMillis() - 90L * 24 * 3600 * 1000
        val outRemoved = JSONArray()
        for ((id, at) in removed) {
            if (at < cutoff) continue
            if ((added[id]?.optLong("at", 0L) ?: 0L) >= at) continue
            outRemoved.put(JSONObject().put("streamId", id).put("at", at))
        }

        return outAdded to outRemoved
    }

    /* ─── progresso e recentes ───────────────────────────────────────── */

    /**
     * Junção por streamId, ganha o timestamp maior. Serve para o progresso
     * (que tem "timestamp") e para os recentes.
     */
    fun mergeByTimestamp(
        local: JSONArray, remote: JSONArray,
        idField:   String = "streamId",
        timeField: String = "timestamp",
        limit:     Int    = 60
    ): JSONArray {
        val best = LinkedHashMap<String, JSONObject>()

        fun collect(arr: JSONArray) {
            for (i in 0 until arr.length()) {
                val o  = arr.optJSONObject(i) ?: continue
                val id = o.optString(idField).ifEmpty { continue }
                val t  = o.optLong(timeField, 0L)
                val prev = best[id]
                if (prev == null || t > prev.optLong(timeField, 0L)) best[id] = o
            }
        }
        collect(local); collect(remote)

        val sorted = best.values.sortedByDescending { it.optLong(timeField, 0L) }
        return JSONArray().apply { sorted.take(limit).forEach { put(it) } }
    }

    /** Assinatura do estado: se não mudou, não vale a pena enviar. */
    fun fingerprint(vararg arrays: JSONArray): Int =
        arrays.joinToString("|") { it.toString() }.hashCode()
}
