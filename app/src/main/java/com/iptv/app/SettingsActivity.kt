package com.iptv.app

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
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

        
        val prefsExp = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
        val expDate = prefsExp.getString("EXP_DATE", "Ilimitado") ?: "Ilimitado"
        val tvValidade = findViewById<TextView>(R.id.tvValidade)
        tvValidade.text = "Validade da Conta: " + expDate

        val tvAppVersion = findViewById<TextView>(R.id.tvAppVersion)
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            tvAppVersion.text = "Versão atual: $version"
        } catch (e: Exception) {
            tvAppVersion.text = "Versão: Desconhecida"
        }

        var secretClickCount = 0
        tvAppVersion.setOnClickListener {
            secretClickCount++
            if (secretClickCount >= 5) {
                secretClickCount = 0
                showCustomProxyDialog()
            }
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

        


        
        val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            Prefs.clearCredentials(this)    // limpa também o armazenamento cifrado + syncId

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

        findViewById<Button>(R.id.btnStreamVpn).setOnClickListener {
            startActivity(android.content.Intent(this, VpnStatusActivity::class.java))
        }

        // Seletor de modo do túnel VPN
        val modeLabels = listOf(
            "Automático",
            "Só proteção local (ByeDPI)",
            "Sempre VPN (WireGuard)",
            "Desligado"
        )
        val modeValues = listOf(
            VpnManager.Mode.AUTO,
            VpnManager.Mode.LOCAL_ONLY,
            VpnManager.Mode.WIREGUARD,
            VpnManager.Mode.OFF
        )
        val spinnerVpnMode = findViewById<Spinner>(R.id.spinnerVpnMode)
        val modeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modeLabels)
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVpnMode.adapter = modeAdapter
        val currentMode = VpnManager.getMode(this)
        spinnerVpnMode.setSelection(modeValues.indexOf(currentMode).coerceAtLeast(0))
        spinnerVpnMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                VpnManager.setMode(this@SettingsActivity, modeValues[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Seletor de rota de vídeo (Auto / Direto / Relay)
        val routeLabels = listOf("Automático", "Direto", "Relay (via servidor)")
        val routeValues = listOf("auto", "direct", "relay")
        val spinnerVideoRoute = findViewById<Spinner>(R.id.spinnerVideoRoute)
        val routeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routeLabels)
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVideoRoute.adapter = routeAdapter
        val currentRoute = VideoRouting.getOverride(this)
        spinnerVideoRoute.setSelection(routeValues.indexOf(currentRoute).coerceAtLeast(0))
        spinnerVideoRoute.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                VideoRouting.setOverride(this@SettingsActivity, routeValues[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val btnPairing = findViewById<Button>(R.id.btnPairing)
        btnPairing.setOnClickListener {
            showGenerateCodeDialog()
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

    /**
     * Gera um código que serve duas finalidades: um novo dispositivo pode
     * usá-lo para descarregar a app (link/QR) e, ao introduzi-lo no ecrã de
     * login, entrar automaticamente nesta conta sem escrever a senha.
     */
    private fun showGenerateCodeDialog() {
        val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
        val username = prefs.getString("USERNAME", "") ?: ""
        val password = prefs.getString("PASSWORD", "") ?: ""
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Sem sessão iniciada.", Toast.LENGTH_LONG).show()
            return
        }

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.gravity = android.view.Gravity.CENTER
        layout.setPadding(50, 30, 50, 10)

        val apkLink = android.widget.TextView(this)
        apkLink.text = "Download: https://tinyurl.com/2985xryp"
        apkLink.textSize = 12f
        apkLink.setTextColor(android.graphics.Color.parseColor("#8A99AD"))
        apkLink.gravity = android.view.Gravity.CENTER
        apkLink.setPadding(0, 0, 0, 20)
        layout.addView(apkLink)

        val tvCode = android.widget.TextView(this)
        tvCode.text = "A gerar..."
        tvCode.textSize = 36f
        tvCode.setTextColor(android.graphics.Color.parseColor("#00E5FF"))
        tvCode.gravity = android.view.Gravity.CENTER
        layout.addView(tvCode)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Gerar Código de Acesso")
            .setMessage("Instale a app no novo dispositivo e, no ecrã de login, escolha \"Tenho um código\" e introduza este número (válido 30 min):")
            .setView(layout)
            .setPositiveButton("Fechar", null)
            .show()

        CoroutineScope(Dispatchers.Main).launch {
            val code = PairingManager.generateSelfCode(this@SettingsActivity, username, password)
            tvCode.text = code ?: "Erro"
        }
    }

    private fun showCustomProxyDialog() {
        val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
        val currentHost = prefs.getString("PROXY_HOST", Constants.DEFAULT_TUNNEL_HOST) ?: Constants.DEFAULT_TUNNEL_HOST
        val currentPort = prefs.getInt("PROXY_PORT", Constants.DEFAULT_TUNNEL_PORT)

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val ipInput = android.widget.EditText(this)
        ipInput.hint = "IP do Servidor"
        ipInput.setText(currentHost)
        layout.addView(ipInput)

        val portInput = android.widget.EditText(this)
        portInput.hint = "Porta (ex: 8443)"
        portInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        portInput.setText(currentPort.toString())
        layout.addView(portInput)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Configuração do Túnel TLS (Servidor Privado)")
            .setMessage("Introduza o domínio/IP e porta HTTPS do seu servidor (precisa de certificado TLS válido a reencaminhar para o servidor de conteúdo):")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val newIp = ipInput.text.toString().trim()
                val newPort = portInput.text.toString().toIntOrNull() ?: 8443
                if (newIp.isNotEmpty()) {
                    prefs.edit()
                        .putString("PROXY_HOST", newIp)
                        .putInt("PROXY_PORT", newPort)
                        .apply()
                    OkHttpProvider.updateProxy(newIp, newPort)
                    Toast.makeText(this, "Novo servidor guardado com sucesso!", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Repor Padrão") { _, _ ->
                prefs.edit()
                    .putString("PROXY_HOST", Constants.DEFAULT_TUNNEL_HOST)
                    .putInt("PROXY_PORT", Constants.DEFAULT_TUNNEL_PORT)
                    .apply()
                OkHttpProvider.updateProxy(Constants.DEFAULT_TUNNEL_HOST, Constants.DEFAULT_TUNNEL_PORT)
                Toast.makeText(this, "Servidor original restaurado!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}

