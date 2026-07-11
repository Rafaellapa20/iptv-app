package com.iptv.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONArray

class VodNetflixActivity : AppCompatActivity() {

    private lateinit var rvSidebar: RecyclerView
    private lateinit var rvMovieGrid: RecyclerView
    private lateinit var tvMainTitle: TextView
    private lateinit var progressBar: ProgressBar
    
    private val categories = mutableListOf<Category>()
    // Mapeia Category_ID para Lista de Filmes
    private val streamsByCategory = mutableMapOf<String, List<Stream>>()
    
    private var username = ""
    private var password = ""
    private var type = "vod"

    private var movieGridAdapter: MovieGridAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vod_netflix)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""
        type = intent.getStringExtra("TYPE") ?: "vod"

        tvMainTitle = findViewById(R.id.tvMainTitle)
        rvSidebar = findViewById(R.id.rvSidebar)
        rvMovieGrid = findViewById(R.id.rvMovieGrid)
        progressBar = findViewById(R.id.progressBar)
        
        rvSidebar.layoutManager = LinearLayoutManager(this)
        rvMovieGrid.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 5)
        
        if (type == "series") {
            tvMainTitle.text = "SÉRIES"
        }

        setupTopNavigation()
        fetchData()
    }

    private fun setupTopNavigation() {
        val navHome = findViewById<TextView>(R.id.nav_home)
        val navCanais = findViewById<TextView>(R.id.nav_canais)
        val navFilmes = findViewById<TextView>(R.id.nav_filmes)
        val navSeries = findViewById<TextView>(R.id.nav_series)
        val btnSearch = findViewById<android.widget.ImageButton>(R.id.btnSearch)

        if (type == "series") {
            navFilmes.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            navSeries.setTextColor(android.graphics.Color.parseColor("#FFCC00"))
        }

        btnSearch.setOnClickListener {
            val intent = Intent(this, StreamsActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("TYPE", type)
            intent.putExtra("CATEGORY_NAME", if (type == "series") "Pesquisa de Séries" else "Pesquisa de Filmes")
            intent.putExtra("CATEGORY_ID", "") // Vazio para pesquisar todos
            startActivity(intent)
        }

        navHome.setOnClickListener { finish() }
        navCanais.setOnClickListener {
            val intent = Intent(this, LiveTvActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            finish()
        }
        navFilmes.setOnClickListener {
            if (type != "vod") {
                val intent = Intent(this, VodNetflixActivity::class.java)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("TYPE", "vod")
                startActivity(intent)
                finish()
            }
        }
        navSeries.setOnClickListener {
            if (type != "series") {
                val intent = Intent(this, VodNetflixActivity::class.java)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("TYPE", "series")
                startActivity(intent)
                finish()
            }
        }
    }
    // Mapeamento de géneros - baseado nas categorias exactas do servidor
    // Funciona para Filmes (por género) e Séries (por plataforma)
    private val genreMap = linkedMapOf(
        "▶️ Continuar a Assistir" to listOf("continue_watching"),
        "❤️ A Minha Lista" to listOf("my_list"),
        // --- FILMES ---
        "🆕 Novidades" to listOf("novidades"),
        "💥 Ação" to listOf("acção", "ação", "acao", "action"),
        "🗺️ Aventura" to listOf("aventura", "adventure"),
        "😂 Comédia" to listOf("comédia", "comedia", "comedy"),
        "🎭 Drama" to listOf("drama"),
        "🔍 Crime" to listOf("crime", "thriller", "policial"),
        "😱 Terror" to listOf("terror", "horror"),
        "🤖 Ficção Científica" to listOf("ficção cientifica", "ficção científica", "sci-fi"),
        "⚔️ Guerra / Histórico" to listOf("guerra", "histórico", "historico"),
        "🎵 Musicais" to listOf("musicais", "musical"),
        "❤️ Romance" to listOf("romance", "romântico"),
        // --- SÉRIES (por plataforma) ---
        "🔴 Netflix" to listOf("netflix"),
        "🟣 HBO" to listOf("hbo"),
        "🏰 Disney+" to listOf("disney"),
        "🛒 Amazon Prime" to listOf("amazon prime"),
        "🍎 Apple TV+" to listOf("apple tv"),
        "🇰🇷 Dorama" to listOf("dorama"),
        // --- COMUNS ---
        "👶 Família / Infantis" to listOf("familia", "família", "family", "tuga kids", "kids", "infantis", "infantil"),
        "🎬 Animação" to listOf("animação", "animacao", "animation", "idade do gelo"),
        "📺 Documentários" to listOf("documentário", "documentários", "documentario", "documentarios", "biografia"),
        "🌐 Multi Legenda" to listOf("multi legenda"),
        "📺 Séries Gerais" to listOf("series")
    )

    private fun fetchData() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val catAction = if (type == "series") "get_series_categories" else "get_vod_categories"
                val catUrl = "http://nelitoplay.top:80/player_api.php?username=$username&password=$password&action=$catAction"
                val catResponse = OkHttpProvider.client.newCall(Request.Builder().url(catUrl).build()).execute()

                if (catResponse.isSuccessful) {
                    val array = JSONArray(catResponse.body?.string() ?: "[]")

                    // Ler todas as categorias do servidor
                    val serverCategories = mutableListOf<Category>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        serverCategories.add(Category(obj.getString("category_id"), obj.getString("category_name"), 0))
                    }

                    // --- CONTINUAR A ASSISTIR ---
                    val recent = ProgressManager.getRecentProgressList(this@VodNetflixActivity).filter { it.type == type }
                    if (recent.isNotEmpty()) {
                        val sList = recent.map { Stream(it.streamId, it.title, it.coverUrl, it.type, "") }
                        categories.add(Category("continue_watching", "▶️ Continuar a Assistir", 0))
                        streamsByCategory["continue_watching"] = sList
                    }

                    // --- A MINHA LISTA ---
                    val favorites = FavoritesManager.getFavorites(this@VodNetflixActivity).filter { it.type == type }
                    if (favorites.isNotEmpty()) {
                        val sList = favorites.map { Stream(it.streamId, it.title, it.coverUrl, it.type, "") }
                        categories.add(Category("my_list", "❤️ A Minha Lista", 0))
                        streamsByCategory["my_list"] = sList
                    }

                    // --- GRUPOS DE GÉNERO ---
                    // Para cada género, encontrar as categorias do servidor que lhe correspondem
                    val streamAction = if (type == "series") "get_series" else "get_vod_streams"
                    val genreStreams = mutableMapOf<String, MutableList<Stream>>()

                    val deferreds = serverCategories.map { serverCat ->
                        async(Dispatchers.IO) {
                            // Descobre a qual género pertence esta categoria
                            val catNameLower = serverCat.category_name.lowercase()
                            val matchedGenre = genreMap.entries
                                .firstOrNull { (_, keywords) ->
                                    keywords.any { kw -> catNameLower.contains(kw) }
                                }?.key

                            val targetGenre = matchedGenre ?: "🎬 Outros Filmes"

                            try {
                                val streamUrl = "http://nelitoplay.top:80/player_api.php?username=$username&password=$password&action=$streamAction&category_id=${serverCat.category_id}"
                                val sRes = OkHttpProvider.client.newCall(Request.Builder().url(streamUrl).build()).execute()
                                if (sRes.isSuccessful) {
                                    val sArray = JSONArray(sRes.body?.string() ?: "[]")
                                    val sLimit = sArray.length()
                                    for (j in 0 until sLimit) {
                                        val sObj = sArray.getJSONObject(j)
                                        val stream = Stream(
                                            if (type == "series") sObj.getString("series_id") else sObj.getString("stream_id"),
                                            sObj.getString("name"),
                                            if (type == "series") sObj.optString("cover", "") else sObj.optString("stream_icon", ""),
                                            type,
                                            sObj.optString("container_extension", "mp4")
                                        )
                                        synchronized(genreStreams) {
                                            genreStreams.getOrPut(targetGenre) { mutableListOf() }.add(stream)
                                        }
                                    }
                                }
                            } catch (e: Exception) { /* ignora timeout individual */ }
                        }
                    }

                    // Mostra loading
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.VISIBLE
                    }

                    deferreds.awaitAll()

                    // Montar categorias por género, na ordem do genreMap
                    for (genreName in genreMap.keys) {
                        if (genreName == "▶️ Continuar a Assistir" || genreName == "❤️ A Minha Lista") continue
                        val list = genreStreams[genreName]
                        if (!list.isNullOrEmpty()) {
                            val catId = "genre_${genreName.replace(" ", "_")}"
                            categories.add(Category(catId, genreName, 0))
                            streamsByCategory[catId] = list
                        }
                    }
                    // "Outros Filmes" sempre no fim
                    val outros = genreStreams["🎬 Outros Filmes"]
                    if (!outros.isNullOrEmpty()) {
                        categories.add(Category("genre_outros", "🎬 Outros Filmes", 0))
                        streamsByCategory["genre_outros"] = outros
                    }

                    // Limpar categorias sem conteúdo
                    categories.removeAll { cat ->
                        val streams = streamsByCategory[cat.category_id]
                        streams == null || streams.isEmpty()
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        val sidebarAdapter = SidebarCategoryAdapter(categories, streamsByCategory) { selectedCategory ->
                            val movies = streamsByCategory[selectedCategory.category_id] ?: emptyList()
                            movieGridAdapter = MovieGridAdapter(movies)
                            rvMovieGrid.adapter = movieGridAdapter
                        }
                        rvSidebar.adapter = sidebarAdapter

                        // Select first category by default
                        if (categories.isNotEmpty()) {
                            val firstCat = categories[0]
                            val movies = streamsByCategory[firstCat.category_id] ?: emptyList()
                            movieGridAdapter = MovieGridAdapter(movies)
                            rvMovieGrid.adapter = movieGridAdapter
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE }
            }
        }
    }

    // Adapter para a Sidebar
    inner class SidebarCategoryAdapter(
        private val catList: List<Category>,
        private val streamsMap: Map<String, List<Stream>>,
        private val onCategorySelected: (Category) -> Unit
    ) : RecyclerView.Adapter<SidebarCategoryAdapter.CategoryHolder>() {

        inner class CategoryHolder(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvCategoryName)
            val tvCount: TextView = v.findViewById(R.id.tvCategoryCount)

            init {
                v.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        tvName.setTextColor(android.graphics.Color.parseColor("#FFCC00"))
                        tvCount.setTextColor(android.graphics.Color.parseColor("#FFCC00"))
                        onCategorySelected(catList[adapterPosition])
                    } else {
                        tvName.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                        tvCount.setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
                    }
                }
                v.setOnClickListener {
                    onCategorySelected(catList[adapterPosition])
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CategoryHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_sidebar_category, parent, false)
        )

        override fun onBindViewHolder(holder: CategoryHolder, position: Int) {
            val cat = catList[position]
            holder.tvName.text = cat.category_name
            val count = streamsMap[cat.category_id]?.size ?: 0
            holder.tvCount.text = count.toString()
        }

        override fun getItemCount() = catList.size
    }

    // Adapter para a Grelha de Filmes
    inner class MovieGridAdapter(private val movies: List<Stream>) : RecyclerView.Adapter<MovieGridAdapter.MovieHolder>() {

        inner class MovieHolder(v: View) : RecyclerView.ViewHolder(v) {
            val ivPoster: ImageView = v.findViewById(R.id.ivMoviePoster)
            val tvFallback: TextView = v.findViewById(R.id.tvMovieTitleFallback)
            val pbProgress: ProgressBar = v.findViewById(R.id.pbMovieProgress)

            init {
                v.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        tvFallback.setTextColor(android.graphics.Color.parseColor("#000000"))
                    } else {
                        tvFallback.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                    }
                }
                
                v.setOnClickListener {
                    val s = movies[adapterPosition]
                    if (s.stream_type == "series") {
                        val intent = Intent(this@VodNetflixActivity, EpisodesActivity::class.java)
                        intent.putExtra("SERIES_ID", s.stream_id)
                        intent.putExtra("USERNAME", username)
                        intent.putExtra("PASSWORD", password)
                        intent.putExtra("SERIES_NAME", s.name)
                        intent.putExtra("SERIES_COVER", s.stream_icon)
                        startActivity(intent)
                    } else {
                        val intent = Intent(this@VodNetflixActivity, MovieInfoActivity::class.java)
                        val url = "http://nelitoplay.top:80/movie/$username/$password/${s.stream_id}.${s.extension}"
                        intent.putExtra("VIDEO_URL", url)
                        intent.putExtra("TITLE", s.name)
                        intent.putExtra("STREAM_ID", s.stream_id)
                        intent.putExtra("USERNAME", username)
                        intent.putExtra("PASSWORD", password)
                        intent.putExtra("COVER", s.stream_icon)
                        
                        // Hollywood Transition
                        ViewCompat.setTransitionName(ivPoster, "poster_transition")
                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this@VodNetflixActivity, ivPoster, "poster_transition"
                        )
                        startActivity(intent, options.toBundle())
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MovieHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_movie_grid, parent, false)
        )

        override fun onBindViewHolder(holder: MovieHolder, position: Int) {
            val s = movies[position]
            holder.tvFallback.text = s.name
            
            if (s.stream_icon.isNotEmpty()) {
                Glide.with(holder.ivPoster.context).load(s.stream_icon).into(holder.ivPoster)
            } else {
                holder.ivPoster.setImageDrawable(null)
            }
            
            // Verifica Progresso Visual
            holder.pbProgress.visibility = View.GONE
            val recents = ProgressManager.getRecentProgressList(holder.itemView.context)
            val match = recents.find { it.streamId == s.stream_id }
            if (match != null && match.duration > 0) {
                holder.pbProgress.visibility = View.VISIBLE
                holder.pbProgress.max = match.duration.toInt()
                holder.pbProgress.progress = match.position.toInt()
            }
        }

        override fun getItemCount() = movies.size
    }
}
