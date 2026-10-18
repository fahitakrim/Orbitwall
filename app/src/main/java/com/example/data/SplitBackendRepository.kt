package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.palette.graphics.Palette
import com.example.BuildConfig
import com.example.model.Wallpaper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Split-Engine High-Bandwidth Free Backend Repository:
 *
 * 1. METADATA DATABASE:
 *    Supabase PostgreSQL Free Tier (Stores lightweight text/JSON rows via PostgREST).
 *
 * 2. HIGH-BANDWIDTH IMAGE STORAGE & CDN:
 *    GitHub Repository Storage via GitHub REST API (`PUT /repos/{owner}/{repo}/contents/{path}`).
 *    Served to worldwide clients via jsDelivr Global CDN (`https://cdn.jsdelivr.net/gh/{owner}/{repo}@{branch}/{path}`)
 *    with zero bandwidth fees and unlimited edge caching.
 */
class SplitBackendRepository(
    private val context: Context? = null,
    private val supabaseUrl: String = getConfig("SUPABASE_URL", "https://your-project.supabase.co"),
    private val supabaseKey: String = getConfig("SUPABASE_ANON_KEY", ""),
    private val githubToken: String = getConfig("GITHUB_STORAGE_TOKEN", ""),
    private val githubOwner: String = getConfig("GITHUB_STORAGE_OWNER", ""),
    private val githubRepo: String = getConfig("GITHUB_STORAGE_REPO", "aurawall-storage"),
    private val githubBranch: String = getConfig("GITHUB_STORAGE_BRANCH", "main")
) {

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val wallpaperListAdapter = moshi.adapter<List<Wallpaper>>(
        Types.newParameterizedType(List::class.java, Wallpaper::class.java)
    )

    private fun loadCatalogFromAssets(): List<Wallpaper> {
        return try {
            val ctx = context ?: return emptyList()
            val jsonString = ctx.assets.open("wallpapers.json").bufferedReader().use { it.readText() }
            wallpaperListAdapter.fromJson(jsonString).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Local cached fallback catalog ensures instant, smooth app previews
    private val localCatalog = mutableListOf<Wallpaper>().apply {
        val fromAssets = loadCatalogFromAssets()
        if (fromAssets.isNotEmpty()) {
            addAll(fromAssets)
        } else {
            addAll(getCuratedCatalog())
        }
    }

    /**
     * Checks if Supabase metadata backend is configured with valid credentials.
     */
    fun isSupabaseConfigured(): Boolean {
        return supabaseUrl.isNotBlank() &&
                supabaseKey.isNotBlank() &&
                !supabaseUrl.contains("your-project.supabase.co") &&
                supabaseUrl.startsWith("http")
    }

    /**
     * Checks if GitHub storage repository backend is configured for uploads.
     */
    fun isGitHubStorageConfigured(): Boolean {
        return githubToken.isNotBlank() &&
                githubOwner.isNotBlank() &&
                githubRepo.isNotBlank() &&
                !githubOwner.contains("your-github-username")
    }

    /**
     * Fetches paginated wallpapers directly from jsDelivr cloud catalog (wallpapers.json).
     * Automatically caches results and supports force refresh with timestamp buster.
     */
    suspend fun fetchWallpapers(
        category: String? = null,
        searchQuery: String? = null,
        page: Int = 0,
        pageSize: Int = 500,
        forceRefresh: Boolean = false
    ): Result<List<Wallpaper>> = withContext(Dispatchers.IO) {
        try {
            val cacheBuster = if (forceRefresh) "?t=${System.currentTimeMillis()}" else ""
            val primaryUrl = "https://raw.githubusercontent.com/fahitakrim/Orbitwall/main/wallpapers.json$cacheBuster"
            val fallbackUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/wallpapers.json$cacheBuster"

            var response = try {
                okHttpClient.newCall(Request.Builder().url(primaryUrl).addHeader("Accept", "application/json").get().build()).execute()
            } catch (_: Exception) { null }

            if (response == null || !response.isSuccessful) {
                response = try {
                    okHttpClient.newCall(Request.Builder().url(fallbackUrl).addHeader("Accept", "application/json").get().build()).execute()
                } catch (_: Exception) { null }
            }

            if (response != null && response.isSuccessful) {
                val jsonBody = response.body?.string().orEmpty()
                val list = wallpaperListAdapter.fromJson(jsonBody)
                if (!list.isNullOrEmpty()) {
                    synchronized(localCatalog) {
                        localCatalog.clear()
                        localCatalog.addAll(list)
                    }
                }
            }
        } catch (_: Exception) {
            // Network fallback to local in-memory catalog
        }

        Result.success(filterLocalCatalog(category, searchQuery, page, pageSize))
    }

    /**
     * Part 2: Uploads a new wallpaper as Admin using the Split-Engine pipeline:
     * 1. Reads selected image URI.
     * 2. Extracts dominant hex color using `androidx.palette:palette-ktx`.
     * 3. Compresses a 450px wide WebP thumbnail (80% quality) in-memory.
     * 4. Base64-encodes and uploads thumbnail + full-res images to GitHub repo via Contents API.
     * 5. Builds permanent jsDelivr CDN URLs.
     * 6. Inserts metadata row into Supabase PostgreSQL table.
     */
    suspend fun uploadWallpaperAsAdmin(
        context: Context,
        imageUri: Uri,
        title: String,
        category: String,
        tags: List<String>
    ): Result<Wallpaper> = withContext(Dispatchers.IO) {
        var originalBitmap: Bitmap? = null
        var thumbnailBitmap: Bitmap? = null

        try {
            val stream: InputStream? = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(IllegalArgumentException("Cannot open image stream from URI"))

            originalBitmap = stream.use { BitmapFactory.decodeStream(it) }
                ?: return@withContext Result.failure(IllegalStateException("Failed to decode bitmap from image"))

            // 1. Dominant Color Extraction using AndroidX Palette
            val dominantColorHex = extractDominantColorHex(originalBitmap)
            val wallpaperId = UUID.randomUUID().toString()

            // 2. Generate 450px WebP thumbnail in-memory (80% quality)
            val targetThumbWidth = 450
            val aspectRatio = originalBitmap.height.toFloat() / originalBitmap.width.toFloat()
            val targetThumbHeight = (targetThumbWidth * aspectRatio).toInt().coerceAtLeast(1)

            thumbnailBitmap = Bitmap.createScaledBitmap(originalBitmap, targetThumbWidth, targetThumbHeight, true)
            val thumbBaos = ByteArrayOutputStream()
            val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            thumbnailBitmap.compress(compressFormat, 80, thumbBaos)
            val thumbBytes = thumbBaos.toByteArray()

            // 3. Full-resolution WebP compression (92% quality)
            val fullBaos = ByteArrayOutputStream()
            originalBitmap.compress(compressFormat, 92, fullBaos)
            val fullBytes = fullBaos.toByteArray()

            var thumbnailUrl: String
            var fullResUrl: String

            if (isGitHubStorageConfigured()) {
                val thumbFilePath = "thumbs/$wallpaperId.webp"
                val fullFilePath = "full/$wallpaperId.webp"

                // Upload thumbnail to GitHub Repo
                uploadFileToGitHub(
                    filePath = thumbFilePath,
                    bytes = thumbBytes,
                    commitMessage = "Upload thumbnail: $wallpaperId.webp"
                )

                // Upload full-res to GitHub Repo
                uploadFileToGitHub(
                    filePath = fullFilePath,
                    bytes = fullBytes,
                    commitMessage = "Upload full-res: $wallpaperId.webp"
                )

                // Permanent jsDelivr CDN URLs
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/$githubOwner/$githubRepo@$githubBranch/$thumbFilePath"
                fullResUrl = "https://cdn.jsdelivr.net/gh/$githubOwner/$githubRepo@$githubBranch/$fullFilePath"
            } else {
                // Local preview fallback URI
                thumbnailUrl = imageUri.toString()
                fullResUrl = imageUri.toString()
            }

            // 4. Save metadata row into Supabase PostgreSQL
            if (isSupabaseConfigured()) {
                insertSupabaseMetadata(
                    id = wallpaperId,
                    title = title,
                    category = category,
                    tags = tags,
                    thumbnailUrl = thumbnailUrl,
                    fullResUrl = fullResUrl,
                    dominantColorHex = dominantColorHex
                )
            }

            val createdWallpaper = Wallpaper(
                id = wallpaperId,
                title = title,
                category = category,
                tags = tags,
                thumbnailUrl = thumbnailUrl,
                fullResUrl = fullResUrl,
                dominantColorHex = dominantColorHex,
                downloads = 0L,
                createdAt = System.currentTimeMillis()
            )

            // Register in local catalog
            localCatalog.add(0, createdWallpaper)

            Result.success(createdWallpaper)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                thumbnailBitmap?.recycle()
                originalBitmap?.recycle()
            } catch (_: Exception) {}
        }
    }

    /**
     * Uploads binary file to GitHub Contents API (`PUT /repos/{owner}/{repo}/contents/{path}`).
     */
    private fun uploadFileToGitHub(
        filePath: String,
        bytes: ByteArray,
        commitMessage: String
    ) {
        val githubApiUrl = "https://api.github.com/repos/$githubOwner/$githubRepo/contents/$filePath"
        val base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val jsonBody = JSONObject().apply {
            put("message", commitMessage)
            put("content", base64Content)
            put("branch", githubBranch)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(githubApiUrl)
            .addHeader("Authorization", "Bearer $githubToken")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            .put(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("GitHub Storage upload failed (${response.code}): ${response.message}")
        }
    }

    /**
     * Inserts metadata row into Supabase PostgreSQL `wallpapers` table via PostgREST.
     */
    private fun insertSupabaseMetadata(
        id: String,
        title: String,
        category: String,
        tags: List<String>,
        thumbnailUrl: String,
        fullResUrl: String,
        dominantColorHex: String
    ) {
        val postgRestUrl = "$supabaseUrl/rest/v1/wallpapers"

        val jsonPayload = JSONObject().apply {
            put("id", id)
            put("title", title)
            put("category", category)
            put("tags", JSONArray(tags))
            put("thumbnail_url", thumbnailUrl)
            put("full_res_url", fullResUrl)
            put("dominant_color_hex", dominantColorHex)
            put("downloads", 0)
            put("created_at", System.currentTimeMillis())
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(postgRestUrl)
            .addHeader("apikey", supabaseKey)
            .addHeader("Authorization", "Bearer $supabaseKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("Supabase metadata insert failed (${response.code}): ${response.message}")
        }
    }

    /**
     * Extracts dominant color hex using AndroidX Palette.
     */
    private fun extractDominantColorHex(bitmap: Bitmap): String {
        return try {
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.dominantSwatch
                ?: palette.vibrantSwatch
                ?: palette.darkVibrantSwatch
                ?: palette.mutedSwatch

            if (swatch != null) {
                String.format("#%06X", 0xFFFFFF and swatch.rgb)
            } else {
                "#1E1B4B"
            }
        } catch (_: Exception) {
            "#1E1B4B"
        }
    }

    private fun filterLocalCatalog(
        category: String?,
        searchQuery: String?,
        page: Int,
        pageSize: Int
    ): List<Wallpaper> {
        var filtered = localCatalog.toList()

        if (!category.isNullOrBlank() && !category.equals("All", ignoreCase = true)) {
            filtered = filtered.filter { it.category.equals(category, ignoreCase = true) }
        }

        if (!searchQuery.isNullOrBlank()) {
            val q = searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                        it.category.lowercase().contains(q) ||
                        it.tags.any { tag -> tag.lowercase().contains(q) }
            }
        }

        if (pageSize <= 0 || (page == 0 && pageSize >= filtered.size)) {
            return filtered
        }
        val fromIndex = page * pageSize
        if (fromIndex >= filtered.size) return emptyList()
        val toIndex = (fromIndex + pageSize).coerceAtMost(filtered.size)
        return filtered.subList(fromIndex, toIndex)
    }

    companion object {
        private fun getConfig(fieldName: String, defaultVal: String = ""): String {
            return try {
                val field = BuildConfig::class.java.getField(fieldName)
                (field.get(null) as? String)?.takeIf { it.isNotBlank() } ?: defaultVal
            } catch (_: Exception) {
                defaultVal
            }
        }

        private fun getCuratedCatalog(): List<Wallpaper> = listOf(
            Wallpaper(
                id = "paper-akatsuki-moonlit-gathering",
                title = "Akatsuki Moonlit Gathering",
                category = "Anime",
                tags = listOf("Anime", "Akatsuki", "Red Moon", "Dark", "Clouds", "Naruto"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-akatsuki-moonlit-gathering.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-akatsuki-moonlit-gathering.png",
                dominantColorHex = "#403E3E",
                downloads = 94200L,
                views = 780000L
            ),
            Wallpaper(
                id = "paper-itachi-red-moon",
                title = "Itachi Uchiha Red Moon",
                category = "Anime",
                tags = listOf("Anime", "Itachi", "Sharingan", "Red Moon", "Uchiha", "Crow"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-itachi-red-moon.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-itachi-red-moon.png",
                dominantColorHex = "#231B19",
                downloads = 108400L,
                views = 912000L
            ),
            Wallpaper(
                id = "paper-kakashi-sharingan-moon",
                title = "Kakashi Hatake Sharingan Moon",
                category = "Anime",
                tags = listOf("Anime", "Kakashi", "Sharingan", "Moon", "Ninja", "Chidori"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-kakashi-sharingan-moon.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-kakashi-sharingan-moon.png",
                dominantColorHex = "#47413F",
                downloads = 88300L,
                views = 720000L
            ),
            Wallpaper(
                id = "paper-obito-spiral-mask",
                title = "Obito Uchiha Spiral Mask",
                category = "Anime",
                tags = listOf("Anime", "Obito", "Spiral Mask", "Kamui", "Moon", "Dark"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-obito-spiral-mask.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-obito-spiral-mask.png",
                dominantColorHex = "#2E2C2B",
                downloads = 96500L,
                views = 815000L
            ),
            Wallpaper(
                id = "paper-minimal-capsule-stack",
                title = "Minimal Pill Capsule Stack",
                category = "Minimal",
                tags = listOf("Minimal", "Pills", "Capsule", "Red Accent", "3D", "AMOLED"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-minimal-capsule-stack.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-minimal-capsule-stack.png",
                dominantColorHex = "#6B4649",
                downloads = 76200L,
                views = 634000L
            ),
            Wallpaper(
                id = "paper-solitary-red-boat",
                title = "Solitary Red Boat in Dark Ocean",
                category = "Nature",
                tags = listOf("Nature", "Ocean", "Waves", "Red Boat", "Aerial", "AMOLED"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-solitary-red-boat.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-solitary-red-boat.png",
                dominantColorHex = "#1F2022",
                downloads = 83400L,
                views = 698000L
            ),
            Wallpaper(
                id = "paper-cherry-blossom-black",
                title = "Cherry Blossom Crimson Buds",
                category = "Nature",
                tags = listOf("Nature", "Sakura", "Cherry Blossom", "Branch", "Minimal", "AMOLED"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-cherry-blossom-black.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-cherry-blossom-black.png",
                dominantColorHex = "#131010",
                downloads = 71800L,
                views = 589000L
            ),
            Wallpaper(
                id = "paper-black-feather-red-spine",
                title = "Black Feather Red Spine",
                category = "AMOLED",
                tags = listOf("AMOLED", "Feather", "Crimson Spine", "Minimal", "Filigree", "Dark"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-black-feather-red-spine.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-black-feather-red-spine.png",
                dominantColorHex = "#110F0F",
                downloads = 68900L,
                views = 574000L
            ),
            Wallpaper(
                id = "paper-eclipse-spheres-red-glow",
                title = "Eclipse Spheres Neon Red Glow",
                category = "AMOLED",
                tags = listOf("AMOLED", "Spheres", "Eclipse", "Red Glow", "Geometry", "Curvature"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-eclipse-spheres-red-glow.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-eclipse-spheres-red-glow.png",
                dominantColorHex = "#281B1B",
                downloads = 85200L,
                views = 743000L
            ),
            Wallpaper(
                id = "paper-eclipse-spheres-amber-glow",
                title = "Eclipse Spheres Amber Glow",
                category = "AMOLED",
                tags = listOf("AMOLED", "Spheres", "Eclipse", "Amber Glow", "Geometry", "Warm"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-eclipse-spheres-amber-glow.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-eclipse-spheres-amber-glow.png",
                dominantColorHex = "#5A3325",
                downloads = 64100L,
                views = 530000L
            ),
            Wallpaper(
                id = "paper-crimson-amber-fluid-flow",
                title = "Crimson & Amber Fluid Petals",
                category = "AMOLED",
                tags = listOf("AMOLED", "Fluid Flow", "Crimson", "Amber", "3D Flow", "Abstract"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-crimson-amber-fluid-flow.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-crimson-amber-fluid-flow.png",
                dominantColorHex = "#7B2513",
                downloads = 79400L,
                views = 672000L
            ),
            Wallpaper(
                id = "paper-monochrome-crimson-petal",
                title = "Monochrome Wave Crimson Petal",
                category = "AMOLED",
                tags = listOf("AMOLED", "Monochrome", "Wave", "Crimson Petal", "Minimal", "Abstract"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-monochrome-crimson-petal.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-monochrome-crimson-petal.png",
                dominantColorHex = "#4A2C2E",
                downloads = 73500L,
                views = 612000L
            ),
            Wallpaper(
                id = "paper-red-solar-eclipse",
                title = "Red Solar Eclipse Minimal",
                category = "AMOLED",
                tags = listOf("AMOLED", "Eclipse", "Solar", "Corona", "Red Sun", "Space"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-red-solar-eclipse.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-red-solar-eclipse.png",
                dominantColorHex = "#190303",
                downloads = 81200L,
                views = 701000L
            ),
            Wallpaper(
                id = "paper-crescent-moon-minimal-orbit",
                title = "Crescent Moon Minimal Orbit",
                category = "AMOLED",
                tags = listOf("AMOLED", "Moon", "Crescent", "Orbit", "Space", "Minimal"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-crescent-moon-minimal-orbit.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-crescent-moon-minimal-orbit.png",
                dominantColorHex = "#181818",
                downloads = 69300L,
                views = 590000L
            ),
            Wallpaper(
                id = "paper-red-moon-misty-forest",
                title = "Red Moon Misty Forest",
                category = "Nature",
                tags = listOf("Nature", "Red Moon", "Forest", "Fog", "Mountains", "Dark Pines"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-red-moon-misty-forest.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-red-moon-misty-forest.png",
                dominantColorHex = "#292222",
                downloads = 91400L,
                views = 765000L
            ),
            Wallpaper(
                id = "paper-circular-speaker-grille",
                title = "Circular Speaker Grille",
                category = "Minimal",
                tags = listOf("Minimal", "Audio", "Acoustic", "Speaker", "Grille", "Hardware"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-circular-speaker-grille.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-circular-speaker-grille.png",
                dominantColorHex = "#1C1C1C",
                downloads = 58200L,
                views = 480000L
            ),
            Wallpaper(
                id = "paper-concentric-metallic-rings",
                title = "Concentric Metallic Rings",
                category = "Minimal",
                tags = listOf("Minimal", "Aperture", "Concentric", "Rings", "Precision", "Optics"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-concentric-metallic-rings.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-concentric-metallic-rings.png",
                dominantColorHex = "#080808",
                downloads = 62700L,
                views = 512000L
            ),
            Wallpaper(
                id = "paper-dotted-sound-wave",
                title = "Dotted Sound Wave Minimal",
                category = "Minimal",
                tags = listOf("Minimal", "Sound Wave", "Dots", "Audio", "AMOLED", "Digital"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-dotted-sound-wave.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-dotted-sound-wave.png",
                dominantColorHex = "#000000",
                downloads = 55400L,
                views = 460000L
            ),
            Wallpaper(
                id = "paper-geometric-circles-diagram",
                title = "Geometric Circles Diagram",
                category = "Minimal",
                tags = listOf("Minimal", "Geometry", "Diagram", "Concentric", "Circles", "Linear"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-geometric-circles-diagram.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-geometric-circles-diagram.png",
                dominantColorHex = "#060606",
                downloads = 51900L,
                views = 432000L
            ),
            Wallpaper(
                id = "paper-transparent-cassette",
                title = "Transparent Audio Cassette",
                category = "Minimal",
                tags = listOf("Minimal", "Cassette", "Retro", "Transparent", "Audio", "Vintage"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-transparent-cassette.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-transparent-cassette.png",
                dominantColorHex = "#1A1919",
                downloads = 74300L,
                views = 628000L
            ),
            Wallpaper(
                id = "paper-bauhaus-geometry-ochre-red",
                title = "Bauhaus Geometry Ochre & Red",
                category = "Minimal",
                tags = listOf("Minimal", "Bauhaus", "Geometric", "Dotted", "Pill", "Modern"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-bauhaus-geometry-ochre-red.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-bauhaus-geometry-ochre-red.png",
                dominantColorHex = "#AC775C",
                downloads = 67200L,
                views = 558000L
            ),
            Wallpaper(
                id = "paper-bauhaus-geometry-monochrome-red",
                title = "Bauhaus Geometry Monochrome & Red",
                category = "Minimal",
                tags = listOf("Minimal", "Bauhaus", "Geometric", "Monochrome", "Red Accent", "Dotted"),
                thumbnailUrl = "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-bauhaus-geometry-monochrome-red.jpg",
                fullResUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/wallpapers-v1/paper-bauhaus-geometry-monochrome-red.png",
                dominantColorHex = "#8B6564",
                downloads = 63800L,
                views = 529000L
            )
        )
    }
}
