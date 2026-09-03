package com.iptv.app

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

        val btnRemote = findViewById<Button>(R.id.btnMobileRemote)
        btnRemote.setOnClickListener {
            btnRemote.text = "A procurar..."
            RemoteManager.discoverTv(this, 
                onFound = { ip -> 
                    Toast.makeText(this, "TV Ligada! IP: ", Toast.LENGTH_SHORT).show()
                    btnRemote.text = "Ligado à TV"
                    btnRemote.setBackgroundColor(android.graphics.Color.parseColor("#00E5FF"))
                },
                onTimeout = {
                    Toast.makeText(this, "Não foi possível encontrar a TV na rede.", Toast.LENGTH_SHORT).show()
                    btnRemote.text = "Ligar à TV (Tentar Novamente)"
                }
            )
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
