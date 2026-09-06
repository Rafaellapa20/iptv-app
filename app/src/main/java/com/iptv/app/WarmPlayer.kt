package com.iptv.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer

/**
 * Um player quente + pre-ligacao do canal em foco.
 * E isto que faz o zapping parecer instantaneo: 300ms depois de o foco
 * parar num canal, o stream ja esta preparado (sem som, sem tocar).
 *
 * Substitui a logica de criar/libertar ExoPlayer por canal.
 */
class WarmPlayer(private val ctx: Context, private val isTv: Boolean) {

    private val handler = Handler(Looper.getMainLooper())
    private var main: ExoPlayer? = null
    private var warm: ExoPlayer? = null
    private var warmUrl: String? = null
    private var pending: Runnable? = null

    fun main(): ExoPlayer =
        main ?: PlayerFactory.create(ctx, isTv).also { main = it }

    /** Chamar quando o foco assenta num canal (grelha, lista, EPG). */
    fun onFocus(url: String) {
        pending?.let { handler.removeCallbacks(it) }
        if (url == warmUrl) return
        val r = Runnable {
            warm?.release()
            warm = PlayerFactory.create(ctx, isTv).apply {
                volume = 0f
                setMediaSource(PlayerFactory.liveSource(ctx, url))
                prepare()                     // primeiro fotograma ja existe
            }
            warmUrl = url
        }
        pending = r
        handler.postDelayed(r, 300)
    }

    /** Chamar no OK. Se o canal e o pre-ligado, a troca e imediata. */
    fun play(url: String): ExoPlayer {
        if (url == warmUrl && warm != null) {
            main?.release()
            main = warm!!.apply { volume = 1f; playWhenReady = true }
            warm = null; warmUrl = null
            return main!!
        }
        return main().apply {
            setMediaSource(PlayerFactory.liveSource(ctx, url))
            prepare(); playWhenReady = true
        }
    }

    fun release() {
        pending?.let { handler.removeCallbacks(it) }
        warm?.release(); warm = null; warmUrl = null
        main?.release(); main = null
    }
}
