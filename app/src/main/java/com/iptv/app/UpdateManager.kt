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
                // Adiciona timestamp para furar a cache do GitHub Raw
                val cacheBusterUrl = "$UPDATE_JSON_URL?t=${System.currentTimeMillis()}"
                val request = Request.Builder().url(cacheBusterUrl).build()
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
        val dialog = AlertDialog.Builder(context)
            .setTitle("A descarregar a atualização")
            .setMessage("Por favor aguarde...")
            .setCancelable(false)
            .create()
        dialog.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val safeVersion = version.replace(".", "_").replace(" ", "")
                val file = File(context.getExternalFilesDir(null), "update_$safeVersion.apk")
                if (file.exists()) file.delete()

                val request = Request.Builder().url(apkUrl).build()
                val response = OkHttpProvider.client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        val inputStream = body.byteStream()
                        val outputStream = java.io.FileOutputStream(file)
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()

                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                            installApk(context, file)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            dialog.dismiss()
                            Toast.makeText(context, "Erro: Resposta vazia do servidor.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Toast.makeText(context, "Erro ao descarregar: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                    Toast.makeText(context, "Falha na transferência: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
