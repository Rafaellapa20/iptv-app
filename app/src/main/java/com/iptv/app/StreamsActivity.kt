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
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation

import okhttp3.Request

class StreamsActivity : AppCompatActivity() {

    private lateinit var rvStreams: RecyclerView
    private lateinit var tvCategoryTitle: TextView
    private lateinit var etSearch: EditText
    private val streams = mutableListOf<Stream>()
    private var filteredStreams = mutableListOf<Stream>()
    private var username = ""
    private var password = ""
    private var categoryId = ""

    private var type = "live"
    private lateinit var adapter: StreamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streams)

        rvStreams = findViewById(R.id.rvStreams)
        tvCategoryTitle = findViewById(R.id.tvCategoryTitle)
        etSearch = findViewById(R.id.etSearch)
        
        // Premium Netflix-style grid
        rvStreams.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 6)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""
        categoryId = intent.getStringExtra("CATEGORY_ID") ?: ""
        type = intent.getStringExtra("TYPE") ?: "live"
        tvCategoryTitle.text = intent.getStringExtra("CATEGORY_NAME") ?: "CANAIS"

        adapter = StreamAdapter(filteredStreams) { stream ->
            if (type == "series") {
                val intent = Intent(this@StreamsActivity, EpisodesActivity::class.java)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("SERIES_ID", stream.stream_id)
                intent.putExtra("SERIES_NAME", stream.name)
                intent.putExtra("SERIES_COVER", stream.stream_icon)
                startActivity(intent)
            } else {
                val intent = Intent(this@StreamsActivity, PlayerActivity::class.java)
                val extension = if (type == "vod") ".${stream.extension}" else ".ts"
                val folder = if (type == "vod") "movie" else "live"
                val streamUrl = "http://nelitoplay.top:80/$folder/$username/$password/${stream.stream_id}$extension"
                
                if (type == "live" || type == "favorites") {
                    val urlsList = ArrayList<String>()
                    var currentIndex = -1
                    for ((index, s) in filteredStreams.withIndex()) {
                        val sExt = if (s.stream_type == "movie") ".${s.extension}" else ".ts"
                        val sFold = if (s.stream_type == "movie") "movie" else "live"
                        val sUrl = "http://nelitoplay.top:80/$sFold/$username/$password/${s.stream_id}$sExt"
                        urlsList.add(sUrl)
                        if (s.stream_id == stream.stream_id) currentIndex = index
                    }
                    intent.putStringArrayListExtra("CHANNEL_URLS", urlsList)
                    intent.putExtra("CURRENT_INDEX", currentIndex)
                }

                intent.putExtra("VIDEO_URL", streamUrl)
                intent.putExtra("TYPE", type)
                intent.putExtra("STREAM_ID", stream.stream_id)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("TITLE", stream.name)
                intent.putExtra("COVER", stream.stream_icon)
                startActivity(intent)
            }
        }
        rvStreams.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                filteredStreams.clear()
                if (query.isEmpty()) {
                    filteredStreams.addAll(streams)
                } else {
                    filteredStreams.addAll(streams.filter { it.name.lowercase().contains(query) })
                }
                adapter.notifyDataSetChanged()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val btnVoiceSearch = findViewById<android.widget.ImageButton>(R.id.btnVoiceSearch)
        btnVoiceSearch.setOnClickListener {
            val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Fale o nome do canal ou filme")
            try {
                startActivityForResult(intent, 100)
            } catch (e: Exception) {
                Toast.makeText(this, "Microfone não encontrado nesta TV/Box", Toast.LENGTH_SHORT).show()
            }
        }

        fetchStreams()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                etSearch.setText(result[0])
            }
        }
    }

    private fun fetchStreams() {
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        progressBar.visibility = android.view.View.VISIBLE

        if (type == "favorites") {
            val favs = FavoritesManager.getFavorites(this).map { it.toStream() }
            streams.clear()
            streams.addAll(favs)
            filteredStreams.clear()
            filteredStreams.addAll(streams)
            adapter.notifyDataSetChanged()
            progressBar.visibility = android.view.View.GONE
            return
        }

        val action = when (type) {
            "vod" -> "get_vod_streams"
            "series" -> "get_series"
            else -> "get_live_streams"
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "http://nelitoplay.top:80/player_api.php?username=$username&password=$password&action=$action&category_id=$categoryId"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(responseBody)

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        
                        val streamId = if (type == "series") obj.getString("series_id") else obj.getString("stream_id")
                        val name = obj.getString("name")
                        val icon = if (type == "series") obj.optString("cover", "") else obj.optString("stream_icon", "")
                        val typeStr = if (type == "series") "series" else obj.optString("stream_type", type)
                        val extension = obj.optString("container_extension", "mp4")

                        streams.add(Stream(streamId, name, icon, typeStr, extension))
                    }

                    withContext(Dispatchers.Main) {
                        filteredStreams.clear()
                        filteredStreams.addAll(streams)
                        progressBar.visibility = android.view.View.GONE
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = android.view.View.GONE
                        Toast.makeText(this@StreamsActivity, "Erro ao carregar lista", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this@StreamsActivity, "Erro de conexão", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class StreamAdapter(
        private val list: List<Stream>,
        private val onClick: (Stream) -> Unit
    ) : RecyclerView.Adapter<StreamAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
            val ivFavorite: ImageView = view.findViewById(R.id.ivFavorite)
            
            init {
                view.setOnClickListener { onClick(list[adapterPosition]) }
                
                view.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            val stream = list[position]
                            val ivBackgroundBlur = this@StreamsActivity.findViewById<ImageView>(R.id.ivBackgroundBlur)
                            if (stream.stream_icon.isNotEmpty() && ivBackgroundBlur != null) {
                                Glide.with(this@StreamsActivity)
                                    .load(stream.stream_icon)
                                    .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
                                    .into(ivBackgroundBlur)
                            } else {
                                ivBackgroundBlur?.setImageResource(0)
                            }
                        }
                    }
                }
                
                // Adicionar aos favoritos ao manter pressionado (Long Click)
                view.setOnLongClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val stream = list[position]
                        val isFav = FavoritesManager.toggleFavorite(it.context, stream)
                        updateFavIcon(ivFavorite, isFav)
                        Toast.makeText(it.context, if (isFav) "⭐ Adicionado aos Favoritos" else "❌ Removido dos Favoritos", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                
                ivFavorite.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val stream = list[position]
                        val isFav = FavoritesManager.toggleFavorite(it.context, stream)
                        updateFavIcon(ivFavorite, isFav)
                        Toast.makeText(it.context, if (isFav) "Adicionado aos Favoritos" else "Removido dos Favoritos", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun updateFavIcon(iv: ImageView, isFav: Boolean) {
            if (isFav) {
                iv.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                iv.setImageResource(android.R.drawable.btn_star_big_off)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val stream = list[position]
            holder.tvName.text = stream.name
            
            val isFav = FavoritesManager.isFavorite(holder.itemView.context, stream.stream_id)
            updateFavIcon(holder.ivFavorite, isFav)
            
            // Marcar como visto se for VOD/Série
            if (ProgressManager.isSeen(holder.itemView.context, stream.stream_id)) {
                holder.tvName.text = "✅ ${stream.name}"
                holder.tvName.setTextColor(android.graphics.Color.GREEN)
            } else {
                holder.tvName.setTextColor(android.graphics.Color.WHITE)
            }
            
            if (stream.stream_icon.isNotEmpty()) {
                Glide.with(holder.ivIcon.context)
                    .load(stream.stream_icon)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .thumbnail(0.1f)
                    .into(holder.ivIcon)
            } else {
                holder.ivIcon.setImageResource(android.R.drawable.ic_media_play)
            }
        }

        override fun getItemCount() = list.size
    }
}
