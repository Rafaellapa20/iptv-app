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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONObject

data class EpisodeInfo(val id: String, val num: String, val title: String, val ext: String)

class SeriesInfoActivity : AppCompatActivity() {
    private var username = ""
    private var password = ""
    private var seriesId = ""
    private var seriesTitle = ""
    private var coverUrl = ""

    private lateinit var tvTitle: TextView
    private lateinit var tvPlot: TextView
    private lateinit var ivBackground: ImageView
    private lateinit var pbLoading: ProgressBar
    private lateinit var rvEpisodes: RecyclerView

    private val epList = mutableListOf<EpisodeInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_info)

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        username = intent.getStringExtra("USERNAME") ?: prefs.getString("USERNAME", "") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: prefs.getString("PASSWORD", "") ?: ""
        seriesId = intent.getStringExtra("SERIES_ID") ?: ""
        seriesTitle = intent.getStringExtra("SERIES_NAME") ?: ""
        coverUrl = intent.getStringExtra("SERIES_COVER") ?: ""

        tvTitle = findViewById(R.id.tvTitle)
        tvPlot = findViewById(R.id.tvPlot)
        ivBackground = findViewById(R.id.ivBackground)
        pbLoading = findViewById(R.id.pbLoading)
        rvEpisodes = findViewById(R.id.rvEpisodes)
        
        rvEpisodes.layoutManager = LinearLayoutManager(this)

        tvTitle.text = seriesTitle
        if (coverUrl.isNotEmpty()) {
            Glide.with(this).load(coverUrl).into(ivBackground)
        }

        fetchSeriesInfo()
    }

    private fun fetchSeriesInfo() {
        pbLoading.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "/player_api.php?username=\&password=\&action=get_series_info&series_id=\"
                val response = OkHttpProvider.client.newCall(Request.Builder().url(url).build()).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    
                    val info = json.optJSONObject("info")
                    val plot = info?.optString("plot", "Sinopse não disponível.") ?: ""
                    
                    epList.clear()
                    val episodesMap = json.optJSONObject("episodes")
                    if (episodesMap != null) {
                        val seasons = episodesMap.keys()
                        for (s in seasons) {
                            val eps = episodesMap.optJSONArray(s)
                            if (eps != null) {
                                for (i in 0 until eps.length()) {
                                    val epObj = eps.getJSONObject(i)
                                    val id = epObj.optString("id")
                                    val num = epObj.optString("episode_num", (i+1).toString())
                                    val title = epObj.optString("title", "Episódio \")
                                    val ext = epObj.optString("container_extension", "mp4")
                                    epList.add(EpisodeInfo(id, "S\E\", title, ext))
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        pbLoading.visibility = View.GONE
                        tvPlot.text = plot
                        rvEpisodes.adapter = EpAdapter(epList)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { pbLoading.visibility = View.GONE }
            }
        }
    }

    inner class EpAdapter(private val list: List<EpisodeInfo>) : RecyclerView.Adapter<EpAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val num: TextView = v.findViewById(R.id.tvEpNum)
            val title: TextView = v.findViewById(R.id.tvEpTitle)
            init {
                v.setOnClickListener {
                    val ep = list[adapterPosition]
                    val url = "/series/\/\/\.\"
                    val intent = Intent(this@SeriesInfoActivity, PlayerActivity::class.java)
                    intent.putExtra("VIDEO_URL", url)
                    intent.putExtra("TYPE", "series")
                    intent.putExtra("STREAM_ID", ep.id)
                    intent.putExtra("USERNAME", username)
                    intent.putExtra("PASSWORD", password)
                    intent.putExtra("TITLE", "\ - \")
                    intent.putExtra("COVER", coverUrl)
                    
                    // Add full episode URLs array to allow "Next Episode" functionality
                    val urls = arrayListOf<String>()
                    val ids = arrayListOf<String>()
                    val names = arrayListOf<String>()
                    for (e in list) {
                        urls.add("/series/\/\/\.\")
                        ids.add(e.id)
                        names.add("\ - \")
                    }
                    intent.putStringArrayListExtra("EPISODE_URLS", urls)
                    intent.putStringArrayListExtra("EPISODE_IDS", ids)
                    intent.putStringArrayListExtra("EPISODE_NAMES", names)
                    intent.putExtra("CURRENT_INDEX", adapterPosition)
                    
                    startActivity(intent)
                }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false))
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.num.text = list[position].num
            holder.title.text = list[position].title
        }
        override fun getItemCount() = list.size
    }
}
