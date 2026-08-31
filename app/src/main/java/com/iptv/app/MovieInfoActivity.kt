package com.iptv.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONObject

class MovieInfoActivity : AppCompatActivity() {

    private var username = ""
    private var password = ""
    private var streamId = ""
    private var streamUrl = ""
    private var movieTitle = ""
    private var coverUrl = ""

    private lateinit var pbLoading: ProgressBar
    private lateinit var tvTitle: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvPlot: TextView
    private lateinit var tvCast: TextView
    private lateinit var ivPoster: ImageView
    private lateinit var ivBackground: ImageView
    private lateinit var btnPlay: Button
    private lateinit var btnFavorite: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_info)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""
        streamId = intent.getStringExtra("STREAM_ID") ?: ""
        movieTitle = intent.getStringExtra("TITLE") ?: ""
        streamUrl = intent.getStringExtra("VIDEO_URL") ?: ""
        coverUrl = intent.getStringExtra("COVER") ?: ""

        pbLoading = findViewById(R.id.pbLoading)
        tvTitle = findViewById(R.id.tvTitle)
        tvRating = findViewById(R.id.tvRating)
        tvDuration = findViewById(R.id.tvDuration)
        tvGenre = findViewById(R.id.tvGenre)
        tvPlot = findViewById(R.id.tvPlot)
        tvCast = findViewById(R.id.tvCast)
        ivPoster = findViewById(R.id.ivPoster)
        ivBackground = findViewById(R.id.ivBackground)
        btnPlay = findViewById(R.id.btnPlay)
        btnFavorite = findViewById(R.id.btnFavorite)

        // Carrega capa imediatamente para não ficar tela preta
        if (coverUrl.isNotEmpty()) {
            Glide.with(this).load(coverUrl).into(ivPoster)
            Glide.with(this).load(coverUrl).into(ivBackground)
        }
        tvTitle.text = movieTitle
        
        // Verifica estado inicial do favorito
        updateFavoriteButtonState()
        
        btnPlay.setOnClickListener {
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("VIDEO_URL", streamUrl)
            intent.putExtra("TYPE", "vod")
            intent.putExtra("STREAM_ID", streamId)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("TITLE", movieTitle)
            intent.putExtra("COVER", coverUrl)
            startActivity(intent)
        }
        
        btnFavorite.setOnClickListener {
            val isNowFav = FavoritesManager.toggleFavorite(this, streamId, movieTitle, coverUrl, "vod")
            updateFavoriteButtonState()
        }
        
        // Botão Play recebe o foco automático nas TVs
        btnPlay.requestFocus()

        fetchMovieInfo()
    }
    
    private fun updateFavoriteButtonState() {
        val isFav = FavoritesManager.isFavorite(this, streamId)
        if (isFav) {
            btnFavorite.setTextColor(android.graphics.Color.parseColor("#E50914"))
        } else {
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
                        val cast = info.optString("cast", "Elenco desconhecido")
                        val director = info.optString("director", "")
                        val rating = info.optString("rating", "N/A")
                        val duration = info.optString("duration", "")
                        val genre = info.optString("genre", "")
                        
                        // Atualiza a capa se a API trouxer uma com melhor qualidade (cover_big)
                        val bestCover = info.optString("cover_big", info.optString("movie_image", coverUrl))

                        withContext(Dispatchers.Main) {
                            pbLoading.visibility = View.GONE
                            
                            tvPlot.text = plot
                            tvCast.text = "Elenco: $cast" + if (director.isNotEmpty()) "\nRealizador: $director" else ""
                            tvRating.text = "⭐ $rating/10"
                            if (duration.isNotEmpty()) tvDuration.text = "⏱ $duration"
                            if (genre.isNotEmpty()) tvGenre.text = genre
                            
                            if (bestCover.isNotEmpty() && bestCover != coverUrl) {
                                Glide.with(this@MovieInfoActivity).load(bestCover).into(ivPoster)
                                Glide.with(this@MovieInfoActivity).load(bestCover).into(ivBackground)
                            }
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
}
