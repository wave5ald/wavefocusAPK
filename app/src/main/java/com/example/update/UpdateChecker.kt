package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Release metadata for the Android APK, read from the `android` block. */
data class AndroidRelease(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val releaseNotes: String,
    val sha256: String? = null,
    val sizeBytes: Long = 0L,
)

/** Observable state of the update workflow. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class Available(val release: AndroidRelease) : UpdateState

    data class Downloading(val receivedBytes: Long, val totalBytes: Long) : UpdateState {
        val percent: Int
            get() = if (totalBytes > 0) ((receivedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
    }

    data class Ready(val release: AndroidRelease, val apk: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/** Outcome of handing a downloaded APK to the system package installer. */
sealed interface InstallOutcome {
    data object Started : InstallOutcome
    data object NeedsPermission : InstallOutcome
    data class Failed(val message: String) : InstallOutcome
}

/**
 * Fetches the shared update manifest and downloads the newest APK.
 *
 * The manifest is the same `version.json` the Windows app reads:
 * ```json
 * {
 *   "version": "2.2.0",
 *   "android": {
 *     "version_name": "2.2",
 *     "version_code": 4,
 *     "apk_url": "https://github.com/wave5ald/wavefocusAPK/releases/download/Release/WaveFocus-2.2.apk",
 *     "release_notes": "…",
 *     "sha256": "optional hex digest",
 *     "size_bytes": 5333070
 *   }
 * }
 * ```
 */
class UpdateChecker(
    private val manifestUrl: String = DEFAULT_MANIFEST_URL,
    private val client: OkHttpClient = defaultClient(),
) {

    suspend fun fetchLatest(): Result<AndroidRelease> = withContext(Dispatchers.IO) {
        runCatching {
            require(manifestUrl.startsWith("https://", ignoreCase = true)) {
                "Update manifest must be served over HTTPS"
            }
            val request = Request.Builder()
                .url(manifestUrl)
                .header("Accept", "application/json")
                .cacheControl(CacheControl.Builder().noCache().build())
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Update server returned HTTP ${response.code}")
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) error("Update server returned an empty manifest")

                val json = JSONObject(body)
                val android = json.optJSONObject("android")
                    ?: error("No Android build has been published yet")

                val versionCode = android.optInt("version_code", 0)
                val versionName = android.optString("version_name").trim()
                    .ifEmpty { json.optString("version").trim() }
                if (versionCode <= 0 || SemVer.parse(versionName) == null) {
                    error("Update manifest has an invalid Android version")
                }

                AndroidRelease(
                    versionName = versionName,
                    versionCode = versionCode,
                    apkUrl = android.optString("apk_url").trim(),
                    releaseNotes = android.optString("release_notes").trim()
                        .ifEmpty { json.optString("release_notes").trim() },
                    sha256 = android.optString("sha256").trim().lowercase().takeIf { it.isNotEmpty() },
                    sizeBytes = android.optLong("size_bytes", 0L).coerceAtLeast(0L),
                )
            }
        }
    }

    suspend fun download(
        context: Context,
        release: AndroidRelease,
        onProgress: (received: Long, total: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(release.apkUrl.startsWith("https://", ignoreCase = true)) {
                "Download link must use HTTPS"
            }
            val target = apkFile(context, release.versionName)
            if (target.exists() && target.length() > 0L) {
                val complete = release.sizeBytes <= 0L || target.length() == release.sizeBytes
                val checksumOk = release.sha256 == null || sha256(target) == release.sha256
                if (complete && checksumOk) {
                    onProgress(target.length(), target.length())
                    return@runCatching target
                }
                target.delete()
            }

            val request = Request.Builder()
                .url(release.apkUrl)
                .header("Accept", "application/octet-stream")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Download failed with HTTP ${response.code}")
                val body = response.body ?: error("Download returned an empty response")
                val total = body.contentLength().takeIf { it > 0L } ?: release.sizeBytes

                var received = 0L
                var lastReport = 0L
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            received += read
                            val now = System.currentTimeMillis()
                            if (now - lastReport >= 150L) {
                                lastReport = now
                                onProgress(received, total)
                            }
                        }
                    }
                }
                onProgress(received, total)

                if (release.sizeBytes > 0L && target.length() != release.sizeBytes) {
                    target.delete()
                    error("Download is incomplete (${target.length()} of ${release.sizeBytes} bytes)")
                }
                if (release.sha256 != null && sha256(target) != release.sha256) {
                    target.delete()
                    error("Downloaded file failed the checksum check")
                }
            }
            target
        }
    }

    /** Opens the system installer for [apk], asking for permission when needed. */
    fun install(context: Context, apk: File): InstallOutcome {
        return try {
            if (!apk.isFile || apk.length() == 0L) {
                return InstallOutcome.Failed("The downloaded installer is missing")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val settings = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settings)
                return InstallOutcome.NeedsPermission
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apk
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            InstallOutcome.Started
        } catch (error: Exception) {
            InstallOutcome.Failed(error.message ?: "Could not open the installer")
        }
    }

    /** True when the device blocks installs from this app (API 26+). */
    fun needsInstallPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return !context.packageManager.canRequestPackageInstalls()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** Shared with the Windows app; hosted on Firebase, no caching. */
        const val DEFAULT_MANIFEST_URL =
            "https://gen-lang-client-0965379779.web.app/updates/version.json"

        const val APK_MIME = "application/vnd.android.package-archive"

        fun apkFile(context: Context, version: String): File {
            val safeVersion = version.replace(Regex("[^0-9A-Za-z._-]"), "_")
            val folder = File(context.cacheDir, "updates").apply { mkdirs() }
            return File(folder, "WaveFocus-$safeVersion.apk")
        }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
