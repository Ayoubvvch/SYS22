package com.acmods.sys22.update

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object UpdateManager {

    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/Ayoubvvch/SYS22/releases/latest"

    private val executor = Executors.newSingleThreadExecutor()

    fun checkForUpdates(activity: Activity, userInitiated: Boolean = true) {
        if (userInitiated) {
            Toast.makeText(activity, "SYS22: جاري التحقق من التحديثات...", Toast.LENGTH_SHORT).show()
        }

        executor.execute {
            try {
                val url = URL(GITHUB_LATEST_RELEASE_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("User-Agent", "SYS22-Android-App")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (connection.responseCode != 200) {
                    if (userInitiated) {
                        activity.runOnUiThread {
                            Toast.makeText(activity, "تعذر التحقق من التحديثات (${connection.responseCode})", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@execute
                }

                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JsonParser.parseString(responseBody).asJsonObject

                val tagName = jsonObject.get("tag_name")?.asString ?: ""
                val releaseName = jsonObject.get("name")?.asString ?: tagName
                val releaseNotes = jsonObject.get("body")?.asString ?: ""

                var apkDownloadUrl = ""
                var apkFileName = "SYS22-$tagName.apk"

                val assets = jsonObject.getAsJsonArray("assets")
                if (assets != null) {
                    for (element in assets) {
                        val assetObj = element.asJsonObject
                        val name = assetObj.get("name")?.asString ?: ""
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = assetObj.get("browser_download_url")?.asString ?: ""
                            apkFileName = name
                            break
                        }
                    }
                }

                val currentVersion = try {
                    activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "2.1.0"
                } catch (_: Exception) {
                    "2.1.0"
                }

                val isNewer = isVersionGreater(tagName, currentVersion)

                activity.runOnUiThread {
                    if (isNewer && apkDownloadUrl.isNotBlank()) {
                        promptUserToUpdate(
                            activity,
                            tagName,
                            releaseName,
                            releaseNotes,
                            apkDownloadUrl,
                            apkFileName
                        )
                    } else {
                        if (userInitiated) {
                            AlertDialog.Builder(activity)
                                .setTitle("⚡ SYS22: النظام محدث")
                                .setMessage("أنت تستخدم بالفعل أحدث إصدار متوفر ($currentVersion).\nلا توجد تحديثات جديدة حالياً.")
                                .setPositiveButton("حسناً", null)
                                .show()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                if (userInitiated) {
                    activity.runOnUiThread {
                        Toast.makeText(
                            activity,
                            "تعذر الاتصال بخادم التحديثات: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun promptUserToUpdate(
        activity: Activity,
        tagName: String,
        name: String,
        notes: String,
        downloadUrl: String,
        fileName: String
    ) {
        val message = "يتوفر إصدار جديد من SYS22 ($tagName)!\n\n" +
                (if (notes.isNotBlank()) "أبرز التحديثات:\n${notes.take(300)}\n\n" else "") +
                "هل ترغب في تنزيل وتثبيت التحديث مباشرة؟"

        AlertDialog.Builder(activity)
            .setTitle("🚀 تحديث جديد متوفر ($tagName)")
            .setMessage(message)
            .setPositiveButton("تنزيل وتثبيت") { _, _ ->
                downloadAndInstallApk(activity, downloadUrl, fileName)
            }
            .setNegativeButton("لاحقاً", null)
            .setCancelable(true)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun downloadAndInstallApk(activity: Activity, downloadUrl: String, fileName: String) {
        val progressDialog = ProgressDialog(activity).apply {
            setTitle("تنزيل تحديث SYS22")
            setMessage("جاري تنزيل ملف التحديث...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            max = 100
            setCancelable(false)
            show()
        }

        executor.execute {
            try {
                val url = URL(downloadUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "SYS22-Android-App")
                }

                // Handle HTTP redirects (GitHub Releases redirect to AWS S3)
                var currentUrl = url
                var currentConn = connection
                var redirect = true
                var redirectsCount = 0

                while (redirect && redirectsCount < 5) {
                    val status = currentConn.responseCode
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER ||
                        status == 307 || status == 308
                    ) {
                        val newUrlStr = currentConn.getHeaderField("Location")
                        currentUrl = URL(newUrlStr)
                        currentConn = (currentUrl.openConnection() as HttpURLConnection).apply {
                            connectTimeout = 15000
                            readTimeout = 30000
                            setRequestProperty("User-Agent", "SYS22-Android-App")
                        }
                        redirectsCount++
                    } else {
                        redirect = false
                    }
                }

                val fileLength = currentConn.contentLength
                val downloadDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: activity.cacheDir
                val destinationFile = File(downloadDir, fileName)

                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                val input = BufferedInputStream(currentConn.inputStream)
                val output = FileOutputStream(destinationFile)

                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt()
                        activity.runOnUiThread {
                            progressDialog.progress = progress
                        }
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                activity.runOnUiThread {
                    progressDialog.dismiss()
                    launchPackageInstaller(activity, destinationFile)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                activity.runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(
                        activity,
                        "فشل تنزيل التحديث: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun launchPackageInstaller(activity: Activity, apkFile: File) {
        try {
            // Android 8.0+ Unknown sources check
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        activity,
                        "يرجى السماح لـ SYS22 بتثبيت التطبيقات للمتابعة",
                        Toast.LENGTH_LONG
                    ).show()
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                    activity.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            activity.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                activity,
                "تعذر فتح مثبت الحزم: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun isVersionGreater(latest: String, current: String): Boolean {
        val cleanLatest = latest.lowercase().replace("v", "").trim()
        val cleanCurrent = current.lowercase().replace("v", "").trim()

        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
