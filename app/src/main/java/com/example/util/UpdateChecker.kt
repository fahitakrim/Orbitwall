package com.example.util

import com.example.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object UpdateChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val VERSION_REGEX = Regex("""(\d+(?:\.\d+)+)""")

    fun extractVersion(raw: String): String {
        return VERSION_REGEX.find(raw)?.value ?: raw.filter { it.isDigit() || it == '.' }.trim('.')
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        val cleanRemote = extractVersion(remote)
        val cleanCurrent = extractVersion(current)
        if (cleanRemote.isBlank() || cleanCurrent.isBlank()) return false

        val remoteParts = cleanRemote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        if (remoteParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    fun cleanReleaseNotes(raw: String): String {
        return raw
            .replace(Regex("""^#+\s*""", RegexOption.MULTILINE), "")
            .replace("**", "")
            .replace("__", "")
            .replace("`", "")
            .trim()
    }

    fun fallbackUpdateInfo(currentVersion: String): AppUpdateInfo {
        return AppUpdateInfo(
            isUpdateAvailable = false,
            latestVersion = "v$currentVersion",
            currentVersion = "v$currentVersion",
            releaseTitle = "Orbitwall v$currentVersion",
            releaseNotes = "You are running the latest version of Orbitwall.",
            downloadUrl = "https://github.com/fahitakrim/Orbitwall/releases"
        )
    }

    suspend fun checkForUpdate(currentVersion: String): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        val cleanCurrent = extractVersion(currentVersion)

        // 1. Primary: Official GitHub Releases API
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/fahitakrim/Orbitwall/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Orbitwall/$currentVersion")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val tagName = json.optString("tag_name", "").trim()
                    val releaseName = json.optString("name", tagName).ifBlank { tagName }
                    val rawBody = json.optString("body", "Bug fixes and performance improvements.").ifBlank {
                        "Bug fixes and performance improvements."
                    }
                    val body = cleanReleaseNotes(rawBody)

                    var downloadUrl = ""
                    var fileSizeFormatted = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null && assets.length() > 0) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.optJSONObject(i)
                            val name = asset?.optString("name", "") ?: ""
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                val size = asset.optLong("size", 0L)
                                if (size > 0) {
                                    fileSizeFormatted = String.format(java.util.Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
                                }
                                break
                            }
                        }
                    }

                    val parsedVersion = extractVersion(tagName).ifBlank { extractVersion(releaseName) }
                    if (downloadUrl.isEmpty()) {
                        downloadUrl = if (parsedVersion.isNotBlank()) {
                            "https://github.com/fahitakrim/Orbitwall/releases/download/v$parsedVersion/Orbitwall-v$parsedVersion.apk"
                        } else {
                            json.optString("html_url", "https://github.com/fahitakrim/Orbitwall/releases/latest")
                        }
                    }

                    val isNewer = isNewerVersion(remote = parsedVersion, current = cleanCurrent)
                    return@withContext Result.success(
                        AppUpdateInfo(
                            isUpdateAvailable = isNewer,
                            latestVersion = "v$parsedVersion",
                            currentVersion = "v$cleanCurrent",
                            releaseTitle = releaseName,
                            releaseNotes = body,
                            downloadUrl = downloadUrl,
                            fileSizeFormatted = fileSizeFormatted
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Secondary fallback: Raw GitHub apps/orbitwall/update.json
        try {
            val req = Request.Builder()
                .url("https://raw.githubusercontent.com/fahitakrim/my-artifacts/main/apps/orbitwall/update.json")
                .header("User-Agent", "Orbitwall/$currentVersion")
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val latestVer = json.optString("latestVersion", "").trim()
                    val parsedVersion = extractVersion(latestVer)
                    val dlUrl = json.optString("downloadUrl", "https://github.com/fahitakrim/my-artifacts/releases/download/orbitwall-latest/Orbitwall.apk")
                    val changelogArr = json.optJSONArray("changelog")
                    val notes = if (changelogArr != null && changelogArr.length() > 0) {
                        val items = mutableListOf<String>()
                        for (i in 0 until changelogArr.length()) {
                            items.add("• " + changelogArr.getString(i))
                        }
                        cleanReleaseNotes(items.joinToString("\n"))
                    } else {
                        "Cloud Admin Sync and wallpaper updates."
                    }

                    val isNewer = isNewerVersion(remote = parsedVersion, current = cleanCurrent)
                    return@withContext Result.success(
                        AppUpdateInfo(
                            isUpdateAvailable = isNewer,
                            latestVersion = "v$parsedVersion",
                            currentVersion = "v$cleanCurrent",
                            releaseTitle = "Orbitwall v$parsedVersion",
                            releaseNotes = notes,
                            downloadUrl = dlUrl,
                            fileSizeFormatted = ""
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 3. Tertiary fallback: Raw GitHub cloud_catalog/version.json
        try {
            val req = Request.Builder()
                .url("https://raw.githubusercontent.com/fahitakrim/Orbitwall/main/cloud_catalog/version.json")
                .header("User-Agent", "Orbitwall/$currentVersion")
                .build()

            client.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val ver = json.optString("version", "").trim()
                    val parsedVersion = extractVersion(ver)
                    val title = json.optString("title", "Orbitwall v$parsedVersion")
                    val body = cleanReleaseNotes(json.optString("body", "Bug fixes and performance improvements."))
                    val dlUrl = json.optString("download_url", "")
                    val size = json.optString("file_size", "")

                    val isNewer = isNewerVersion(remote = parsedVersion, current = cleanCurrent)
                    return@withContext Result.success(
                        AppUpdateInfo(
                            isUpdateAvailable = isNewer,
                            latestVersion = "v$parsedVersion",
                            currentVersion = "v$cleanCurrent",
                            releaseTitle = title,
                            releaseNotes = body,
                            downloadUrl = dlUrl,
                            fileSizeFormatted = size
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        // 4. Quaternary fallback: High-speed CDN version.json
        try {
            val cdnReq = Request.Builder()
                .url("https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/version.json")
                .build()

            client.newCall(cdnReq).execute().use { res ->
                if (res.isSuccessful) {
                    val json = JSONObject(res.body?.string() ?: "{}")
                    val ver = json.optString("version", "").trim()
                    val parsedVersion = extractVersion(ver)
                    val title = json.optString("title", "Orbitwall v$parsedVersion")
                    val body = cleanReleaseNotes(json.optString("body", "Bug fixes and improvements."))
                    val dlUrl = json.optString("download_url", "")
                    val size = json.optString("file_size", "")

                    val isNewer = isNewerVersion(remote = parsedVersion, current = cleanCurrent)
                    return@withContext Result.success(
                        AppUpdateInfo(
                            isUpdateAvailable = isNewer,
                            latestVersion = "v$parsedVersion",
                            currentVersion = "v$cleanCurrent",
                            releaseTitle = title,
                            releaseNotes = body,
                            downloadUrl = dlUrl,
                            fileSizeFormatted = size
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        Result.failure(IOException("Unable to check for updates. Check internet connection."))
    }
}
