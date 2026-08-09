package com.iptv.app

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object PlayerManager {
    var sharedPlayer: ExoPlayer? = null
    var currentStreamId: String? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (sharedPlayer == null) {
            sharedPlayer = ExoPlayer.Builder(context.applicationContext).build()
        }
        return sharedPlayer!!
    }

    fun releasePlayer() {
        sharedPlayer?.release()
        sharedPlayer = null
        currentStreamId = null
    }
}
