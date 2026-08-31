package com.iptv.app

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl

object PlayerManager {
    var sharedPlayer: ExoPlayer? = null
    var currentStreamId: String? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (sharedPlayer == null) {
            // Buffer ultra rápido para eliminação do ecrã preto e abertura instantânea
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15000, // min buffer
                    60000,  // max buffer
                    500,    // buffer for playback (apenas 0.5s para arrancar logo!)
                    1000    // buffer for playback after rebuffer
                ).build()

            sharedPlayer = ExoPlayer.Builder(context.applicationContext)
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
