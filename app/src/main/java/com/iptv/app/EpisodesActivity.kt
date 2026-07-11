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
import java.net.HttpURLConnection
import java.net.URL

import okhttp3.Request

class EpisodesActivity : AppCompatActivity() {

    private lateinit var rvEpisodes: RecyclerView
    private lateinit var tvSeriesTitle: TextView
    private val episodesList = mutableListOf<Episode>()
    private var username = ""
    private var password = ""
    private var seriesId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episodes)

        rvEpisodes = findViewById(R.id.rvEpisodes)
        tvSeriesTitle = findViewById(R.id.tvSeriesTitle)
        
        // Premium grid for episodes
        rvEpisodes.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 6)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""
        seriesId = intent.getStringExtra("SERIES_ID") ?: ""
        tvSeriesTitle.text = intent.getStringExtra("SERIES_NAME") ?: "EPISÓDIOS"

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
                    
                    if (jsonObject.has("episodes")) {
                        val episodesObj = jsonObject.getJSONObject("episodes")
                        val seasonsKeys = episodesObj.keys()
                        
                        while(seasonsKeys.hasNext()) {
                            val seasonNum = seasonsKeys.next()
                            val episodesArray = episodesObj.getJSONArray(seasonNum)
                            
                            for (i in 0 until episodesArray.length()) {
                                val epObj = episodesArray.getJSONObject(i)
                                val id = epObj.getString("id")
                                val epNum = epObj.optInt("episode_num", 0)
                                val title = epObj.optString("title", "Episódio $epNum")
                                val ext = epObj.optString("container_extension", "mp4")
                                
                                episodesList.add(Episode(id, epNum, "T$seasonNum - $title", ext))
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        rvEpisodes.adapter = EpisodeAdapter(episodesList) { episode ->
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
}
