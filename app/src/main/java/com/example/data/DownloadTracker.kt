package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Lightweight telemetry service to track wallpaper downloads both locally
 * (persisted in SharedPreferences) and remotely via an optional Google Sheets
 * Webhook or serverless endpoint.
 */
object DownloadTracker {
    private const val TAG = "DownloadTracker"
    private const val PREFS_NAME = "orbitwall_download_stats"

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Configurable endpoint (e.g. Google Apps Script Webhook URL)
    var remoteTelemetryUrl: String = ""

    /**
     * Records a download event locally and attempts an asynchronous remote sync ping
     * to the configured Google Apps Script Webhook.
     */
    suspend fun recordDownload(
        context: Context,
        wallpaperId: String,
        wallpaperTitle: String
    ): Long = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCount = prefs.getLong("dl_$wallpaperId", 0L) + 1L
        prefs.edit().putLong("dl_$wallpaperId", currentCount).apply()

        val totalDownloads = prefs.getLong("total_downloads", 0L) + 1L
        prefs.edit().putLong("total_downloads", totalDownloads).apply()

        Log.d(TAG, "Wallpaper downloaded: $wallpaperTitle ($wallpaperId), local count: $currentCount, total: $totalDownloads")

        if (remoteTelemetryUrl.isNotBlank() && remoteTelemetryUrl.startsWith("http")) {
            try {
                val json = JSONObject().apply {
                    put("action", "download")
                    put("id", wallpaperId)
                    put("title", wallpaperTitle)
                    put("timestamp", System.currentTimeMillis())
                    put("count", currentCount)
                    put("app", "Orbitwall")
                }
                val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(remoteTelemetryUrl)
                    .header("Accept", "application/json")
                    .header("User-Agent", "Orbitwall-Android")
                    .post(body)
                    .build()

                val resp = client.newCall(request).execute()
                if (resp.isSuccessful) {
                    val respBody = resp.body?.string().orEmpty()
                    if (respBody.isNotBlank()) {
                        try {
                            val respJson = JSONObject(respBody)
                            if (respJson.has("total_downloads")) {
                                val srvTotal = respJson.getLong("total_downloads")
                                prefs.edit().putLong("remote_total_downloads", srvTotal).apply()
                            }
                        } catch (_: Exception) {}
                    }
                }
                resp.close()
            } catch (e: Exception) {
                Log.w(TAG, "Remote telemetry ping failed: ${e.message}")
            }
        }

        currentCount
    }

    /**
     * Fetches unified remote download stats from the Google Apps Script Webhook.
     * Returns a map of wallpaperId -> downloadCount.
     */
    suspend fun fetchRemoteStats(context: Context): Map<String, Long> = withContext(Dispatchers.IO) {
        if (remoteTelemetryUrl.isBlank() || !remoteTelemetryUrl.startsWith("http")) {
            return@withContext emptyMap()
        }

        try {
            val url = if (remoteTelemetryUrl.contains("?")) "$remoteTelemetryUrl&action=stats" else "$remoteTelemetryUrl?action=stats"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Orbitwall-Android")
                .get()
                .build()

            val resp = client.newCall(request).execute()
            if (!resp.isSuccessful) {
                resp.close()
                return@withContext emptyMap()
            }

            val body = resp.body?.string().orEmpty()
            resp.close()

            if (body.isBlank()) return@withContext emptyMap()

            val json = JSONObject(body)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            if (json.has("total_downloads")) {
                val total = json.optLong("total_downloads", 0L)
                if (total > 0L) {
                    editor.putLong("remote_total_downloads", total)
                }
            }

            val statsMap = mutableMapOf<String, Long>()
            val downloadsObj = json.optJSONObject("downloads")
            if (downloadsObj != null) {
                val keys = downloadsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val count = downloadsObj.optLong(key, 0L)
                    statsMap[key] = count
                    editor.putLong("dl_$key", count)
                }
            }
            editor.apply()
            statsMap
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch remote stats: ${e.message}")
            emptyMap()
        }
    }

    fun getLocalDownloadCount(context: Context, wallpaperId: String): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong("dl_$wallpaperId", 0L)
    }

    fun setLocalDownloadCount(context: Context, wallpaperId: String, count: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("dl_$wallpaperId", maxOf(0L, count)).apply()
    }

    fun getTotalDownloads(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remoteTotal = prefs.getLong("remote_total_downloads", 0L)
        if (remoteTotal > 0L) return remoteTotal
        return prefs.getLong("total_downloads", 0L)
    }

    fun setTotalDownloads(context: Context, count: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("total_downloads", maxOf(0L, count)).apply()
    }

    fun resetAllStats(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
