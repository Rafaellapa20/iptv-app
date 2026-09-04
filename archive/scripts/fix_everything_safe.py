# -*- coding: utf-8 -*-

with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

xml = xml.replace('android:id="@+id/tvVencimento"', 'android:id="@+id/tvVencimento"\n            android:visibility="gone"')

# Find Quick Access Bar Buttons and rewrite them:
# Instead of replacing everything, let's just replace the text of btnQuickRadios and btnQuickCatchup
xml = xml.replace('android:text="📱 Telemóvel (QR)"', 'android:text="📱 Emparelhar"')
xml = xml.replace('android:text="👴 Modo Idosos"', 'android:text="🟢 Modo Fácil"')
xml = xml.replace('android:text="🔄 Definições"', 'android:text="🟢 Modo Fácil"') # Just in case

# Actually, the user says the QR button doesn't work, let's make sure its ID is btnQuickRadios in MainActivity
# and Modo Facil is btnQuickCatchup

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# Fix the button listeners carefully
# 1. btnQuickRadios -> QR
# 2. btnQuickCatchup -> Senior Mode
# Wait, in the code, btnQuickCatchup might have two listeners.
# Let's replace the block with a single one.

import re
kt = re.sub(r'findViewById<View>\(R\.id\.btnQuickRadios\)\?\.setOnClickListener \{.*?\}', 'findViewById<View>(R.id.btnQuickRadios)?.setOnClickListener { showQrDialog() }', kt, flags=re.DOTALL)

kt = re.sub(r'findViewById<View>\(R\.id\.btnQuickCatchup\)\?\.setOnClickListener \{.*?\}', '''findViewById<View>(R.id.btnQuickCatchup)?.setOnClickListener {
            val p = getSharedPreferences("IPTV_PREFS", android.content.Context.MODE_PRIVATE)
            p.edit().putBoolean("is_senior_mode", true).apply()
            val intent = android.content.Intent(this, SeniorMainActivity::class.java)
            val username = intent.getStringExtra("USERNAME") ?: p.getString("USERNAME", "") ?: ""
            val password = intent.getStringExtra("PASSWORD") ?: p.getString("PASSWORD", "") ?: ""
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            finish()
        }''', kt, flags=re.DOTALL)


# Fix the movies parser to use JSONArray instead of JsonReader (JsonReader was crashing because response might be an object if empty)
# Actually, the crash might be OOM. Let's use JSONObject/JSONArray carefully.
movies_parser = '''private fun fetchMoviesPosters(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_vod_streams"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    var array = org.json.JSONArray()
                    if (body.trim().startsWith("[")) {
                        array = org.json.JSONArray(body)
                    }
                    recentMovies.clear()
                    
                    val list = mutableListOf<Pair<Stream, Long>>()
                    val limit = if (array.length() > 300) 300 else array.length()
                    for (i in 0 until limit) {
                        val obj = array.getJSONObject(i)
                        val icon = obj.optString("stream_icon", "")
                        val added = obj.optString("added", "0").toLongOrNull() ?: 0L
                        if (icon.isNotEmpty()) {
                            list.add(Stream(obj.getString("stream_id"), obj.getString("name"), icon, "movie", "mp4") to added)
                        }
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
kt = re.sub(r'private fun fetchMoviesPosters\(.*?catch \(e: Exception\) \{\}\s*\}\s*\}', movies_parser, kt, flags=re.DOTALL)

series_parser = '''private fun fetchSeriesPosters(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val url = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password&action=get_series"
                val request = okhttp3.Request.Builder().url(url).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    var array = org.json.JSONArray()
                    if (body.trim().startsWith("[")) {
                        array = org.json.JSONArray(body)
                    }
                    recentSeries.clear()
                    
                    val limit = if (array.length() > 300) 300 else array.length()
                    for (i in 0 until limit) {
                        val obj = array.getJSONObject(i)
                        val icon = obj.optString("cover", "")
                        if (icon.isNotEmpty()) {
                            recentSeries.add(Stream(obj.getString("series_id"), obj.getString("name"), icon, "series", ""))
                        }
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
kt = re.sub(r'private fun fetchSeriesPosters\(.*?catch \(e: Exception\) \{\}\s*\}\s*\}', series_parser, kt, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
