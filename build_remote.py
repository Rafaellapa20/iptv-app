# -*- coding: utf-8 -*-
code = '''package com.iptv.app

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

    // =============== TV SIDE (RECEIVER) ===============
    fun startTvServer(context: Context, username: String, password: String) {
        if (isServerRunning) return
        isServerRunning = true

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
                    val line = reader.readLine()
                    if (line != null) {
                        handleCommand(context, line, username, password)
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
                    Toast.makeText(context, "A reproduzir via Telemóvel: \", Toast.LENGTH_LONG).show()
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
}
'''
with open('app/src/main/java/com/iptv/app/RemoteManager.kt', 'w', encoding='utf-8') as f:
    f.write(code)

print("Done")
