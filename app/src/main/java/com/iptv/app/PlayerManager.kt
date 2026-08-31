package com.iptv.app

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

import androidx.media3.exoplayer.DefaultLoadControl

object PlayerManager {
    var sharedPlayer: ExoPlayer? = null
    var currentStreamId: String? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (sharedPlayer == null) {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    32000, // min buffer
                    120000, // max buffer
                    2500, // buffer for playback
                    5000 // buffer for playback after rebuffer
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
