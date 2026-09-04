import re

code = '''package com.iptv.app

import android.content.Context

object Constants {
    const val ORIGIN_URL = "http://nelitoplay.top:80"
    
    // Relay TLS privado padrǜo (nginx + Let's Encrypt, reencaminha para o servidor de origem)
    const val DEFAULT_TUNNEL_HOST = "rafaiptv2026.duckdns.org"
    const val DEFAULT_TUNNEL_PORT = 443

    // Forçar a ligação direta (sem VPN) porque o servidor foi desligado.
    val SERVER_URL: String
        get() = ORIGIN_URL
}
'''

with open('app/src/main/java/com/iptv/app/Constants.kt', 'w', encoding='utf-8') as f:
    f.write(code)

print("Fixed Constants.kt")
