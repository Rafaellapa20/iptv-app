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
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.SocketAddress
import java.nio.channels.SocketChannel
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory

/**
 * Escreve o primeiro payload (o TLS ClientHello) partido em dois envios TCP
 * separados por um pequeno atraso, em vez de um único write(). Muitos
 * sistemas de DPI/WAF (incluindo bloqueios "anti-VPN" como o que vimos no
 * Cloudflare do fornecedor) analisam o ClientHello à procura de padrões
 * (SNI, extensões, ordem de cifras) assumindo que chega inteiro num único
 * segmento TCP. Fragmentá-lo é uma técnica pública e amplamente documentada
 * de contorno de censura (usada por ferramentas como GoodbyeDPI/ByeDPI,
 * método "split"); esta é uma implementação própria, escrita de raiz.
 */
private class FragmentingOutputStream(private val out: OutputStream) : OutputStream() {
    private var firstWriteDone = false

    override fun write(b: Int) {
        out.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (!firstWriteDone && len > 8) {
            firstWriteDone = true
            try {
                val splitAt = 1 + (0 until minOf(4, len - 1)).random()
                out.write(b, off, splitAt)
                out.flush()
                try { Thread.sleep(2) } catch (e: InterruptedException) {}
                out.write(b, off + splitAt, len - splitAt)
                return
            } catch (e: Exception) {
                // Se algo correr mal a fragmentar, cai para o envio normal.
            }
        }
        out.write(b, off, len)
    }

    override fun flush() = out.flush()
    override fun close() = out.close()
}

/** Socket que delega tudo para um socket real já ligado, exceto o
 *  OutputStream, que é envolvido em [FragmentingOutputStream]. */
private class AntiDpiSocket(private val delegate: Socket) : Socket() {
    override fun connect(endpoint: SocketAddress?) = delegate.connect(endpoint)
    override fun connect(endpoint: SocketAddress?, timeout: Int) = delegate.connect(endpoint, timeout)
    override fun bind(bindpoint: SocketAddress?) = delegate.bind(bindpoint)
    override fun getInetAddress(): InetAddress = delegate.inetAddress
    override fun getLocalAddress(): InetAddress = delegate.localAddress
    override fun getPort(): Int = delegate.port
    override fun getLocalPort(): Int = delegate.localPort
    override fun getRemoteSocketAddress(): SocketAddress = delegate.remoteSocketAddress
    override fun getLocalSocketAddress(): SocketAddress = delegate.localSocketAddress
    override fun getChannel(): SocketChannel? = delegate.channel
    override fun getInputStream(): InputStream = delegate.getInputStream()
    override fun getOutputStream(): OutputStream = FragmentingOutputStream(delegate.getOutputStream())
    override fun setTcpNoDelay(on: Boolean) = delegate.setTcpNoDelay(on)
    override fun getTcpNoDelay(): Boolean = delegate.tcpNoDelay
    override fun setSoLinger(on: Boolean, linger: Int) = delegate.setSoLinger(on, linger)
    override fun getSoLinger(): Int = delegate.soLinger
    override fun sendUrgentData(data: Int) = delegate.sendUrgentData(data)
    override fun setOOBInline(on: Boolean) = delegate.setOOBInline(on)
    override fun getOOBInline(): Boolean = delegate.oobInline
    override fun setSoTimeout(timeout: Int) = delegate.setSoTimeout(timeout)
    override fun getSoTimeout(): Int = delegate.soTimeout
    override fun setSendBufferSize(size: Int) = delegate.setSendBufferSize(size)
    override fun getSendBufferSize(): Int = delegate.sendBufferSize
    override fun setReceiveBufferSize(size: Int) = delegate.setReceiveBufferSize(size)
    override fun getReceiveBufferSize(): Int = delegate.receiveBufferSize
    override fun setKeepAlive(on: Boolean) = delegate.setKeepAlive(on)
    override fun getKeepAlive(): Boolean = delegate.keepAlive
    override fun setTrafficClass(tc: Int) = delegate.setTrafficClass(tc)
    override fun getTrafficClass(): Int = delegate.trafficClass
    override fun setReuseAddress(on: Boolean) = delegate.setReuseAddress(on)
    override fun getReuseAddress(): Boolean = delegate.reuseAddress
    override fun close() = delegate.close()
    override fun shutdownInput() = delegate.shutdownInput()
    override fun shutdownOutput() = delegate.shutdownOutput()
    override fun toString(): String = delegate.toString()
    override fun isConnected(): Boolean = delegate.isConnected
    override fun isBound(): Boolean = delegate.isBound
    override fun isClosed(): Boolean = delegate.isClosed
    override fun isInputShutdown(): Boolean = delegate.isInputShutdown
    override fun isOutputShutdown(): Boolean = delegate.isOutputShutdown
    override fun setPerformancePreferences(connectionTime: Int, latency: Int, bandwidth: Int) =
        delegate.setPerformancePreferences(connectionTime, latency, bandwidth)
}

/** Igual ao FastSocketFactory mas SEM fragmentacao do primeiro write:
 *  para media nao ha ClientHello a esconder e os 2 ms de sleep por
 *  ligacao pagam-se em latencia no primeiro fotograma. */
class MediaSocketFactory : SocketFactory() {
    private val delegate: SocketFactory = SocketFactory.getDefault()

    private fun tune(socket: Socket): Socket {
        try {
            socket.receiveBufferSize = 4 * 1024 * 1024
            socket.sendBufferSize = 512 * 1024
            socket.tcpNoDelay = true
            socket.keepAlive = true
        } catch (e: Exception) {}
        return socket
    }

