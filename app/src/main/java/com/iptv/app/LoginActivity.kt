package com.iptv.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        OkHttpProvider.init(applicationContext)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnPair = findViewById<Button>(R.id.btnSyncTv)
        btnPair.visibility = android.view.View.VISIBLE
        btnPair.text = "🔗 Tenho um Código"
        btnPair.setOnClickListener { showEnterCodeDialog() }

        // Verificar atualizações OTA mesmo no ecrã de login
        UpdateManager.checkForUpdates(this)

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val savedUser = prefs.getString("USERNAME", "") ?: ""
        val savedPass = prefs.getString("PASSWORD", "") ?: ""

        // Aplicar estado inicial da VPN antes de tentar login
        val isVpnEnabled = prefs.getBoolean("VPN_ENABLED", false)
        if (isVpnEnabled) {
            OkHttpProvider.enableDoH()
        } else {
            OkHttpProvider.disableDoH()
        }

        if (savedUser.isNotEmpty() && savedPass.isNotEmpty()) {
            etUsername.setText(savedUser)
            etPassword.setText(savedPass)
            performLogin(savedUser, savedPass, isAutoLogin = true)
        }

        btnLogin.setOnClickListener {
            val u = etUsername.text.toString().trim()
            val p = etPassword.text.toString().trim()

            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha o Usuário e a Senha.", Toast.LENGTH_SHORT).show()
            } else {
                performLogin(u, p, isAutoLogin = false)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pairingJob?.cancel()
    }

    private var pairingJob: kotlinx.coroutines.Job? = null

    /**
     * Ecrã inicial (sem sessão) para introduzir o código gerado noutro
     * dispositivo já com a conta ligada (ver MainActivity.showQrDialog /
     * SettingsActivity "Gerar Código"). Ao confirmar, busca as credenciais
     * associadas ao código e faz login automaticamente, sem o utilizador
     * escrever nada.
     */
    private fun showEnterCodeDialog() {
        val codeInput = EditText(this)
        codeInput.hint = "Código de 6 dígitos"
        codeInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        codeInput.gravity = android.view.Gravity.CENTER
        codeInput.textSize = 26f

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(60, 20, 60, 10)
        layout.addView(codeInput)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Entrar com Código")
            .setMessage("Introduza o código gerado noutro dispositivo (ecrã inicial > ícone de partilha/QR, ou Definições > Gerar Código):")
            .setView(layout)
            .setPositiveButton("Entrar", null)
            .setNegativeButton("Cancelar") { _, _ -> pairingJob?.cancel() }
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val code = codeInput.text.toString().trim()
                if (code.length != 6) {
                    Toast.makeText(this, "Código inválido.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                codeInput.isEnabled = false
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false

                pairingJob = CoroutineScope(Dispatchers.Main).launch {
                    var attempts = 0
                    val maxAttempts = 5
                    while (attempts < maxAttempts) {
                        val result = PairingManager.pollOnce(code)
                        if (result.expired) {
                            Toast.makeText(this@LoginActivity, "Código inválido ou expirado.", Toast.LENGTH_LONG).show()
                            codeInput.isEnabled = true
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            return@launch
                        }
                        if (result.credentials != null) {
                            dialog.dismiss()
                            findViewById<EditText>(R.id.etUsername).setText(result.credentials.username)
                            findViewById<EditText>(R.id.etPassword).setText(result.credentials.password)
                            performLogin(result.credentials.username, result.credentials.password, isAutoLogin = true)
                            return@launch
                        }
                        attempts++
                        kotlinx.coroutines.delay(1000)
                    }
                    Toast.makeText(this@LoginActivity, "Código ainda não confirmado. Tente novamente.", Toast.LENGTH_LONG).show()
                    codeInput.isEnabled = true
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                }
            }
        }
        dialog.show()
    }

    private fun performLogin(usernameInput: String, passwordInput: String, isAutoLogin: Boolean) {
        val username = usernameInput.trim()
        val password = passwordInput.trim()
        
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        btnLogin.isEnabled = false
        btnLogin.text = "AGUARDE..."
        progressBar.visibility = android.view.View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Login vai sempre direto à origem (não pelo túnel/relay) para passar o IP lock
                // do fornecedor, que costuma bloquear pedidos vindos de IPs de datacenter/VPS
                OkHttpProvider.disableDoH()

                val apiUrl = "${Constants.ORIGIN_URL}/player_api.php?username=$username&password=$password"
                val request = Request.Builder().url(apiUrl).build()
                var responseBody = ""
                
                try {
                    val response = OkHttpProvider.client.newCall(request).execute()
                    if (response.isSuccessful) {
                        responseBody = response.body?.string() ?: ""
                    }
                } catch (e: Exception) {}

                if (!responseBody.contains("user_info")) {
                    try {
                        val fallbackReq = Request.Builder().url(apiUrl).build()
                        val fallbackResp = OkHttpProvider.client.newCall(fallbackReq).execute()
                        if (fallbackResp.isSuccessful) {
                            val fBody = fallbackResp.body?.string() ?: ""
                            if (fBody.contains("user_info")) {
                                responseBody = fBody
                            }
                        }
                    } catch (e: Exception) {}
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    if (responseBody.contains("user_info")) {
                        if (!isAutoLogin) {
                            Toast.makeText(this@LoginActivity, "Login Aprovado!", Toast.LENGTH_LONG).show()
                        }
                        
                        var expDateFormated = "Indefinido"
                        try {
                            val jsonObject = org.json.JSONObject(responseBody)
                            val userInfo = jsonObject.getJSONObject("user_info")
                            val expDateString = userInfo.getString("exp_date")
                            val timestamp = expDateString.toLong() * 1000
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            expDateFormated = sdf.format(java.util.Date(timestamp))
                        } catch (e: Exception) {}

                        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
                        // Nota: Túnel TLS fica desligado por padrão — o relay privado está
                        // atualmente a ser bloqueado pela Cloudflare do fornecedor de origem
                        // (IP de datacenter). Reativar manualmente nas Definições assim que
                        // o IP do relay for autorizado junto do fornecedor.
                        prefs.edit()
                            .putString("USERNAME", username)
                            .putString("PASSWORD", password)
                            .putString("EXP_DATE", expDateFormated)
                            .apply()

                        OkHttpProvider.disableDoH()

                        // Guardar a conta na Lista Multi-Utilizador
                        AccountsManager.saveAccount(
                            this@LoginActivity,
                            SavedAccount(username, password, expDateFormated, System.currentTimeMillis())
                        )

                        val target = if (DeviceUtils.isTv(this@LoginActivity)) MainActivity::class.java else MobileMainActivity::class.java
                        val intent = Intent(this@LoginActivity, target)
                        intent.putExtra("VENCIMENTO", expDateFormated)
                        intent.putExtra("USERNAME", username)
                        intent.putExtra("PASSWORD", password)
                        
                        SyncManager.syncFromCloud(this@LoginActivity) {
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        btnLogin.isEnabled = true
                        btnLogin.text = "ENTRAR"
                        Toast.makeText(this@LoginActivity, "Usuário ou senha incorretos.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = android.view.View.GONE
                    btnLogin.isEnabled = true
                    btnLogin.text = "ENTRAR"
                    Toast.makeText(this@LoginActivity, "Erro de conexão", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
