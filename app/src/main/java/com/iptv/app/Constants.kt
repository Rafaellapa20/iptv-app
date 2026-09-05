package com.iptv.app

import android.content.Context

object Constants {
    const val ORIGIN_URL = "http://nelitoplay.top:80"
    
    // Relay TLS privado padrǜo (nginx + Let's Encrypt, reencaminha para o servidor de origem)
    const val DEFAULT_TUNNEL_HOST = "rafaiptv2026.duckdns.org"
    const val DEFAULT_TUNNEL_PORT = 443

    // Backend StreamVPN (VPS PTisp) — gestão de VPN, quota, servidores.
    // Alterável em runtime no ecrã StreamVPN ("Servidor API").
    const val STREAMVPN_BASE_URL = "http://streamvpn.faktio.ch:3000/api"

    // Nota: o emparelhamento TV <-> telemóvel (PairingManager) usava aqui um
    // servidor próprio (rafaiptv2026.duckdns.org:9443) que deixou de existir
    // (domínio morto). Passou a usar o Firestore do mesmo projeto Firebase
    // do sync — ver PairingManager.kt.

    // Forçar a ligação direta (sem VPN) porque o servidor foi desligado.
    val SERVER_URL: String
        get() = ORIGIN_URL
}
