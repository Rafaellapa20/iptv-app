@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.iptv.app

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

object PlayerManager {
    var sharedPlayer: ExoPlayer? = null
    var currentStreamId: String? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (sharedPlayer == null) {
            val trackSelector = DefaultTrackSelector(context.applicationContext).apply {
                setParameters(
                    buildUponParameters()
                        .setAllowVideoMixedMimeTypeAdaptiveness(true)
                        .setAllowVideoNonSeamlessAdaptiveness(true)
                        .setExceedVideoConstraintsIfNecessary(true)
                        .setExceedRendererCapabilitiesIfNecessary(true)
                )
            }

            // Meio-termo entre "zero delay" e buffer normal — ver PlayerActivity
            // para o mesmo ajuste e explicação (reduz falhas sem atraso notável).
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(2500, 8000, 1200, 2200)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(5000, true)
                .build()

            val renderersFactory = DefaultRenderersFactory(context.applicationContext)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                .setAllowedVideoJoiningTimeMs(4000)

            sharedPlayer = ExoPlayer.Builder(context.applicationContext)
                .setRenderersFactory(renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build()
        }
        return sharedPlayer!!
    }

    fun releasePlayer() {
        sharedPlayer?.release()
        sharedPlayer = null
        currentStreamId = null
    }
}
