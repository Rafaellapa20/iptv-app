# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

# We will rewrite fetchMoviesPosters to use manual lightweight parsing or JsonReader.
# To avoid missing OkHttp timeout, we will use a custom client with larger timeout.

search = r'private fun fetchMoviesPosters\(username: String, password: String\) \{.*?catch \(e: Exception\) \{\}\s*\}\s*\}'

replace = '''private fun fetchMoviesPosters(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "/player_api.php?username=&password=&action=get_vod_streams"
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val inputStream = response.body?.byteStream()
                    if (inputStream != null) {
                        val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, "UTF-8"))
                        recentMovies.clear()
                        val list = mutableListOf<Pair<Stream, Long>>()
                        
                        try {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                var id = ""
                                var name = ""
                                var icon = ""
                                var addedStr = "0"
                                
                                while (reader.hasNext()) {
                                    val key = reader.nextName()
                                    if (reader.peek() == android.util.JsonToken.NULL) {
                                        reader.nextNull()
                                        continue
                                    }
                                    when (key) {
                                        "stream_id" -> id = reader.nextString()
                                        "name" -> name = reader.nextString()
                                        "stream_icon" -> icon = reader.nextString()
                                        "added" -> addedStr = reader.nextString()
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                
                                if (icon.isNotEmpty()) {
                                    val added = addedStr.toLongOrNull() ?: 0L
                                    list.add(Stream(id, name, icon, "movie", "mp4") to added)
                                }
                            }
                            reader.endArray()
                        } catch (e: Exception) {
                            // Stop parsing if malformed, keep what we have
                        } finally {
                            reader.close()
                        }
                        
                        list.sortByDescending { it.second }
                        recentMovies.addAll(list.map { it.first }.take(30))

                        withContext(Dispatchers.Main) {
                            startMoviesCardSlideshow()
                            setupFeaturedMovies(recentMovies)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }'''

text = re.sub(search, replace, text, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)

print("Done")
