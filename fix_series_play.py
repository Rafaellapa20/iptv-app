# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/SeriesInfoActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re
search = r'val url = "[^"]+"\s*val intent = Intent\(this@SeriesInfoActivity, PlayerActivity::class\.java\)\s*intent\.putExtra\("VIDEO_URL", url\)\s*intent\.putExtra\("TYPE", "series"\)[\s\S]*?startActivity\(intent\)'
replace = '''val url = "/series///."
                    val epTitle = " - "
                    if (RemoteManager.connectedTvIp != null) {
                        RemoteManager.sendPlayCommand(this@SeriesInfoActivity, "series", url, epTitle, ep.id)
                    } else {
                        val intent = Intent(this@SeriesInfoActivity, PlayerActivity::class.java)
                        intent.putExtra("VIDEO_URL", url)
                        intent.putExtra("TYPE", "series")
                        intent.putExtra("STREAM_ID", ep.id)
                        intent.putExtra("USERNAME", username)
                        intent.putExtra("PASSWORD", password)
                        intent.putExtra("TITLE", epTitle)
                        intent.putExtra("COVER", coverUrl)
                        startActivity(intent)
                    }'''
text = re.sub(search, replace, text)

with open('app/src/main/java/com/iptv/app/SeriesInfoActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
