package com.iptv.app.meta

/**
 * A MESMA limpeza que o enrich.js faz no servidor — o cliente precisa dela
 * para calcular a chave e encontrar o metadado que o servidor ja preparou.
 *
 * Se mudares um regex aqui, muda no enrich.js tambem, senao as chaves
 * deixam de coincidir.
 */
object TitleCleaner {

    private val RE_PREFIX = Regex("""^\s*(PT|BR|EN|ES|VOD|FILME|FILMES|SERIE|SERIES)\s*[|:\-]\s*""", RegexOption.IGNORE_CASE)
    private val RE_YEAR = Regex("""\((19|20)\d{2}\)|\b(19|20)\d{2}\b""")
    private val RE_TAGS = Regex("""\[?\b(4K|UHD|FHD|1080p?|720p?|480p?|HD|SD|DUAL|MULTI|LEG|LEGENDADO|DUB|DUBLADO|IMAX|EXTENDED|REMUX|WEB-?DL|BLURAY)\b\]?""", RegexOption.IGNORE_CASE)
    private val RE_EXT = Regex("""\.(mkv|mp4|avi|ts|m4v)$""", RegexOption.IGNORE_CASE)
    private val RE_EP = Regex("""\b(?:S(\d{1,2})[\s._-]?E(\d{1,3})|(\d{1,2})x(\d{1,3})|T(\d{1,2})\s*Ep?\.?\s*(\d{1,3}))\b""", RegexOption.IGNORE_CASE)

    data class Parsed(
        val title: String,
        val year: Int?,
        val season: Int?,
        val episode: Int?
    ) {
        val isSeries: Boolean get() = season != null
        /** Chave de emparelhamento. Igual a do servidor. */
        val key: String get() = normalize(title) + "|" + (year?.toString() ?: "") + if (isSeries) "|tv" else ""
    }

    fun parse(raw: String): Parsed {
        var s = raw.trim()

        // episodio primeiro: a busca e pela serie, nao pelo episodio
        val ep = RE_EP.find(s)
        var season: Int? = null
        var episode: Int? = null
        if (ep != null) {
            val g = ep.groupValues
            season = (g[1].ifEmpty { g[3] }.ifEmpty { g[5] }).toIntOrNull()
            episode = (g[2].ifEmpty { g[4] }.ifEmpty { g[6] }).toIntOrNull()
            s = RE_EP.replace(s, " ")
        }

        val year = RE_YEAR.find(s)?.value?.filter { it.isDigit() }?.toIntOrNull()

        s = RE_EXT.replace(s, " ")
        s = RE_PREFIX.replace(s, "")
        s = RE_YEAR.replace(s, " ")
        s = RE_TAGS.replace(s, " ")
        s = s.replace(Regex("""[._]+"""), " ")
        s = s.replace(Regex("""[\[\](){}]"""), " ")
        s = s.replace(Regex("""\s{2,}"""), " ").trim()

        return Parsed(s, year, season, episode)
    }

    fun normalize(t: String): String = t.lowercase()
        .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
        .replace(Regex("""\p{Mn}+"""), "")
        .replace(Regex("""[^a-z0-9 ]"""), "")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
}
