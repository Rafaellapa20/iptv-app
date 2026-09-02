package com.iptv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var rvHomeFavorites: RecyclerView
    private lateinit var tvFavTitle: TextView
    private var clockJob: kotlinx.coroutines.Job? = null
    private var backgroundJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Verificar atualizações OTA via GitHub silenciosamente
        UpdateManager.checkForUpdates(this)

        rvHomeFavorites = findViewById(R.id.rvHomeFavorites)
        tvFavTitle = findViewById(R.id.tvFavTitle)

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

        loadHomeFavorites(username, password)
        fetchRecentMovies(username, password)
    }

    private fun openCategories(user: String, pass: String, type: String) {
        val intent = Intent(this, VodNetflixActivity::class.java)
        intent.putExtra("USERNAME", user)
        intent.putExtra("PASSWORD", pass)
        intent.putExtra("TYPE", type)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun loadHomeFavorites(user: String, pass: String) {
        val favs = FavoritesManager.getFavorites(this).take(10)
        if (favs.isNotEmpty()) {
            tvFavTitle.visibility = View.VISIBLE
            rvHomeFavorites.visibility = View.VISIBLE
            rvHomeFavorites.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            
            rvHomeFavorites.adapter = object : RecyclerView.Adapter<FavViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavViewHolder {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
                    return FavViewHolder(view)
                }

                override fun onBindViewHolder(holder: FavViewHolder, position: Int) {
                    val stream = favs[position]
                    holder.name.text = stream.name
                    if (stream.stream_icon.isNotEmpty()) {
                        com.bumptech.glide.Glide.with(this@MainActivity).load(stream.stream_icon).into(holder.icon)
                    }
                    holder.itemView.setOnClickListener {
                        val intent = Intent(this@MainActivity, PlayerActivity::class.java)
                        val ext = if (stream.stream_type == "movie") ".${stream.extension}" else ".ts"
                        val folder = if (stream.stream_type == "movie") "movie" else "live"
                        val url = "${Constants.SERVER_URL}/$folder/$user/$pass/${stream.stream_id}$ext"
                        
                        intent.putExtra("VIDEO_URL", url)
                        intent.putExtra("STREAM_ID", stream.stream_id)
                        intent.putExtra("TYPE", stream.stream_type)
                        intent.putExtra("USERNAME", user)
                        intent.putExtra("PASSWORD", pass)
                        startActivity(intent)
                    }
                }

                override fun getItemCount() = favs.size
            }
        } else {
            tvFavTitle.visibility = View.GONE
            rvHomeFavorites.visibility = View.GONE
        }
    }

    class FavViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val icon: ImageView = view.findViewById(R.id.ivIcon)
    }

    private fun fetchRecentMovies(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_vod_streams"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(responseBody)
                    
                    val moviesData = mutableListOf<Pair<Stream, Long>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val addedStr = obj.optString("added", "0")
                        val added = addedStr.toLongOrNull() ?: 0L
                        val streamId = obj.getString("stream_id")
                        val name = obj.getString("name")
                        val streamIcon = obj.optString("stream_icon", "")
                        val streamType = obj.optString("stream_type", "movie")
                        val extension = obj.optString("container_extension", "mp4")
                        
                        if (streamIcon.isNotEmpty()) {
                            moviesData.add(Stream(streamId, name, streamIcon, streamType, extension) to added)
                        }
                    }
                    
                    moviesData.sortByDescending { it.second }
                    recentMovies.clear()
                    recentMovies.addAll(moviesData.map { it.first }.take(15))

                    withContext(Dispatchers.Main) {
                        startBackgroundSlideshow()
                    }
                }
            } catch (e: Exception) {}
        }
    }

    private fun startBackgroundSlideshow() {
        if (recentMovies.isEmpty()) return
        val ivMainBackgroundBlur = findViewById<ImageView>(R.id.ivMainBackgroundBlur) ?: return

        backgroundJob?.cancel()
        backgroundJob = CoroutineScope(Dispatchers.Main).launch {
            var currentIndex = 0
            while (isActive) {
                val movie = recentMovies[currentIndex]
                if (movie.stream_icon.isNotEmpty()) {
                    com.bumptech.glide.Glide.with(this@MainActivity)
                        .load(movie.stream_icon)
                        .apply(com.bumptech.glide.request.RequestOptions.bitmapTransform(jp.wasabeef.glide.transformations.BlurTransformation(25, 3)))
                        .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(1200))
                        .into(ivMainBackgroundBlur)
                }
                kotlinx.coroutines.delay(8000)
                currentIndex = (currentIndex + 1) % recentMovies.size
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clockJob?.cancel()
        backgroundJob?.cancel()
    }
}
