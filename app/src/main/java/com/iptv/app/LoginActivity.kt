package com.iptv.app

import android.content.Context
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

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnTogglePassword = findViewById<android.widget.ImageView>(R.id.btnTogglePassword)

        var isPasswordVisible = false
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnTogglePassword.alpha = 1.0f // Highlight icon
            } else {
                etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnTogglePassword.alpha = 0.5f // Dim icon
            }
            etPassword.setSelection(etPassword.text.length)
        }
        btnTogglePassword.alpha = 0.5f // Initially dimmed

        // Verifica Auto-Login e Configura DNS
        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        
        // Inicializa o DNS Seguro se estiver ativado (padrão é true)
        val isVpnEnabled = prefs.getBoolean("VPN_ENABLED", true)
        if (isVpnEnabled) {
            OkHttpProvider.enableDoH()
        } else {
            OkHttpProvider.disableDoH()
        }

        val savedUser = prefs.getString("USERNAME", null)
        val savedPass = prefs.getString("PASSWORD", null)

        if (savedUser != null && savedPass != null) {
            etUsername.setText(savedUser)
            etPassword.setText(savedPass)
            performLogin(savedUser, savedPass, isAutoLogin = true)
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                performLogin(username, password, isAutoLogin = false)
            } else {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin(username: String, password: String, isAutoLogin: Boolean) {
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        btnLogin.isEnabled = false
        btnLogin.text = "AGUARDE..."
        progressBar.visibility = android.view.View.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiUrl = "http://nelitoplay.top:80/player_api.php?username=$username&password=$password"
                val request = Request.Builder().url(apiUrl).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = android.view.View.GONE
                        if (responseBody.contains("user_info")) {
                            if (!isAutoLogin) {
                                Toast.makeText(this@LoginActivity, "Login Aprovado!", Toast.LENGTH_LONG).show()
                            }
                            
                            // Salva no SharedPreferences
                            val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
                            prefs.edit().putString("USERNAME", username).putString("PASSWORD", password).apply()
                            
                            var expDateFormated = "Indefinido"
                            try {
                                val jsonObject = org.json.JSONObject(responseBody)
                                val userInfo = jsonObject.getJSONObject("user_info")
                                val expDateString = userInfo.getString("exp_date")
                                val timestamp = expDateString.toLong() * 1000
                                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                expDateFormated = sdf.format(java.util.Date(timestamp))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            val intent = android.content.Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("VENCIMENTO", expDateFormated)
                            intent.putExtra("USERNAME", username)
                            intent.putExtra("PASSWORD", password)
                            startActivity(intent)
                            finish()
                        } else {
                            btnLogin.isEnabled = true
                            btnLogin.text = "ENTRAR"
                            if (isAutoLogin) {
                                val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
                                prefs.edit().clear().apply()
                            }
                            Toast.makeText(this@LoginActivity, "Usuário ou senha incorretos.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = android.view.View.GONE
                        btnLogin.isEnabled = true
                        btnLogin.text = "ENTRAR"
                        Toast.makeText(this@LoginActivity, "Erro no servidor: ${response.code}", Toast.LENGTH_LONG).show()
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
