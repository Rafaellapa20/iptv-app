import re

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# Fix openTv intent
kt = re.sub(
    r'val intent = Intent\(this, PlayerActivity::class\.java\)\s+intent\.putExtra\("STREAM_ID", recents\[0\]\.stream_id\)\s+intent\.putExtra\("STREAM_NAME", recents\[0\]\.name\)\s+intent\.putExtra\("STREAM_ICON", recents\[0\]\.stream_icon\)\s+intent\.putExtra\("STREAM_TYPE", "live"\)\s+intent\.putExtra\("STREAM_EXT", recents\[0\]\.extension\)\s+intent\.putExtra\("USERNAME", username\)\s+intent\.putExtra\("PASSWORD", password\)\s+startActivity\(intent\)',
    r'''val intent = Intent(this, PlayerActivity::class.java)
                intent.putExtra("STREAM_ID", recents[0].stream_id)
                intent.putExtra("TITLE", recents[0].name)
                intent.putExtra("COVER", recents[0].stream_icon)
                intent.putExtra("TYPE", "live")
                val ext = if (recents[0].extension.isNullOrEmpty()) "ts" else recents[0].extension
                intent.putExtra("VIDEO_URL", "/live///.")
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                startActivity(intent)''',
    kt
)

# Fix adapter intent
kt = re.sub(
    r'val intent = Intent\(this, PlayerActivity::class\.java\)\s+intent\.putExtra\("STREAM_ID", stream\.stream_id\)\s+intent\.putExtra\("STREAM_NAME", stream\.name\)\s+intent\.putExtra\("STREAM_ICON", stream\.stream_icon\)\s+intent\.putExtra\("STREAM_TYPE", "live"\)\s+intent\.putExtra\("STREAM_EXT", stream\.extension\)\s+intent\.putExtra\("USERNAME", username\)\s+intent\.putExtra\("PASSWORD", password\)\s+startActivity\(intent\)',
    r'''val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("STREAM_ID", stream.stream_id)
            intent.putExtra("TITLE", stream.name)
            intent.putExtra("COVER", stream.stream_icon)
            intent.putExtra("TYPE", "live")
            val ext = if (stream.extension.isNullOrEmpty()) "ts" else stream.extension
            intent.putExtra("VIDEO_URL", "/live///.")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)''',
    kt
)

with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Crash fix applied.")
