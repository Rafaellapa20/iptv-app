package com.iptv.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val recentMovies = mutableListOf<Stream>()
    private val recentSeries = mutableListOf<Stream>()

    private var clockJob: kotlinx.coroutines.Job? = null
    private var moviesCardJob: kotlinx.coroutines.Job? = null
    private var seriesCardJob: kotlinx.coroutines.Job? = null

    private var miniPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)

        // Versão real instalada no rodapé (antes era texto fixo no XML,
        // ficava sempre desatualizado a cada release)
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            findViewById<TextView>(R.id.tvAppVersion)?.text = "v$versionName"
        } catch (e: Exception) {}

        // Carregar proxy customizada do utilizador
        val proxyHost = prefs.getString("PROXY_HOST", "176.111.109.14") ?: "176.111.109.14"
        val proxyPort = prefs.getInt("PROXY_PORT", 443)
        OkHttpProvider.updateProxy(proxyHost, proxyPort)

        // Aplicar estado inicial da VPN
        val isVpnEnabled = prefs.getBoolean("VPN_ENABLED", false)
        if (isVpnEnabled) {
            OkHttpProvider.enableDoH()
        } else {
            OkHttpProvider.disableDoH()
        }

        // Verificar atualizações OTA via GitHub silenciosamente
        UpdateManager.checkForUpdates(this)

        val username = intent.getStringExtra("USERNAME") ?: prefs.getString("USERNAME", "") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: prefs.getString("PASSWORD", "") ?: ""
        val vencimento = intent.getStringExtra("VENCIMENTO") ?: "Ilimitado"

        val tvVencimento = findViewById<TextView>(R.id.tvVencimento)
        val tvUserLogged = findViewById<TextView>(R.id.tvUserLogged)
        
        if (vencimento == "null" || vencimento == "Indefinido") {
            tvVencimento.text = "Validade : Ilimitado"
        } else {
            tvVencimento.text = "Validade : $vencimento"
        }

        if (username.isNotEmpty()) {
            tvUserLogged.text = "Perfil: $username"
        }

        // Relógio digital
        val tvClock = findViewById<TextView>(R.id.tvClock)
        clockJob = CoroutineScope(Dispatchers.Main).launch {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            while (isActive) {
                tvClock.text = sdf.format(Date())
                kotlinx.coroutines.delay(1000)
            }
        }

        // HERO ZONE CARTÕES PRINCIPAIS
        findViewById<View>(R.id.cardTv).setOnClickListener { openLiveTv(username, password) }
        findViewById<View>(R.id.cardFilmes).setOnClickListener { openCategories(username, password, "vod") }
        findViewById<View>(R.id.cardSeries).setOnClickListener { openCategories(username, password, "series") }

        // QUICK ACCESS BAR (BARRA DE ACESSO RÁPIDO IGUAL À IMAGEM DE REFERÊNCIA)
        // Botão "Favoritos" removido a pedido do utilizador (redundante com o
        // acesso a favoritos já existente dentro de Live TV).
        findViewById<View>(R.id.btnQuickEpg)?.setOnClickListener {
            val intent = Intent(this, EpgGridActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }
        findViewById<View>(R.id.btnQuickSettings)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<View>(R.id.btnQuickRadios)?.setOnClickListener { showQrDialog() }
                findViewById<View>(R.id.btnQuickCatchup)?.setOnClickListener {
            val p = getSharedPreferences("IPTV_PREFS", android.content.Context.MODE_PRIVATE)
            p.edit().putBoolean("is_senior_mode", true).apply()
            val intent = android.content.Intent(this, SeniorMainActivity::class.java)
            val username = intent.getStringExtra("USERNAME") ?: p.getString("USERNAME", "") ?: ""
            val password = intent.getStringExtra("PASSWORD") ?: p.getString("PASSWORD", "") ?: ""
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            finish()
        }

        // FORÇAR D-PAD: quando o utilizador carrega para BAIXO nos cards, forçar foco na Quick Access Bar
        val cardTv = findViewById<View>(R.id.cardTv)
        val cardFilmes = findViewById<View>(R.id.cardFilmes)
        val cardSeries = findViewById<View>(R.id.cardSeries)

        // O foco já está definido via XML com nextFocusDown nos cartões.
        // Se a navegação não estiver a funcionar, é devido ao RecyclerView em baixo "puxar" o foco.

        findViewById<View>(R.id.btnRefresh)?.setOnClickListener {
            fetchSeriesPosters(username, password)
            Toast.makeText(this, "Listas e Conteúdos Atualizados!", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnSwitchUser)?.setOnClickListener {
            val intent = Intent(this, UsersActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSearch).setOnClickListener {
            val intent = Intent(this, GlobalSearchActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Sair do Aplicativo")
                    .setMessage("Deseja realmente sair?")
                    .setPositiveButton("Sim") { _, _ -> finishAffinity() }
                    .setNegativeButton("Não", null)
                    .show()
            }
        })

        // Configurar Cartão de TV Em Direto com o ÚLTIMO CANAL VISTO (e não o logótipo da app)
        loadLastWatchedLiveChannel(username, password)

        // Configurar Linha "CONTINUAR A VER" (Apenas Filmes e Séries, SEM canais em direto)
        setupContinueWatching()

        // Carregar capas de Filmes e Séries em rotação
        fetchMoviesPosters(username, password)
        fetchSeriesPosters(username, password)

        // Verificar atualizações automaticamente sempre que a app inicia
        UpdateManager.checkForUpdates(this, showNoUpdateToast = false)
    }

    override fun onResume() {
        super.onResume()
        setupContinueWatching()
        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        val password = prefs.getString("PASSWORD", "") ?: ""
        loadLastWatchedLiveChannel(username, password)
    }

    // CARREGAR ÚLTIMO CANAL VISTO COM MINI-PLAYER EM DIRETO NO CARTÃO LIVE TV
    private fun loadLastWatchedLiveChannel(username: String, password: String) {
        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val lastChannelName = prefs.getString("LAST_STREAM_NAME", "") ?: ""
        val lastChannelId = prefs.getString("LAST_STREAM_ID", "") ?: ""

        val tvTitleTv = findViewById<TextView>(R.id.tvTitleTv) ?: return
        val tvSubTv = findViewById<TextView>(R.id.tvSubTv) ?: return
        val cardTv = findViewById<View>(R.id.cardTv)

        if (lastChannelName.isNotEmpty() && lastChannelId.isNotEmpty()) {
            tvTitleTv.text = lastChannelName
            tvSubTv.text = "🔴 A DAR: Programação TV"

            startMiniPlayer(username, password, lastChannelId)
            fetchChannelEpgTitle(username, password, lastChannelId, tvSubTv)

            // Removed cardTv.setOnClickListener here because it overrides the one in onCreate that opens LiveTvActivity
        } else {
            fetchFirstChannelAndPlay(username, password)
        }
    }

    private fun startMiniPlayer(user: String, pass: String, streamId: String) {
        try {
            miniPlayer?.release()
            miniPlayer = ExoPlayer.Builder(this).build()
            val playerView = findViewById<PlayerView>(R.id.miniPlayerView)
            playerView?.player = miniPlayer

            val streamUrl = "${Constants.SERVER_URL}/live/$user/$pass/$streamId.ts"
            miniPlayer?.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
            miniPlayer?.volume = 0f  // Silencioso no menu principal
            miniPlayer?.playWhenReady = true
            miniPlayer?.prepare()
        } catch (e: Exception) {}
    }

    private fun fetchChannelEpgTitle(user: String, pass: String, streamId: String, tvSub: TextView) {
        if (user.isEmpty() || pass.isEmpty() || streamId.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$user&password=$pass&action=get_short_epg&stream_id=$streamId"
                val response = OkHttpProvider.client.newCall(Request.Builder().url(url).build()).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val listings = json.optJSONArray("epg_listings")
                    if (listings != null && listings.length() > 0) {
                        val current = listings.getJSONObject(0)
                        val titleEnc = current.optString("title", "")
                        val title = try {
                            String(android.util.Base64.decode(titleEnc, android.util.Base64.DEFAULT), Charsets.UTF_8)
                        } catch (e: Exception) { titleEnc }

                        withContext(Dispatchers.Main) {
                            if (title.isNotEmpty()) {
                                tvSub.text = "🔴 A DAR: $title"
                            }
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun fetchFirstChannelAndPlay(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_live_streams"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val array = JSONArray(body)
                    if (array.length() > 0) {
                        val firstObj = array.getJSONObject(0)
                        val name = firstObj.getString("name")
                        val sId = firstObj.getString("stream_id")

                        withContext(Dispatchers.Main) {
                            findViewById<TextView>(R.id.tvTitleTv)?.text = name
                            findViewById<TextView>(R.id.tvSubTv)?.text = "🔴 A DAR: Programação TV"

                            startMiniPlayer(username, password, sId)
                            fetchChannelEpgTitle(username, password, sId, findViewById(R.id.tvSubTv))

                            // Removed cardTv.setOnClickListener here because it overrides the one in onCreate that opens LiveTvActivity
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    // overridePendingTransition está deprecated a partir da API 34 (substituída por
    // overrideActivityTransition), mas continua a funcionar em todas as versões
    // suportadas (minSdk 24). Mantido por agora para não arriscar mudar o
    // comportamento de transições sem poder testar em dispositivo real.
    @Suppress("DEPRECATION")
    private fun openLiveTv(user: String, pass: String) {
        val intent = Intent(this, LiveTvActivity::class.java)
        intent.putExtra("USERNAME", user)
        intent.putExtra("PASSWORD", pass)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    @Suppress("DEPRECATION")
    private fun openCategories(user: String, pass: String, type: String) {
        val intent = Intent(this, VodNetflixActivity::class.java)
        intent.putExtra("USERNAME", user)
        intent.putExtra("PASSWORD", pass)
        intent.putExtra("TYPE", type)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    // LINHA INFERIOR "CONTINUAR A VER" (FILTRADA: APENAS FILMES E SÉRIES, SEM CANAIS EM DIRETO)
    private fun setupContinueWatching() {
        val rv = findViewById<RecyclerView>(R.id.rvContinueWatching) ?: return
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        
        val allRecents = RecentManager.getRecent(this)
        // Filtra para manter EXCLUSIVAMENTE Filmes e Séries VOD
        val vodOnlyRecents = allRecents.filter { item ->
            item.stream_type == "movie" || item.stream_type == "series" || item.stream_type == "vod"
        }

        if (vodOnlyRecents.isEmpty()) {
            findViewById<View>(R.id.tvContinueTitleHeader)?.visibility = View.GONE
            rv.visibility = View.GONE
        } else {
            findViewById<View>(R.id.tvContinueTitleHeader)?.visibility = View.VISIBLE
            rv.visibility = View.VISIBLE
            rv.adapter = ContinueWatchingAdapter(vodOnlyRecents)
        }
    }

    // SLIDESHOW DE CAPAS DE FILMES DENTRO DO CARD DE FILMES
    private fun fetchMoviesPosters(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_vod_streams"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream() ?: return@launch
                    val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, "UTF-8"))
                    recentMovies.clear()
                    
                    val list = mutableListOf<Pair<Stream, Long>>()
                    
                    try {
                        // Sometimes the API might return an object with error or no data. Check token.
                        if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                var streamId = ""
                                var name = ""
                                var icon = ""
                                var added = 0L
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    if (reader.peek() == android.util.JsonToken.NULL) {
                                        reader.skipValue()
                                        continue
                                    }
                                    when (key) {
                                        "stream_id" -> streamId = reader.nextString()
                                        "name" -> name = reader.nextString()
                                        "stream_icon" -> icon = reader.nextString()
                                        "added" -> added = reader.nextString().toLongOrNull() ?: 0L
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                if (icon.isNotEmpty()) {
                                    list.add(Stream(streamId, name, icon, "movie", "mp4") to added)
                                }
                                if (list.size > 2000) break // Speed up parsing by limiting to 2000 covers

                            }
                            reader.endArray()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try { reader.close() } catch(e:Exception){}
                    }

                    list.sortByDescending { it.second }
                    recentMovies.addAll(list.map { it.first }.take(30))

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        startMoviesCardSlideshow()
                        setupFeaturedMovies(recentMovies)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startMoviesCardSlideshow() {
        if (recentMovies.isEmpty()) return
        val ivCardFilmesBg = findViewById<ImageView>(R.id.ivCardFilmesBg) ?: return
        val tvTitleFilmes = findViewById<TextView>(R.id.tvTitleFilmes)
        val ivMainBackgroundBlur = findViewById<ImageView>(R.id.ivMainBackgroundBlur)

        moviesCardJob?.cancel()
        moviesCardJob = CoroutineScope(Dispatchers.Main).launch {
            var index = 0
            while (isActive) {
                val movie = recentMovies[index]
                if (movie.stream_icon.isNotEmpty()) {
                    Glide.with(this@MainActivity)
                        .load(movie.stream_icon)
                        .transition(DrawableTransitionOptions.withCrossFade(1000))
                        .into(ivCardFilmesBg)

                    if (tvTitleFilmes != null) {
                        tvTitleFilmes.text = movie.name
                    }

                    if (ivMainBackgroundBlur != null && index % 2 == 0) {
                        Glide.with(this@MainActivity)
                            .load(movie.stream_icon)
                            .apply(com.bumptech.glide.request.RequestOptions.bitmapTransform(jp.wasabeef.glide.transformations.BlurTransformation(25, 3)))
                            .transition(DrawableTransitionOptions.withCrossFade(1200))
                            .into(ivMainBackgroundBlur)
                    }
                }
                kotlinx.coroutines.delay(6000)
                index = (index + 1) % recentMovies.size
            }
        }
    }

    // SLIDESHOW DE CAPAS DE SÉRIES DENTRO DO CARD DE SÉRIES
    private fun fetchSeriesPosters(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_series"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream() ?: return@launch
                    val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, "UTF-8"))
                    recentSeries.clear()
                    
                    try {
                        if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                var seriesId = ""
                                var name = ""
                                var icon = ""
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    if (reader.peek() == android.util.JsonToken.NULL) {
                                        reader.skipValue()
                                        continue
                                    }
                                    when (key) {
                                        "series_id" -> seriesId = reader.nextString()
                                        "name" -> name = reader.nextString()
                                        "cover" -> icon = reader.nextString()
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                if (icon.isNotEmpty()) {
                                    recentSeries.add(Stream(seriesId, name, icon, "series", ""))
                                }
                                if (recentSeries.size > 2000) break // Speed up parsing by limiting to 2000 covers

                                if (recentSeries.size > 200) break // Limit to save memory
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try { reader.close() } catch(e:Exception){}
                    }

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        startSeriesCardSlideshow()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startSeriesCardSlideshow() {
        if (recentSeries.isEmpty()) return
        val ivCardSeriesBg = findViewById<ImageView>(R.id.ivCardSeriesBg) ?: return
        val tvTitleSeries = findViewById<TextView>(R.id.tvTitleSeries)

        seriesCardJob?.cancel()
        seriesCardJob = CoroutineScope(Dispatchers.Main).launch {
            var index = 0
            while (isActive) {
                val s = recentSeries[index]
                if (s.stream_icon.isNotEmpty()) {
                    Glide.with(this@MainActivity)
                        .load(s.stream_icon)
                        .transition(DrawableTransitionOptions.withCrossFade(1000))
                        .into(ivCardSeriesBg)

                    if (tvTitleSeries != null) {
                        tvTitleSeries.text = s.name
                    }
                }
                kotlinx.coroutines.delay(6500)
                index = (index + 1) % recentSeries.size
            }
        }
    }

    inner class ContinueWatchingAdapter(private val list: List<Stream>) : RecyclerView.Adapter<ContinueWatchingAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val poster: ImageView = v.findViewById(R.id.ivContinuePoster)
            val title: TextView = v.findViewById(R.id.tvContinueTitle)
            val pb: ProgressBar = v.findViewById(R.id.pbContinueProgress)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_continue_watching, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.title.text = item.name
            if (item.stream_icon.isNotEmpty()) {
                Glide.with(holder.itemView.context).load(item.stream_icon).into(holder.poster)
            } else {
                holder.poster.setImageResource(R.drawable.logo)
            }
            holder.pb.progress = (30..80).random()

            holder.itemView.setOnClickListener {
                val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
                val user = prefs.getString("USERNAME", "") ?: ""
                val pass = prefs.getString("PASSWORD", "") ?: ""

                val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                val url = if (item.stream_type == "movie" || item.stream_type == "vod") {
                    "${Constants.SERVER_URL}/movie/$user/$pass/${item.stream_id}.${item.extension}"
                } else if (item.stream_type == "series") {
                    "${Constants.SERVER_URL}/series/$user/$pass/${item.stream_id}.${item.extension}"
                } else {
                    "${Constants.SERVER_URL}/live/$user/$pass/${item.stream_id}.ts"
                }

                intent.putExtra("VIDEO_URL", url)
                intent.putExtra("STREAM_ID", item.stream_id)
                intent.putExtra("TITLE", item.name)
                intent.putExtra("TYPE", item.stream_type)
                startActivity(intent)
            }
        }

        override fun getItemCount() = list.size
    }

    private fun setupFeaturedMovies(movies: List<Stream>) {
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFeaturedMovies) ?: return
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        if (movies.isEmpty()) {
            findViewById<View>(R.id.tvFeaturedTitleHeader)?.visibility = View.GONE
            rv.visibility = View.GONE
        } else {
            findViewById<View>(R.id.tvFeaturedTitleHeader)?.visibility = View.VISIBLE
            rv.visibility = View.VISIBLE
            rv.adapter = FeaturedMoviesAdapter(movies)
        }
    }

    inner class FeaturedMoviesAdapter(private val list: List<Stream>) : androidx.recyclerview.widget.RecyclerView.Adapter<FeaturedMoviesAdapter.VH>() {
        inner class VH(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
            val poster: ImageView = v.findViewById(R.id.ivMoviePosterCard)
            val title: TextView = v.findViewById(R.id.tvMoviePosterTitle)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_movie_poster_card, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.title.text = item.name
            if (item.stream_icon.isNotEmpty()) {
                com.bumptech.glide.Glide.with(holder.itemView.context).load(item.stream_icon).into(holder.poster)
            } else {
                holder.poster.setImageResource(R.drawable.logo)
            }

            holder.itemView.setOnClickListener {
                val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
                val user = prefs.getString("USERNAME", "") ?: ""
                val pass = prefs.getString("PASSWORD", "") ?: ""

                val intent = android.content.Intent(this@MainActivity, MovieInfoActivity::class.java)
                val videoUrl = "${Constants.SERVER_URL}/movie/$user/$pass/${item.stream_id}.${item.extension}"
                intent.putExtra("STREAM_ID", item.stream_id)
                intent.putExtra("TITLE", item.name)
                intent.putExtra("COVER", item.stream_icon)
                intent.putExtra("VIDEO_URL", videoUrl)
                intent.putExtra("USERNAME", user)
                intent.putExtra("PASSWORD", pass)
                startActivity(intent)
            }
        }

        override fun getItemCount() = list.size
    }

    override fun onPause() {
        super.onPause()
        miniPlayer?.pause()
    }

    
    // Não chama super.onBackPressed() de propósito: queremos sempre confirmar
    // antes de sair, em vez do comportamento por omissão (sair imediatamente).
    @Deprecated("Deprecated in Java", ReplaceWith("super.onBackPressed()"))
    @Suppress("MissingSuperCall", "DEPRECATION")
    override fun onBackPressed() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Sair")
            .setMessage("Tem a certeza que deseja sair da aplicação?")
            .setPositiveButton("Sim") { _, _ ->
                finishAffinity() // Closes all activities
            }
            .setNegativeButton("Não", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockJob?.cancel()
        moviesCardJob?.cancel()
        seriesCardJob?.cancel()
        miniPlayer?.release()
        miniPlayer = null
    }

    private fun showQrDialog() {
        val dialog = android.app.Dialog(this@MainActivity)
        dialog.setContentView(R.layout.dialog_qr)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val ivQr = dialog.findViewById<android.widget.ImageView>(R.id.ivQrCode)
        val apkUrl = "https://tinyurl.com/2985xryp"
        val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=" + android.net.Uri.encode(apkUrl)

        com.bumptech.glide.Glide.with(this@MainActivity).load(qrUrl).into(ivQr)
        dialog.show()

        val tvCode = dialog.findViewById<TextView>(R.id.tvPairCode)
        val tvStatus = dialog.findViewById<TextView>(R.id.tvPairCodeStatus)
        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        val password = prefs.getString("PASSWORD", "") ?: ""
        if (username.isEmpty() || password.isEmpty()) {
            tvStatus.text = "Sem sessão iniciada."
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            val code = PairingManager.generateSelfCode(username, password)
            if (!dialog.isShowing) return@launch
            if (code != null) {
                tvCode.text = code
            } else {
                tvCode.text = "Erro"
                tvStatus.text = "Não foi possível ligar ao servidor de emparelhamento."
            }
        }
    }
}