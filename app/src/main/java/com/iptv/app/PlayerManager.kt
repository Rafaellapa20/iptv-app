package com.iptv.app

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory

object PlayerManager {
    var sharedPlayer: ExoPlayer? = null
    var currentStreamId: String? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (sharedPlayer == null) {
            // LoadControl otimizado para abertura INSTANTÂNEA de canais (300ms)
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    5000,  // min buffer
                    30000, // max buffer
                    300,   // buffer for playback (0.3s - Abertura instantânea!)
                    600    // buffer for playback after rebuffer
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .setBackBuffer(10000, true)
                .build()

            val renderersFactory = DefaultRenderersFactory(context.applicationContext)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setAllowedVideoJoiningTimeMs(2000)

            sharedPlayer = ExoPlayer.Builder(context.applicationContext)
                .setRenderersFactory(renderersFactory)
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
