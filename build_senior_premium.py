import os

# Create colors and backgrounds
os.makedirs('app/src/main/res/color', exist_ok=True)
os.makedirs('app/src/main/res/drawable', exist_ok=True)

with open('app/src/main/res/color/color_senior_menu_text.xml', 'w', encoding='utf-8') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="#000000" android:state_focused="true" />
    <item android:color="#FFFFFF" />
</selector>
''')

with open('app/src/main/res/drawable/bg_senior_menu_item.xml', 'w', encoding='utf-8') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="#FBC02D" />
            <corners android:radius="8dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@android:color/transparent" />
            <corners android:radius="8dp" />
        </shape>
    </item>
</selector>
''')

with open('app/src/main/res/drawable/bg_senior_card.xml', 'w', encoding='utf-8') as f:
    f.write('''<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="#252834" />
            <stroke android:width="3dp" android:color="#FBC02D" />
            <corners android:radius="12dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="#252834" />
            <stroke android:width="1dp" android:color="#3A3D49" />
            <corners android:radius="12dp" />
        </shape>
    </item>
</selector>
''')

# Create XML Layout
xml_layout = '''<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="horizontal"
    android:background="#15171E">

    <!-- Left Sidebar -->
    <LinearLayout
        android:layout_width="220dp"
        android:layout_height="match_parent"
        android:background="#1A1C23"
        android:orientation="vertical"
        android:padding="24dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="📺 Modo Fácil"
            android:textColor="#FBC02D"
            android:textSize="20sp"
            android:textStyle="bold"
            android:layout_marginBottom="48dp" />

        <TextView
            android:id="@+id/btnSeniorTv"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_senior_menu_item"
            android:textColor="@color/color_senior_menu_text"
            android:text="TV ao vivo"
            android:textSize="18sp"
            android:textStyle="bold"
            android:padding="12dp"
            android:layout_marginBottom="8dp"
            android:focusable="true"
            android:clickable="true" />

        <TextView
            android:id="@+id/btnSeniorFilmes"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_senior_menu_item"
            android:textColor="@color/color_senior_menu_text"
            android:text="Filmes"
            android:textSize="18sp"
            android:textStyle="bold"
            android:padding="12dp"
            android:layout_marginBottom="8dp"
            android:focusable="true"
            android:clickable="true" />

        <TextView
            android:id="@+id/btnSeniorSeries"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_senior_menu_item"
            android:textColor="@color/color_senior_menu_text"
            android:text="Séries"
            android:textSize="18sp"
            android:textStyle="bold"
            android:padding="12dp"
            android:layout_marginBottom="8dp"
            android:focusable="true"
            android:clickable="true" />

        <TextView
            android:id="@+id/btnSeniorFavoritos"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_senior_menu_item"
            android:textColor="@color/color_senior_menu_text"
            android:text="Favoritos"
            android:textSize="18sp"
            android:textStyle="bold"
            android:padding="12dp"
            android:focusable="true"
            android:clickable="true" />

        <View
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1" />

        <TextView
            android:id="@+id/btnSairSenior"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_senior_menu_item"
            android:textColor="@color/color_senior_menu_text"
            android:text="Sair do Modo"
            android:textSize="16sp"
            android:padding="12dp"
            android:focusable="true"
            android:clickable="true" />
    </LinearLayout>

    <!-- Main Content -->
    <RelativeLayout
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:padding="32dp">

        <!-- Top Bar -->
        <TextView
            android:id="@+id/tvSectionTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Início"
            android:textColor="#FFFFFF"
            android:textSize="28sp"
            android:textStyle="bold"
            android:layout_alignParentStart="true"
            android:layout_alignParentTop="true" />

        <TextView
            android:id="@+id/tvClockSenior"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="20:15"
            android:textColor="#FFFFFF"
            android:textSize="22sp"
            android:textStyle="bold"
            android:layout_alignParentEnd="true"
            android:layout_alignParentTop="true" />

        <!-- Big Highlight Card -->
        <LinearLayout
            android:id="@+id/btnSeniorTvBig"
            android:layout_width="match_parent"
            android:layout_height="220dp"
            android:layout_below="@id/tvSectionTitle"
            android:layout_marginTop="32dp"
            android:background="@drawable/bg_senior_card"
            android:orientation="vertical"
            android:padding="24dp"
            android:focusable="true"
            android:clickable="true">
            
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="• DESTAQUE"
                android:textColor="#FF5252"
                android:textSize="14sp"
                android:textStyle="bold" />
                
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Aceder à Televisão"
                android:textColor="#FFFFFF"
                android:textSize="32sp"
                android:textStyle="bold"
                android:layout_marginTop="8dp" />
                
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Assista aos seus canais favoritos em direto."
                android:textColor="#9E9E9E"
                android:textSize="18sp"
                android:layout_marginTop="4dp" />
                
            <View
                android:layout_width="match_parent"
                android:layout_height="0dp"
                android:layout_weight="1" />
                
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="▶  Assistir agora"
                android:textColor="#000000"
                android:background="#FBC02D"
                android:paddingHorizontal="16dp"
                android:paddingVertical="8dp"
                android:textStyle="bold"
                android:textSize="16sp" />
        </LinearLayout>

        <!-- Category Row -->
        <TextView
            android:id="@+id/tvShortcutsTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_below="@id/btnSeniorTvBig"
            android:layout_marginTop="24dp"
            android:text="Atalhos rápidos"
            android:textColor="#FFFFFF"
            android:textSize="20sp"
            android:textStyle="bold" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="120dp"
            android:layout_below="@id/tvShortcutsTitle"
            android:layout_marginTop="16dp"
            android:orientation="horizontal"
            android:weightSum="3">

            <LinearLayout
                android:id="@+id/btnSeniorFilmesBig"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:layout_marginEnd="16dp"
                android:background="@drawable/bg_senior_card"
                android:orientation="vertical"
                android:padding="16dp"
                android:focusable="true"
                android:clickable="true">
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="🎬"
                    android:textSize="32sp" />
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Filmes"
                    android:textColor="#FFFFFF"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:layout_marginTop="8dp" />
            </LinearLayout>

            <LinearLayout
                android:id="@+id/btnSeniorSeriesBig"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:layout_marginEnd="16dp"
                android:background="@drawable/bg_senior_card"
                android:orientation="vertical"
                android:padding="16dp"
                android:focusable="true"
                android:clickable="true">
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="📽️"
                    android:textSize="32sp" />
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Séries"
                    android:textColor="#FFFFFF"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:layout_marginTop="8dp" />
            </LinearLayout>

            <LinearLayout
                android:id="@+id/btnSeniorFavoritosBig"
                android:layout_width="0dp"
                android:layout_height="match_parent"
                android:layout_weight="1"
                android:background="@drawable/bg_senior_card"
                android:orientation="vertical"
                android:padding="16dp"
                android:focusable="true"
                android:clickable="true">
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="⭐"
                    android:textSize="32sp" />
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Favoritos"
                    android:textColor="#FFFFFF"
                    android:textSize="18sp"
                    android:textStyle="bold"
                    android:layout_marginTop="8dp" />
            </LinearLayout>

        </LinearLayout>

        <!-- Footer -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_alignParentBottom="true"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:background="#0F1116"
            android:padding="12dp">
            
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="▲ ▼ ◀ ▶"
                android:textColor="#FFFFFF"
                android:textStyle="bold"
                android:textSize="14sp" />
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text=" escolher    "
                android:textColor="#9E9E9E"
                android:textSize="14sp" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="OK"
                android:background="#333"
                android:paddingHorizontal="6dp"
                android:textColor="#FFFFFF"
                android:textStyle="bold"
                android:textSize="14sp" />
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text=" assistir    "
                android:textColor="#9E9E9E"
                android:textSize="14sp" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="VOLTAR"
                android:background="#333"
                android:paddingHorizontal="6dp"
                android:textColor="#FFFFFF"
                android:textStyle="bold"
                android:textSize="14sp" />
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text=" sair"
                android:textColor="#9E9E9E"
                android:textSize="14sp" />
        </LinearLayout>

    </RelativeLayout>
</LinearLayout>
'''

