package com.iptv.app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Ecrã "StreamVPN" acessível a partir das Definições.
 * Estado da ligação, quota mensal, servidores, reconectar e teste de velocidade.
 */
class VpnStatusActivity : AppCompatActivity() {

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private lateinit var tvStatus: TextView
    private lateinit var tvServer: TextView
    private lateinit var tvIp: TextView
    private lateinit var tvQuota: TextView
    private lateinit var pbQuota: ProgressBar
    private lateinit var tvSpeed: TextView
    private lateinit var llServers: LinearLayout
    private lateinit var llLogin: LinearLayout
    private lateinit var llContent: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn_status)

        tvStatus = findViewById(R.id.tvVpnStatus)
        tvServer = findViewById(R.id.tvVpnServer)
        tvIp = findViewById(R.id.tvVpnIp)
        tvQuota = findViewById(R.id.tvVpnQuota)
        pbQuota = findViewById(R.id.pbVpnQuota)
        tvSpeed = findViewById(R.id.tvVpnSpeed)
        llServers = findViewById(R.id.llVpnServers)
        llLogin = findViewById(R.id.llVpnLogin)
        llContent = findViewById(R.id.llVpnContent)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnVpnRefresh).setOnClickListener { refreshAll() }
        findViewById<Button>(R.id.btnVpnReconnect).setOnClickListener { reconnect() }
        findViewById<Button>(R.id.btnVpnSpeedTest).setOnClickListener { speedTest() }
        findViewById<Button>(R.id.btnVpnLogout).setOnClickListener {
            StreamVpnApi.logout(this); showLoginOrContent()
        }
        findViewById<Button>(R.id.btnVpnLogin).setOnClickListener { doLogin() }
        findViewById<Button>(R.id.btnVpnServerUrl).setOnClickListener { showServerUrlDialog() }

        showLoginOrContent()
    }

    private fun showLoginOrContent() {
        val logged = StreamVpnApi.isLoggedIn(this)
        llLogin.visibility = if (logged) View.GONE else View.VISIBLE
        llContent.visibility = if (logged) View.VISIBLE else View.GONE
        if (logged) { refreshAll(); return }
    }

    private fun doLogin() {
        val code = findViewById<EditText>(R.id.etVpnEmail).text.toString().trim()
        if (code.length < 8) { toast("Introduz o código de ativação"); return }
        toast("A ativar...")
        uiScope.launch {
            StreamVpnApi.activate(this@VpnStatusActivity, code)
                .onSuccess { toast("StreamVPN ativada neste aparelho"); showLoginOrContent() }
                .onFailure { toast("Ativação falhou: ${it.message}") }
        }
    }

    private fun refreshAll() {
        tvStatus.text = "A carregar..."
        uiScope.launch {
            StreamVpnApi.status(this@VpnStatusActivity).onSuccess { s ->
                val on = s.status == "active"
                tvStatus.text = if (on) "● LIGADA" else "○ DESLIGADA"
                tvStatus.setTextColor(Color.parseColor(if (on) "#00E676" else "#FF5252"))
                tvServer.text = "Servidor: ${s.currentServer ?: "—"}"
            }.onFailure { handleError(it) }

            StreamVpnApi.connectionInfo(this@VpnStatusActivity).onSuccess { c ->
                tvIp.text = "IP público: ${c.publicIp}  ·  ${c.protocol}\n" +
                    "↓ ${fmtBytes(c.bytesDownloaded)}   ↑ ${fmtBytes(c.bytesUploaded)}"
            }

            StreamVpnApi.quota(this@VpnStatusActivity).onSuccess { q ->
                pbQuota.progress = q.percentUsed
                tvQuota.text = "${q.usedGb} GB de ${q.monthlyGb} GB usados (${q.percentUsed}%)"
                tvQuota.setTextColor(Color.parseColor(when (q.status) {
                    "exceeded" -> "#FF5252"; "warning" -> "#FFC107"; else -> "#B0BEC5"
                }))
            }

            StreamVpnApi.availableServers(this@VpnStatusActivity).onSuccess { list ->
                renderServers(list)
            }
        }
    }

    private fun renderServers(list: List<StreamVpnApi.VpnServer>) {
        llServers.removeAllViews()
        if (list.isEmpty()) {
            llServers.addView(TextView(this).apply {
                text = "Nenhum servidor disponível"; setTextColor(Color.GRAY)
            })
            return
        }
        list.forEach { s ->
            val btn = Button(this).apply {
                text = "${s.name}  ·  ${s.location ?: s.country ?: ""}  ·  ${s.ping} ms"
                setBackgroundResource(R.drawable.bg_smarters_sage)
                setTextColor(resources.getColor(R.color.text_primary, theme))
                isFocusable = true
                setOnClickListener { changeServer(s) }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
            llServers.addView(btn, lp)
        }
    }

    private fun reconnect() {
        toast("A reconectar...")
        uiScope.launch {
            StreamVpnApi.reconnect(this@VpnStatusActivity)
                .onSuccess { toast(it); refreshAll() }
                .onFailure { handleError(it) }
        }
    }

    private fun changeServer(s: StreamVpnApi.VpnServer) {
        toast("A mudar para ${s.name}...")
        uiScope.launch {
            StreamVpnApi.changeServer(this@VpnStatusActivity, s.id)
                .onSuccess { toast(it); refreshAll() }
                .onFailure { handleError(it) }
        }
    }

    private fun speedTest() {
        tvSpeed.text = "A testar velocidade..."
        uiScope.launch {
            StreamVpnApi.speedTest(this@VpnStatusActivity)
                .onSuccess { tvSpeed.text = "↓ ${it.download}   ↑ ${it.upload}   ping ${it.ping}" }
                .onFailure { tvSpeed.text = "Teste falhou"; handleError(it) }
        }
    }

    private fun showServerUrlDialog() {
        val input = EditText(this).apply {
            setText(StreamVpnApi.baseUrl(this@VpnStatusActivity))
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("Servidor StreamVPN")
            .setMessage("URL base da API (ex.: https://streamvpn.faktio.ch:3000/api)")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                StreamVpnApi.setBaseUrl(this, input.text.toString())
                toast("Servidor atualizado"); refreshAll()
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Repor Padrão") { _, _ ->
                StreamVpnApi.setBaseUrl(this, Constants.STREAMVPN_BASE_URL)
                toast("Servidor reposto"); refreshAll()
            }
            .show()
    }

    private fun handleError(e: Throwable) {
        val msg = e.message ?: "Erro"
        if (msg.contains("401") || msg.contains("Sem sessão")) {
            StreamVpnApi.logout(this); showLoginOrContent()
        }
        toast(msg)
    }

    private fun fmtBytes(b: Long): String = when {
        b >= 1L shl 30 -> "%.2f GB".format(b / (1L shl 30).toDouble())
        b >= 1L shl 20 -> "%.1f MB".format(b / (1L shl 20).toDouble())
        b >= 1L shl 10 -> "%d KB".format(b shr 10)
        else -> "$b B"
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }
}
