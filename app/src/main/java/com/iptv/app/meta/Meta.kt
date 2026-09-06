package com.iptv.app.meta

/**
 * Uma linha por titulo. So metadados — as imagens ficam na cache de
 * ficheiros do Glide, nunca em base de dados.
 */
data class Meta(
    val key: String,               // TitleCleaner.Parsed.key
    val tmdbId: Int? = null,
    val kind: String? = null,      // "movie" | "tv"
    val title: String? = null,     // titulo oficial, nao o da playlist
    val year: Int? = null,
    val overview: String? = null,  // se vazio, o bloco desaparece do ecra
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val runtime: Int? = null,
    val rating: Double? = null,
    val certification: String? = null,
    val fetchedAt: Long = 0L,
    val failed: Boolean = false
) {
    /** URL do cartaz no tamanho certo para a caixa de destino.
     *  Pedir w1280 para uma miniatura e o erro mais caro. */
    fun poster(widthDp: Int): String? = posterPath?.let {
        val size = when {
            widthDp <= 120 -> "w185"
            widthDp <= 200 -> "w342"
            widthDp <= 400 -> "w500"
            else -> "w780"
        }
        "https://image.tmdb.org/t/p/$size$it"
    }

    /** Backdrop: so o herói justifica w1280. */
    fun backdrop(full: Boolean = false): String? = backdropPath?.let {
        "https://image.tmdb.org/t/p/" + (if (full) "w1280" else "w780") + it
    }
}
