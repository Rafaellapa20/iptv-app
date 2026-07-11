package com.iptv.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.File
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    private var cacheDir: File? = null
    private val appCache: Cache by lazy {
        Cache(File(cacheDir ?: File("."), "http_cache"), 100L * 1024L * 1024L) // 100MB Cache
    }
    
    fun init(context: Context) {
        cacheDir = context.cacheDir
        // Limpeza automática de cache antigo
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheFile = File(context.cacheDir, "http_cache")
                if (cacheFile.exists()) {
                    val lastModified = cacheFile.lastModified()
                    if (System.currentTimeMillis() - lastModified > 7L * 24 * 60 * 60 * 1000) {
                        cacheFile.deleteRecursively()
                    }
                }
            } catch (e: Exception) {}
        }
    }
    
    // Cliente Base para fazer as consultas DNS (precisa ser sem o DNS modificado para não dar loop infinito)
    private val bootstrapClient by lazy {
        OkHttpClient.Builder()
            .cache(appCache)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // Classe de DNS com Defesa Tripla (Cloudflare -> Google -> Quad9 -> Sistema)
    class TripleArmorDns(private val bootstrapClient: OkHttpClient) : okhttp3.Dns {
        private val cloudflareDns by lazy {
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
                .bootstrapDnsHosts(InetAddress.getByName("1.1.1.1"), InetAddress.getByName("1.0.0.1"))
                .build()
        }

        private val googleDns by lazy {
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://dns.google/dns-query".toHttpUrl())
                .bootstrapDnsHosts(InetAddress.getByName("8.8.8.8"), InetAddress.getByName("8.8.4.4"))
                .build()
        }

        private val quad9Dns by lazy {
            DnsOverHttps.Builder().client(bootstrapClient)
                .url("https://dns.quad9.net/dns-query".toHttpUrl())
                .bootstrapDnsHosts(InetAddress.getByName("9.9.9.9"), InetAddress.getByName("149.112.112.112"))
                .build()
        }

        override fun lookup(hostname: String): List<InetAddress> {
            try {
                val results = cloudflareDns.lookup(hostname)
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {
                android.util.Log.e("TripleArmorDns", "Cloudflare falhou, pulando para Google...")
            }

            try {
                val results = googleDns.lookup(hostname)
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {
                android.util.Log.e("TripleArmorDns", "Google falhou, pulando para Quad9...")
            }

            try {
                val results = quad9Dns.lookup(hostname)
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {
                android.util.Log.e("TripleArmorDns", "Quad9 falhou, voltando para DNS Original do Sistema...")
            }

            // Fallback final: DNS da operadora
            return okhttp3.Dns.SYSTEM.lookup(hostname)
        }
    }

    private val safeDns by lazy { TripleArmorDns(bootstrapClient) }

    var client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .dns(safeDns)
        .build()

    fun enableDoH() {
        client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .dns(safeDns)
            .build()
    }

    fun disableDoH() {
        client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
