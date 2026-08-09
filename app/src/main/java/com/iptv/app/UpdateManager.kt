package com.iptv.app

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object UpdateManager {

    private const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/Rafaellapa20/iptv-app/main/update.json"

    fun checkForUpdates(context: Context, showNoUpdateToast: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(UPDATE_JSON_URL).build()
                val response = OkHttpProvider.client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@launch
                    val json = JSONObject(body)
                    
                    val remoteVersionCode = json.optInt("versionCode", 0)
                    val remoteVersionName = json.optString("versionName", "")
                    val apkUrl = json.optString("apkUrl", "")
                    val releaseNotes = json.optString("releaseNotes", "Nova versão disponível!")
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    }
                    
                    if (remoteVersionCode > currentVersionCode && apkUrl.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            showUpdateDialog(context, remoteVersionName, releaseNotes, apkUrl)
                        }
                    } else {
                        if (showNoUpdateToast) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "O aplicativo já está na versão mais recente.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore silent network errors on startup
            }
        }
    }

    private fun showUpdateDialog(context: Context, version: String, notes: String, apkUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Atualização Disponível: $version")
            .setMessage(notes)
            .setCancelable(false)
            .setPositiveButton("Atualizar Agora") { _, _ ->
                downloadAndInstall(context, apkUrl, version)
            }
            .setNegativeButton("Mais Tarde") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun downloadAndInstall(context: Context, apkUrl: String, version: String) {
        Toast.makeText(context, "A descarregar a atualização...", Toast.LENGTH_LONG).show()

        val safeVersion = version.replace(".", "_").replace(" ", "")
        val destination = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/update_$safeVersion.apk"
        val file = File(destination)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Atualização do Aplicativo IPTV")
            .setDescription("A transferir a nova versão...")
            .setDestinationUri(Uri.fromFile(file))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == id) {
                    installApk(context, file)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao instalar atualização: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
