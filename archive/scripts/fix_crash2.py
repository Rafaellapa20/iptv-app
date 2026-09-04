import re

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# I will just write a function to build the intent in SeniorMainActivity
new_func = '''
    private fun playChannel(stream: RecentManager.RecentStream, recents: List<RecentManager.RecentStream>, username: String, password: String) {
        val intent = Intent(this, PlayerActivity::class.java)
        
        val urls = ArrayList<String>()
        val ids = ArrayList<String>()
        val names = ArrayList<String>()
        val covers = ArrayList<String>()
        
        var currentIndex = 0
        for ((index, item) in recents.withIndex()) {
            val ext = if (item.extension.isNullOrEmpty()) "ts" else item.extension
            val url = "/live///."
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
        intent.putExtra("VIDEO_URL", "/live///.")
        intent.putExtra("USERNAME", username)
        intent.putExtra("PASSWORD", password)
        startActivity(intent)
    }
'''

# Find the class SeniorMainActivity { and insert the function
kt = kt.replace('class SeniorMainActivity : AppCompatActivity() {', 'class SeniorMainActivity : AppCompatActivity() {' + new_func)

# Fix openTv
kt = re.sub(
    r'val intent = Intent\(this, PlayerActivity::class\.java\)\s+intent\.putExtra\("STREAM_ID".*?startActivity\(intent\)',
    r'playChannel(recents[0], recents, username, password)',
    kt, flags=re.DOTALL, count=1
)

# Fix adapter intent
kt = re.sub(
    r'val intent = Intent\(this, PlayerActivity::class\.java\)\s+intent\.putExtra\("STREAM_ID".*?startActivity\(intent\)',
    r'playChannel(stream, recents, username, password)',
    kt, flags=re.DOTALL, count=1
)

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Crash fix 2 applied.")
