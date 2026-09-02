package com.iptv.app

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress

class SpeedTestActivity : AppCompatActivity() {

    private lateinit var tvSpeedValue: TextView
    private lateinit var tvSpeedStatus: TextView
    private lateinit var tvPing: TextView
    private lateinit var tvDownload: TextView
    private lateinit var tvServer: TextView
    private lateinit var tvQuality: TextView
    private lateinit var tvCountry: TextView
    private lateinit var tvPublicIp: TextView
    private lateinit var tvVpnStatus: TextView
    private lateinit var speedGauge: SpeedGaugeView
    private lateinit var btnStart: Button

    private var testJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speedtest)

        tvSpeedValue = findViewById(R.id.tvSpeedValue)
        tvSpeedStatus = findViewById(R.id.tvSpeedStatus)
        tvPing = findViewById(R.id.tvPing)
        tvDownload = findViewById(R.id.tvDownload)
        tvServer = findViewById(R.id.tvServer)
        tvQuality = findViewById(R.id.tvQuality)
        tvCountry = findViewById(R.id.tvCountry)
        tvPublicIp = findViewById(R.id.tvPublicIp)
        tvVpnStatus = findViewById(R.id.tvVpnStatus)
        speedGauge = findViewById(R.id.speedGaugeView)
        btnStart = findViewById(R.id.btnStartTest)

        tvServer.text = "nelitoplay.top"

        btnStart.setOnClickListener {
            startSpeedTest()
        }

        // Carregar info de IP/País imediatamente ao abrir
        fetchIpLocation()

        btnStart.requestFocus()
    }

    private fun fetchIpLocation() {
        tvCountry.text = "A verificar..."
        tvPublicIp.text = "A verificar..."
        tvVpnStatus.text = "A verificar..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Usar ip-api.com (gratuita, sem chave)
                val request = Request.Builder()
                    .url("http://ip-api.com/json/?fields=status,country,countryCode,city,regionName,isp,org,query")
                    .build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)

                    val ip = json.optString("query", "Desconhecido")
                    val country = json.optString("country", "Desconhecido")
                    val countryCode = json.optString("countryCode", "")
                    val city = json.optString("city", "")
                    val region = json.optString("regionName", "")
                    val isp = json.optString("isp", "")
                    val org = json.optString("org", "")

                    // Converter countryCode em emoji de bandeira
                    val flag = countryCodeToFlag(countryCode)

                    // Verificar se é VPN: PTisp, Hetzner, ou fora de PT
                    val isVpn = ip == "176.111.109.14" || ip == "65.21.178.77" ||
                        isp.contains("PTisp", ignoreCase = true) ||
                        isp.contains("Hetzner", ignoreCase = true) ||
                        org.contains("Hetzner", ignoreCase = true) ||
                        country != "Portugal"

                    withContext(Dispatchers.Main) {
                        tvPublicIp.text = ip

                        val locationText = if (city.isNotEmpty()) {
                            "$flag $city, $country"
                        } else {
                            "$flag $country"
                        }
                        tvCountry.text = locationText

                        if (isVpn) {
                            tvVpnStatus.text = "🟢 VPN ATIVA"
                            tvVpnStatus.setTextColor(android.graphics.Color.parseColor("#00FF88"))
                            tvCountry.setTextColor(android.graphics.Color.parseColor("#00E5FF"))
                        } else {
                            tvVpnStatus.text = "🔴 SEM VPN"
                            tvVpnStatus.setTextColor(android.graphics.Color.parseColor("#FF6600"))
                            tvCountry.setTextColor(android.graphics.Color.parseColor("#FFD600"))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvCountry.text = "Erro ao verificar"
                    tvPublicIp.text = "Sem acesso"
                    tvVpnStatus.text = "❌ Erro"
                    tvVpnStatus.setTextColor(android.graphics.Color.parseColor("#FF0040"))
                }
            }
        }
    }

    private fun countryCodeToFlag(countryCode: String): String {
        if (countryCode.length != 2) return "🌍"
        val first = Character.toChars(0x1F1E6 - 'A'.code + countryCode[0].uppercaseChar().code)
        val second = Character.toChars(0x1F1E6 - 'A'.code + countryCode[1].uppercaseChar().code)
        return String(first) + String(second)
    }

    private fun startSpeedTest() {
        testJob?.cancel()
        btnStart.isEnabled = false
        btnStart.text = "⏳ A TESTAR..."

        tvPing.text = "-- ms"
        tvDownload.text = "-- Mbps"
        tvQuality.text = "--"
        tvSpeedValue.text = "0.0"
        speedGauge.setSpeed(0f)

        // Refrescar localização
        fetchIpLocation()

        testJob = CoroutineScope(Dispatchers.IO).launch {
            // 1. PING TEST
            withContext(Dispatchers.Main) {
                tvSpeedStatus.text = "A medir latência (ping)..."
            }

            var pingMs = -1L
            try {
                val startPing = System.currentTimeMillis()
                val addr = InetAddress.getByName("nelitoplay.top")
                val reachable = addr.isReachable(5000)
                pingMs = System.currentTimeMillis() - startPing
                if (!reachable) {
                    val startHttp = System.currentTimeMillis()
                    val req = Request.Builder().url("${Constants.SERVER_URL}/player_api.php").head().build()
                    OkHttpProvider.client.newCall(req).execute().close()
                    pingMs = System.currentTimeMillis() - startHttp
                }
            } catch (e: Exception) {
                try {
                    val startHttp = System.currentTimeMillis()
                    val req = Request.Builder().url("${Constants.SERVER_URL}/player_api.php").head().build()
                    OkHttpProvider.client.newCall(req).execute().close()
                    pingMs = System.currentTimeMillis() - startHttp
                } catch (e2: Exception) {
                    pingMs = -1
                }
            }

            withContext(Dispatchers.Main) {
                if (pingMs > 0) {
                    tvPing.text = "$pingMs ms"
                    val pingColor = when {
                        pingMs < 50 -> "#00FF88"
                        pingMs < 150 -> "#FFD600"
                        else -> "#FF6600"
                    }
                    tvPing.setTextColor(android.graphics.Color.parseColor(pingColor))
                } else {
                    tvPing.text = "Timeout"
                    tvPing.setTextColor(android.graphics.Color.parseColor("#FF0040"))
                }
            }

            // 2. DOWNLOAD SPEED TEST
            withContext(Dispatchers.Main) {
                tvSpeedStatus.text = "A medir velocidade de download..."
            }

            val prefs = getSharedPreferences("IPTV_PREFS", MODE_PRIVATE)
            val username = prefs.getString("USERNAME", "") ?: ""
            val password = prefs.getString("PASSWORD", "") ?: ""

            var totalBytes = 0L
            var totalTimeMs = 0L

            try {
                // Em vez de baixar um Live Stream (que o servidor IPTV limita a ~5Mbps para bater certo com a emissão real),
                // usamos um ficheiro puro da Cloudflare para esgotar o túnel ao máximo.
                val streamUrl = "https://speed.cloudflare.com/__down?bytes=50000000"
                val testDurationMs = 8000L
                val startTime = System.currentTimeMillis()

                val request = Request.Builder().url(streamUrl).build()
                val directClient = okhttp3.OkHttpClient.Builder().build()
                val response = directClient.newCall(request).execute()
                val inputStream = response.body?.byteStream()

                if (inputStream != null) {
                    val buffer = ByteArray(65536)
                    while (isActive) {
                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed > testDurationMs) break

                        val read = inputStream.read(buffer)
                        if (read == -1) break

                        totalBytes += read
                        totalTimeMs = elapsed

                        if (totalTimeMs > 0) {
                            val speedBps = (totalBytes * 8.0 * 1000.0) / totalTimeMs
                            val currentMbps = speedBps / 1_000_000.0

                            withContext(Dispatchers.Main) {
                                tvSpeedValue.text = String.format("%.1f", currentMbps)
                                animateGauge(currentMbps.toFloat())
                            }
                        }
                    }
                    inputStream.close()
                }
                response.close()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvSpeedStatus.text = "Erro no teste: ${e.message}"
                }
            }

            // 3. FINAL RESULTS
            val finalMbps = if (totalTimeMs > 0) {
                (totalBytes * 8.0 * 1000.0) / totalTimeMs / 1_000_000.0
            } else 0.0

            withContext(Dispatchers.Main) {
                tvSpeedValue.text = String.format("%.1f", finalMbps)
                tvDownload.text = String.format("%.1f Mbps", finalMbps)
                animateGauge(finalMbps.toFloat())

                val quality = when {
                    finalMbps >= 50 -> "EXCELENTE ✅"
                    finalMbps >= 25 -> "MUITO BOM 👍"
                    finalMbps >= 10 -> "BOM 👌"
                    finalMbps >= 5  -> "ACEITÁVEL ⚠"
                    else            -> "FRACO ❌"
                }
                tvQuality.text = quality

                val qualColor = when {
                    finalMbps >= 50 -> "#00FF88"
                    finalMbps >= 25 -> "#00E5FF"
                    finalMbps >= 10 -> "#FFD600"
                    finalMbps >= 5  -> "#FF6600"
                    else            -> "#FF0040"
                }
                tvQuality.setTextColor(android.graphics.Color.parseColor(qualColor))
                tvDownload.setTextColor(android.graphics.Color.parseColor(qualColor))

                tvSpeedStatus.text = "Teste concluído!"
                btnStart.isEnabled = true
                btnStart.text = "🔄 TESTAR OUTRA VEZ"
            }
        }
    }

    private fun animateGauge(targetMbps: Float) {
        val animator = ValueAnimator.ofFloat(speedGauge.tag as? Float ?: 0f, targetMbps)
        animator.duration = 600
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { anim ->
            val value = anim.animatedValue as Float
            speedGauge.setSpeed(value)
        }
        animator.start()
        speedGauge.tag = targetMbps
    }

    override fun onDestroy() {
        super.onDestroy()
        testJob?.cancel()
    }
}
