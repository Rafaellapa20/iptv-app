package com.iptv.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.media3.extractor.DefaultExtractorsFactory
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView1: PlayerView
    private lateinit var playerView2: PlayerView
    private lateinit var rvQuickChannels: androidx.recyclerview.widget.RecyclerView
    private lateinit var epgContainer: View
    private lateinit var llFloatingControls: View
    private lateinit var btnRec: android.widget.ImageButton
    private lateinit var btnSplitScreen: android.widget.ImageButton
    private lateinit var tvEpgProgram: android.widget.TextView
    private lateinit var tvEpgTime: android.widget.TextView
    private lateinit var pbEpgProgress: android.widget.ProgressBar
    private lateinit var ivNetworkStatus: android.widget.ImageView
    private lateinit var tvNetworkSpeed: android.widget.TextView
    
    // Next Episode Overlay
    private lateinit var llNextEpisode: View
    private lateinit var btnNextEpisode: android.widget.Button
    private lateinit var tvNextCountdown: android.widget.TextView
    private lateinit var btnFavPlayer: android.widget.ImageButton
    
    private var player1: ExoPlayer? = null
    private var player2: ExoPlayer? = null
    
    // 1 for player1, 2 for player2
    private var activePlayerNum = 1
    private var isMultiScreen = false
    
    private var streamId: String? = null
    private var username: String? = null
    private var password: String? = null
    private var isMovieOrEpisode: Boolean = false
    
    private var hideOverlaysRunnable = Runnable { 
        epgContainer.visibility = View.GONE
        llFloatingControls.visibility = View.GONE
    }

    // DVR State
    private var isRecording = false
    private var recordingJob: Job? = null
    private var progressJob: Job? = null
    private var currentStreamUrl = ""
    private var isNextEpisodeOverlayVisible = false

    private lateinit var rlBufferingOverlay: View
    private lateinit var ivLoadingCover: android.widget.ImageView
    private lateinit var tvLoadingTitle: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView1 = findViewById(R.id.player_view)
        playerView2 = findViewById(R.id.player_view_secondary)
        epgContainer = findViewById(R.id.epgContainer)
        llFloatingControls = findViewById(R.id.llFloatingControls)
        btnRec = findViewById(R.id.btnRec)
        btnSplitScreen = findViewById(R.id.btnSplitScreen)
        tvEpgProgram = findViewById(R.id.tvEpgProgram)
        tvEpgTime = findViewById(R.id.tvEpgTime)
        pbEpgProgress = findViewById(R.id.pbEpgProgress)
        ivNetworkStatus = findViewById(R.id.ivNetworkStatus)
        tvNetworkSpeed = findViewById(R.id.tvNetworkSpeed)
        
        llNextEpisode = findViewById(R.id.llNextEpisode)
        btnNextEpisode = findViewById(R.id.btnNextEpisode)
        tvNextCountdown = findViewById(R.id.tvNextCountdown)

        rlBufferingOverlay = findViewById(R.id.rlBufferingOverlay)
        ivLoadingCover = findViewById(R.id.ivLoadingCover)
        tvLoadingTitle = findViewById(R.id.tvLoadingTitle)

        val initialTitle = intent.getStringExtra("TITLE") ?: "A carregar canal..."
        val initialCover = intent.getStringExtra("COVER") ?: ""
        tvLoadingTitle.text = initialTitle
        if (initialCover.isNotEmpty()) {
            com.bumptech.glide.Glide.with(this).load(initialCover).into(ivLoadingCover)
        } else {
            ivLoadingCover.setImageResource(R.drawable.logo)
        }
        rlBufferingOverlay.visibility = View.VISIBLE

        streamId = intent.getStringExtra("STREAM_ID")
        username = intent.getStringExtra("USERNAME")
        password = intent.getStringExtra("PASSWORD")
        val type = intent.getStringExtra("TYPE")
        isMovieOrEpisode = type == "vod" || type == "series"

        // Forçar Imersão com API moderna
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        currentStreamUrl = intent.getStringExtra("VIDEO_URL") ?: ""
        
        // Initialize Players
        val isSeamlessLive = type == "live" && PlayerManager.sharedPlayer != null
        if (isSeamlessLive) {
            player1 = PlayerManager.getPlayer(this)
            // Remover listeners antigos para evitar memory leaks ou duplicados
            player1?.clearVideoSurface() 
        } else {
            player1 = createExoPlayer()
        }
        player2 = createExoPlayer()
        
        playerView1.player = player1
        playerView2.player = player2
        
        playerView1.controllerShowTimeoutMs = 1500
        playerView2.controllerShowTimeoutMs = 1500
        playerView1.setShowNextButton(false)
        playerView1.setShowPreviousButton(false)
        playerView2.setShowNextButton(false)
        playerView2.setShowPreviousButton(false)

        if (currentStreamUrl.isNotEmpty()) {
            if (isSeamlessLive && PlayerManager.currentStreamId == streamId && player1!!.playbackState == androidx.media3.common.Player.STATE_READY) {
                // Já está a tocar este canal do mini-player, não vamos parar!
                // Apenas adicionamos o listener para a UI do PlayerActivity
                addPlayerListener(player1!!)
                player1!!.playWhenReady = true
                rlBufferingOverlay.visibility = View.GONE
            } else {
                if (isSeamlessLive && player1 != null) {
                    addPlayerListener(player1!!)
                }
                playUrlInPlayer(player1!!, currentStreamUrl)
            }
            playerView1.visibility = View.VISIBLE
            playerView2.visibility = View.INVISIBLE
            activePlayerNum = 1
        }

        if (getActivePlayer().isPlaying || getActivePlayer().playbackState == androidx.media3.common.Player.STATE_READY) {
            rlBufferingOverlay.visibility = View.GONE
        }

        setupButtons()
        setupSwipeGestures()
        startProgressMonitor()
    }

    private fun setupButtons() {
        btnFavPlayer = findViewById(R.id.btnFavPlayer)
        updateFavButtonState()
        btnFavPlayer.setOnClickListener {
            if (streamId != null) {
                val title = intent.getStringExtra("TITLE") ?: "Canal"
                val cover = intent.getStringExtra("COVER") ?: ""
                val type = intent.getStringExtra("TYPE") ?: "live"
                val isFav = FavoritesManager.toggleFavorite(this, streamId!!, title, cover, type)
                updateFavButtonState()
                val msg = if (isFav) "⭐ Adicionado aos Favoritos!" else "❌ Removido dos Favoritos"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        btnSplitScreen.setOnClickListener {
            toggleMultiScreen()
        }

        btnRec.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
        
        btnNextEpisode.setOnClickListener {
            playNextEpisode()
        }
    }

    private fun updateFavButtonState() {
        if (streamId != null) {
            val isFav = FavoritesManager.isFavorite(this, streamId!!)
            if (isFav) {
                btnFavPlayer.setColorFilter(android.graphics.Color.YELLOW)
            } else {
                btnFavPlayer.setColorFilter(android.graphics.Color.WHITE)
            }
        }
    }

    private fun createExoPlayer(): ExoPlayer {
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(this)
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000, // min buffer
                60000,  // max buffer
                1500,   // buffer for playback (1.5s - abertura estável de 100% dos canais)
                2500    // buffer for playback after rebuffer
            )
            .setBackBuffer(20000, true)
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setAllowedVideoJoiningTimeMs(5000)

        return ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setVideoScalingMode(androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            .build().apply {
                addPlayerListener(this)
            }
    }

    private fun addPlayerListener(exoPlayer: ExoPlayer) {
        if (exoPlayer == getActivePlayer() && (exoPlayer.isPlaying || exoPlayer.playbackState == androidx.media3.common.Player.STATE_READY)) {
            rlBufferingOverlay.visibility = View.GONE
        }

        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && exoPlayer == getActivePlayer()) {
                    runOnUiThread { rlBufferingOverlay.visibility = View.GONE }
                }
            }

            override fun onRenderedFirstFrame() {
                if (exoPlayer == getActivePlayer()) {
                    runOnUiThread { rlBufferingOverlay.visibility = View.GONE }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (exoPlayer == getActivePlayer()) {
                    updateNetworkStatus(playbackState)
                    if (playbackState == androidx.media3.common.Player.STATE_READY) {
                        runOnUiThread { rlBufferingOverlay.visibility = View.GONE }
                    }
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("PlayerActivity", "Playback error: ${error.message}")
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.prepare()
                } else {
                    if (currentStreamUrl.isNotEmpty()) {
                        val retryUrl = if (currentStreamUrl.endsWith(".ts")) {
                            currentStreamUrl.replace(".ts", ".m3u8")
                        } else {
                            currentStreamUrl
                        }
                        currentStreamUrl = retryUrl
                        val dataSourceFactory = OkHttpDataSource.Factory(OkHttpProvider.client)
                        val extractorsFactory = DefaultExtractorsFactory().setTsExtractorFlags(1 or 16)
                        val mediaSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
                            .createMediaSource(MediaItem.fromUri(Uri.parse(retryUrl)))
                        exoPlayer.setMediaSource(mediaSource)
                        exoPlayer.prepare()
                    }
                }
                exoPlayer.playWhenReady = true
            }
        })
    }

    private fun getActivePlayer(): ExoPlayer = if (activePlayerNum == 1) player1!! else player2!!
    private fun getInactivePlayer(): ExoPlayer = if (activePlayerNum == 1) player2!! else player1!!
    
    private fun getActivePlayerView(): PlayerView = if (activePlayerNum == 1) playerView1 else playerView2
    private fun getInactivePlayerView(): PlayerView = if (activePlayerNum == 1) playerView2 else playerView1

    private fun playUrlInPlayer(exoPlayer: ExoPlayer, url: String, showLoadingOverlay: Boolean = false) {
        if (showLoadingOverlay && exoPlayer == getActivePlayer() && exoPlayer.playbackState != androidx.media3.common.Player.STATE_READY) {
            rlBufferingOverlay.visibility = View.VISIBLE
        } else {
            rlBufferingOverlay.visibility = View.GONE
        }

        val dataSourceFactory = OkHttpDataSource.Factory(OkHttpProvider.client)
        val extractorsFactory = DefaultExtractorsFactory().setTsExtractorFlags(1 or 16)
        val mediaSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(url)))
        
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        
        // Se for VOD, restaura progresso apenas no player ativo principal
        if (isMovieOrEpisode && streamId != null && exoPlayer == getActivePlayer()) {
            val savedProgress = ProgressManager.getProgress(this, streamId!!)
            if (savedProgress > 0) exoPlayer.seekTo(savedProgress)
        }
        
        exoPlayer.playWhenReady = true
        
        if (!isMovieOrEpisode && streamId != null) fetchEPG()
        
        isNextEpisodeOverlayVisible = false
        llNextEpisode.visibility = View.GONE
    }
    
    private fun playNextEpisode() {
        val type = intent.getStringExtra("TYPE")
        if (type == "series") {
            val urls = intent.getStringArrayListExtra("EPISODE_URLS")
            var currentIndex = intent.getIntExtra("CURRENT_INDEX", -1)
            if (urls != null && currentIndex >= 0 && currentIndex < urls.size - 1) {
                currentIndex++
                intent.putExtra("CURRENT_INDEX", currentIndex)
                currentStreamUrl = urls[currentIndex]
                playUrlInPlayer(getActivePlayer(), currentStreamUrl)
            } else {
                Toast.makeText(this, "Último episódio da série", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun startProgressMonitor() {
        progressJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                if (isMovieOrEpisode && intent.getStringExtra("TYPE") == "series") {
                    val p = getActivePlayer()
                    val duration = p.duration
                    val currentPos = p.currentPosition
                    if (duration > 0 && duration - currentPos < 15000) { // Faltam 15 segundos
                        if (!isNextEpisodeOverlayVisible) {
                            isNextEpisodeOverlayVisible = true
                            llNextEpisode.visibility = View.VISIBLE
                            btnNextEpisode.requestFocus()
                        }
                        val remaining = ((duration - currentPos) / 1000).toInt()
                        tvNextCountdown.text = "Próximo Episódio em ${remaining}s"
                        
                        if (remaining <= 0) {
                            playNextEpisode()
                        }
                    } else if (isNextEpisodeOverlayVisible) {
                        isNextEpisodeOverlayVisible = false
                        llNextEpisode.visibility = View.GONE
                    }
                }
                delay(1000)
            }
        }
    }

    private fun changeChannel(isNext: Boolean) {
        val urls = intent.getStringArrayListExtra("CHANNEL_URLS")
        val ids = intent.getStringArrayListExtra("CHANNEL_IDS")
        val names = intent.getStringArrayListExtra("CHANNEL_NAMES")
        var currentIndex = intent.getIntExtra("CURRENT_INDEX", -1)

        if (urls != null && currentIndex >= 0) {
            if (isNext && currentIndex < urls.size - 1) currentIndex++
            else if (!isNext && currentIndex > 0) currentIndex--
            else {
                Toast.makeText(this, "Fim da lista", Toast.LENGTH_SHORT).show()
                return
            }

            intent.putExtra("CURRENT_INDEX", currentIndex)
            currentStreamUrl = urls[currentIndex]
            
            if (ids != null && names != null && currentIndex < ids.size && currentIndex < names.size) {
                streamId = ids[currentIndex]
                val name = names[currentIndex]
                intent.putExtra("STREAM_ID", streamId)
                intent.putExtra("TITLE", name)
                
                LiveTvActivity.lastPlayedStreamId = streamId
                LiveTvActivity.lastPlayedStreamName = name
                
                val prefs = getSharedPreferences("IPTV_PREFS", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("LAST_STREAM_ID", streamId)
                    .putString("LAST_STREAM_NAME", name)
                    .apply()
            }

            Toast.makeText(this, "A sintonizar...", Toast.LENGTH_SHORT).show()

            if (isMultiScreen) {
                // Em Multi-tela, mudamos o canal no player que está focado/ativo
                playUrlInPlayer(getActivePlayer(), currentStreamUrl)
            } else {
                // Desativado o Seamless Zapping para poupar o CPU da Box TV e acabar com os "soluços"
                val active = getActivePlayer()
                active.stop()
                active.clearMediaItems()
                playUrlInPlayer(active, currentStreamUrl)
            }
        }
    }

    private fun swapPlayers() {
        val activeView = getActivePlayerView()
        val inactiveView = getInactivePlayerView()
        val activePlayer = getActivePlayer()
        
        // Torna o inativo visível (que já está a tocar o novo canal)
        inactiveView.visibility = View.VISIBLE
        // Esconde o antigo
        activeView.visibility = View.INVISIBLE
        // Pára o antigo
        activePlayer.stop()
        activePlayer.clearMediaItems()
        
        // Troca os papéis
        activePlayerNum = if (activePlayerNum == 1) 2 else 1
    }

    private fun toggleMultiScreen() {
        isMultiScreen = !isMultiScreen
        
        val widthPixels = resources.displayMetrics.widthPixels
        val halfWidth = widthPixels / 2

        if (isMultiScreen) {
            Toast.makeText(this, "Multi-Tela Ativado! Use setas para trocar áudio", Toast.LENGTH_SHORT).show()
            btnSplitScreen.setColorFilter(android.graphics.Color.YELLOW)
            
            // Coloca os dois lado a lado
            playerView1.layoutParams = FrameLayout.LayoutParams(halfWidth, ViewGroup.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.START }
            playerView2.layoutParams = FrameLayout.LayoutParams(halfWidth, ViewGroup.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.END }
            
            playerView1.visibility = View.VISIBLE
            playerView2.visibility = View.VISIBLE
            
            // Silencia o inativo
            getInactivePlayer().volume = 0f
            getActivePlayer().volume = 1f
            
            // Inicia um canal aleatório no segundo player
            val urls = intent.getStringArrayListExtra("CHANNEL_URLS")
            if (urls != null && urls.size > 1) {
                playUrlInPlayer(getInactivePlayer(), urls[1])
            }
        } else {
            Toast.makeText(this, "Multi-Tela Desativado", Toast.LENGTH_SHORT).show()
            btnSplitScreen.setColorFilter(android.graphics.Color.CYAN)
            
            getInactivePlayer().stop()
            getInactivePlayerView().visibility = View.INVISIBLE
            
            val activeView = getActivePlayerView()
            activeView.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            getActivePlayer().volume = 1f
        }
    }

    private fun startRecording() {
        if (currentStreamUrl.isEmpty()) return
        
        // Permissão checada no Manifest. Para Android 10+ gravar na pasta Public Movies é nativo.
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val fileName = "IPTV_REC_${System.currentTimeMillis()}.ts"
        val outputFile = File(moviesDir, fileName)

        isRecording = true
        btnRec.setColorFilter(android.graphics.Color.WHITE) // Fica branco a piscar psicologicamente
        Toast.makeText(this, "🔴 Gravação Iniciada! Guardando em Filmes...", Toast.LENGTH_LONG).show()

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = java.net.URL(currentStreamUrl)
                val connection = url.openConnection()
                connection.connect()
                
                val inputStream: InputStream = connection.getInputStream()
                val outputStream = FileOutputStream(outputFile)
                val buffer = ByteArray(8192)
                var bytesRead: Int = 0

                while (isRecording && inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlayerActivity, "Erro na gravação: ${e.message}", Toast.LENGTH_SHORT).show()
                    stopRecording()
                }
            }
        }
    }

    private fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        recordingJob?.cancel()
        btnRec.setColorFilter(android.graphics.Color.RED)
        Toast.makeText(this, "⏹ Gravação Parada e Salva com Sucesso!", Toast.LENGTH_LONG).show()
    }

    // --- Outras Funções Padrao ---

    private fun setupQuickChannels() {
        val urls = intent.getStringArrayListExtra("CHANNEL_URLS") ?: return
        rvQuickChannels.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<QuickChannelViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickChannelViewHolder {
                val view = layoutInflater.inflate(R.layout.item_quick_channel, parent, false)
                return QuickChannelViewHolder(view)
            }
            override fun onBindViewHolder(holder: QuickChannelViewHolder, position: Int) {
                holder.tvName.text = "Canal ${position + 1}"
                holder.itemView.setOnClickListener {
                    intent.putExtra("CURRENT_INDEX", position)
                    currentStreamUrl = urls[position]
                    playUrlInPlayer(getActivePlayer(), currentStreamUrl)
                    hideOverlays()
                }
            }
            override fun getItemCount() = urls.size
        }
        rvQuickChannels.adapter = adapter
    }

    class QuickChannelViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val tvName: android.widget.TextView = view.findViewById(R.id.tvQuickChannelName)
    }

    private fun updateNetworkStatus(playbackState: Int) {
        when (playbackState) {
            androidx.media3.common.Player.STATE_BUFFERING -> {
                ivNetworkStatus.setImageResource(android.R.drawable.presence_away)
                ivNetworkStatus.setColorFilter(android.graphics.Color.YELLOW)
                tvNetworkSpeed.text = "Buffering"
                tvNetworkSpeed.setTextColor(android.graphics.Color.YELLOW)
            }
            androidx.media3.common.Player.STATE_READY -> {
                ivNetworkStatus.setImageResource(android.R.drawable.presence_online)
                ivNetworkStatus.setColorFilter(android.graphics.Color.GREEN)
                tvNetworkSpeed.text = "Estável"
                tvNetworkSpeed.setTextColor(android.graphics.Color.GREEN)
            }
        }
    }

    private fun fetchEPG() {
        if (username == null || password == null || streamId == null) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_short_epg&stream_id=$streamId"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = org.json.JSONObject(body)
                    val epgList = json.optJSONArray("epg_listings")
                    if (epgList != null && epgList.length() > 0) {
                        val current = epgList.getJSONObject(0)
                        val title = android.util.Base64.decode(current.getString("title"), android.util.Base64.DEFAULT).decodeToString()
                        val desc = current.optString("description", "")
                        val decodedDesc = if (desc.isNotEmpty()) android.util.Base64.decode(desc, android.util.Base64.DEFAULT).decodeToString() else ""
                        
                        val startTimestamp = current.optLong("start_timestamp", 0L)
                        val stopTimestamp = current.optLong("stop_timestamp", 0L)
                        
                        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        var timeStr = ""
                        var progressPct = 0
                        
                        if (startTimestamp > 0L) {
                            val start = sdf.format(java.util.Date(startTimestamp * 1000))
                            val stop = sdf.format(java.util.Date(stopTimestamp * 1000))
                            timeStr = "$start - $stop"
                            
                            val now = System.currentTimeMillis() / 1000
                            if (now in startTimestamp..stopTimestamp) {
                                val total = stopTimestamp - startTimestamp
                                val passed = now - startTimestamp
                                if (total > 0) progressPct = ((passed * 100) / total).toInt()
                            }
                        } else {
                            val start = current.optString("start", "")
                            val end = current.optString("end", "")
                            if (start.isNotEmpty()) timeStr = "$start - $end"
                        }

                        var nextText = ""
                        if (epgList.length() > 1) {
                            val nextProg = epgList.getJSONObject(1)
                            val nextTitle = android.util.Base64.decode(nextProg.getString("title"), android.util.Base64.DEFAULT).decodeToString()
                            val nextStartTs = nextProg.optLong("start_timestamp", 0L)
                            if (nextStartTs > 0L) {
                                val nextStart = sdf.format(java.util.Date(nextStartTs * 1000))
                                nextText = "\n\nA seguir ($nextStart):\n$nextTitle"
                            }
                        }

                        withContext(Dispatchers.Main) {
                            tvEpgProgram.text = "$title\n$decodedDesc$nextText"
                            tvEpgTime.text = timeStr
                            if (progressPct > 0) {
                                pbEpgProgress.visibility = View.VISIBLE
                                pbEpgProgress.progress = progressPct
                            } else {
                                pbEpgProgress.visibility = View.GONE
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun showOverlays() {
        epgContainer.visibility = View.VISIBLE
        llFloatingControls.visibility = View.VISIBLE
        btnFavPlayer.requestFocus()
        
        epgContainer.removeCallbacks(hideOverlaysRunnable)
        epgContainer.postDelayed(hideOverlaysRunnable, 6000)
    }

    private fun hideOverlays() {
        hideOverlaysRunnable.run()
    }

    private fun setupSwipeGestures() {
        // Gestos horizontais desativados a pedido para não trocar de canal por engano
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val isControlsVisible = llFloatingControls.visibility == View.VISIBLE || epgContainer.visibility == View.VISIBLE
            val isFocusedOnControls = btnFavPlayer.hasFocus() || btnRec.hasFocus() || btnSplitScreen.hasFocus()

            if (isControlsVisible && isFocusedOnControls) {
                epgContainer.removeCallbacks(hideOverlaysRunnable)
                epgContainer.postDelayed(hideOverlaysRunnable, 6000)

                if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT || event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    return super.dispatchKeyEvent(event)
                }
            }

            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (isMultiScreen) {
                        // Foca no player 2
                        activePlayerNum = 2
                        player1?.volume = 0f
                        player2?.volume = 1f
                        Toast.makeText(this, "Áudio: Canal Direito", Toast.LENGTH_SHORT).show()
                    } else if (isMovieOrEpisode) {
                        // Avançar 10s em VOD
                        getActivePlayer().seekTo(getActivePlayer().currentPosition + 10000)
                    } else {
                        showOverlays()
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (isMultiScreen) {
                        // Foca no player 1
                        activePlayerNum = 1
                        player2?.volume = 0f
                        player1?.volume = 1f
                        Toast.makeText(this, "Áudio: Canal Esquerdo", Toast.LENGTH_SHORT).show()
                    } else if (isMovieOrEpisode) {
                        // Recuar 10s em VOD
                        getActivePlayer().seekTo(kotlin.math.max(0L, getActivePlayer().currentPosition - 10000))
                    } else {
                        showOverlays()
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (isControlsVisible && isFocusedOnControls) {
                        return super.dispatchKeyEvent(event)
                    }
                    changeChannel(event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (!isControlsVisible) {
                        showOverlays()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun saveCurrentProgress() {
        if (isMovieOrEpisode && streamId != null) {
            val p = getActivePlayer()
            val currentPos = p.currentPosition
            val duration = p.duration
            val type = intent.getStringExtra("TYPE") ?: "vod"
            
            if (duration > 0) {
                if (currentPos > duration * 0.9) {
                    ProgressManager.markAsSeen(this, streamId!!)
                    ProgressManager.saveProgressFull(this, streamId!!, intent.getStringExtra("TITLE") ?: "", intent.getStringExtra("COVER") ?: "", type, 0L, duration)
                } else if (currentPos > 5000) { // Salvar após 5 seg
                    val cover = intent.getStringExtra("COVER") ?: ""
                    val title = intent.getStringExtra("TITLE") ?: ""
                    val episodeIndex = intent.getIntExtra("CURRENT_INDEX", 0)
                    ProgressManager.saveProgressFull(this, streamId!!, title, cover, type, currentPos, duration, episodeIndex)
                } else {
                    ProgressManager.saveProgressFull(this, streamId!!, intent.getStringExtra("TITLE") ?: "", intent.getStringExtra("COVER") ?: "", type, currentPos, duration)
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        saveCurrentProgress()
        player1?.pause()
        player2?.pause()
    }

    override fun onPause() {
        super.onPause()
        saveCurrentProgress()
        player1?.pause()
        player2?.pause()
    }

    override fun onStop() {
        super.onStop()
        saveCurrentProgress()
        if (player1 != PlayerManager.sharedPlayer) {
            player1?.release()
        }
        playerView1.player = null
        player2?.release()
        stopRecording()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressJob?.cancel()
        if (player1 != PlayerManager.sharedPlayer) {
            player1?.release()
        }
        player2?.release()
        stopRecording()
        System.gc()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }



    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            playerView1.useController = false
            playerView2.useController = false
            llNextEpisode.visibility = View.GONE
            rvQuickChannels.visibility = View.GONE
        } else {
            playerView1.useController = true
            playerView2.useController = true
            if (!isMovieOrEpisode) rvQuickChannels.visibility = View.VISIBLE
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent != null) {
            currentStreamUrl = intent.getStringExtra("VIDEO_URL") ?: ""
            streamId = intent.getStringExtra("STREAM_ID")
            isMovieOrEpisode = (intent.getStringExtra("TYPE") == "vod" || intent.getStringExtra("TYPE") == "series")
            
            if (currentStreamUrl.isNotEmpty()) {
                playUrlInPlayer(getActivePlayer(), currentStreamUrl)
            }
        }
    }
}
