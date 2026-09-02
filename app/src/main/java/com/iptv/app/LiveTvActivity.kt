package com.iptv.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class LiveTvActivity : AppCompatActivity() {

    companion object {
        var lastPlayedStreamId: String? = null
        var lastPlayedStreamName: String? = null
    }

    private lateinit var rvCategories: RecyclerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var tvPreviewName: TextView
    private lateinit var tvEpgCurrent: TextView
    private lateinit var pbEpgProgress: ProgressBar
    private lateinit var miniPlayerView: PlayerView
    private lateinit var progressBar: ProgressBar

    private var miniPlayer: ExoPlayer? = null
    private val categories = mutableListOf<Category>()
    private val channels = mutableListOf<Stream>()
    private val categoryCounts = mutableMapOf<String, Int>()
    
    private var username = ""
    private var password = ""
    private var selectedCategoryId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tv)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""

        rvCategories = findViewById(R.id.rvCategories)
        rvChannels = findViewById(R.id.rvChannels)
        tvPreviewName = findViewById(R.id.tvPreviewName)
        tvEpgCurrent = findViewById(R.id.tvEpgCurrent)
        pbEpgProgress = findViewById(R.id.pbEpgProgress)
        miniPlayerView = findViewById(R.id.mini_player_view)
        progressBar = findViewById(R.id.progressBar)

        rvCategories.layoutManager = LinearLayoutManager(this)
        rvChannels.layoutManager = LinearLayoutManager(this)

        val prefs = getSharedPreferences("IPTV_PREFS", android.content.Context.MODE_PRIVATE)
        lastPlayedStreamId = prefs.getString("LAST_STREAM_ID", null)
        lastPlayedStreamName = prefs.getString("LAST_STREAM_NAME", null)
        selectedCategoryId = prefs.getString("LAST_CATEGORY_ID", "") ?: ""

        setupTopNavigation()

        initializeMiniPlayer()
        fetchCategories()
    }

    private fun setupTopNavigation() {
        val navHome = findViewById<TextView>(R.id.nav_home)
        val navFilmes = findViewById<TextView>(R.id.nav_filmes)
        val navSeries = findViewById<TextView>(R.id.nav_series)

        navHome.setOnClickListener {
            finish()
        }

        navFilmes.setOnClickListener {
            val intent = Intent(this, VodNetflixActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("TYPE", "vod")
            startActivity(intent)
            finish()
        }

        navSeries.setOnClickListener {
            val intent = Intent(this, VodNetflixActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("TYPE", "series")
            startActivity(intent)
            finish()
        }
    }

    private fun initializeMiniPlayer() {
        miniPlayer = PlayerManager.getPlayer(this)
        miniPlayerView.player = miniPlayer
        
        miniPlayer?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    miniPlayer?.seekToDefaultPosition()
                    miniPlayer?.prepare()
                } else {
                    miniPlayer?.prepare()
                }
                miniPlayer?.playWhenReady = true
            }
        })
    }

    private fun playMiniVideo(streamId: String) {
        if (PlayerManager.currentStreamId == streamId) {
            if (miniPlayer?.isPlaying == false) {
                miniPlayer?.playWhenReady = true
            }
            return // Já está carregado este canal, não recarregar do zero
        }
        PlayerManager.currentStreamId = streamId
        val streamUrl = "${Constants.SERVER_URL}/live/$username/$password/$streamId.ts"
        val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
        miniPlayer?.setMediaItem(mediaItem)
        miniPlayer?.prepare()
        miniPlayer?.playWhenReady = true
    }

    private fun fetchCategories() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Primeiro busca todos os canais para contar quantos existem em cada categoria
                val allUrl = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_live_streams"
                val allResponse = OkHttpProvider.client.newCall(Request.Builder().url(allUrl).build()).execute()
                if (allResponse.isSuccessful) {
                    val allArray = JSONArray(allResponse.body?.string() ?: "[]")
                    categoryCounts.clear()
                    categoryCounts["all"] = allArray.length()
                    for (i in 0 until allArray.length()) {
                        val catId = allArray.getJSONObject(i).getString("category_id")
                        categoryCounts[catId] = categoryCounts.getOrDefault(catId, 0) + 1
                    }
                }

                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_live_categories"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val array = JSONArray(body)
                    categories.clear()
                    
                    categories.add(Category("recent", "Visualizado recentemente", 0))
                    categories.add(Category("fav", "Favoritos", 0))
                    
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        categories.add(Category(obj.getString("category_id"), obj.getString("category_name"), 0))
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        val adapter = CategoryAdapter(categories) { category ->
                            selectedCategoryId = category.category_id
                            fetchChannels(category.category_id)
                        }
                        rvCategories.adapter = adapter
                        
                        var defaultCat = categories.find { it.category_id == selectedCategoryId }
                        if (defaultCat == null) {
                            defaultCat = categories.find { it.category_name.lowercase().contains("portugal") } ?: 
                                       if (categories.size > 2) categories[2] else categories[0]
                        }
                        
                        selectedCategoryId = defaultCat.category_id
                        fetchChannels(selectedCategoryId)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE }
            }
        }
    }

    private fun fetchChannels(catId: String) {
        if (catId == "recent") {
            channels.clear()
            channels.addAll(RecentManager.getRecent(this))
            updateChannelAdapter()
            return
        }
        if (catId == "fav") {
            channels.clear()
            channels.addAll(FavoritesManager.getFavorites(this).map { it.toStream() })
            updateChannelAdapter()
            return
        }

        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_live_streams&category_id=$catId"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val array = JSONArray(body)
                    channels.clear()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        channels.add(Stream(
                            obj.getString("stream_id"),
                            obj.getString("name"),
                            obj.optString("stream_icon", ""),
                            "live",
                            "ts"
                        ))
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        updateChannelAdapter()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE }
            }
        }
    }

    private fun updateChannelAdapter() {
        val adapter = ChannelAdapter(channels) { stream ->
            tvPreviewName.text = stream.name
            playMiniVideo(stream.stream_id)
            fetchShortEpg(stream.stream_id)
            RecentManager.addRecent(this, stream)
        }
        rvChannels.adapter = adapter

        val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
        val lastStreamId = prefs.getString("LAST_STREAM_ID", "") ?: ""
        if (lastStreamId.isNotEmpty()) {
            val idx = channels.indexOfFirst { it.stream_id == lastStreamId }
            if (idx != -1) {
                rvChannels.scrollToPosition(idx)
                val targetStream = channels[idx]
                tvPreviewName.text = targetStream.name
                playMiniVideo(targetStream.stream_id)
                fetchShortEpg(targetStream.stream_id)
            } else if (channels.isNotEmpty()) {
                val first = channels[0]
                tvPreviewName.text = first.name
                playMiniVideo(first.stream_id)
                fetchShortEpg(first.stream_id)
            }
        } else if (channels.isNotEmpty()) {
            val first = channels[0]
            tvPreviewName.text = first.name
            playMiniVideo(first.stream_id)
            fetchShortEpg(first.stream_id)
        }
    }

    private fun fetchShortEpg(streamId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_short_epg&stream_id=$streamId"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val epgList = json.optJSONArray("epg_listings")
                    
                    if (epgList != null && epgList.length() > 0) {
                        val current = epgList.getJSONObject(0)
                        val title = android.util.Base64.decode(current.getString("title"), android.util.Base64.DEFAULT).decodeToString()
                        val desc = current.optString("description", "")
                        val decodedDesc = if (desc.isNotEmpty()) android.util.Base64.decode(desc, android.util.Base64.DEFAULT).decodeToString() else "Sem descrição"
                        val startTimestamp = current.getLong("start_timestamp")
                        val stopTimestamp = current.getLong("stop_timestamp")
                        
                        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        val startTime = sdf.format(java.util.Date(startTimestamp * 1000))
                        val stopTime = sdf.format(java.util.Date(stopTimestamp * 1000))
                        
                        var nextText = ""
                        if (epgList.length() > 1) {
                            val nextProg = epgList.getJSONObject(1)
                            val nextTitle = android.util.Base64.decode(nextProg.getString("title"), android.util.Base64.DEFAULT).decodeToString()
                            val nextStart = sdf.format(java.util.Date(nextProg.getLong("start_timestamp") * 1000))
                            nextText = "\n\nA seguir ($nextStart):\n$nextTitle"
                        }
                        
                        val now = System.currentTimeMillis() / 1000

                        withContext(Dispatchers.Main) {
                            tvEpgCurrent.text = "$startTime - $stopTime\n$title\n$decodedDesc$nextText"
                            pbEpgProgress.visibility = View.VISIBLE
                            val total = stopTimestamp - startTimestamp
                            val progress = now - startTimestamp
                            if (total > 0) {
                                pbEpgProgress.max = total.toInt()
                                pbEpgProgress.progress = progress.toInt()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            tvEpgCurrent.text = "Sem informação de guia"
                            pbEpgProgress.visibility = View.GONE
                        }
                    }
                }
            } catch (e: Exception) { }
        }
    }

    inner class CategoryAdapter(private val list: List<Category>, private val onClick: (Category) -> Unit) :
        RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        
        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvCategoryName)
            val count: TextView = v.findViewById(R.id.tvCategoryCount)
            init { 
                v.setOnClickListener { 
                    val cat = list[adapterPosition]
                    onClick(cat)
                    val prefs = v.context.getSharedPreferences("IPTV_PREFS", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putString("LAST_CATEGORY_ID", cat.category_id).apply()
                    findViewById<RecyclerView>(R.id.rvChannels).requestFocus()
                } 
            }
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(
            LayoutInflater.from(p.context).inflate(R.layout.item_live_category, p, false)
        )

        override fun onBindViewHolder(h: ViewHolder, p: Int) { 
            val cat = list[p]
            h.name.text = cat.category_name
            val c = when(cat.category_id) {
                "recent" -> RecentManager.getRecent(h.itemView.context).size
                "fav" -> FavoritesManager.getFavorites(h.itemView.context).size
                else -> categoryCounts[cat.category_id] ?: 0
            }
            h.count.text = c.toString()
        }
        override fun getItemCount() = list.size
    }

    inner class ChannelAdapter(
        private val list: List<Stream>,
        private val onChannelSelected: (Stream) -> Unit
    ) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val num: TextView = view.findViewById(R.id.tvChannelNum)
            val icon: ImageView = view.findViewById(R.id.ivChannelIcon)
            val name: TextView = view.findViewById(R.id.tvChannelName)
            val favIcon: ImageView = view.findViewById(R.id.ivFavIcon)

            init {
                var isLongPressHandled = false

                view.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER || keyCode == android.view.KeyEvent.KEYCODE_PROG_YELLOW || keyCode == android.view.KeyEvent.KEYCODE_BUTTON_Y) {
                        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                            if (event.repeatCount >= 4 && !isLongPressHandled) {
                                isLongPressHandled = true
                                val pos = adapterPosition
                                if (pos != RecyclerView.NO_POSITION) {
                                    val s = list[pos]
                                    val isFav = FavoritesManager.toggleFavorite(this@LiveTvActivity, s)
                                    notifyItemChanged(pos)
                                    val statusText = if (isFav) "⭐ Adicionado aos Favoritos!" else "❌ Removido dos Favoritos"
                                    android.widget.Toast.makeText(this@LiveTvActivity, "${s.name}\n$statusText", android.widget.Toast.LENGTH_SHORT).show()
                                    if (selectedCategoryId == "fav") {
                                        fetchChannels("fav")
                                    }
                                }
                                return@setOnKeyListener true
                            }
                        } else if (event.action == android.view.KeyEvent.ACTION_UP) {
                            if (isLongPressHandled) {
                                return@setOnKeyListener true
                            }
                        }
                    }
                    false
                }

                view.setOnClickListener { v ->
                    if (isLongPressHandled) {
                        isLongPressHandled = false
                        return@setOnClickListener
                    }
                    val pos = adapterPosition
                    if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                    val s = list[pos]
                    onChannelSelected(s)
                    
                    lastPlayedStreamId = s.stream_id
                    lastPlayedStreamName = s.name
                    
                    val prefs = v.context.getSharedPreferences("IPTV_PREFS", android.content.Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("LAST_STREAM_ID", s.stream_id)
                        .putString("LAST_STREAM_NAME", s.name)
                        .apply()
                    
                    val intent = Intent(this@LiveTvActivity, PlayerActivity::class.java)
                    val streamUrl = "${Constants.SERVER_URL}/live/$username/$password/${s.stream_id}.ts"
                    
                    val urlsList = ArrayList<String>()
                    val idsList = ArrayList<String>()
                    val namesList = ArrayList<String>()
                    var currentIndex = -1
                    for ((index, item) in list.withIndex()) {
                        urlsList.add("${Constants.SERVER_URL}/live/$username/$password/${item.stream_id}.ts")
                        idsList.add(item.stream_id)
                        namesList.add(item.name)
                        if (item.stream_id == s.stream_id) currentIndex = index
                    }
                    
                    intent.putStringArrayListExtra("CHANNEL_URLS", urlsList)
                    intent.putStringArrayListExtra("CHANNEL_IDS", idsList)
                    intent.putStringArrayListExtra("CHANNEL_NAMES", namesList)
                    intent.putExtra("CURRENT_INDEX", currentIndex)
                    intent.putExtra("VIDEO_URL", streamUrl)
                    intent.putExtra("STREAM_ID", s.stream_id)
                    intent.putExtra("USERNAME", username)
                    intent.putExtra("PASSWORD", password)
                    intent.putExtra("TYPE", "live")
                    intent.putExtra("TITLE", s.name)
                    startActivity(intent)
                }

                favIcon.setOnClickListener {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val s = list[pos]
                        val isFav = FavoritesManager.toggleFavorite(this@LiveTvActivity, s)
                        notifyItemChanged(pos)
                        val statusText = if (isFav) "⭐ Adicionado aos Favoritos!" else "❌ Removido dos Favoritos"
                        android.widget.Toast.makeText(this@LiveTvActivity, "${s.name}\n$statusText", android.widget.Toast.LENGTH_SHORT).show()
                        if (selectedCategoryId == "fav") {
                            fetchChannels("fav")
                        }
                    }
                }

                view.setOnLongClickListener {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val s = list[pos]
                        val isFav = FavoritesManager.toggleFavorite(this@LiveTvActivity, s)
                        notifyItemChanged(pos)
                        val statusText = if (isFav) "⭐ Adicionado aos Favoritos!" else "❌ Removido dos Favoritos"
                        android.widget.Toast.makeText(this@LiveTvActivity, "${s.name}\n$statusText", android.widget.Toast.LENGTH_SHORT).show()
                        if (selectedCategoryId == "fav") {
                            fetchChannels("fav")
                        }
                    }
                    true
                }

                var zappingJob: kotlinx.coroutines.Job? = null
                view.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus && adapterPosition != RecyclerView.NO_POSITION) {
                        val s = list[adapterPosition]
                        tvPreviewName.text = s.name
                        fetchShortEpg(s.stream_id)

                        zappingJob?.cancel()
                        zappingJob = CoroutineScope(Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(250)
                            if (view.hasFocus()) {
                                playMiniVideo(s.stream_id)
                            }
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) = ViewHolder(
            LayoutInflater.from(p.context).inflate(R.layout.item_live_channel, p, false)
        )

        override fun onBindViewHolder(h: ViewHolder, p: Int) {
            val s = list[p]
            h.num.text = (p + 1).toString()
            h.name.text = s.name
            if (s.stream_icon.isNotEmpty()) {
                Glide.with(h.icon.context).load(s.stream_icon).into(h.icon)
            } else {
                h.icon.setImageResource(android.R.drawable.ic_media_play)
            }

            val isFav = FavoritesManager.isFavorite(h.itemView.context, s.stream_id)
            if (isFav) {
                h.favIcon.setImageResource(android.R.drawable.btn_star_big_on)
                h.favIcon.alpha = 1.0f
            } else {
                h.favIcon.setImageResource(android.R.drawable.btn_star_big_off)
                h.favIcon.alpha = 0.35f
            }
            h.favIcon.visibility = View.VISIBLE
        }
        override fun getItemCount() = list.size
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("IPTV_PREFS", android.content.Context.MODE_PRIVATE)
        lastPlayedStreamId = prefs.getString("LAST_STREAM_ID", lastPlayedStreamId)
        lastPlayedStreamName = prefs.getString("LAST_STREAM_NAME", lastPlayedStreamName)
        
        if (lastPlayedStreamId != null) {
            tvPreviewName.text = lastPlayedStreamName
            fetchShortEpg(lastPlayedStreamId!!)
            // Re-assign the player view in case it was detached by PlayerActivity
            miniPlayerView.player = miniPlayer
            playMiniVideo(lastPlayedStreamId!!)
        } else {
            miniPlayerView.player = miniPlayer
            miniPlayer?.playWhenReady = true
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        miniPlayer?.pause()
    }

    override fun onPause() {
        super.onPause()
        miniPlayer?.stop()
        miniPlayer?.clearMediaItems()
        miniPlayerView.player = null
        PlayerManager.currentStreamId = null
    }

    override fun onDestroy() {
        super.onDestroy()
        miniPlayerView.player = null
        miniPlayer = null
        // We do not release the shared player here, because it might be used by another Activity
    }
}
