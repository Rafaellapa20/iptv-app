import re

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    code = f.read()

# For Movies
code = code.replace(
'''                                if (icon.isNotEmpty()) {
                                    list.add(Stream(streamId, name, icon, "movie", "mp4") to added)
                                }''',
'''                                if (icon.isNotEmpty()) {
                                    list.add(Stream(streamId, name, icon, "movie", "mp4") to added)
                                }
                                if (list.size > 2000) break // Speed up parsing by limiting to 2000 covers
'''
)

# For Series
code = code.replace(
'''                                if (icon.isNotEmpty()) {
                                    recentSeries.add(Stream(seriesId, name, icon, "series", ""))
                                }''',
'''                                if (icon.isNotEmpty()) {
                                    recentSeries.add(Stream(seriesId, name, icon, "series", ""))
                                }
                                if (recentSeries.size > 2000) break // Speed up parsing by limiting to 2000 covers
'''
)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(code)

print("Limited JsonReader items")
