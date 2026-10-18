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
 * (persisted in SharedPreferences) and remotely via an optional webhook
 * (Google Sheets Webhook, Firebase RTDB, or Supabase).
 */
object DownloadTracker {
    private const val TAG = "DownloadTracker"
    private const val PREFS_NAME = "orbitwall_download_stats"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Configurable endpoint (e.g. Google Apps Script Webhook or Firebase endpoint)
    var remoteTelemetryUrl: String = ""

    /**
     * Records a download event locally and attempts an asynchronous remote sync ping.
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
                    put("id", wallpaperId)
                    put("title", wallpaperTitle)
                    put("timestamp", System.currentTimeMillis())
                    put("count", currentCount)
                    put("app", "Orbitwall")
                }
                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url(remoteTelemetryUrl)
                    .post(body)
                    .build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "Remote telemetry ping failed: ${e.message}")
            }
        }

        currentCount
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
