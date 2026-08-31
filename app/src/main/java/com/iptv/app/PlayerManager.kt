package com.iptv.app

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl

object PlayerManager {
    var sharedPlayer: ExoPlayer? = null
    var currentStreamId: String? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (sharedPlayer == null) {
            // Buffer ideal para estabilidade máxima sem falhas em transmissões ao vivo
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    15000, // min buffer
                    60000, // max buffer
                    2500,  // buffer for playback (2.5s para estabilidade total contra travamentos)
                    3500   // buffer for playback after rebuffer
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
