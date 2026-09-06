package com.iptv.app

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Manages ByeDPI local VPN protection — same style as StreamVpnTunnel.
 */
object LocalProtection {

    private const val TAG = "LocalProtection"

    enum class State { OFF, CONNECTING, ACTIVE, ERROR }

    interface Listener {
        fun onStateChanged(state: State)
    }

    @Volatile var state: State = State.OFF
        private set

    private val listeners = mutableListOf<Listener>()

    fun addListener(l: Listener)    { synchronized(listeners) { listeners.add(l) } }
    fun removeListener(l: Listener) { synchronized(listeners) { listeners.remove(l) } }

    fun setState(s: State) {
        state = s
        Log.d(TAG, "State -> $s")
        synchronized(listeners) { listeners.toList() }.forEach { it.onStateChanged(s) }
    }

    fun start(ctx: Context) {
        if (state == State.ACTIVE || state == State.CONNECTING) return
        val intent = Intent(ctx, ByeDpiVpnService::class.java)
            .apply { action = ByeDpiVpnService.ACTION_START }
        ctx.startForegroundService(intent)
    }

    fun stop(ctx: Context) {
        val intent = Intent(ctx, ByeDpiVpnService::class.java)
            .apply { action = ByeDpiVpnService.ACTION_STOP }
        ctx.startService(intent)
    }

    val isActive: Boolean get() = state == State.ACTIVE
}
