package com.iptv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
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

        findViewById<View>(R.id.btnSeniorTv).setOnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            intent.putExtra("TYPE", "live")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSeniorFilmes).setOnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            intent.putExtra("TYPE", "vod")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSeniorSeries).setOnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            intent.putExtra("TYPE", "series")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSeniorFavoritos).setOnClickListener {
            val intent = Intent(this, LiveTvActivity::class.java)
            intent.putExtra("CATEGORY_ID", "favorites")
            intent.putExtra("TYPE", "live")
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSairSenior).setOnClickListener {
            prefs.edit().putBoolean("is_senior_mode", false).apply()
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
            finish()
        }

        // Clock
        val tvClock = findViewById<TextView>(R.id.tvClockSenior)
        clockJob = CoroutineScope(Dispatchers.Default).launch {
            while(true) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                withContext(Dispatchers.Main) {
                    tvClock?.text = time
                }
                delay(1000)
            }
        }
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
