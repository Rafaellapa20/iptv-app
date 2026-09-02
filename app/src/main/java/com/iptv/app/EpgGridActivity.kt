package com.iptv.app

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class EpgGridActivity : AppCompatActivity() {

    private lateinit var username: String
    private lateinit var password: String
    private val channelsList = mutableListOf<Stream>()
    private val epgMap = mutableMapOf<String, List<EpgItem>>()

    private lateinit var rvEpgGrid: RecyclerView
    private lateinit var pbEpgLoading: ProgressBar
    private lateinit var tvInfoChannelName: TextView
    private lateinit var tvInfoTime: TextView
    private lateinit var tvInfoProgramTitle: TextView
    private lateinit var tvInfoProgramDesc: TextView
    private lateinit var ivInfoChannelIcon: ImageView

    data class EpgItem(
        val id: String,
        val title: String,
        val description: String,
        val startTime: String,
        val endTime: String,
        val isLive: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_epg_grid)

        username = intent.getStringExtra("USERNAME") ?: ""
        password = intent.getStringExtra("PASSWORD") ?: ""

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        rvEpgGrid = findViewById(R.id.rvEpgGrid)
        pbEpgLoading = findViewById(R.id.pbEpgLoading)
        tvInfoChannelName = findViewById(R.id.tvInfoChannelName)
        tvInfoTime = findViewById(R.id.tvInfoTime)
        tvInfoProgramTitle = findViewById(R.id.tvInfoProgramTitle)
        tvInfoProgramDesc = findViewById(R.id.tvInfoProgramDesc)
        ivInfoChannelIcon = findViewById(R.id.ivInfoChannelIcon)

        rvEpgGrid.layoutManager = LinearLayoutManager(this)

        loadEpgData()
    }

    private fun loadEpgData() {
        pbEpgLoading.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_live_streams"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(body)
                    channelsList.clear()

                    // Pega até 50 canais principais para fluidez perfeita na TV Box
                    val count = Math.min(jsonArray.length(), 50)
                    for (i in 0 until count) {
                        val obj = jsonArray.getJSONObject(i)
                        val streamId = obj.getString("stream_id")
                        val name = obj.getString("name")
                        val icon = obj.optString("stream_icon", "")
                        channelsList.add(Stream(streamId, name, icon, "live", "ts"))
                    }

                    withContext(Dispatchers.Main) {
                        pbEpgLoading.visibility = View.GONE
                        rvEpgGrid.adapter = EpgRowAdapter()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { pbEpgLoading.visibility = View.GONE }
            }
        }
    }

    private fun playChannel(stream: Stream) {
        val intent = Intent(this, PlayerActivity::class.java)
        val url = "${Constants.SERVER_URL}/live/$username/$password/${stream.stream_id}.ts"
        intent.putExtra("VIDEO_URL", url)
        intent.putExtra("STREAM_ID", stream.stream_id)
        intent.putExtra("TITLE", stream.name)
        intent.putExtra("TYPE", "live")
        intent.putExtra("USERNAME", username)
        intent.putExtra("PASSWORD", password)
        startActivity(intent)
    }

    private fun decodeBase64(text: String): String {
        return try {
            String(Base64.decode(text, Base64.DEFAULT))
        } catch (e: Exception) {
            text
        }
    }

    // ADAPTER DAS LINHAS DOS CANAIS
    inner class EpgRowAdapter : RecyclerView.Adapter<EpgRowAdapter.RowViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_epg_row, parent, false)
            return RowViewHolder(view)
        }

        override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
            val stream = channelsList[position]
            holder.tvChannelName.text = stream.name
            if (stream.stream_icon.isNotEmpty()) {
                Glide.with(this@EpgGridActivity).load(stream.stream_icon).into(holder.ivChannelLogo)
            }

            holder.cardChannelHeader.setOnClickListener {
                playChannel(stream)
            }

            holder.cardChannelHeader.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    tvInfoChannelName.text = stream.name
                    tvInfoProgramTitle.text = "Emissão em Direto de ${stream.name}"
                    tvInfoProgramDesc.text = "Prima OK no comando para assistir a este canal em ecrã inteiro."
                    tvInfoTime.text = "--:--"
                    if (stream.stream_icon.isNotEmpty()) {
                        Glide.with(this@EpgGridActivity).load(stream.stream_icon).into(ivInfoChannelIcon)
                    }
                }
            }

            // Carregar os programas EPG deste canal
            loadChannelPrograms(stream.stream_id, holder.rvProgramsHorizontal, stream)
        }

        override fun getItemCount() = channelsList.size

        inner class RowViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val cardChannelHeader: CardView = v.findViewById(R.id.cardChannelHeader)
            val ivChannelLogo: ImageView = v.findViewById(R.id.ivChannelLogo)
            val tvChannelName: TextView = v.findViewById(R.id.tvChannelName)
            val rvProgramsHorizontal: RecyclerView = v.findViewById(R.id.rvProgramsHorizontal)
        }
    }

    private fun loadChannelPrograms(streamId: String, rvPrograms: RecyclerView, stream: Stream) {
        if (epgMap.containsKey(streamId)) {
            rvPrograms.adapter = EpgProgramAdapter(epgMap[streamId] ?: emptyList(), stream)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_short_epg&stream_id=$streamId"
                val request = Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                val items = mutableListOf<EpgItem>()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val json = JSONObject(body)
                    val listing = json.optJSONArray("epg_listings") ?: JSONArray()

                    val now = System.currentTimeMillis() / 1000
                    val sdfIn = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val sdfOut = SimpleDateFormat("HH:mm", Locale.getDefault())

                    for (i in 0 until listing.length()) {
                        val obj = listing.getJSONObject(i)
                        val titleEnc = obj.optString("title", "")
                        val descEnc = obj.optString("description", "")
                        val title = decodeBase64(titleEnc)
                        val desc = decodeBase64(descEnc)
                        val startStr = obj.optString("start", "")
                        val endStr = obj.optString("end", "")

                        var startTime = ""
                        var endTime = ""
                        var isLive = false

                        try {
                            val startDate = sdfIn.parse(startStr)
                            val endDate = sdfIn.parse(endStr)
                            if (startDate != null && endDate != null) {
                                startTime = sdfOut.format(startDate)
                                endTime = sdfOut.format(endDate)
                                val sTimeSec = startDate.time / 1000
                                val eTimeSec = endDate.time / 1000
                                isLive = now in sTimeSec..eTimeSec
                            }
                        } catch (e: Exception) {}

                        items.add(EpgItem(obj.optString("id", "$i"), title, desc, startTime, endTime, isLive))
                    }
                }

                if (items.isEmpty()) {
                    items.add(EpgItem("0", "Programação Indisponível", "Informação de EPG em atualização.", "--:--", "--:--", false))
                }

                epgMap[streamId] = items

                withContext(Dispatchers.Main) {
                    rvPrograms.adapter = EpgProgramAdapter(items, stream)
                }
            } catch (e: Exception) {}
        }
    }

    // ADAPTER DOS PROGRAMAS EPG HORIZONTAIS
    inner class EpgProgramAdapter(private val programs: List<EpgItem>, private val stream: Stream) :
        RecyclerView.Adapter<EpgProgramAdapter.ProgramViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_epg_program, parent, false)
            return ProgramViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProgramViewHolder, position: Int) {
            val prog = programs[position]
            holder.tvProgTitle.text = prog.title
            holder.tvProgTime.text = "${prog.startTime} - ${prog.endTime}"

            if (prog.isLive) {
                holder.tvLiveBadge.visibility = View.VISIBLE
            } else {
                holder.tvLiveBadge.visibility = View.GONE
            }

            holder.cardEpgProgram.setOnClickListener {
                playChannel(stream)
            }

            holder.cardEpgProgram.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    tvInfoChannelName.text = stream.name
                    tvInfoProgramTitle.text = prog.title
                    tvInfoProgramDesc.text = if (prog.description.isNotEmpty()) prog.description else "Sem descrição disponível."
                    tvInfoTime.text = "${prog.startTime} - ${prog.endTime}"
                    if (stream.stream_icon.isNotEmpty()) {
                        Glide.with(this@EpgGridActivity).load(stream.stream_icon).into(ivInfoChannelIcon)
                    }
                }
            }
        }

        override fun getItemCount() = programs.size

        inner class ProgramViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val cardEpgProgram: CardView = v.findViewById(R.id.cardEpgProgram)
            val tvProgTime: TextView = v.findViewById(R.id.tvProgTime)
            val tvProgTitle: TextView = v.findViewById(R.id.tvProgTitle)
            val tvLiveBadge: TextView = v.findViewById(R.id.tvLiveBadge)
        }
    }
}
