package com.iptv.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray

class MultiScreenActivity : AppCompatActivity() {

    private var player1: ExoPlayer? = null
    private var player2: ExoPlayer? = null

    private lateinit var playerView1: PlayerView
    private lateinit var playerView2: PlayerView

    private lateinit var tvTitleScreen1: TextView
    private lateinit var tvTitleScreen2: TextView
    private lateinit var btnToggleAudio: Button

    private val allChannels = mutableListOf<Stream>()
    private var activeAudioScreen = 1 // 1 ou 2

    private var username = ""
    private var password = ""

    private var uiHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiscreen)

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        username = intent.getStringExtra("USERNAME") ?: prefs.getString("USERNAME", "") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: prefs.getString("PASSWORD", "") ?: ""

        playerView1 = findViewById(R.id.playerView1)
        playerView2 = findViewById(R.id.playerView2)

        tvTitleScreen1 = findViewById(R.id.tvTitleScreen1)
        tvTitleScreen2 = findViewById(R.id.tvTitleScreen2)
        btnToggleAudio = findViewById(R.id.btnToggleAudio)

        findViewById<ImageButton>(R.id.btnBackMulti).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnChangeChannel1).setOnClickListener {
            showChannelPicker(1)
        }

        findViewById<Button>(R.id.btnChangeChannel2).setOnClickListener {
            showChannelPicker(2)
        }

        btnToggleAudio.setOnClickListener {
            if (activeAudioScreen == 1) {
                activeAudioScreen = 2
                player1?.volume = 0.0f
                player2?.volume = 1.0f
                btnToggleAudio.text = "🔊 SOM: ECRÃ 2"
            } else {
                activeAudioScreen = 1
                player1?.volume = 1.0f
                player2?.volume = 0.0f
                btnToggleAudio.text = "🔊 SOM: ECRÃ 1"
            }
        }

        createPlayers()
        fetchLiveChannels()
    }

    private fun createPlayers() {
        val renderersFactory1 = androidx.media3.exoplayer.DefaultRenderersFactory(this).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }
        player1 = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory1)
            .build().apply { volume = 1.0f }

        val renderersFactory2 = androidx.media3.exoplayer.DefaultRenderersFactory(this).apply {
            setEnableDecoderFallback(true)
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        }
        player2 = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory2)
            .build().apply { volume = 0.0f }

        playerView1.player = player1
        playerView2.player = player2
    }

    private fun fetchLiveChannels() {
        if (username.isEmpty() || password.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_live_streams"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val array = JSONArray(body)
                    allChannels.clear()

                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        allChannels.add(
                            Stream(
                                obj.getString("stream_id"),
                                obj.getString("name"),
                                obj.optString("stream_icon", ""),
                                "live",
                                "ts"
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (allChannels.size >= 2) {
                            playChannelOnScreen(1, allChannels[0])
                            playChannelOnScreen(2, allChannels[1])
                        } else if (allChannels.size == 1) {
                            playChannelOnScreen(1, allChannels[0])
                            playChannelOnScreen(2, allChannels[0])
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MultiScreenActivity, "Erro ao carregar canais", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playChannelOnScreen(screenNum: Int, stream: Stream) {
        val streamUrl = "${Constants.SERVER_URL}/live/$username/$password/${stream.stream_id}.ts"
        val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))

        if (screenNum == 1) {
            tvTitleScreen1.text = "Ecrã 1: ${stream.name}"
            player1?.setMediaItem(mediaItem)
            player1?.prepare()
            player1?.playWhenReady = true
        } else {
            tvTitleScreen2.text = "Ecrã 2: ${stream.name}"
            player2?.setMediaItem(mediaItem)
            player2?.prepare()
            player2?.playWhenReady = true
        }
    }

    private fun showChannelPicker(screenNum: Int) {
        if (allChannels.isEmpty()) {
            Toast.makeText(this, "Carregando canais...", Toast.LENGTH_SHORT).show()
            return
        }

        val namesArray = allChannels.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecionar Canal para Ecrã $screenNum")
            .setItems(namesArray) { _, which ->
                val selectedStream = allChannels[which]
                playChannelOnScreen(screenNum, selectedStream)
                hideUI()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun hideUI() {
        uiHidden = true
        findViewById<View>(R.id.headerMultiScreen).visibility = View.GONE
        findViewById<View>(R.id.bannerScreen1).visibility = View.GONE
        findViewById<View>(R.id.bannerScreen2).visibility = View.GONE
    }

    private fun showUI() {
        uiHidden = false
        findViewById<View>(R.id.headerMultiScreen).visibility = View.VISIBLE
        findViewById<View>(R.id.bannerScreen1).visibility = View.VISIBLE
        findViewById<View>(R.id.bannerScreen2).visibility = View.VISIBLE
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (uiHidden) {
                    showUI()
                    return true
                }
            } else if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                if (uiHidden) {
                    showUI()
                    return true // intercepta o voltar para apenas mostrar UI
                }
            } else {
                if (uiHidden) {
                    showUI()
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        player1?.release()
        player2?.release()
        player1 = null
        player2 = null
    }
}
