package com.topal.fx.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Model representing remote update details.
 */
data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String
)

/**
 * Helper object managing update checks, APK downloads, and package installations.
 */
object UpdateManager {

    /**
     * Safely fetches remote version info (supports both GitHub Releases API and custom version.json).
     */
    suspend fun fetchUpdateInfo(updateUrl: String): AppUpdateInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(updateUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "TopalFX-Android")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(jsonString)

                // 1. Check if response is from GitHub Releases API
                if (json.has("tag_name") && json.has("assets")) {
                    val tagName = json.optString("tag_name", "").replace("v", "")
                    val changelog = json.optString("body", "")
                    val assets = json.optJSONArray("assets")
                    var downloadUrl = ""

                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

                    // Convert tag_name like "1.8.0" to versionCode (e.g., 1.8.0 -> 9, or parse major.minor.patch)
                    val parts = tagName.split(".")
                    var derivedVersionCode = 0
                    if (parts.size >= 2) {
                        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
                        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
                        derivedVersionCode = (major * 100) + (minor * 10) + patch
                    }

                    if (downloadUrl.isNotEmpty()) {
                        return@withContext AppUpdateInfo(
                            versionCode = if (derivedVersionCode > 0) derivedVersionCode else 999,
                            versionName = tagName,
                            apkUrl = downloadUrl,
                            changelog = changelog
                        )
                    }
                }

                // 2. Custom version.json format
                val versionCode = json.optInt("versionCode", 0)
                val versionName = json.optString("versionName", "")
                val apkUrl = json.optString("apkUrl", "")
                val changelog = json.optString("changelog", "")

                if (versionCode > 0 && apkUrl.isNotEmpty()) {
                    return@withContext AppUpdateInfo(versionCode, versionName, apkUrl, changelog)
                }
            }
            null
        } catch (e: Exception) {
            // Silently handle offline status or HTTP errors
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Downloads the APK file and launches native package installation.
     */
    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(apkUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                withContext(Dispatchers.Main) {
                    onError("Server returned HTTP ${connection.responseCode}")
                }
                return@withContext
            }

            val fileLength = connection.contentLength
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val apkFile = File(downloadDir, "TopalFX-latest.apk")

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val data = ByteArray(4096)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)
                        if (fileLength > 0) {
                            val progress = (total.toFloat() / fileLength.toFloat())
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onComplete()
                installApk(context, apkFile)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Failed to download update")
            }
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Prompts Android package installer via FileProvider.
     */
    private fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }
}
