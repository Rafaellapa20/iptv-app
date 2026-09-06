package com.iptv.app

import android.content.Context

object Constants {
    const val ORIGIN_URL = "http://nelitoplay.top:80"
    
    // Relay TLS (nginx na 8446 + Let's Encrypt, reencaminha para o servidor de origem)
    const val DEFAULT_TUNNEL_HOST = "relay.faktio.ch"
    const val DEFAULT_TUNNEL_PORT = 8446

    // Backend StreamVPN (VPS PTisp) — gestão de VPN, quota, servidores.
    // HTTPS via Nginx + Let's Encrypt na porta 8444 (a 443 está ocupada por
    // outro serviço no VPS; o backend Node em si só fala HTTP na 3000,
    // que já não está exposta ao exterior).
    // Alterável em runtime no ecrã StreamVPN ("Servidor API").
    const val STREAMVPN_BASE_URL = "https://streamvpn.faktio.ch:8444/api"

    // Nota: o emparelhamento TV <-> telemóvel (PairingManager) usava aqui um
    // servidor próprio que deixou de existir. Passou a usar o Firestore do
    // mesmo projeto Firebase do sync — ver PairingManager.kt.

    // URL base para canais IPTV — via relay nginx (relay.faktio.ch:8446 → nelitoplay.top:80)
    val SERVER_URL: String
        get() = "https://$DEFAULT_TUNNEL_HOST:$DEFAULT_TUNNEL_PORT"
}
