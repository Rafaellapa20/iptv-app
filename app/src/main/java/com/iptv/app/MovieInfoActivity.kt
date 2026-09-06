package com.iptv.app

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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONObject

data class ActorMember(val name: String, var photoUrl: String = "")

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
    private lateinit var tvPlotToggle: TextView
    private lateinit var ivPoster: ImageView
    private lateinit var ivBackground: ImageView
    private lateinit var btnPlay: Button
    private lateinit var btnBackup: Button
    private lateinit var btnFavorite: Button
    private lateinit var rvCast: RecyclerView

    private val castList = mutableListOf<ActorMember>()
    private var plotExpanded = false

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
        tvPlotToggle = findViewById(R.id.tvPlotToggle)
        ivPoster = findViewById(R.id.ivPoster)
        ivBackground = findViewById(R.id.ivBackground)
        btnPlay = findViewById(R.id.btnPlay)
        btnBackup = findViewById(R.id.btnBackup)
        btnFavorite = findViewById(R.id.btnFavorite)
        rvCast = findViewById(R.id.rvCast)

        rvCast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        if (coverUrl.isNotEmpty()) {
            Glide.with(this).load(coverUrl).into(ivPoster)
            Glide.with(this).load(coverUrl).into(ivBackground)
        }
        tvTitle.text = movieTitle

        // Expandir / recolher sinopse ao clicar
        val togglePlot = View.OnClickListener {
            plotExpanded = !plotExpanded
            if (plotExpanded) {
                tvPlot.maxLines = Int.MAX_VALUE
                tvPlot.ellipsize = null
                tvPlotToggle.text = "▲ Ver menos"
            } else {
                tvPlot.maxLines = 3
                tvPlot.ellipsize = android.text.TextUtils.TruncateAt.END
                tvPlotToggle.text = "▼ Ver mais"
            }
        }
        tvPlot.setOnClickListener(togglePlot)
        tvPlotToggle.setOnClickListener(togglePlot)

        updateFavoriteButtonState()

        btnPlay.setOnClickListener { playMovie(streamUrl) }
        btnBackup.setOnClickListener { playMovie(streamUrl) }

        btnFavorite.setOnClickListener {
            FavoritesManager.toggleFavorite(this, streamId, movieTitle, coverUrl, "vod")
            updateFavoriteButtonState()
        }

        btnPlay.requestFocus()
        fetchMovieInfo()
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
                        val plot = info.optString("plot", "").ifBlank { "Sinopse não disponível." }
                        val castStr = info.optString("cast", "")
                        val rating = info.optString("rating", "")
                        val duration = info.optString("duration", "")
                        val genre = info.optString("genre", "")
                        val releasedate = info.optString("releasedate", info.optString("year", ""))
                        val bestCover = info.optString("cover_big", info.optString("movie_image", coverUrl))

                        // Build cast list from names
                        castList.clear()
                        if (castStr.isNotEmpty()) {
                            castStr.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .take(10)
                                .forEach { castList.add(ActorMember(it)) }
                        }

                        // Fetch actor photos from Wikipedia concurrently
                        val photoJobs = castList.map { actor ->
                            async { actor.photoUrl = fetchWikipediaPhoto(actor.name) }
                        }
                        photoJobs.awaitAll()

                        withContext(Dispatchers.Main) {
                            pbLoading.visibility = View.GONE

                            tvPlot.text = plot
                            // Mostrar toggle só se o texto for longo o suficiente para ser cortado
                            tvPlot.post {
                                if (tvPlot.lineCount > 3) tvPlotToggle.visibility = View.VISIBLE
                            }

                            if (rating.isNotEmpty()) tvRating.text = "IMDb $rating ★"
                            val yearDur = listOf(releasedate, duration).filter { it.isNotEmpty() }.joinToString(" · ")
                            if (yearDur.isNotEmpty()) tvDuration.text = yearDur
                            if (genre.isNotEmpty()) tvGenre.text = genre

                            if (bestCover.isNotEmpty() && bestCover != coverUrl) {
                                Glide.with(this@MovieInfoActivity).load(bestCover).into(ivPoster)
                                Glide.with(this@MovieInfoActivity).load(bestCover).into(ivBackground)
                            }

                            if (castList.isNotEmpty()) {
                                rvCast.adapter = CastAdapter(castList)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) { pbLoading.visibility = View.GONE }
                    }
                } else {
                    withContext(Dispatchers.Main) { pbLoading.visibility = View.GONE }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { pbLoading.visibility = View.GONE }
            }
        }
    }

    /**
     * Busca a miniatura de um ator/atriz na Wikipedia REST API.
     * Retorna URL da foto ou string vazia se não encontrar.
     */
    private fun fetchWikipediaPhoto(actorName: String): String {
        return try {
            val encoded = actorName.trim().replace(" ", "_")
            val url = "https://en.wikipedia.org/api/rest_v1/page/summary/$encoded"
            val resp = OkHttpProvider.client.newCall(Request.Builder().url(url).build()).execute()
            if (resp.isSuccessful) {
                val j = JSONObject(resp.body?.string() ?: "")
                j.optJSONObject("thumbnail")?.optString("source") ?: ""
            } else ""
        } catch (e: Exception) { "" }
    }

    inner class CastAdapter(private val list: List<ActorMember>) :
        RecyclerView.Adapter<CastAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val photo: ImageView = v.findViewById(R.id.ivActorPhoto)
            val name: TextView = v.findViewById(R.id.tvActorName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cast_member, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val actor = list[position]
            holder.name.text = actor.name
            if (actor.photoUrl.isNotEmpty()) {
                Glide.with(holder.photo.context)
                    .load(actor.photoUrl)
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .into(holder.photo)
            } else {
                holder.photo.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
        }

        override fun getItemCount() = list.size
    }
}
