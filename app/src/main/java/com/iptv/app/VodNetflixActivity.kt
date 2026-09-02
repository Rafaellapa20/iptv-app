package com.iptv.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
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

    private fun updateHeroBanner(stream: Stream) {
        val ivBackgroundBlur = findViewById<ImageView>(R.id.ivBackgroundBlur)
        val llHeroBanner = findViewById<LinearLayout>(R.id.llHeroBanner)
        val ivHeroPoster = findViewById<ImageView>(R.id.ivHeroPoster)
        val tvHeroTitle = findViewById<TextView>(R.id.tvHeroTitle)
        val tvHeroRating = findViewById<TextView>(R.id.tvHeroRating)
        val tvHeroDesc = findViewById<TextView>(R.id.tvHeroDesc)

        llHeroBanner.visibility = View.VISIBLE
        tvHeroTitle.text = stream.name
        
        // Simular rating (ex: a API tem rating, mas para simplificar vamos por um texto padrão ou limpar)
        tvHeroRating.text = if (type == "series") "Série" else "Filme"
        tvHeroDesc.text = "As melhores escolhas preparadas para si." // Ideally, fetch VOD info, mas para não encravar a grelha, pomos um default.

        if (stream.stream_icon.isNotEmpty()) {
            // Atualizar o poster pequeno
            Glide.with(this).load(stream.stream_icon).into(ivHeroPoster)
            
            // Atualizar o fundo com efeito Blur
            Glide.with(this)
                .load(stream.stream_icon)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 3)))
                .into(ivBackgroundBlur)
        } else {
            ivHeroPoster.setImageResource(R.drawable.logo)
            ivBackgroundBlur.setImageResource(0)
        }
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
        "▶️ Continuar a Ver" to listOf("continue_watching"),
        "✅ Já Visto" to listOf("already_watched"),
        "❤️ Favoritos" to listOf("favorites"),
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
                val catUrl = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=$catAction"
                val catResponse = OkHttpProvider.client.newCall(Request.Builder().url(catUrl).build()).execute()

                if (catResponse.isSuccessful) {
                    val array = JSONArray(catResponse.body?.string() ?: "[]")

                    // Ler todas as categorias do servidor
                    val serverCategories = mutableListOf<Category>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        serverCategories.add(Category(obj.getString("category_id"), obj.getString("category_name"), 0))
                    }

                    // --- CONTINUAR A VER e JÁ VISTO ---
                    val recent = ProgressManager.getRecentProgressList(this@VodNetflixActivity).filter { it.type == type }
                    
                    val continueWatchingList = recent.filter { 
                        it.duration == 0L || (it.position.toDouble() / it.duration) < 0.90
                    }
                    if (continueWatchingList.isNotEmpty()) {
                        val sList = continueWatchingList.map { Stream(it.streamId, it.title, it.coverUrl, it.type, "") }
                        categories.add(Category("continue_watching", "▶️ Continuar a Ver", 0))
                        streamsByCategory["continue_watching"] = sList
                    }

                    val alreadyWatchedList = recent.filter { 
                        it.duration > 0L && (it.position.toDouble() / it.duration) >= 0.90
                    }
                    if (alreadyWatchedList.isNotEmpty()) {
                        val sList = alreadyWatchedList.map { Stream(it.streamId, it.title, it.coverUrl, it.type, "") }
                        categories.add(Category("already_watched", "✅ Já Visto", 0))
                        streamsByCategory["already_watched"] = sList
                    }

                    // --- FAVORITOS ---
                    val favorites = FavoritesManager.getFavorites(this@VodNetflixActivity).filter { it.type == type }
                    if (favorites.isNotEmpty()) {
                        val sList = favorites.map { Stream(it.streamId, it.title, it.coverUrl, it.type, "") }
                        categories.add(Category("favorites", "❤️ Favoritos", 0))
                        streamsByCategory["favorites"] = sList
                    }

                    // Grupos de categorias
                    val genreToCatIds = mutableMapOf<String, MutableList<String>>()
                    for (serverCat in serverCategories) {
                        val catNameLower = serverCat.category_name.lowercase()
                        val matchedGenre = genreMap.entries
                            .firstOrNull { (_, keywords) ->
                                keywords.any { kw -> catNameLower.contains(kw) }
                            }?.key ?: "🎬 Outros Filmes"
                        genreToCatIds.getOrPut(matchedGenre) { mutableListOf() }.add(serverCat.category_id)
                    }

                    for (genreName in genreMap.keys) {
                        if (genreName == "▶️ Continuar a Ver" || genreName == "✅ Já Visto" || genreName == "❤️ Favoritos") continue
                        if (genreToCatIds.containsKey(genreName)) {
                            val catId = "genre_${genreName.replace(" ", "_")}"
                            categories.add(Category(catId, genreName, 0))
                            // Guardamos os IDs reais no parent_id ou num map global, mas podemos usar o id para mapear.
                            // Para simplificar, vamos armazenar os Server Category IDs num mapa global:
                            genreServerCatIds[catId] = genreToCatIds[genreName]!!
                        }
                    }
                    val outros = genreToCatIds["🎬 Outros Filmes"]
                    if (outros != null && outros.isNotEmpty()) {
                        categories.add(Category("genre_outros", "🎬 Outros Filmes", 0))
                        genreServerCatIds["genre_outros"] = outros
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        val sidebarAdapter = SidebarCategoryAdapter(categories, streamsByCategory) { selectedCategory ->
                            loadCategoryStreams(selectedCategory.category_id)
                        }
                        rvSidebar.adapter = sidebarAdapter

                        if (categories.isNotEmpty()) {
                            // Carrega a primeira categoria (pode ser "continue_watching" ou um género)
                            loadCategoryStreams(categories[0].category_id)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE }
            }
        }
    }

    private val genreServerCatIds = mutableMapOf<String, List<String>>()

    private fun loadCategoryStreams(catId: String) {
        // Se já temos as streams (ex: "continue_watching", "favorites" ou já feito cache), mostra logo
        if (streamsByCategory.containsKey(catId) && streamsByCategory[catId]!!.isNotEmpty()) {
            movieGridAdapter = MovieGridAdapter(streamsByCategory[catId]!!)
            rvMovieGrid.adapter = movieGridAdapter
            return
        }

        val serverIds = genreServerCatIds[catId] ?: return
        progressBar.visibility = View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            val fetchedStreams = mutableListOf<Stream>()
            val streamAction = if (type == "series") "get_series" else "get_vod_streams"

            val deferreds = serverIds.map { serverCatId ->
                async {
                    try {
                        val streamUrl = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=$streamAction&category_id=$serverCatId"
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
                                    sObj.optString("container_extension", "mp4"),
                                    sObj.optString("added", "0")
                                )
                                synchronized(fetchedStreams) {
                                    fetchedStreams.add(stream)
                                }
                            }
                        }
                    } catch (e: Exception) { }
                }
            }
            deferreds.awaitAll()

            val sortedStreams = fetchedStreams.sortedByDescending { it.added.toLongOrNull() ?: 0L }
            streamsByCategory[catId] = sortedStreams
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                movieGridAdapter = MovieGridAdapter(sortedStreams)
                rvMovieGrid.adapter = movieGridAdapter
                
                // Força atualização da sidebar para mostrar a contagem
                rvSidebar.adapter?.notifyDataSetChanged()
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
                    rvMovieGrid.requestFocus()
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
                        tvFallback.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                        updateHeroBanner(movies[adapterPosition])
                    } else {
                        tvFallback.setTextColor(android.graphics.Color.parseColor("#DDDDDD"))
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
                        val url = "${Constants.SERVER_URL}/movie/$username/$password/${s.stream_id}.${s.extension}"
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


