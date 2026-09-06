package com.iptv.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.OkHttpClient

/**
 * Decides whether video should go direct or via relay for the current network.
 *
 * Default: direct. If direct fails repeatedly, switches to relay for this
 * network and persists the choice. Can be overridden in Settings.
 */
object VideoRouting {

    private const val PREFS = "video_routing"
    private const val KEY_OVERRIDE = "override"   // "auto" | "direct" | "relay"
    private const val KEY_RELAY_NETS = "relay_nets" // comma-separated network fingerprints

    /** Call this to get the right OkHttpClient for video. */
    fun client(ctx: Context, relayFallback: Boolean = false): OkHttpClient {
        val override = prefs(ctx).getString(KEY_OVERRIDE, "auto") ?: "auto"
        val useRelay = when (override) {
            "relay" -> true
            "direct" -> false
            else -> relayFallback || networkNeedsRelay(ctx)
        }
        return OkHttpProvider.mediaClientVia(relay = useRelay)
    }

    /** Call after a successful relay fallback to remember this network. */
    fun markNetworkNeedsRelay(ctx: Context) {
        val fp = networkFingerprint(ctx) ?: return
        val prefs = prefs(ctx)
        val current = prefs.getString(KEY_RELAY_NETS, "") ?: ""
        val nets = current.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (nets.add(fp)) prefs.edit().putString(KEY_RELAY_NETS, nets.joinToString(",")).apply()
    }

    /** Call after a successful direct connection to forget relay requirement. */
    fun markNetworkDirect(ctx: Context) {
        val fp = networkFingerprint(ctx) ?: return
        val prefs = prefs(ctx)
        val current = prefs.getString(KEY_RELAY_NETS, "") ?: ""
        val nets = current.split(",").filter { it.isNotBlank() && it != fp }.toMutableSet()
        prefs.edit().putString(KEY_RELAY_NETS, nets.joinToString(",")).apply()
    }

    fun getOverride(ctx: Context): String =
        prefs(ctx).getString(KEY_OVERRIDE, "auto") ?: "auto"

    fun setOverride(ctx: Context, mode: String) {   // "auto" | "direct" | "relay"
        prefs(ctx).edit().putString(KEY_OVERRIDE, mode).apply()
    }

    // ── private ──────────────────────────────────────────────────────────────

    private fun networkNeedsRelay(ctx: Context): Boolean {
        val fp = networkFingerprint(ctx) ?: return false
        val saved = prefs(ctx).getString(KEY_RELAY_NETS, "") ?: ""
        return saved.split(",").any { it == fp }
    }

    private fun networkFingerprint(ctx: Context): String? {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(net) ?: return null
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi_${caps.networkSpecifier ?: "any"}"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cell_${caps.linkDownstreamBandwidthKbps}"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
            else -> "unknown"
        }
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
