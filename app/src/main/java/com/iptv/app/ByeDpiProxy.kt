package com.iptv.app

/**
 * JNI wrapper for libbyedpi.so.
 * Runs a SOCKS5 proxy on 127.0.0.1:1080 that fragments TLS to bypass DPI.
 */
object ByeDpiProxy {

    init {
        System.loadLibrary("byedpi")
    }

    // Creates the listening socket, returns fd or -1
    external fun jniCreateSocketWithCommandLine(args: Array<String>): Int

    // Blocks running the event loop — call on a background thread
    external fun jniStartProxy(fd: Int): Int

    // Signals stop
    external fun jniStopProxy(fd: Int): Int
}
