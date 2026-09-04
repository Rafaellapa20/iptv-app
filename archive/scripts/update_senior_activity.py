import re

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# Replace all intents pointing to CategoriesActivity with intents to the actual activities.
kt = kt.replace('val intent = Intent(this, CategoriesActivity::class.java)', 
'''// CategoriesActivity gave errors because of OkHttp/JSON or simply wasn't desired.
            val intent = if (this == openTv) Intent(this@SeniorMainActivity, LiveTvActivity::class.java) else Intent(this@SeniorMainActivity, VodNetflixActivity::class.java)''')

kt = kt.replace('val openTv = View.OnClickListener {', '''val openTv = View.OnClickListener {
            // "nos canais quero que abra o canal antes visto"
            val recents = RecentManager.getRecent(this).filter { it.stream_type == "live" }
            if (recents.isNotEmpty()) {
                val intent = Intent(this, PlayerActivity::class.java)
                intent.putExtra("STREAM_ID", recents[0].stream_id)
                intent.putExtra("STREAM_NAME", recents[0].name)
                intent.putExtra("STREAM_ICON", recents[0].stream_icon)
                intent.putExtra("STREAM_TYPE", "live")
                intent.putExtra("STREAM_EXT", recents[0].extension)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                startActivity(intent)
                return@OnClickListener
            }''')

kt = kt.replace('import androidx.appcompat.app.AppCompatActivity', '''import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup''')

setup_rv = '''
        val rvRecent = findViewById<RecyclerView>(R.id.rvSeniorRecentChannels)
        val recents = RecentManager.getRecent(this).filter { it.stream_type == "live" }.take(5)
        rvRecent.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvRecent.adapter = RecentChannelAdapter(recents) { stream ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("STREAM_ID", stream.stream_id)
            intent.putExtra("STREAM_NAME", stream.name)
            intent.putExtra("STREAM_ICON", stream.stream_icon)
            intent.putExtra("STREAM_TYPE", "live")
            intent.putExtra("STREAM_EXT", stream.extension)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }
        
        // Remove listeners for removed big buttons
'''
kt = re.sub(r'findViewById<View>\(R.id.btnSeniorFilmesBig\).setOnClickListener\(openMovies\).*?findViewById<View>\(R.id.btnSeniorFavoritosBig\).setOnClickListener\(openFavorites\)', setup_rv, kt, flags=re.DOTALL)


adapter = '''
    inner class RecentChannelAdapter(
        private val list: List<Stream>,
        private val onClick: (Stream) -> Unit
    ) : RecyclerView.Adapter<RecentChannelAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            init {
                view.setOnClickListener { onClick(list[adapterPosition]) }
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
'''
kt = re.sub(r'}\s*$', adapter, kt)

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("SeniorMainActivity updated.")
