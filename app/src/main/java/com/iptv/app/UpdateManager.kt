package com.iptv.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateManager {

    private const val UPDATE_JSON_URL = "http://176.111.109.14/iptv_app/update.json"

    // Standard client to avoid DNS loop/issues when checking GitHub raw
    private val simpleClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun checkForUpdates(context: Context, showNoUpdateToast: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cacheBusterUrl = "$UPDATE_JSON_URL?t=${System.currentTimeMillis()}"
                val request = Request.Builder().url(cacheBusterUrl).build()
                
                // Try simple client first, fallback to OkHttpProvider.client
                val response = try {
                    simpleClient.newCall(request).execute()
                } catch (e: Exception) {
                    OkHttpProvider.client.newCall(request).execute()
                }
                
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.isEmpty()) {
                        if (showNoUpdateToast) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Resposta vazia do servidor.", Toast.LENGTH_LONG).show()
                            }
                        }
                        return@launch
                    }

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
                    val currentVersionName = packageInfo.versionName ?: ""
                    
                    if (remoteVersionCode > currentVersionCode && apkUrl.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            showUpdateDialog(context, remoteVersionName, releaseNotes, apkUrl)
                        }
                    } else {
                        if (showNoUpdateToast) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Já possui a versão mais recente ($currentVersionName / v$currentVersionCode).",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } else {
                    if (showNoUpdateToast) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Servidor retornou código ${response.code}.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                if (showNoUpdateToast) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Erro ao procurar atualizações: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
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
            .setTitle("A descarregar atualização v$version")
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
                val response = try {
                    simpleClient.newCall(request).execute()
                } catch (e: Exception) {
                    OkHttpProvider.client.newCall(request).execute()
                }

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
                            Toast.makeText(context, "Erro: Resposta vazia ao descarregar.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        Toast.makeText(context, "Erro ao descarregar: código ${response.code}", Toast.LENGTH_LONG).show()
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
