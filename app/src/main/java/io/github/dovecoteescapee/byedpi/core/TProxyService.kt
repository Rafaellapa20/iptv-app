package io.github.dovecoteescapee.byedpi.core

/**
 * JNI wrapper for the pre-built libhev-socks5-tunnel.so.
 * Creates a TUN -> SOCKS5 bridge: takes the VPN TUN fd and routes all
 * traffic through the ByeDPI SOCKS5 proxy.
 */
object TProxyService {
    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    external fun TProxyStartService(configPath: String, fd: Int): Boolean
    external fun TProxyStopService(): Boolean
    external fun TProxyIsRunning(): Boolean
}
