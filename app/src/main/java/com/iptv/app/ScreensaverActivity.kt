package com.iptv.app

import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class ScreensaverActivity : AppCompatActivity() {

    private lateinit var tvClock: TextView
    private var updateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screensaver)

        tvClock = findViewById(R.id.tvClock)
        
        // Hide UI completely
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        updateJob = CoroutineScope(Dispatchers.Main).launch {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            while (isActive) {
                tvClock.text = sdf.format(Date())
                
                // Random position to prevent burn-in
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                
                // Allow some margins
                val maxX = (screenWidth - 300).coerceAtLeast(1)
                val maxY = (screenHeight - 200).coerceAtLeast(1)
                
                tvClock.x = Random.nextInt(maxX).toFloat()
                tvClock.y = Random.nextInt(maxY).toFloat()
                
                delay(60000) // Move every minute
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Any key dismisses the screensaver
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
    }
}
