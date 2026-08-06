package com.iptv.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.bumptech.glide.Glide
import java.net.HttpURLConnection
import java.net.URL
import android.widget.ImageView

import okhttp3.Request

class EpisodesActivity : AppCompatActivity() {

    private lateinit var rvEpisodes: RecyclerView
    private lateinit var rvSeasons: RecyclerView
    private lateinit var tvSeriesTitle: TextView
    private lateinit var btnFavorite: ImageView
    
    // Novas views de informação
    private lateinit var ivBackground: ImageView
    private lateinit var ivPoster: ImageView
    private lateinit var tvRating: TextView
    private lateinit var tvReleaseDate: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvPlot: TextView
    private lateinit var tvDirector: TextView
    private lateinit var tvCast: TextView

    private val episodesList = mutableListOf<Episode>()
    private var username = ""
    private var password = ""
    private var seriesId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episodes)

        rvEpisodes = findViewById(R.id.rvEpisodes)
        rvSeasons = findViewById(R.id.rvSeasons)
        tvSeriesTitle = findViewById(R.id.tvSeriesTitle)
        btnFavorite = findViewById(R.id.btnFavorite)
        
        ivBackground = findViewById(R.id.ivBackground)
        ivPoster = findViewById(R.id.ivPoster)
        tvRating = findViewById(R.id.tvRating)
        tvReleaseDate = findViewById(R.id.tvReleaseDate)
        tvGenre = findViewById(R.id.tvGenre)
        tvPlot = findViewById(R.id.tvPlot)
        tvDirector = findViewById(R.id.tvDirector)
        tvCast = findViewById(R.id.tvCast)
        
        // Premium grid for episodes
        rvEpisodes.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 6)
        
        // Horizontal list for seasons
        rvSeasons.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""
        seriesId = intent.getStringExtra("SERIES_ID") ?: ""
        
        val seriesName = intent.getStringExtra("SERIES_NAME") ?: "EPISÓDIOS"
        val seriesCover = intent.getStringExtra("SERIES_COVER") ?: ""
        tvSeriesTitle.text = seriesName

        // Update favorite star icon
        val isFav = FavoritesManager.isFavorite(this, seriesId)
        btnFavorite.setImageResource(if (isFav) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)

        btnFavorite.setOnClickListener {
            val nowFav = FavoritesManager.toggleFavorite(this, seriesId, seriesName, seriesCover, "series")
            btnFavorite.setImageResource(if (nowFav) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off)
            Toast.makeText(this, if (nowFav) "Adicionado aos Favoritos" else "Removido dos Favoritos", Toast.LENGTH_SHORT).show()
        }

        fetchEpisodes()
    }

    private fun fetchEpisodes() {
        Toast.makeText(this, "Carregando episódios...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "http://nelitoplay.top:80/player_api.php?username=$username&password=$password&action=get_series_info&series_id=$seriesId"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "{}"
                    val jsonObject = JSONObject(responseBody)
                    
                    var backdropPath = ""
                    var seriesName = intent.getStringExtra("SERIES_NAME") ?: "EPISÓDIOS"
                    var seriesCover = intent.getStringExtra("SERIES_COVER") ?: ""
                    var plot = ""
                    var cast = ""
                    var director = ""
                    var genre = ""
                    var rating = ""
                    var releaseDate = ""

                    if (jsonObject.has("info")) {
                        val info = jsonObject.getJSONObject("info")
                        seriesName = info.optString("name", seriesName)
                        seriesCover = info.optString("cover", seriesCover)
                        plot = info.optString("plot", "Sem sinopse disponível.")
                        cast = info.optString("cast", "Desconhecido")
                        director = info.optString("director", "Desconhecido")
                        genre = info.optString("genre", "")
                        rating = info.optString("rating", "")
                        releaseDate = info.optString("releaseDate", "")
                        
                        val backdropNode = info.opt("backdrop_path")
                        if (backdropNode is org.json.JSONArray && backdropNode.length() > 0) {
                            backdropPath = backdropNode.getString(0)
                        } else if (backdropNode is String && backdropNode.isNotEmpty()) {
                            backdropPath = backdropNode
                        }
                    }

                    val seasonsList = mutableListOf<String>()

                    if (jsonObject.has("episodes")) {
                        val episodesObj = jsonObject.getJSONObject("episodes")
                        val seasonsKeys = episodesObj.keys().asSequence().toList()
                        
                        val sortedSeasons = seasonsKeys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
                        
                        for (seasonNum in sortedSeasons) {
                            seasonsList.add(seasonNum)
                            val episodesArray = episodesObj.getJSONArray(seasonNum)
                            val seasonEpisodes = mutableListOf<Episode>()
                            
                            for (i in 0 until episodesArray.length()) {
                                val epObj = episodesArray.getJSONObject(i)
                                val id = epObj.getString("id")
                                val epNum = epObj.optInt("episode_num", 0)
                                val title = epObj.optString("title", "Episódio $epNum")
                                var ext = epObj.optString("container_extension", "mp4")
                                if (ext.isEmpty()) ext = "mp4"
                                
                                seasonEpisodes.add(Episode(id, epNum, "T$seasonNum - $title", ext, seasonNum))
                            }
                            
                            seasonEpisodes.sortBy { it.episode_num }
                            episodesList.addAll(seasonEpisodes)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        tvSeriesTitle.text = seriesName
                        tvPlot.text = plot
                        tvCast.text = "Atores: $cast"
                        tvDirector.text = "Realizador: $director"
                        tvGenre.text = genre
                        tvRating.text = if (rating.isNotEmpty()) "⭐ $rating" else ""
                        tvReleaseDate.text = releaseDate
                        
                        if (seriesCover.isNotEmpty()) {
                            Glide.with(this@EpisodesActivity).load(seriesCover).into(ivPoster)
                            if (backdropPath.isEmpty()) {
                                Glide.with(this@EpisodesActivity).load(seriesCover).into(ivBackground)
                            }
                        }
                        
                        if (backdropPath.isNotEmpty()) {
                            Glide.with(this@EpisodesActivity).load(backdropPath).into(ivBackground)
                        }

                        val updateEpisodesForSeason = { season: String ->
                            val filteredEpisodes = episodesList.filter { it.seasonNum == season }
                            rvEpisodes.adapter = EpisodeAdapter(filteredEpisodes) { episode ->
                                val intent = Intent(this@EpisodesActivity, PlayerActivity::class.java)
                                
                                val urls = ArrayList<String>()
                                for (ep in episodesList) {
                                    urls.add("http://nelitoplay.top:80/series/$username/$password/${ep.id}.${ep.container_extension}")
                                }
                                
                                val currentIndex = episodesList.indexOf(episode)
                                
                                intent.putExtra("VIDEO_URL", urls[currentIndex])
                                intent.putStringArrayListExtra("EPISODE_URLS", urls)
                                intent.putExtra("CURRENT_INDEX", currentIndex)
                                intent.putExtra("TITLE", episode.title)
                                intent.putExtra("COVER", intent.getStringExtra("SERIES_COVER") ?: "")
                                intent.putExtra("TYPE", "series")
                                intent.putExtra("STREAM_ID", seriesId)
                                intent.putExtra("USERNAME", username)
                                intent.putExtra("PASSWORD", password)
                                
                                startActivity(intent)
                            }
                        }

                        if (seasonsList.isNotEmpty()) {
                            rvSeasons.adapter = SeasonAdapter(seasonsList) { selectedSeason ->
                                updateEpisodesForSeason(selectedSeason)
                            }
                            // Select first season initially
                            updateEpisodesForSeason(seasonsList[0])
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EpisodesActivity, "Erro ao carregar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EpisodesActivity, "Erro de conexão", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class EpisodeAdapter(
        private val list: List<Episode>,
        private val onClick: (Episode) -> Unit
    ) : RecyclerView.Adapter<EpisodeAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            init {
                view.setOnClickListener { onClick(list[adapterPosition]) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvName.text = list[position].title
        }

        override fun getItemCount() = list.size
    }

    inner class SeasonAdapter(
        private val list: List<String>,
        private val onClick: (String) -> Unit
    ) : RecyclerView.Adapter<SeasonAdapter.ViewHolder>() {

        private var selectedPosition = 0

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvSeasonName: TextView = view.findViewById(R.id.tvSeasonName)
            init {
                view.setOnClickListener { 
                    val oldPos = selectedPosition
                    selectedPosition = adapterPosition
                    notifyItemChanged(oldPos)
                    notifyItemChanged(selectedPosition)
                    onClick(list[adapterPosition]) 
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_season, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvSeasonName.text = "Temporada ${list[position]}"
            holder.tvSeasonName.isSelected = position == selectedPosition
        }

        override fun getItemCount() = list.size
    }
}
