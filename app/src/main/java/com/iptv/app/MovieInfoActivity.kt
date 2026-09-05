package com.iptv.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONObject

data class ActorMember(val name: String, val photoUrl: String = "")

class MovieInfoActivity : AppCompatActivity() {

    private var username = ""
    private var password = ""
    private var streamId = ""
    private var streamUrl = ""
    private var movieTitle = ""
    private var coverUrl = ""
    private var youtubeTrailerId = ""

    private lateinit var pbLoading: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvPlot: TextView
    private lateinit var ivPoster: ImageView
    private lateinit var ivBackground: ImageView
    private lateinit var btnPlay: Button
    private lateinit var btnTrailer: Button
    private lateinit var btnBackup: Button
    private lateinit var btnFavorite: Button
    private lateinit var rvCast: RecyclerView

    private val castList = mutableListOf<ActorMember>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_info)

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        username = intent.getStringExtra("USERNAME") ?: prefs.getString("USERNAME", "") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: prefs.getString("PASSWORD", "") ?: ""
        streamId = intent.getStringExtra("STREAM_ID") ?: ""
        movieTitle = intent.getStringExtra("TITLE") ?: ""
        streamUrl = intent.getStringExtra("VIDEO_URL") ?: ""
        coverUrl = intent.getStringExtra("COVER") ?: ""

        pbLoading = findViewById(R.id.pbLoading)
        tvTitle = findViewById(R.id.tvTitle)
        tvRating = findViewById(R.id.tvRating)
        tvDuration = findViewById(R.id.tvYear)
        tvGenre = findViewById(R.id.tvGenre)
        tvPlot = findViewById(R.id.tvPlot)
        ivPoster = findViewById(R.id.ivPoster)
        ivBackground = findViewById(R.id.ivBackground)
        btnPlay = findViewById(R.id.btnPlay)
        btnTrailer = findViewById(R.id.btnTrailer)
        btnBackup = findViewById(R.id.btnBackup)
        btnFavorite = findViewById(R.id.btnFavorite)
        rvCast = findViewById(R.id.rvCast)

        rvCast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        if (coverUrl.isNotEmpty()) {
            Glide.with(this).load(coverUrl).into(ivPoster)
            Glide.with(this).load(coverUrl).into(ivBackground)
        }
        tvTitle.text = movieTitle
        
        updateFavoriteButtonState()

        btnPlay.setOnClickListener {
            playMovie(streamUrl)
        }

        btnBackup.setOnClickListener {
            playMovie(streamUrl)
        }

        btnTrailer.setOnClickListener {
            if (youtubeTrailerId.isNotEmpty()) {
                showTrailerDialog(youtubeTrailerId)
            } else {
                Toast.makeText(this, "Trailer não disponível para este título.", Toast.LENGTH_SHORT).show()
            }
        }

        btnFavorite.setOnClickListener {
            val isNowFav = FavoritesManager.toggleFavorite(this, streamId, movieTitle, coverUrl, "vod")
            updateFavoriteButtonState()
        }

        btnPlay.requestFocus()
        fetchMovieInfo()
    }

    // Reproduz o trailer num WebView embutido (iframe da API de embed do
    // YouTube), em vez de um Intent.ACTION_VIEW que abria a app do YouTube e
    // tirava o utilizador da nossa app. android:usesCleartextTraffic não é
    // necessário aqui porque o embed do YouTube corre em https.
    @SuppressLint("SetJavaScriptEnabled")
    private fun showTrailerDialog(videoId: String) {
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_trailer)

        val webView = dialog.findViewById<android.webkit.WebView>(R.id.webViewTrailer)
        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.webChromeClient = android.webkit.WebChromeClient()
        webView.loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1")

        dialog.findViewById<View>(R.id.btnCloseTrailer).setOnClickListener {
            webView.loadUrl("about:blank")
            dialog.dismiss()
        }
        dialog.setOnDismissListener {
            webView.loadUrl("about:blank")
        }
        dialog.show()
    }

    private fun playMovie(url: String) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra("VIDEO_URL", url)
        intent.putExtra("TYPE", "vod")
        intent.putExtra("STREAM_ID", streamId)
        intent.putExtra("USERNAME", username)
        intent.putExtra("PASSWORD", password)
        intent.putExtra("TITLE", movieTitle)
        intent.putExtra("COVER", coverUrl)
        startActivity(intent)
    }

    private fun updateFavoriteButtonState() {
        val isFav = FavoritesManager.isFavorite(this, streamId)
        if (isFav) {
            btnFavorite.text = "⭐ Removido dos Favoritos"
            btnFavorite.setTextColor(android.graphics.Color.parseColor("#FFCC00"))
        } else {
            btnFavorite.text = "⭐ Adicionar aos Favoritos"
            btnFavorite.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
        }
    }

    private fun fetchMovieInfo() {
        pbLoading.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_vod_info&vod_id=$streamId"
                val response = OkHttpProvider.client.newCall(Request.Builder().url(url).build()).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    
                    val info = json.optJSONObject("info")
                    if (info != null) {
                        val plot = info.optString("plot", "Sinopse não disponível.")
                        val castStr = info.optString("cast", "")
                        val rating = info.optString("rating", "8.2")
                        val duration = info.optString("duration", "")
                        val genre = info.optString("genre", "Sci-Fi/Action")
                        val releasedate = info.optString("releasedate", info.optString("year", "2024"))
                        youtubeTrailerId = info.optString("youtube_trailer", "")

                        val bestCover = info.optString("cover_big", info.optString("movie_image", coverUrl))

                        castList.clear()
                        if (castStr.isNotEmpty()) {
                            val names = castStr.split(",")
                            for (n in names) {
                                val clean = n.trim()
                                if (clean.isNotEmpty()) {
                                    castList.add(ActorMember(clean))
                                }
                            }
                        }

                        // Atores por defeito se a lista vier vazia
                        if (castList.isEmpty()) {
                            castList.add(ActorMember("Cillian Murphy"))
                            castList.add(ActorMember("Emily Blunt"))
                            castList.add(ActorMember("Matt Damon"))
                            castList.add(ActorMember("Robert Downey Jr."))
                            castList.add(ActorMember("Florence Pugh"))
                        }

                        withContext(Dispatchers.Main) {
                            pbLoading.visibility = View.GONE
                            
                            tvPlot.text = plot
                            tvRating.text = "IMDb $rating ★"
                            if (duration.isNotEmpty()) tvDuration.text = "$releasedate · $duration" else tvDuration.text = releasedate
                            if (genre.isNotEmpty()) tvGenre.text = genre
                            
                            if (bestCover.isNotEmpty() && bestCover != coverUrl) {
                                Glide.with(this@MovieInfoActivity).load(bestCover).into(ivPoster)
                                Glide.with(this@MovieInfoActivity).load(bestCover).into(ivBackground)
                            }

                            rvCast.adapter = CastAdapter(castList)
                        }
                    } else {
                        withContext(Dispatchers.Main) { pbLoading.visibility = View.GONE }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { pbLoading.visibility = View.GONE }
            }
        }
    }

    inner class CastAdapter(private val list: List<ActorMember>) : RecyclerView.Adapter<CastAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val photo: ImageView = v.findViewById(R.id.ivActorPhoto)
            val name: TextView = v.findViewById(R.id.tvActorName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cast_member, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val actor = list[position]
            holder.name.text = actor.name
            holder.photo.setImageResource(android.R.drawable.ic_menu_myplaces)
        }

        override fun getItemCount() = list.size
    }
}
