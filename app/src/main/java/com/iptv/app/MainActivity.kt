package com.iptv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val recentMovies = mutableListOf<Stream>()
    private val recentSeries = mutableListOf<Stream>()

    private var clockJob: kotlinx.coroutines.Job? = null
    private var moviesCardJob: kotlinx.coroutines.Job? = null
    private var seriesCardJob: kotlinx.coroutines.Job? = null

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
        val tvUserLogged = findViewById<TextView>(R.id.tvUserLogged)
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        
        if (vencimento == "null" || vencimento == "Indefinido") {
            tvVencimento.text = "Validade : Ilimitado"
        } else {
            tvVencimento.text = "Validade : $vencimento"
        }

        if (username.isNotEmpty()) {
            tvUserLogged.text = "Utilizador : $username"
            tvGreeting.text = "Olá, $username"
        }

        // Relógio estilo Pulse TV (20:30)
        val tvClock = findViewById<TextView>(R.id.tvClock)
        clockJob = CoroutineScope(Dispatchers.Main).launch {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            while (isActive) {
                tvClock.text = sdf.format(Date())
                kotlinx.coroutines.delay(1000)
            }
        }

        // MENU SUPERIOR DE NAVEGAÇÃO
        findViewById<View>(R.id.navTv)?.setOnClickListener { openLiveTv(username, password) }
        findViewById<View>(R.id.navMovies)?.setOnClickListener { openCategories(username, password, "vod") }
        findViewById<View>(R.id.navSeries)?.setOnClickListener { openCategories(username, password, "series") }
        findViewById<View>(R.id.navSettings)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        // CARTÕES NEON PRINCIPAIS
        findViewById<View>(R.id.cardTv).setOnClickListener { openLiveTv(username, password) }
        findViewById<View>(R.id.cardFilmes).setOnClickListener { openCategories(username, password, "vod") }
        findViewById<View>(R.id.cardSeries).setOnClickListener { openCategories(username, password, "series") }
        findViewById<View>(R.id.cardEpg).setOnClickListener {
            val intent = Intent(this, EpgGridActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
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

        // Configurar Linha "CONTINUAR A VER"
        setupContinueWatching()

        // Carregar capas de Filmes e Séries em rotação
        fetchMoviesPosters(username, password)
        fetchSeriesPosters(username, password)
    }

    private fun openLiveTv(user: String, pass: String) {
        val intent = Intent(this, LiveTvActivity::class.java)
        intent.putExtra("USERNAME", user)
        intent.putExtra("PASSWORD", pass)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun openCategories(user: String, pass: String, type: String) {
        val intent = Intent(this, VodNetflixActivity::class.java)
        intent.putExtra("USERNAME", user)
        intent.putExtra("PASSWORD", pass)
        intent.putExtra("TYPE", type)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    // LINHA INFERIOR "CONTINUAR A VER"
    private fun setupContinueWatching() {
        val rv = findViewById<RecyclerView>(R.id.rvContinueWatching) ?: return
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val recents = RecentManager.getRecent(this)
        rv.adapter = ContinueWatchingAdapter(recents)
    }

    // SLIDESHOW DE CAPAS DE FILMES DENTRO DO CARD DE FILMES
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
                val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                intent.putExtra("VIDEO_URL", "${Constants.SERVER_URL}/live/${intent.getStringExtra("USERNAME")}/${intent.getStringExtra("PASSWORD")}/${item.stream_id}.ts")
                intent.putExtra("STREAM_ID", item.stream_id)
                intent.putExtra("TITLE", item.name)
                startActivity(intent)
            }
        }

        override fun getItemCount() = list.size
    }

    override fun onDestroy() {
        super.onDestroy()
        clockJob?.cancel()
        moviesCardJob?.cancel()
        seriesCardJob?.cancel()
    }
}
