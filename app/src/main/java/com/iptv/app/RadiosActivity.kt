package com.iptv.app

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RadiosActivity : AppCompatActivity() {

    private lateinit var rvRadios: RecyclerView
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvNowPlaying: TextView
    private var mediaPlayer: MediaPlayer? = null
    private var isRadioPlaying = false
    private var currentPlayingUrl: String? = null

    data class RadioStation(val name: String, val logoUrl: String, val streamUrl: String)

    private val radioStations = listOf(
        RadioStation("RÃ¡dio Comercial", "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/R%C3%A1dio_Comercial_logo.svg/1200px-R%C3%A1dio_Comercial_logo.svg.png", "https://mcrscast1.mcr.iol.pt/comercial"),
        RadioStation("RFM", "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4b/Logo_RFM.svg/1024px-Logo_RFM.svg.png", "http://20853.live.streamtheworld.com/RFMAAC.aac"),
        RadioStation("M80", "https://upload.wikimedia.org/wikipedia/pt/e/eb/Logo_M80_Radio.png", "https://mcrscast.mcr.iol.pt/m80"),
        RadioStation("Cidade FM", "https://upload.wikimedia.org/wikipedia/pt/f/fb/Logo_Cidade.png", "https://mcrscast.mcr.iol.pt/cidadefm"),
        RadioStation("RenascenÃ§a", "https://upload.wikimedia.org/wikipedia/commons/e/ea/Logo_RR.png", "http://20853.live.streamtheworld.com/RADIO_RENASCENCA.mp3"),
        RadioStation("Mega Hits", "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Mega_Hits_logo.svg/1200px-Mega_Hits_logo.svg.png", "http://20853.live.streamtheworld.com/MEGA_HITS.mp3"),
        RadioStation("Antena 1", "https://upload.wikimedia.org/wikipedia/commons/thumb/6/67/Antena_1_Portugal_logo.svg/1024px-Antena_1_Portugal_logo.svg.png", "https://radiocast.rtp.pt/antena180a.mp3"),
        RadioStation("TSF", "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/TSF_R%C3%A1dio_Not%C3%ADcias_logo.svg/1200px-TSF_R%C3%A1dio_Not%C3%ADcias_logo.svg.png", "https://tsfdirecto.tsf.pt/tsf/smil:tsf.smil/playlist.m3u8")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radios)

        rvRadios = findViewById(R.id.rvRadios)
        pbLoading = findViewById(R.id.pbLoadingRadio)
        tvNowPlaying = findViewById(R.id.tvNowPlaying)

        rvRadios.layoutManager = GridLayoutManager(this, 4)
        rvRadios.adapter = RadioAdapter(radioStations) { station ->
            playRadio(station)
        }
    }

    private fun playRadio(station: RadioStation) {
        if (currentPlayingUrl == station.streamUrl && isRadioPlaying) {
            // Stop if clicking the same
            stopRadio()
            return
        }
        
        stopRadio()
        pbLoading.visibility = View.VISIBLE
        tvNowPlaying.text = "A ligar a: " + station.name + "..."

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(station.streamUrl)
                setOnPreparedListener {
                    pbLoading.visibility = View.GONE
                    tvNowPlaying.text = "A tocar:  ðŸ”Š"
                    start()
                    isRadioPlaying = true
                    currentPlayingUrl = station.streamUrl
                }
                setOnErrorListener { _, _, _ ->
                    pbLoading.visibility = View.GONE
                    tvNowPlaying.text = "Erro ao reproduzir " + station.name
                    Toast.makeText(this@RadiosActivity, "Erro na emissÃ£o", Toast.LENGTH_SHORT).show()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            pbLoading.visibility = View.GONE
            tvNowPlaying.text = "Erro ao iniciar rÃ¡dio."
        }
    }

    private fun stopRadio() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.stop()
                }
                mediaPlayer!!.release()
            } catch (e: Exception) {
            }
            mediaPlayer = null
        }
        isRadioPlaying = false
        currentPlayingUrl = null
        tvNowPlaying.text = "Selecione uma RÃ¡dio"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRadio()
    }

    inner class RadioAdapter(private val stations: List<RadioStation>, private val onClick: (RadioStation) -> Unit) : RecyclerView.Adapter<RadioAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val ivLogo: ImageView = v.findViewById(R.id.ivRadioLogo)
            val tvName: TextView = v.findViewById(R.id.tvRadioName)
            init {
                v.setOnFocusChangeListener { _, hasFocus ->
                    v.animate().scaleX(if (hasFocus) 1.1f else 1.0f).scaleY(if (hasFocus) 1.1f else 1.0f).setDuration(150).start()
                }
                v.setOnClickListener { onClick(stations[adapterPosition]) }
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_radio, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = stations[position]
            holder.tvName.text = s.name
            Glide.with(holder.itemView.context).load(s.logoUrl).into(holder.ivLogo)
        }
        override fun getItemCount() = stations.size
    }
}


