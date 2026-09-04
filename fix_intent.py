with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

kt = kt.replace('val intent = if (this == openTv) Intent(this@SeniorMainActivity, LiveTvActivity::class.java) else Intent(this@SeniorMainActivity, VodNetflixActivity::class.java)', 
'''// fixed
            val intent = Intent(this@SeniorMainActivity, VodNetflixActivity::class.java)''')

kt = kt.replace('''val openTv = View.OnClickListener {
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
            }
            // CategoriesActivity gave errors because of OkHttp/JSON or simply wasn't desired.
            // fixed
            val intent = Intent(this@SeniorMainActivity, VodNetflixActivity::class.java)''',
'''val openTv = View.OnClickListener {
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
            }
            val intent = Intent(this@SeniorMainActivity, LiveTvActivity::class.java)''')

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Fixed intents.")
