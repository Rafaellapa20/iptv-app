package com.iptv.app

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter

/**
 * Qualidade de imagem + velocidade, num sitio so.
 * Ver secoes 10d e 10e do guia de implementacao.
 *
 * Regras:
 *  - o video NAO passa pelo proxy SOCKS5 (so a playlist e o EPG passam)
 *  - preparacao sem chunks: o maior ganho isolado a abrir um canal
 *  - arranque de ABR alto: sem os primeiros 20s a parecerem maus
 *  - UMA instancia viva, reutilizada; nunca recriada por canal
 */
object PlayerFactory {

    // Cliente LIMPO, sem proxy: so para media.
    private fun mediaDataSource(ctx: Context): DataSource.Factory =
        OkHttpDataSource.Factory(OkHttpProvider.mediaClient())
            .setUserAgent("IPTVGlobal/2.4.1")

    private fun bandwidthMeter(ctx: Context) =
        DefaultBandwidthMeter.Builder(ctx)
            .setInitialBitrateEstimate(8_000_000)   // assume boa rede, corrige se falhar
            .build()

    private fun loadControl() =
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                20_000,  // min
                60_000,  // max: menos descidas de qualidade a meio
                800,     // para comecar a tocar: arranque rapido
                2_000    // depois de rebuffer
            )
            .build()

    fun create(ctx: Context, isTv: Boolean): ExoPlayer {
        val trackSelector = DefaultTrackSelector(ctx).apply {
            parameters = buildUponParameters()
                .setTunnelingEnabled(isTv)          // deixa o descodificador sair em nativo
                .setMinVideoBitrate(1_500_000)      // nunca comecar na variante mais baixa
                .build()
        }

        return ExoPlayer.Builder(ctx)
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(bandwidthMeter(ctx))
            .setLoadControl(loadControl())
            .build()
    }

    fun liveSource(ctx: Context, url: String): HlsMediaSource =
        HlsMediaSource.Factory(mediaDataSource(ctx))
            .setAllowChunklessPreparation(true)     // <-- o maior ganho isolado
            .createMediaSource(MediaItem.fromUri(url))

    /** Canal em directo: forcar a variante mais alta assim que houver buffer. */
    fun preferHighest(player: ExoPlayer, force: Boolean) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setForceHighestSupportedBitrate(force)
            .build()
    }
}
