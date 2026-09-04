package com.iptv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SeniorMainActivity : AppCompatActivity() {
    private fun playChannel(stream: Stream, recents: List<Stream>, username: String, password: String) {
        val intent = Intent(this, PlayerActivity::class.java)
        
        val urls = ArrayList<String>()
        val ids = ArrayList<String>()
        val names = ArrayList<String>()
        val covers = ArrayList<String>()
        
        var currentIndex = 0
        for ((index, item) in recents.withIndex()) {
            val ext = if (item.extension.isNullOrEmpty()) "ts" else item.extension
            val url = "${Constants.SERVER_URL}/live/$username/$password/${item.stream_id}.$ext"
            urls.add(url)
            ids.add(item.stream_id)
            names.add(item.name)
            covers.add(item.stream_icon)
            
            if (item.stream_id == stream.stream_id) {
                currentIndex = index
            }
        }
        
        intent.putStringArrayListExtra("CHANNEL_URLS", urls)
        intent.putStringArrayListExtra("CHANNEL_IDS", ids)
        intent.putStringArrayListExtra("CHANNEL_NAMES", names)
        intent.putStringArrayListExtra("CHANNEL_COVERS", covers)
        intent.putExtra("CURRENT_INDEX", currentIndex)
        
        intent.putExtra("STREAM_ID", stream.stream_id)
        intent.putExtra("TITLE", stream.name)
        intent.putExtra("COVER", stream.stream_icon)
        intent.putExtra("TYPE", "live")
        val ext = if (stream.extension.isNullOrEmpty()) "ts" else stream.extension
        intent.putExtra("VIDEO_URL", "${Constants.SERVER_URL}/live/$username/$password/${stream.stream_id}.$ext")
        intent.putExtra("USERNAME", username)
        intent.putExtra("PASSWORD", password)
        startActivity(intent)
    }

    private var clockJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_senior_main)

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = intent.getStringExtra("USERNAME") ?: prefs.getString("USERNAME", "") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: prefs.getString("PASSWORD", "") ?: ""

        RemoteManager.startTvServer(this, username, password) { pin ->
            findViewById<TextView>(R.id.tvPairingPin)?.text = "Código de emparelhamento: $pin"
        }

        val openTv = View.OnClickListener {
            val intent = Intent(this@SeniorMainActivity, LiveTvActivity::class.java)
            intent.putExtra("TYPE", "live")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        val openMovies = View.OnClickListener {
            // CategoriesActivity gave errors because of OkHttp/JSON or simply wasn't desired.
            // fixed
            val intent = Intent(this@SeniorMainActivity, VodNetflixActivity::class.java)
            intent.putExtra("TYPE", "vod")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        val openSeries = View.OnClickListener {
            // CategoriesActivity gave errors because of OkHttp/JSON or simply wasn't desired.
            // fixed
            val intent = Intent(this@SeniorMainActivity, VodNetflixActivity::class.java)
            intent.putExtra("TYPE", "series")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        val openFavorites = View.OnClickListener {
            val intent = Intent(this, LiveTvActivity::class.java)
            intent.putExtra("CATEGORY_ID", "favorites")
            intent.putExtra("TYPE", "live")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSeniorTv).setOnClickListener(openTv)
        findViewById<View>(R.id.btnSeniorTvBig).setOnClickListener(openTv)
        
        findViewById<View>(R.id.btnSeniorFilmes).setOnClickListener(openMovies)
        findViewById<View>(R.id.btnSeniorSeries).setOnClickListener(openSeries)
        findViewById<View>(R.id.btnSeniorFavoritos).setOnClickListener(openFavorites)
        
        val rvRecent = findViewById<RecyclerView>(R.id.rvSeniorRecentChannels)
        val recents = RecentManager.getRecent(this).filter { it.stream_type == "live" }.take(5)
        rvRecent.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvRecent.adapter = RecentChannelAdapter(recents) { stream ->
            playChannel(stream, recents, username, password)
        }
        
        // Remove listeners for removed big buttons


        findViewById<View>(R.id.btnSairSenior).setOnClickListener {
            prefs.edit().putBoolean("is_senior_mode", false).apply()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            finish()
        }

        val tvClock = findViewById<TextView>(R.id.tvClockSenior)
        clockJob = CoroutineScope(Dispatchers.Default).launch {
            while(true) {
                // "20:15 Quinta, 3 de setembro" format roughly
                val sdf = SimpleDateFormat("HH:mm  EEEE, d 'de' MMMM", Locale("pt", "PT"))
                val time = sdf.format(Date())
                withContext(Dispatchers.Main) {
                    tvClock?.text = time.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                delay(1000)
            }
        }
        
        // Request initial focus
        findViewById<View>(R.id.btnSeniorTvBig).requestFocus()
    }
    
    // Não chama super.onBackPressed() de propósito: queremos sempre confirmar
    // antes de sair, em vez do comportamento por omissão (sair imediatamente).
    @Deprecated("Deprecated in Java", ReplaceWith("super.onBackPressed()"))
    @Suppress("MissingSuperCall", "DEPRECATION")
    override fun onBackPressed() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Sair")
        builder.setMessage("Tem a certeza que deseja fechar a aplicação?")
        builder.setPositiveButton("Sim") { _, _ -> finishAffinity() }
        builder.setNegativeButton("Não", null)
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockJob?.cancel()
    }

    inner class RecentChannelAdapter(
        private val list: List<Stream>,
        private val onClick: (Stream) -> Unit
    ) : RecyclerView.Adapter<RecentChannelAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            init {
                view.setOnClickListener { onClick(list[bindingAdapterPosition]) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvName.text = list[position].name
        }

        override fun getItemCount() = list.size
    }
}
