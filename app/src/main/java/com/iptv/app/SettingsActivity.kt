package com.iptv.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val tvAppVersion = findViewById<TextView>(R.id.tvAppVersion)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            tvAppVersion.text = "Versão atual: $version"
        } catch (e: Exception) {
            tvAppVersion.text = "Versão: Desconhecida"
        }

        val btnClearCache = findViewById<Button>(R.id.btnClearCache)
        btnClearCache.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                Glide.get(this@SettingsActivity).clearDiskCache()
                runOnUiThread {
                    Glide.get(this@SettingsActivity).clearMemory()
                    Toast.makeText(this@SettingsActivity, "Memória limpa com sucesso!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val switchVpn = findViewById<Switch>(R.id.switchVpn)
        val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
        
        // Anti-Bloqueio vem ativado por padrão
        val isVpnEnabled = prefs.getBoolean("VPN_ENABLED", true)
        switchVpn.isChecked = isVpnEnabled

        switchVpn.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("VPN_ENABLED", isChecked).apply()
            VpnHelper.setEnabled(this, isChecked)
            if (isChecked) {
                Toast.makeText(this, "Anti-Bloqueio ATIVADO (DNS Seguro)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Anti-Bloqueio DESATIVADO", Toast.LENGTH_SHORT).show()
            }
        }


        
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            
            // Clear favorites
            getSharedPreferences("IPTV_FAVORITES", MODE_PRIVATE).edit().clear().apply()

            val intent = android.content.Intent(this, LoginActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val btnUpdateApp = findViewById<Button>(R.id.btnUpdateApp)
        btnUpdateApp.setOnClickListener {
            Toast.makeText(this, "A procurar atualizações...", Toast.LENGTH_SHORT).show()
            UpdateManager.checkForUpdates(this, showNoUpdateToast = true)
        }

        val btnSpeedTest = findViewById<Button>(R.id.btnSpeedTest)
        val tvSpeedResult = findViewById<TextView>(R.id.tvSpeedResult)

        btnSpeedTest.setOnClickListener {
            tvSpeedResult.visibility = View.VISIBLE
            tvSpeedResult.text = "A testar a sua internet... (Aguarde 5s)"
            tvSpeedResult.setTextColor(android.graphics.Color.parseColor("#00FFFF"))
            btnSpeedTest.isEnabled = false

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val request = okhttp3.Request.Builder()
                        .url("https://speed.cloudflare.com/__down?bytes=50000000") // 50MB
                        .build()

                    val response = OkHttpProvider.client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val inputStream = response.body?.byteStream()
                        val buffer = ByteArray(32768)
                        var bytesReadTotal = 0L
                        val startTime = System.currentTimeMillis()
                        if (inputStream != null) {
                            var read: Int
                            while (inputStream.read(buffer).also { read = it } != -1) {
                                bytesReadTotal += read
                            }
                            inputStream.close()
                        }
                        val endTime = System.currentTimeMillis()
                        
                        val timeTakenMs = endTime - startTime
                        val timeTakenSecs = timeTakenMs / 1000.0
                        
                        // Convert bytes to megabits: (bytes * 8) / 1,000,000
                        val megabits = (bytesReadTotal * 8.0) / 1000000.0
                        // Fix infinity/NaN if time is too short
                        val safeTimeSecs = if (timeTakenSecs < 0.1) 0.1 else timeTakenSecs
                        
                        val speedMbps = (megabits / safeTimeSecs).toInt()

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            btnSpeedTest.isEnabled = true
                            tvSpeedResult.text = "Velocidade: $speedMbps Megas (Mbps)\n" +
                                if (speedMbps > 25) {
                                    tvSpeedResult.setTextColor(android.graphics.Color.parseColor("#00FF00"))
                                    "🟢 Excelente! Sem travamentos."
                                } else if (speedMbps in 10..25) {
                                    tvSpeedResult.setTextColor(android.graphics.Color.parseColor("#FFFF00"))
                                    "🟡 Razoável. Pode haver pequenos delays."
                                } else {
                                    tvSpeedResult.setTextColor(android.graphics.Color.parseColor("#FF0000"))
                                    "🔴 Lenta! Sua internet causará travamentos."
                                }
                        }
                    } else {
                        throw Exception("Falha no download")
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        btnSpeedTest.isEnabled = true
                        tvSpeedResult.setTextColor(android.graphics.Color.parseColor("#FF0000"))
                        tvSpeedResult.text = "🔴 Erro no Teste. Verifique a sua conexão."
                    }
                }
            }
        }

        val btnOptimize = findViewById<Button>(R.id.btnOptimize)
        val tvOptimizeResult = findViewById<TextView>(R.id.tvOptimizeResult)

        btnOptimize.setOnClickListener {
            btnOptimize.isEnabled = false
            tvOptimizeResult.visibility = View.VISIBLE
            tvOptimizeResult.setTextColor(android.graphics.Color.parseColor("#00FFFF"))
            tvOptimizeResult.text = "A limpar lixo e libertar RAM... 0%"

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    com.bumptech.glide.Glide.get(this@SettingsActivity).clearDiskCache()
                    
                    kotlinx.coroutines.delay(800)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        tvOptimizeResult.text = "A acelerar processamento... 50%"
                    }
                    
                    val cacheDir = cacheDir
                    if (cacheDir != null && cacheDir.isDirectory) {
                        cacheDir.deleteRecursively()
                    }
                    
                    kotlinx.coroutines.delay(800)
                    
                    System.gc()
                    Runtime.getRuntime().gc()

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        com.bumptech.glide.Glide.get(this@SettingsActivity).clearMemory()
                        
                        btnOptimize.isEnabled = true
                        tvOptimizeResult.setTextColor(android.graphics.Color.parseColor("#00FF00"))
                        tvOptimizeResult.text = "🚀 100% Concluído!\nMemória RAM libertada. TV Box otimizada!"
                        Toast.makeText(this@SettingsActivity, "TV Box Otimizada com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        btnOptimize.isEnabled = true
                        tvOptimizeResult.text = "Erro ao otimizar. Tente reiniciar a TV."
                    }
                }
            }
        }
    }
}
