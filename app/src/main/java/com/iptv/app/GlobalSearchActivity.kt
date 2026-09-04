package com.iptv.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray

class GlobalSearchActivity : AppCompatActivity() {

    private lateinit var etSearchInput: EditText
    private lateinit var pbSearchLoading: ProgressBar
    private lateinit var tvLoadingText: TextView
    private lateinit var tvNoResults: TextView
    private lateinit var rvSearchResults: RecyclerView

    private val allStreams = mutableListOf<Stream>()
    private val filteredStreams = mutableListOf<Stream>()

    private var username = ""
    private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_search)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""

        etSearchInput = findViewById(R.id.etSearchInput)
        pbSearchLoading = findViewById(R.id.pbSearchLoading)
        tvLoadingText = findViewById(R.id.tvLoadingText)
        tvNoResults = findViewById(R.id.tvNoResults)
        rvSearchResults = findViewById(R.id.rvSearchResults)

        rvSearchResults.layoutManager = GridLayoutManager(this, 5)
        rvSearchResults.adapter = SearchAdapter(filteredStreams)

        etSearchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterStreams(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadAllData()
    }

    private fun loadAllData() {
        pbSearchLoading.visibility = View.VISIBLE
        tvLoadingText.visibility = View.VISIBLE
        etSearchInput.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch Live
                val liveUrl = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_live_streams"
                val liveReq = Request.Builder().url(liveUrl).build()
                val liveResp = OkHttpProvider.client.newCall(liveReq).execute()
                if (liveResp.isSuccessful) {
                    val arr = JSONArray(liveResp.body?.string() ?: "[]")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("stream_id", "")
                        val name = obj.optString("name", "")
                        val icon = obj.optString("stream_icon", "")
                        if (id.isNotEmpty()) {
                            allStreams.add(Stream(id, name, icon, "live", ""))
                        }
                    }
                }

                // Fetch VOD
                val vodUrl = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_vod_streams"
                val vodReq = Request.Builder().url(vodUrl).build()
                val vodResp = OkHttpProvider.client.newCall(vodReq).execute()
                if (vodResp.isSuccessful) {
                    val arr = JSONArray(vodResp.body?.string() ?: "[]")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("stream_id", "")
                        val name = obj.optString("name", "")
                        val icon = obj.optString("stream_icon", "")
                        val ext = obj.optString("container_extension", "mp4")
                        if (id.isNotEmpty()) {
                            allStreams.add(Stream(id, name, icon, "vod", ext))
                        }
                    }
                }

                // Fetch Series
                val seriesUrl = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_series"
                val seriesReq = Request.Builder().url(seriesUrl).build()
                val seriesResp = OkHttpProvider.client.newCall(seriesReq).execute()
                if (seriesResp.isSuccessful) {
                    val arr = JSONArray(seriesResp.body?.string() ?: "[]")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.optString("series_id", "")
                        val name = obj.optString("name", "")
                        val icon = obj.optString("cover", "")
                        if (id.isNotEmpty()) {
                            allStreams.add(Stream(id, name, icon, "series", ""))
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    pbSearchLoading.visibility = View.GONE
                    tvLoadingText.visibility = View.GONE
                    etSearchInput.isEnabled = true
                    etSearchInput.requestFocus()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbSearchLoading.visibility = View.GONE
                    tvLoadingText.text = "Erro ao sincronizar. Tente novamente."
                }
            }
        }
    }

    private fun filterStreams(query: String) {
        filteredStreams.clear()
        if (query.length >= 2) {
            val q = query.lowercase()
            for (s in allStreams) {
                if (s.name.lowercase().contains(q)) {
                    filteredStreams.add(s)
                }
            }
        }
        
        rvSearchResults.adapter?.notifyDataSetChanged()
        
        if (query.length >= 2) {
            rvSearchResults.visibility = if (filteredStreams.isEmpty()) View.GONE else View.VISIBLE
            tvNoResults.visibility = if (filteredStreams.isEmpty()) View.VISIBLE else View.GONE
        } else {
            rvSearchResults.visibility = View.GONE
            tvNoResults.visibility = View.GONE
        }
    }

    inner class SearchAdapter(private val list: List<Stream>) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivMoviePoster)
            val tvName: TextView = view.findViewById(R.id.tvMovieTitleFallback)
            
            init {
                view.setOnClickListener {
                    val s = list[bindingAdapterPosition]
                    when (s.stream_type) {
                        "live" -> {
                            val intent = Intent(this@GlobalSearchActivity, PlayerActivity::class.java)
                            intent.putExtra("VIDEO_URL", "${Constants.SERVER_URL}/live/$username/$password/${s.stream_id}.ts")
                            intent.putExtra("TITLE", s.name)
                            intent.putExtra("STREAM_ID", s.stream_id)
                            intent.putExtra("TYPE", "live")
                            intent.putExtra("USERNAME", username)
                            intent.putExtra("PASSWORD", password)
                            startActivity(intent)
                        }
                        "vod" -> {
                            val intent = Intent(this@GlobalSearchActivity, MovieInfoActivity::class.java)
                            intent.putExtra("VIDEO_URL", "${Constants.SERVER_URL}/movie/$username/$password/${s.stream_id}.${s.extension}")
                            intent.putExtra("TITLE", s.name)
                            intent.putExtra("STREAM_ID", s.stream_id)
                            intent.putExtra("TYPE", "vod")
                            intent.putExtra("USERNAME", username)
                            intent.putExtra("PASSWORD", password)
                            intent.putExtra("COVER", s.stream_icon)
                            startActivity(intent)
                        }
                        "series" -> {
                            val intent = Intent(this@GlobalSearchActivity, EpisodesActivity::class.java)
                            intent.putExtra("SERIES_ID", s.stream_id)
                            intent.putExtra("SERIES_NAME", s.name)
                            intent.putExtra("SERIES_COVER", s.stream_icon)
                            intent.putExtra("USERNAME", username)
                            intent.putExtra("PASSWORD", password)
                            startActivity(intent)
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_grid, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val s = list[position]
            holder.tvName.text = s.name
            if (s.stream_icon.isNotEmpty()) {
                Glide.with(holder.ivIcon.context).load(s.stream_icon).into(holder.ivIcon)
            } else {
                holder.ivIcon.setImageDrawable(null)
            }
            // Add a badge indicating if it's Live, VOD or Series would be nice, 
            // but we're reusing item_movie_grid which might not have a badge.
        }

        override fun getItemCount() = list.size
    }
}
