package com.iptv.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

object OkHttpProvider {

    private const val BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    // Proxy VPN Dedicado Privado (Hetzner Gigabit - 65.21.178.77:8443)
    private val vpsProxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("65.21.178.77", 8443))

    private val userAgentInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "pt-PT,pt;q=0.9,en-US;q=0.8,en;q=0.7")
            .header("Accept-Encoding", "gzip, deflate")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Sec-Fetch-User", "?1")
            .header("Cache-Control", "max-age=0")
        chain.proceed(requestBuilder.build())
    }

    private var cacheDir: File? = null
    private val appCache: Cache by lazy {
        Cache(File(cacheDir ?: File("."), "http_cache"), 100L * 1024L * 1024L)
    }

    private val connectionPool = ConnectionPool(10, 5, TimeUnit.MINUTES)
    
    fun init(context: Context) {
        cacheDir = context.cacheDir
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
    
    private val bootstrapClient by lazy {
        OkHttpClient.Builder()
            .cache(appCache)
            .addInterceptor(userAgentInterceptor)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(connectionPool)
            .build()
    }

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
                val results = okhttp3.Dns.SYSTEM.lookup(hostname)
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {}

            try {
                val results = cloudflareDns.lookup(hostname)
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {}

            try {
                val results = googleDns.lookup(hostname)
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {}

            try {
                val results = quad9Dns.lookup(hostname)
                if (results.isNotEmpty()) return results
            } catch (e: Exception) {}

            return okhttp3.Dns.SYSTEM.lookup(hostname)
        }
    }

    private val safeDns by lazy { TripleArmorDns(bootstrapClient) }

    // Cliente Principal para Login e APIs (Direct Streamer + TripleArmorDns + Chrome Headers)
    // Evita o bloqueio de IP do painel de login Xtream Codes
    var client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(connectionPool)
        .addInterceptor(userAgentInterceptor)
        .dns(safeDns)
        .build()

    // Cliente VPS VPN Dedicado (usado para streaming de alta velocidade sem cortes)
    var vpsClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(connectionPool)
        .addInterceptor(userAgentInterceptor)
        .proxy(vpsProxy)
        .dns(safeDns)
        .build()

    fun enableDoH() {}
    fun disableDoH() {}
}
