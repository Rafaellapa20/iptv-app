# -*- coding: utf-8 -*-
with open('app/src/main/res/layout/activity_mobile_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Insert media route button below the logo
cast_btn = '''    <androidx.mediarouter.app.MediaRouteButton
        android:id="@+id/media_route_button"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        app:mediaRouteTypes="user" />
        
    <TextView'''

xml = xml.replace('<TextView', cast_btn, 1)

with open('app/src/main/res/layout/activity_mobile_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

# Update MobileMainActivity
kt = '''package com.iptv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener

class MobileMainActivity : AppCompatActivity() {

    private var username = ""
    private var password = ""
    private var castContext: CastContext? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mobile_main)

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        username = intent.getStringExtra("USERNAME") ?: prefs.getString("USERNAME", "") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: prefs.getString("PASSWORD", "") ?: ""

        // Iniciar Chromecast
        try {
            castContext = CastContext.getSharedInstance(this)
            val mediaRouteButton = findViewById<MediaRouteButton>(R.id.media_route_button)
            CastButtonFactory.setUpMediaRouteButton(applicationContext, mediaRouteButton)
            
            castContext?.addCastStateListener { state ->
                if (state == CastState.CONNECTED) {
                    Toast.makeText(this, "Chromecast Ligado!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        findViewById<Button>(R.id.btnMobileRemote).setOnClickListener {
            Toast.makeText(this, "A procurar Box TV na rede Wi-Fi...", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnMobileLive).setOnClickListener {
            val intent = Intent(this, LiveTvActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnMobileVod).setOnClickListener {
            val intent = Intent(this, VodNetflixActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }
    }
}
'''
with open('app/src/main/java/com/iptv/app/MobileMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
