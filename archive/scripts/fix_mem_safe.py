# -*- coding: utf-8 -*-
import re

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

movies_parser = '''private fun fetchMoviesPosters(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_vod_streams"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream() ?: return@launch
                    val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, "UTF-8"))
                    recentMovies.clear()
                    
                    val list = mutableListOf<Pair<Stream, Long>>()
                    
                    try {
                        // Sometimes the API might return an object with error or no data. Check token.
                        if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                var streamId = ""
                                var name = ""
                                var icon = ""
                                var added = 0L
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    if (reader.peek() == android.util.JsonToken.NULL) {
                                        reader.skipValue()
                                        continue
                                    }
                                    when (key) {
                                        "stream_id" -> streamId = reader.nextString()
                                        "name" -> name = reader.nextString()
                                        "stream_icon" -> icon = reader.nextString()
                                        "added" -> added = reader.nextString().toLongOrNull() ?: 0L
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                if (icon.isNotEmpty()) {
                                    list.add(Stream(streamId, name, icon, "movie", "mp4") to added)
                                }
                            }
                            reader.endArray()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try { reader.close() } catch(e:Exception){}
                    }

                    list.sortByDescending { it.second }
                    recentMovies.addAll(list.map { it.first }.take(30))

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        startMoviesCardSlideshow()
                        setupFeaturedMovies(recentMovies)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }'''

series_parser = '''private fun fetchSeriesPosters(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_series"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream() ?: return@launch
                    val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, "UTF-8"))
                    recentSeries.clear()
                    
                    try {
                        if (reader.peek() == android.util.JsonToken.BEGIN_ARRAY) {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                var seriesId = ""
                                var name = ""
                                var icon = ""
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    if (reader.peek() == android.util.JsonToken.NULL) {
                                        reader.skipValue()
                                        continue
                                    }
                                    when (key) {
                                        "series_id" -> seriesId = reader.nextString()
                                        "name" -> name = reader.nextString()
                                        "cover" -> icon = reader.nextString()
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                if (icon.isNotEmpty()) {
                                    recentSeries.add(Stream(seriesId, name, icon, "series", ""))
                                }
                                if (recentSeries.size > 200) break // Limit to save memory
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try { reader.close() } catch(e:Exception){}
                    }

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        startSeriesCardSlideshow()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }'''

kt = re.sub(r'private fun fetchMoviesPosters\(.*?catch \(e: Exception\) \{\s*e\.printStackTrace\(\)\s*\}\s*\}\s*\}', movies_parser, kt, flags=re.DOTALL)
kt = re.sub(r'private fun fetchSeriesPosters\(.*?catch \(e: Exception\) \{\s*e\.printStackTrace\(\)\s*\}\s*\}\s*\}', series_parser, kt, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done mem safe parsing")
