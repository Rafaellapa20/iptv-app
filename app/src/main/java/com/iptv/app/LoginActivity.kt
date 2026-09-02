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

        val prefs = getSharedPreferences("IPTV_PREFS", Context.MODE_PRIVATE)
        val savedUser = prefs.getString("USERNAME", "") ?: ""
        val savedPass = prefs.getString("PASSWORD", "") ?: ""

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
                val apiUrl = "${Constants.SERVER_URL}/player_api.php?username=$username&password=$password"
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
                        prefs.edit().putString("USERNAME", username).putString("PASSWORD", password).apply()

                        // Guardar a conta na Lista Multi-Utilizador
                        AccountsManager.saveAccount(
                            this@LoginActivity,
                            SavedAccount(username, password, expDateFormated, System.currentTimeMillis())
                        )

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.putExtra("VENCIMENTO", expDateFormated)
                        intent.putExtra("USERNAME", username)
                        intent.putExtra("PASSWORD", password)
                        startActivity(intent)
                        finish()
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