    override fun createSocket(): Socket = tune(delegate.createSocket())
    override fun createSocket(host: String, port: Int): Socket = tune(delegate.createSocket(host, port))
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
        tune(delegate.createSocket(host, port, localHost, localPort))
    override fun createSocket(host: InetAddress, port: Int): Socket = tune(delegate.createSocket(host, port))
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
        tune(delegate.createSocket(address, port, localAddress, localPort))
}

class FastSocketFactory : SocketFactory() {
    private val delegate = SocketFactory.getDefault()

    private fun setupSocket(socket: Socket): Socket {
        try {
            socket.receiveBufferSize = 2 * 1024 * 1024 // 2MB
            socket.sendBufferSize = 2 * 1024 * 1024
            socket.tcpNoDelay = true
        } catch (e: Exception) {}
        return try { AntiDpiSocket(socket) } catch (e: Exception) { socket }
    }

    override fun createSocket(): Socket = setupSocket(delegate.createSocket())
    override fun createSocket(host: String, port: Int): Socket = setupSocket(delegate.createSocket(host, port))
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket = setupSocket(delegate.createSocket(host, port, localHost, localPort))
    override fun createSocket(host: InetAddress, port: Int): Socket = setupSocket(delegate.createSocket(host, port))
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket = setupSocket(delegate.createSocket(address, port, localAddress, localPort))
}

object OkHttpProvider {

    private const val BROWSER_USER_AGENT = "IPTVSmartersPlayer/3.0.9 (Linux; Android 10)"

    // Nota: o "túnel" já não usa Proxy.Type.HTTP (isso enviava os pedidos em texto
    // simples, legível por DPI da operadora mesmo indo para o servidor privado).
    // Agora o destino é decidido em Constants.SERVER_URL, que aponta diretamente
    // (via HTTPS) para o servidor relay quando o túnel está ativo. Aqui só
    // guardamos host/porta para forçar o rebuild do client quando o utilizador
    // muda a configuração em SettingsActivity.
    fun updateProxy(host: String, port: Int) {
        client = buildClient()
    }

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

        // Resolvedores por ordem de preferência "de fábrica". Guardamos o índice
        // do último que teve sucesso e tentamos esse primeiro da próxima vez —
        // evita perder tempo a tentar sempre um DNS que já se sabe estar morto
        // nesta rede (ex.: operadora que bloqueia o DNS do sistema). Cada
        // resolvedor recebe o hostname como parâmetro (nada de estado partilhado
        // entre threads, já que lookup() pode ser chamado em paralelo pelo OkHttp).
        private val resolverNames = listOf("sistema", "cloudflare", "google", "quad9")

        private fun resolveWith(index: Int, hostname: String): List<InetAddress> = when (index) {
            0 -> okhttp3.Dns.SYSTEM.lookup(hostname)
            1 -> cloudflareDns.lookup(hostname)
            2 -> googleDns.lookup(hostname)
            3 -> quad9Dns.lookup(hostname)
            else -> emptyList()
        }

        @Volatile private var lastWorkingIndex = 0

        override fun lookup(hostname: String): List<InetAddress> {
            // Lê o índice preferido uma única vez no início — se outra thread
            // o alterar entretanto, esta chamada simplesmente não beneficia
            // dessa atualização (sem problema, não há estado partilhado a corromper).
            val preferred = lastWorkingIndex
            val order = listOf(preferred) + (resolverNames.indices - preferred)

            for (index in order) {
                try {
                    val results = resolveWith(index, hostname)
                    if (results.isNotEmpty()) {
                        if (index != preferred) {
                            android.util.Log.i("TripleArmorDns", "A mudar para DNS '${resolverNames[index]}' como preferido")
                        }
                        lastWorkingIndex = index
                        return results
                    }
                } catch (e: Exception) {}
            }

            // Nenhum resolvedor respondeu — última tentativa desesperada com o sistema.
            return okhttp3.Dns.SYSTEM.lookup(hostname)
        }
    }

    private val safeDns by lazy { TripleArmorDns(bootstrapClient) }

    // "useProxy" = túnel TLS até ao servidor relay privado ativo (ver Constants.SERVER_URL)
    private var useProxy = true

    fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .socketFactory(FastSocketFactory())
            .connectionPool(connectionPool)
            .addInterceptor(userAgentInterceptor)

        if (useProxy) {
            // DNS seguro: resolve o domínio do relay por DoH, contornando bloqueios
            // de DNS da operadora sobre esse domínio.
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

    // ── Media client (PlayerFactory) ─────────────────────────────────────────
    // Pool próprio: ligações de media não competem com chamadas de API.
    private val mediaPool = ConnectionPool(6, 5, TimeUnit.MINUTES)

    private val mediaUa = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Connection", "keep-alive")
                .build()
        )
    }

    private val mediaClientInstance: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // SEM .cache(): segmentos de vídeo não entram na cache HTTP.
            .connectTimeout(6, TimeUnit.SECONDS)   // falhar depressa → tentar outra fonte
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)      // 0 = sem limite: live não termina
            .retryOnConnectionFailure(true)
            .socketFactory(MediaSocketFactory())
            .connectionPool(mediaPool)
            .addInterceptor(mediaUa)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /** Usado só pelo PlayerFactory — sem proxy, sem cache, buffers grandes. */
    fun mediaClient(): OkHttpClient = mediaClientInstance

    /** Se o relay for necessário também para vídeo nesta rede, usa o cliente normal. */
    fun mediaClientVia(relay: Boolean): OkHttpClient =
        if (relay) client else mediaClientInstance
}
