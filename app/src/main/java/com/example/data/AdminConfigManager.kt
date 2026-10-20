package com.example.data

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class AdminConfig(
    val featuredIds: Set<String> = emptySet(),
    val onboardingIds: Set<String> = emptySet(),
    val promotedIds: List<String> = emptyList(),
    val demotedIds: List<String> = emptyList(),
    val deletedIds: Set<String> = emptySet(),
    val globalTotalDownloadsOverride: Long? = null,
    val telemetryUrl: String = "",
    val lastUpdated: String = ""
)

object AdminConfigManager {
    private const val TAG = "AdminConfigManager"
    private const val PREFS_NAME = "orbitwall_prefs"

    private const val PRIMARY_CONFIG_URL =
        "https://raw.githubusercontent.com/fahitakrim/Orbitwall/main/admin_config.json"
    private const val FALLBACK_CONFIG_URL =
        "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/admin_config.json"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    var skipNetworkForTesting = false

    suspend fun fetchAdminConfig(context: Context): AdminConfig = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (skipNetworkForTesting) {
            return@withContext getCachedAdminConfig(prefs)
        }

        // 1. Try Primary GitHub Raw
        var jsonString: String? = null
        try {
            val req = Request.Builder()
                .url("$PRIMARY_CONFIG_URL?t=${System.currentTimeMillis()}")
                .header("Cache-Control", "no-cache, no-store")
                .header("Accept", "application/json")
                .build()
            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                jsonString = resp.body?.string()
            }
            resp.close()
        } catch (e: Exception) {
            Log.w(TAG, "Primary admin_config fetch failed: ${e.message}")
        }

        // 2. Try Fallback CDN
        if (jsonString.isNullOrBlank()) {
            try {
                val req = Request.Builder()
                    .url("$FALLBACK_CONFIG_URL?t=${System.currentTimeMillis()}")
                    .header("Cache-Control", "no-cache, no-store")
                    .header("Accept", "application/json")
                    .build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    jsonString = resp.body?.string()
                }
                resp.close()
            } catch (e: Exception) {
                Log.w(TAG, "Fallback admin_config fetch failed: ${e.message}")
            }
        }

        // 3. Fallback to bundled Asset
        if (jsonString.isNullOrBlank()) {
            try {
                jsonString = context.assets.open("admin_config.json").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.w(TAG, "Asset admin_config read failed: ${e.message}")
            }
        }

        if (jsonString.isNullOrBlank()) {
            return@withContext getCachedAdminConfig(prefs)
        }

        try {
            val config = parseJson(jsonString)
            saveToCache(prefs, config)
            if (config.telemetryUrl.isNotBlank()) {
                DownloadTracker.remoteTelemetryUrl = config.telemetryUrl
            }
            config
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse admin_config.json: ${e.message}", e)
            getCachedAdminConfig(prefs)
        }
    }

    private fun parseJson(jsonString: String): AdminConfig {
        val obj = JSONObject(jsonString)

        val featured = mutableSetOf<String>()
        val featuredArr = obj.optJSONArray("featured_ids") ?: JSONArray()
        for (i in 0 until featuredArr.length()) {
            featured.add(featuredArr.optString(i))
        }

        val onboarding = mutableSetOf<String>()
        val onboardingArr = obj.optJSONArray("onboarding_ids") ?: JSONArray()
        for (i in 0 until onboardingArr.length()) {
            onboarding.add(onboardingArr.optString(i))
        }

        val promoted = mutableListOf<String>()
        val promotedArr = obj.optJSONArray("promoted_ids") ?: JSONArray()
        for (i in 0 until promotedArr.length()) {
            promoted.add(promotedArr.optString(i))
        }

        val demoted = mutableListOf<String>()
        val demotedArr = obj.optJSONArray("demoted_ids") ?: JSONArray()
        for (i in 0 until demotedArr.length()) {
            demoted.add(demotedArr.optString(i))
        }

        val deleted = mutableSetOf<String>()
        val deletedArr = obj.optJSONArray("deleted_ids") ?: JSONArray()
        for (i in 0 until deletedArr.length()) {
            deleted.add(deletedArr.optString(i))
        }

        val globalOverride = if (obj.has("global_total_downloads_override") && !obj.isNull("global_total_downloads_override")) {
            obj.optLong("global_total_downloads_override")
        } else null

        val telemetryUrl = obj.optString("telemetry_url", "")
        val lastUpdated = obj.optString("last_updated", "")

        return AdminConfig(
            featuredIds = featured,
            onboardingIds = onboarding,
            promotedIds = promoted,
            demotedIds = demoted,
            deletedIds = deleted,
            globalTotalDownloadsOverride = globalOverride,
            telemetryUrl = telemetryUrl,
            lastUpdated = lastUpdated
        )
    }

    private fun getCachedAdminConfig(prefs: android.content.SharedPreferences): AdminConfig {
        val defaultFeatured = setOf(
            "paper-2k-prism-smoke-cube",
            "paper-2k-black-hole-singularity",
            "paper-2k-cherry-blossom-maiden",
            "paper-2k-crimson-eclipse-orbit",
            "paper-obito-split-panels",
            "paper-itachi-crimson-smoke"
        )
        val defaultOnboarding = setOf(
            "paper-akatsuki-moonlit-gathering",
            "paper-minimal-capsule-stack",
            "paper-itachi-red-moon",
            "paper-solitary-red-boat",
            "paper-eclipse-spheres-red-glow",
            "paper-kakashi-sharingan-moon",
            "paper-crimson-amber-fluid-flow",
            "paper-cherry-blossom-black"
        )

        val featured = prefs.getStringSet("featured_ids", defaultFeatured) ?: defaultFeatured
        val onboarding = prefs.getStringSet("onboarding_ids", defaultOnboarding) ?: defaultOnboarding
        val promotedStr = prefs.getString("promoted_ids", "") ?: ""
        val promoted = if (promotedStr.isNotBlank()) promotedStr.split(",").filter { it.isNotBlank() } else emptyList()
        val demotedStr = prefs.getString("demoted_ids", "") ?: ""
        val demoted = if (demotedStr.isNotBlank()) demotedStr.split(",").filter { it.isNotBlank() } else emptyList()
        val deleted = prefs.getStringSet("deleted_ids", emptySet()) ?: emptySet()
        val override = if (prefs.contains("global_total_downloads_override")) {
            prefs.getLong("global_total_downloads_override", 0L)
        } else null
        val telemetry = prefs.getString("telemetry_url", "") ?: ""
        val lastUpdated = prefs.getString("admin_config_last_updated", "") ?: ""

        return AdminConfig(
            featuredIds = featured,
            onboardingIds = onboarding,
            promotedIds = promoted,
            demotedIds = demoted,
            deletedIds = deleted,
            globalTotalDownloadsOverride = override,
            telemetryUrl = telemetry,
            lastUpdated = lastUpdated
        )
    }

    private fun saveToCache(prefs: android.content.SharedPreferences, config: AdminConfig) {
        val editor = prefs.edit()
            .putStringSet("featured_ids", config.featuredIds)
            .putStringSet("onboarding_ids", config.onboardingIds)
            .putString("promoted_ids", config.promotedIds.joinToString(","))
            .putString("demoted_ids", config.demotedIds.joinToString(","))
            .putStringSet("deleted_ids", config.deletedIds)
            .putString("telemetry_url", config.telemetryUrl)
            .putString("admin_config_last_updated", config.lastUpdated)

        if (config.globalTotalDownloadsOverride != null) {
            editor.putLong("global_total_downloads_override", config.globalTotalDownloadsOverride)
        } else {
            editor.remove("global_total_downloads_override")
        }
        editor.apply()
    }

    fun serializeToJson(config: AdminConfig): String {
        val obj = JSONObject()
        obj.put("featured_ids", JSONArray(config.featuredIds))
        obj.put("onboarding_ids", JSONArray(config.onboardingIds))
        obj.put("promoted_ids", JSONArray(config.promotedIds))
        obj.put("demoted_ids", JSONArray(config.demotedIds))
        obj.put("deleted_ids", JSONArray(config.deletedIds))
        if (config.globalTotalDownloadsOverride != null) {
            obj.put("global_total_downloads_override", config.globalTotalDownloadsOverride)
        } else {
            obj.put("global_total_downloads_override", JSONObject.NULL)
        }
        obj.put("telemetry_url", config.telemetryUrl)

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        obj.put("last_updated", sdf.format(Date()))

        return obj.toString(2)
    }

    suspend fun pushAdminConfigViaCloudProxy(
        context: Context,
        adminPasscode: String,
        config: AdminConfig
    ): Result<String> = withContext(Dispatchers.IO) {
        val webhookUrl = DownloadTracker.remoteTelemetryUrl.ifBlank {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString("telemetry_url", "") ?: ""
        }

        if (webhookUrl.isBlank() || !webhookUrl.startsWith("http")) {
            return@withContext Result.failure(IllegalStateException("Cloud Webhook URL is not configured. Please save your Webhook URL in the SYNC tab."))
        }

        try {
            val payload = JSONObject().apply {
                put("action", "sync_admin_config")
                put("admin_passcode", adminPasscode)
                put("config", JSONObject(serializeToJson(config)))
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val req = Request.Builder()
                .url(webhookUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "Orbitwall-Admin")
                .post(body)
                .build()

            val resp = httpClient.newCall(req).execute()
            val respBody = resp.body?.string().orEmpty()
            val isSuccess = resp.isSuccessful
            resp.close()

            if (isSuccess && respBody.isNotBlank()) {
                val respJson = JSONObject(respBody)
                val status = respJson.optString("status", "")
                if (status == "success") {
                    val msg = respJson.optString("message", "Changes synced to Cloud! Live for all users worldwide 🚀")
                    Result.success(msg)
                } else {
                    val errorMsg = respJson.optString("message", "Cloud sync rejected.")
                    Result.failure(Exception(errorMsg))
                }
            } else {
                Result.failure(Exception("HTTP ${resp.code}: Failed to contact Cloud Webhook"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pushAdminConfigToGitHub(
        token: String,
        config: AdminConfig,
        owner: String = "fahitakrim",
        repo: String = "Orbitwall"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("GitHub Personal Access Token is required"))
        }

        try {
            // 1. Get current file SHA
            val getUrl = "https://api.github.com/repos/$owner/$repo/contents/admin_config.json"
            val getReq = Request.Builder()
                .url(getUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Orbitwall-Admin")
                .build()

            var sha: String? = null
            val getResp = httpClient.newCall(getReq).execute()
            if (getResp.isSuccessful) {
                val bodyStr = getResp.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                sha = json.optString("sha")
            }
            getResp.close()

            // 2. Prepare PUT payload
            val jsonContent = serializeToJson(config)
            val base64Content = Base64.encodeToString(jsonContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val putPayload = JSONObject().apply {
                put("message", "chore: sync admin_config from Orbitwall Admin Console")
                put("content", base64Content)
                put("branch", "main")
                if (!sha.isNullOrBlank()) {
                    put("sha", sha)
                }
            }

            val putBody = putPayload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val putReq = Request.Builder()
                .url(getUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Orbitwall-Admin")
                .put(putBody)
                .build()

            val putResp = httpClient.newCall(putReq).execute()
            val putRespBody = putResp.body?.string().orEmpty()
            val success = putResp.isSuccessful
            putResp.close()

            if (success) {
                Result.success("Changes synced to GitHub! All users will receive this update worldwide 🚀")
            } else {
                val errorMsg = try {
                    JSONObject(putRespBody).optString("message", "HTTP ${putResp.code}")
                } catch (_: Exception) {
                    "HTTP ${putResp.code}"
                }
                Result.failure(Exception("GitHub API Error: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
