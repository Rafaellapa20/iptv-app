# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/LiveTvActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

search = '''                        val idsList = ArrayList(currentList.map { it.stream_id })
                        val namesList = ArrayList(currentList.map { it.name })'''

replace = '''                        val idsList = ArrayList(currentList.map { it.stream_id })
                        val namesList = ArrayList(currentList.map { it.name })
                        val coversList = ArrayList(currentList.map { it.stream_icon })'''

text = text.replace(search, replace)

search2 = 'intent.putStringArrayListExtra("CHANNEL_NAMES", namesList)'
replace2 = 'intent.putStringArrayListExtra("CHANNEL_NAMES", namesList)\n                        intent.putStringArrayListExtra("CHANNEL_COVERS", coversList)'
text = text.replace(search2, replace2)

with open('app/src/main/java/com/iptv/app/LiveTvActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