with open('app/src/main/res/layout/activity_senior_main.xml', 'w', encoding='utf-8') as f:
    f.write(xml_layout)

# Update Kotlin class to attach the "Big" buttons too
kotlin_code = '''package com.iptv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SeniorMainActivity : AppCompatActivity() {
    private var clockJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_senior_main)

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val username = intent.getStringExtra("USERNAME") ?: prefs.getString("USERNAME", "") ?: ""
        val password = intent.getStringExtra("PASSWORD") ?: prefs.getString("PASSWORD", "") ?: ""

        RemoteManager.startTvServer(this, username, password)

        val openTv = View.OnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            intent.putExtra("TYPE", "live")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        val openMovies = View.OnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            intent.putExtra("TYPE", "vod")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        val openSeries = View.OnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            intent.putExtra("TYPE", "series")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        val openFavorites = View.OnClickListener {
            val intent = Intent(this, LiveTvActivity::class.java)
            intent.putExtra("CATEGORY_ID", "favorites")
            intent.putExtra("TYPE", "live")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSeniorTv).setOnClickListener(openTv)
        findViewById<View>(R.id.btnSeniorTvBig).setOnClickListener(openTv)
        
        findViewById<View>(R.id.btnSeniorFilmes).setOnClickListener(openMovies)
        findViewById<View>(R.id.btnSeniorFilmesBig).setOnClickListener(openMovies)
        
        findViewById<View>(R.id.btnSeniorSeries).setOnClickListener(openSeries)
        findViewById<View>(R.id.btnSeniorSeriesBig).setOnClickListener(openSeries)
        
        findViewById<View>(R.id.btnSeniorFavoritos).setOnClickListener(openFavorites)
        findViewById<View>(R.id.btnSeniorFavoritosBig).setOnClickListener(openFavorites)

        findViewById<View>(R.id.btnSairSenior).setOnClickListener {
            prefs.edit().putBoolean("is_senior_mode", false).apply()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            finish()
        }

        val tvClock = findViewById<TextView>(R.id.tvClockSenior)
        clockJob = CoroutineScope(Dispatchers.Default).launch {
            while(true) {
                // "20:15 Quinta, 3 de setembro" format roughly
                val sdf = SimpleDateFormat("HH:mm  EEEE, d 'de' MMMM", Locale("pt", "PT"))
                val time = sdf.format(Date())
                withContext(Dispatchers.Main) {
                    tvClock?.text = time.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
                delay(1000)
            }
        }
        
        // Request initial focus
        findViewById<View>(R.id.btnSeniorTvBig).requestFocus()
    }
    
    override fun onBackPressed() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Sair")
        builder.setMessage("Tem a certeza que deseja fechar a aplicação?")
        builder.setPositiveButton("Sim") { _, _ -> finishAffinity() }
        builder.setNegativeButton("Não", null)
        builder.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockJob?.cancel()
    }
}
'''
with open('app/src/main/java/com/iptv/app/SeniorMainActivity.kt', 'w', encoding='utf-8') as f:
    f.write(kotlin_code)

print("Premium Senior Mode Applied")
