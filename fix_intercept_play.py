# -*- coding: utf-8 -*-
import re

# 1. MovieInfoActivity
with open('app/src/main/java/com/iptv/app/MovieInfoActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

search = r'private fun playMovie\(url: String\) \{'
replace = '''private fun playMovie(url: String) {
        if (RemoteManager.connectedTvIp != null) {
            RemoteManager.sendPlayCommand(this, "vod", url, movieTitle, streamId)
            return
        }'''
text = text.replace(search, replace)

with open('app/src/main/java/com/iptv/app/MovieInfoActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

# 2. LiveTvActivity (when clicking a channel to play)
with open('app/src/main/java/com/iptv/app/LiveTvActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# We need to find the place where it opens PlayerActivity.
search = r'val intent = Intent\(this@LiveTvActivity, PlayerActivity::class\.java\)\s*intent\.putExtra\("VIDEO_URL", url\)\s*intent\.putExtra\("TYPE", "live"\)[\s\S]*?startActivity\(intent\)'
replace = '''if (RemoteManager.connectedTvIp != null) {
                        RemoteManager.sendPlayCommand(this@LiveTvActivity, "live", url, channel.name, channel.stream_id)
                    } else {
                        val intent = Intent(this@LiveTvActivity, PlayerActivity::class.java)
                        intent.putExtra("VIDEO_URL", url)
                        intent.putExtra("TYPE", "live")
                        intent.putExtra("STREAM_ID", channel.stream_id)
                        intent.putExtra("USERNAME", username)
                        intent.putExtra("PASSWORD", password)
                        intent.putExtra("TITLE", channel.name)
                        intent.putExtra("COVER", channel.stream_icon)
                        startActivity(intent)
                    }'''
text = re.sub(search, replace, text)

with open('app/src/main/java/com/iptv/app/LiveTvActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
