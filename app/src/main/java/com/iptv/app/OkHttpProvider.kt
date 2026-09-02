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

    private const val BROWSER_USER_AGENT = "IPTVSmartersPlayer/3.0.9 (Linux; Android 10)"

    // Proxy VPN Dedicado Privado (Hetzner Gigabit - 65.21.178.77:8443)
    private val vpsProxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("65.21.178.77", 8443))

    private val userAgentInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "*/*")
            .header("Connection", "keep-alive")
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
            .proxy(vpsProxy)
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

    private var useProxy = true

    fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(connectionPool)
            .addInterceptor(userAgentInterceptor)

        if (useProxy) {
            builder.proxy(vpsProxy)
            builder.dns(safeDns)
        }
        return builder.build()
    }

    // Cliente principal com VPN Dedicada Hetzner para 100% das chamadas (Login + Vídeos)
    var client: OkHttpClient = buildClient()

    var vpsClient: OkHttpClient = client

    fun enableDoH() {
        useProxy = true
        client = buildClient()
    }
    
    fun disableDoH() {
        useProxy = false
        client = buildClient()
    }
}
