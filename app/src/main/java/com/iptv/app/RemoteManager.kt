package com.iptv.app

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object RemoteManager {
    const val TCP_PORT = 9999
    const val UDP_PORT = 8888

    var connectedTvIp: String? = null
    private var isServerRunning = false

    // PIN de emparelhamento: gerado de novo sempre que a TV arranca o servidor.
    // Sem isto, qualquer dispositivo na mesma rede Wi-Fi conseguia pedir
    // SYNC_LOGIN e receber o username/password da conta IPTV sem qualquer
    // autenticacao — bastava conhecer o protocolo (que está no código-fonte
    // público). Agora a TV só responde com as credenciais se o pedido incluir
    // o PIN correto, mostrado no ecrã da TV para o utilizador copiar para o
    // telemóvel.
    private var pairingPin: String = ""

    // =============== TV SIDE (RECEIVER) ===============
    fun startTvServer(context: Context, username: String, password: String, onPinReady: (String) -> Unit = {}) {
        if (isServerRunning) return
        isServerRunning = true

        pairingPin = (100000..999999).random().toString()
        onPinReady(pairingPin)

        // 1. Start UDP Listener (for discovery)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val udpSocket = DatagramSocket(UDP_PORT)
                val buffer = ByteArray(1024)
                while (isServerRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    if (message == "IPTV_DISCOVER") {
                        val reply = "IPTV_TV_HERE".toByteArray()
                        val replyPacket = DatagramPacket(reply, reply.size, packet.address, packet.port)
                        udpSocket.send(replyPacket)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Start TCP Listener (for commands)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val serverSocket = ServerSocket(TCP_PORT)
                while (isServerRunning) {
                    val client = serverSocket.accept()
                    val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                    val out = PrintWriter(client.getOutputStream(), true)
                    val line = reader.readLine()
                    if (line != null) {
                        val json = JSONObject(line)
                        if (json.optString("action") == "SYNC_LOGIN") {
                            // Só devolve as credenciais se o pedido souber o PIN
                            // mostrado no ecrã da TV — impede que qualquer
                            // dispositivo na rede local peça e receba a conta.
                            if (json.optString("pin") == pairingPin && pairingPin.isNotEmpty()) {
                                val resp = JSONObject()
                                resp.put("username", username)
                                resp.put("password", password)
                                out.println(resp.toString())
                            } else {
                                val resp = JSONObject()
                                resp.put("error", "invalid_pin")
                                out.println(resp.toString())
                            }
                        } else {
                            handleCommand(context, line, username, password)
                        }
                    }
                    client.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleCommand(context: Context, jsonStr: String, user: String, pass: String) {
        try {
            val json = JSONObject(jsonStr)
            val action = json.optString("action")
            if (action == "PLAY") {
                val type = json.optString("type")
                val url = json.optString("url")
                val title = json.optString("title")
                val streamId = json.optString("streamId")
                
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "A reproduzir via Telemóvel: ", Toast.LENGTH_LONG).show()
                    val intent = Intent(context, PlayerActivity::class.java)
                    intent.putExtra("VIDEO_URL", url)
                    intent.putExtra("TYPE", type)
                    intent.putExtra("STREAM_ID", streamId)
                    intent.putExtra("USERNAME", user)
                    intent.putExtra("PASSWORD", pass)
                    intent.putExtra("TITLE", title)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // =============== PHONE SIDE (SENDER) ===============
    fun discoverTv(context: Context, onFound: (String) -> Unit, onTimeout: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val udpSocket = DatagramSocket()
                udpSocket.soTimeout = 3000
                udpSocket.broadcast = true
                
                val message = "IPTV_DISCOVER".toByteArray()
                val packet = DatagramPacket(message, message.size, getBroadcastAddress(context), UDP_PORT)
                udpSocket.send(packet)
                
                val buffer = ByteArray(1024)
                val responsePacket = DatagramPacket(buffer, buffer.size)
                
                // Wait for response
                udpSocket.receive(responsePacket)
                val responseStr = String(responsePacket.data, 0, responsePacket.length)
                
                if (responseStr == "IPTV_TV_HERE") {
                    connectedTvIp = responsePacket.address.hostAddress
                    withContext(Dispatchers.Main) { onFound(connectedTvIp!!) }
                }
                udpSocket.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onTimeout() }
            }
        }
    }

    // Nota: WifiManager.dhcpInfo está deprecated desde a API 26, mas continua a
    // funcionar em todas as versões suportadas (minSdk 24). A alternativa
    // (LinkProperties via ConnectivityManager) exigiria testar descoberta de
    // TV na rede em dispositivos reais antes de trocar.
    @Suppress("DEPRECATION")
    private fun getBroadcastAddress(context: Context): InetAddress {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val dhcp = wifi.dhcpInfo
        val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
        val quads = ByteArray(4)
        for (k in 0..3) {
            quads[k] = (broadcast shr (k * 8) and 0xFF).toByte()
        }
        return InetAddress.getByAddress(quads)
    }

    fun sendPlayCommand(context: Context, type: String, url: String, title: String, streamId: String) {
        val ip = connectedTvIp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, TCP_PORT), 2000)
                val out = PrintWriter(socket.getOutputStream(), true)
                val json = JSONObject()
                json.put("action", "PLAY")
                json.put("type", type)
                json.put("url", url)
                json.put("title", title)
                json.put("streamId", streamId)
                out.println(json.toString())
                socket.close()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Enviado para a TV!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro a comunicar com a TV.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // O 'pin' tem de corresponder ao código mostrado no ecrã da TV (ver
    // startTvServer/onPinReady) — sem ele, a TV recusa devolver as credenciais.
    fun fetchLoginFromTv(context: Context, pin: String, onSuccess: (String, String) -> Unit, onError: () -> Unit) {
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
                        json.put("pin", pin)
                        out.println(json.toString())

                        val responseLine = reader.readLine()
                        socket.close()

                        if (responseLine != null) {
                            val respJson = JSONObject(responseLine)
                            if (respJson.has("error")) {
                                withContext(Dispatchers.Main) { onError() }
                                return@launch
                            }
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
}
