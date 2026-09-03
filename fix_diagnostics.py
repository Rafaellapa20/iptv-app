# -*- coding: utf-8 -*-
with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    text = f.read()

import re

# Add tvDiagnostics
text = text.replace('private lateinit var tvLoadingTitle: TextView', 'private lateinit var tvLoadingTitle: TextView\n    private lateinit var tvDiagnostics: TextView')
text = text.replace('tvLoadingTitle = findViewById(R.id.tvLoadingTitle)', 'tvLoadingTitle = findViewById(R.id.tvLoadingTitle)\n        tvDiagnostics = findViewById(R.id.tvDiagnostics)')

# Add diagnostic logic
diag_code = '''
    private var diagnosticJob: kotlinx.coroutines.Job? = null

    private fun startDiagnostics() {
        diagnosticJob?.cancel()
        tvDiagnostics.visibility = View.VISIBLE
        tvDiagnostics.text = "A analisar ligação..."
        
        diagnosticJob = CoroutineScope(Dispatchers.IO).launch {
            delay(5000) // Wait 5s before declaring an issue
            
            val isGoogleReachable = try {
                val ping = Runtime.getRuntime().exec("ping -c 1 8.8.8.8")
                ping.waitFor() == 0
            } catch (e: Exception) { false }

            withContext(Dispatchers.Main) {
                if (!isGoogleReachable) {
                    tvDiagnostics.text = "⚠️ Diagnóstico: A sua ligação à Internet (Wi-Fi/Cabo) caiu ou está muito lenta!"
                } else {
                    tvDiagnostics.text = "⚠️ Diagnóstico: A sua Internet está boa. O servidor IPTV está com lentidão neste canal específico."
                }
            }
        }
    }

    private fun stopDiagnostics() {
        diagnosticJob?.cancel()
        runOnUiThread {
            tvDiagnostics.visibility = View.GONE
        }
    }
'''

if 'startDiagnostics' not in text:
    text = text.replace('private fun updateNetworkStatus', diag_code + '\n    private fun updateNetworkStatus')

# Call start/stop
text = text.replace('tvNetworkSpeed.setTextColor(android.graphics.Color.YELLOW)', 'tvNetworkSpeed.setTextColor(android.graphics.Color.YELLOW)\n                startDiagnostics()')
text = text.replace('tvNetworkSpeed.setTextColor(android.graphics.Color.GREEN)', 'tvNetworkSpeed.setTextColor(android.graphics.Color.GREEN)\n                stopDiagnostics()')

# Add error diagnostic
err_search = 'android.util.Log.e("PlayerActivity", "Playback error: ")'
err_replace = '''android.util.Log.e("PlayerActivity", "Playback error: ")
                runOnUiThread {
                    rlBufferingOverlay.visibility = View.VISIBLE
                    tvDiagnostics.visibility = View.VISIBLE
                    tvDiagnostics.text = "⚠️ Erro de Transmissão: O canal está offline no servidor."
                }'''
text = text.replace(err_search, err_replace)

with open('app/src/main/java/com/iptv/app/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.write(text)
print("Done")
