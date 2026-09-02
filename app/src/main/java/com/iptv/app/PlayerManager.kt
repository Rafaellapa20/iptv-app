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

            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15000, // min buffer (15s)
                    50000, // max buffer (50s)
                    2500,  // buffer for playback (2.5s - Fluidez 100% sem travamentos)
                    3500   // buffer for playback after rebuffer
                )
                .setBackBuffer(15000, true)
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
