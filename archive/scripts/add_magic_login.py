# -*- coding: utf-8 -*-
import re

# 1. Update RemoteManager.kt
with open('app/src/main/java/com/iptv/app/RemoteManager.kt', 'r', encoding='utf-8') as f:
    remote_kt = f.read()

# Update the TCP Listener to reply to SYNC_LOGIN
tcp_search = r'''val reader = BufferedReader\(InputStreamReader\(client\.getInputStream\(\)\)\)\s*val line = reader\.readLine\(\)\s*if \(line != null\) \{\s*handleCommand\(context, line, username, password\)\s*\}'''
tcp_replace = '''val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val out = PrintWriter(client.getOutputStream(), true)
                    val line = reader.readLine()
                    if (line != null) {
                        val json = JSONObject(line)
                        if (json.optString("action") == "SYNC_LOGIN") {
                            val resp = JSONObject()
                            resp.put("username", username)
                            resp.put("password", password)
                            out.println(resp.toString())
                        } else {
                            handleCommand(context, line, username, password)
                        }
                    }'''
remote_kt = re.sub(tcp_search, tcp_replace, remote_kt)

# Add fetchLoginFromTv function
fetch_login_code = '''
    fun fetchLoginFromTv(context: Context, onSuccess: (String, String) -> Unit, onError: () -> Unit) {
        discoverTv(context, 
            onFound = { ip ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val socket = Socket()
                        socket.connect(InetSocketAddress(ip, TCP_PORT), 3000)
                        val out = PrintWriter(socket.getOutputStream(), true)
                        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                        
                        val json = JSONObject()
                        json.put("action", "SYNC_LOGIN")
                        out.println(json.toString())
                        
                        val responseLine = reader.readLine()
                        socket.close()
                        
                        if (responseLine != null) {
                            val respJson = JSONObject(responseLine)
                            val user = respJson.optString("username")
                            val pass = respJson.optString("password")
                            withContext(Dispatchers.Main) { onSuccess(user, pass) }
                        } else {
                            withContext(Dispatchers.Main) { onError() }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onError() }
                    }
                }
            },
            onTimeout = { onError() }
        )
    }
}'''
remote_kt = remote_kt.replace('}\n}', '}\n' + fetch_login_code)

with open('app/src/main/java/com/iptv/app/RemoteManager.kt', 'w', encoding='utf-8') as f:
    f.write(remote_kt)

# 2. Update activity_login.xml
with open('app/src/main/res/layout/activity_login.xml', 'r', encoding='utf-8') as f:
    login_xml = f.read()

sync_btn = '''
            <Button
                android:id="@+id/btnSyncTv"
                android:layout_width="match_parent"
                android:layout_height="48dp"
                android:layout_marginTop="16dp"
                android:background="@drawable/bg_smarters_sage"
                android:text="📡 Puxar Login da TV (Automático)"
                android:textColor="#00E5FF"
                android:textStyle="bold" />
            
            <ProgressBar'''
login_xml = login_xml.replace('<ProgressBar', sync_btn, 1)

with open('app/src/main/res/layout/activity_login.xml', 'w', encoding='utf-8') as f:
    f.write(login_xml)

# 3. Update LoginActivity.kt
with open('app/src/main/java/com/iptv/app/LoginActivity.kt', 'r', encoding='utf-8') as f:
    login_kt = f.read()

btn_logic = '''
        val btnSync = findViewById<Button>(R.id.btnSyncTv)
        // Só mostrar o botão de sincronizar se estiver num telemóvel
        if (DeviceUtils.isTv(this)) {
            btnSync.visibility = View.GONE
        } else {
            btnSync.setOnClickListener {
                btnSync.text = "A procurar TV..."
                RemoteManager.fetchLoginFromTv(this,
                    onSuccess = { u, p ->
                        Toast.makeText(this, "Login Sincronizado!", Toast.LENGTH_SHORT).show()
                        etUsername.setText(u)
                        etPassword.setText(p)
                        btnSync.text = "📡 Sincronizado com Sucesso!"
                        // Pode fazer auto-login a seguir se quiser, clicando no botão entrar
                        btnEntrar.performClick()
                    },
                    onError = {
                        Toast.makeText(this, "TV não encontrada. Certifique-se que a app está aberta na TV.", Toast.LENGTH_LONG).show()
                        btnSync.text = "📡 Puxar Login da TV (Automático)"
                    }
                )
            }
        }
        
        btnEntrar.setOnClickListener {
'''
login_kt = login_kt.replace('btnEntrar.setOnClickListener {', btn_logic, 1)

with open('app/src/main/java/com/iptv/app/LoginActivity.kt', 'w', encoding='utf-8') as f:
    f.write(login_kt)

print("Done")
