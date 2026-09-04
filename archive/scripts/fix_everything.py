# -*- coding: utf-8 -*-
import re

# 1. FIX XML
with open('app/src/main/res/layout/activity_main.xml', 'r', encoding='utf-8') as f:
    xml = f.read()

# Hide validade
xml = xml.replace('android:id="@+id/tvVencimento"', 'android:id="@+id/tvVencimento"\n            android:visibility="gone"')

# Fix Quick Access Bar buttons (Rewrite the whole Quick Access Bar block)
start_idx = xml.find('<!-- QUICK ACCESS BAR -->')
end_idx = xml.find('<!-- FILMES EM DESTAQUE')
if start_idx != -1 and end_idx != -1:
    new_bar = '''<!-- QUICK ACCESS BAR -->
    <LinearLayout
        android:id="@+id/llQuickAccessBar"
        android:layout_width="match_parent"
        android:layout_height="40dp"
        android:orientation="horizontal"
        android:paddingHorizontal="20dp"
        android:layout_marginBottom="6dp"
        android:weightSum="5"
        android:descendantFocusability="afterDescendants"
        android:focusable="false">

        <Button
            android:id="@+id/btnQuickFavorites"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="⭐ Favoritos"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textAllCaps="false"
            android:focusable="true"
            android:clickable="true" />

        <Button
            android:id="@+id/btnQuickEpg"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="📺 Guia EPG"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textAllCaps="false"
            android:focusable="true"
            android:clickable="true" />

        <Button
            android:id="@+id/btnQuickSettings"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="🔧 Definições"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textAllCaps="false"
            android:focusable="true"
            android:clickable="true" />

        <Button
            android:id="@+id/btnQrCode"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:layout_marginEnd="6dp"
            android:background="@drawable/bg_smarters_sage"
            android:text="📱 Telemóvel"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textAllCaps="false"
            android:focusable="true"
            android:clickable="true" />

        <Button
            android:id="@+id/btnQuickSenior"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:background="#2E7D32"
            android:text="🟢 Modo Fácil"
            android:textColor="#FFFFFF"
            android:textSize="11sp"
            android:textStyle="bold"
            android:textAllCaps="false"
            android:focusable="true"
            android:clickable="true" />
    </LinearLayout>

    '''
    xml = xml[:start_idx] + new_bar + xml[end_idx:]

with open('app/src/main/res/layout/activity_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml)

# 2. FIX KT
with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'r', encoding='utf-8') as f:
    kt = f.read()

# Fix Button Listeners
kt = re.sub(r'findViewById<View>\(R\.id\.btnQuickCatchup\)\?\.setOnClickListener \{.*?\}', '', kt, flags=re.DOTALL)
kt = re.sub(r'findViewById<View>\(R\.id\.btnQuickRadios\)\?\.setOnClickListener \{.*?\}', '', kt, flags=re.DOTALL)
kt = re.sub(r'findViewById<View>\(R\.id\.btnQuickSenior\)\?\.setOnClickListener \{.*?\}', '', kt, flags=re.DOTALL)
kt = re.sub(r'findViewById<View>\(R\.id\.btnQrCode\)\?\.setOnClickListener \{.*?\}', '', kt, flags=re.DOTALL)
kt = re.sub(r'findViewById<View>\(R\.id\.btnQuickSettings\)\?\.setOnClickListener \{.*?\}', '', kt, flags=re.DOTALL)

listeners = '''
        findViewById<View>(R.id.btnQuickSettings)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<View>(R.id.btnQrCode)?.setOnClickListener { showQrDialog() }
        findViewById<View>(R.id.btnQuickSenior)?.setOnClickListener {
            val p = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
            p.edit().putBoolean("is_senior_mode", true).apply()
            val intent = Intent(this, SeniorMainActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            finish()
        }
'''
kt = kt.replace('findViewById<View>(R.id.btnQuickEpg)?.setOnClickListener {', listeners + '\n        findViewById<View>(R.id.btnQuickEpg)?.setOnClickListener {')

# Fix fetchMoviesPosters parsing (use basic json array to string to avoid reader crashes, just read string and parse)
movies_parser = '''private fun fetchMoviesPosters(username: String, password: String) {
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
                    val body = response.body?.string() ?: "[]"
                    val array = org.json.JSONArray(body)
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

                    withContext(Dispatchers.Main) {
                        startMoviesCardSlideshow()
                        setupFeaturedMovies(recentMovies)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }'''
kt = re.sub(r'private fun fetchMoviesPosters.*?\}\s*\}\s*\}', movies_parser, kt, flags=re.DOTALL)


# Fix fetchSeriesPosters
series_parser = '''private fun fetchSeriesPosters(username: String, password: String) {
        if (username.isEmpty() || password.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "/player_api.php?username=&password=&action=get_series"
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    val array = org.json.JSONArray(body)
                    recentSeries.clear()

                    val limit = if (array.length() > 300) 300 else array.length()
                    for (i in 0 until limit) {
                        val obj = array.getJSONObject(i)
                        val icon = obj.optString("cover", "")
                        if (icon.isNotEmpty()) {
                            recentSeries.add(Stream(obj.getString("series_id"), obj.getString("name"), icon, "series", ""))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        startSeriesCardSlideshow()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }'''
kt = re.sub(r'private fun fetchSeriesPosters\(username: String, password: String\) \{.*?catch \(e: Exception\) \{\}\s*\}\s*\}', series_parser, kt, flags=re.DOTALL)

with open('app/src/main/java/com/iptv/app/MainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kt)

print("Done")
