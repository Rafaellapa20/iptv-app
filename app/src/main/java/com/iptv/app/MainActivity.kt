package com.iptv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val recentMovies = mutableListOf<Stream>()
    private val recentSeries = mutableListOf<Stream>()
    private val recentTv = mutableListOf<Stream>()

    private var clockJob: kotlinx.coroutines.Job? = null
    private var moviesCardJob: kotlinx.coroutines.Job? = null
    private var seriesCardJob: kotlinx.coroutines.Job? = null
    private var tvCardJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Verificar atualizações OTA via GitHub silenciosamente
        UpdateManager.checkForUpdates(this)

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = intent.getStringExtra("USERNAME") ?: prefs.getString("USERNAME", "") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: prefs.getString("PASSWORD", "") ?: ""
        val vencimento = intent.getStringExtra("VENCIMENTO") ?: "Ilimitado"

        val tvVencimento = findViewById<TextView>(R.id.tvVencimento)
        
        if (vencimento == "null" || vencimento == "Indefinido") {
            tvVencimento.text = "VALIDADE: Ilimitado"
        } else {
            tvVencimento.text = "VALIDADE: $vencimento"
        }

        // Relógio em tempo real estilo IPTV Smarters Pro
        val tvClock = findViewById<TextView>(R.id.tvClock)
        clockJob = CoroutineScope(Dispatchers.Main).launch {
            val sdf = SimpleDateFormat("HH:mm - EEE, dd MMM", Locale("pt", "PT"))
            while (isActive) {
                tvClock.text = sdf.format(Date())
                kotlinx.coroutines.delay(1000)
            }
        }

        // Card 1: TV Em Direto
        findViewById<View>(R.id.cardTv).setOnClickListener {
            val intent = Intent(this, LiveTvActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        // Card 2: Filmes VOD
        findViewById<View>(R.id.cardFilmes).setOnClickListener {
            openCategories(username, password, "vod")
        }

        // Card 3: Séries
        findViewById<View>(R.id.cardSeries).setOnClickListener {
            openCategories(username, password, "series")
        }

        // Card 4: Favoritos
        findViewById<View>(R.id.cardFavorites).setOnClickListener {
            val intent = Intent(this, StreamsActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("TYPE", "favorites")
            intent.putExtra("CATEGORY_NAME", "Meus Favoritos")
            startActivity(intent)
        }

        findViewById<View>(R.id.btnExit).setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnRefresh).setOnClickListener {
            android.widget.Toast.makeText(this, "Atualizando Portal...", android.widget.Toast.LENGTH_SHORT).show()
            val intent = intent
            finish()
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        findViewById<View>(R.id.btnEpg)?.setOnClickListener {
            val intent = Intent(this, EpgGridActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
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
                    .setPositiveButton("Sim") { _, _ ->
                        finishAffinity()
                    }
                    .setNegativeButton("Não", null)
                    .show()
            }
        })

        // Carregar capas de Filmes, Séries e Canais para exibição dinâmica nos cartões
        fetchMoviesPosters(username, password)
        fetchSeriesPosters(username, password)
        fetchTvLogos(username, password)
    }

    private fun openCategories(user: String, pass: String, type: String) {
        val intent = Intent(this, VodNetflixActivity::class.java)
        intent.putExtra("USERNAME", user)
        intent.putExtra("PASSWORD", pass)
        intent.putExtra("TYPE", type)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    // SLIDESHOW DE CAPAS DE FILMES DENTRO DO CARD DE FILMES VOD
    private fun fetchMoviesPosters(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_vod_streams"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val array = JSONArray(body)
                    recentMovies.clear()

                    val list = mutableListOf<Pair<Stream, Long>>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val icon = obj.optString("stream_icon", "")
                        val added = obj.optString("added", "0").toLongOrNull() ?: 0L
                        if (icon.isNotEmpty()) {
                            list.add(Stream(obj.getString("stream_id"), obj.getString("name"), icon, "movie", "mp4") to added)
                        }
                    }
                    list.sortByDescending { it.second }
                    recentMovies.addAll(list.map { it.first }.take(20))

                    withContext(Dispatchers.Main) {
                        startMoviesCardSlideshow()
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun startMoviesCardSlideshow() {
        if (recentMovies.isEmpty()) return
        val ivCardFilmesBg = findViewById<ImageView>(R.id.ivCardFilmesBg) ?: return
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_series"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val array = JSONArray(body)
                    recentSeries.clear()

                    for (i in 0 until Math.min(array.length(), 20)) {
                        val obj = array.getJSONObject(i)
                        val icon = obj.optString("cover", "")
                        if (icon.isNotEmpty()) {
                            recentSeries.add(Stream(obj.getString("series_id"), obj.getString("name"), icon, "series", "mp4"))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        startSeriesCardSlideshow()
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun startSeriesCardSlideshow() {
        if (recentSeries.isEmpty()) return
        val ivCardSeriesBg = findViewById<ImageView>(R.id.ivCardSeriesBg) ?: return

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
                }
                kotlinx.coroutines.delay(6500)
                index = (index + 1) % recentSeries.size
            }
        }
    }

    // SLIDESHOW DE LOGOS DE CANAIS DENTRO DO CARD DE TV EM DIRETO
    private fun fetchTvLogos(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_live_streams"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val array = JSONArray(body)
                    recentTv.clear()

                    for (i in 0 until Math.min(array.length(), 25)) {
                        val obj = array.getJSONObject(i)
                        val icon = obj.optString("stream_icon", "")
                        if (icon.isNotEmpty()) {
                            recentTv.add(Stream(obj.getString("stream_id"), obj.getString("name"), icon, "live", "ts"))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        startTvCardSlideshow()
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun startTvCardSlideshow() {
        if (recentTv.isEmpty()) return
        val ivCardTvBg = findViewById<ImageView>(R.id.ivCardTvBg) ?: return

        tvCardJob?.cancel()
        tvCardJob = CoroutineScope(Dispatchers.Main).launch {
            var index = 0
            while (isActive) {
                val tv = recentTv[index]
                if (tv.stream_icon.isNotEmpty()) {
                    Glide.with(this@MainActivity)
                        .load(tv.stream_icon)
                        .transition(DrawableTransitionOptions.withCrossFade(1000))
                        .into(ivCardTvBg)
                }
                kotlinx.coroutines.delay(7000)
                index = (index + 1) % recentTv.size
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clockJob?.cancel()
        moviesCardJob?.cancel()
        seriesCardJob?.cancel()
        tvCardJob?.cancel()
    }
}
