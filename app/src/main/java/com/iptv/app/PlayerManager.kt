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
            // Ajuste Ultra Low Latency (Tempo Real) graças à velocidade Gigabit da nossa VPN privada
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15000, // min buffer
                    60000, // max buffer
                    1000,  // buffer for playback (apenas 1.0s para arranque em TEMPO REAL sem atrasos no sinal)
                    1500   // buffer for playback after rebuffer
                )
                .setBackBuffer(15000, true)
                .build()

            val renderersFactory = DefaultRenderersFactory(context.applicationContext)
                .setEnableDecoderFallback(true)
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
