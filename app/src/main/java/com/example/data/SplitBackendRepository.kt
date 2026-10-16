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

    // Local cached fallback catalog ensures instant, smooth app previews
    private val localCatalog = mutableListOf<Wallpaper>().apply {
        addAll(getCuratedCatalog())
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
     * Part 1: Fetches paginated wallpapers from Supabase PostgreSQL REST API.
     */
    suspend fun fetchWallpapers(
        category: String? = null,
        searchQuery: String? = null,
        page: Int = 0,
        pageSize: Int = 20
    ): Result<List<Wallpaper>> = withContext(Dispatchers.IO) {
        if (!isSupabaseConfigured()) {
            return@withContext Result.success(filterLocalCatalog(category, searchQuery, page, pageSize))
        }

        try {
            val urlBuilder = StringBuilder("$supabaseUrl/rest/v1/wallpapers?select=*&order=created_at.desc")
            urlBuilder.append("&limit=").append(pageSize)
            urlBuilder.append("&offset=").append(page * pageSize)

            if (!category.isNullOrBlank() && !category.equals("All", ignoreCase = true)) {
                urlBuilder.append("&category=eq.").append(category)
            }

            if (!searchQuery.isNullOrBlank()) {
                val q = searchQuery.trim()
                urlBuilder.append("&or=(title.ilike.*$q*,category.ilike.*$q*)")
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.success(filterLocalCatalog(category, searchQuery, page, pageSize))
            }

            val jsonBody = response.body?.string().orEmpty()
            val list = wallpaperListAdapter.fromJson(jsonBody) ?: emptyList()
            Result.success(list)
        } catch (_: Exception) {
            Result.success(filterLocalCatalog(category, searchQuery, page, pageSize))
        }
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
                id = "paper-01-crimson-flow",
                title = "Crimson Flow Abstract",
                category = "AMOLED",
                tags = listOf("AMOLED", "Abstract", "Crimson", "Minimal", "3D Flow", "Dark"),
                thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#D00000",
                downloads = 48200L,
                views = 412000L
            ),
            Wallpaper(
                id = "paper-02-eclipse-spheres",
                title = "Eclipse Horizon Spheres",
                category = "Minimal",
                tags = listOf("Minimal", "Spheres", "Eclipse", "AMOLED", "Red Rim", "Geometry"),
                thumbnailUrl = "https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#C01525",
                downloads = 39400L,
                views = 328000L
            ),
            Wallpaper(
                id = "paper-03-hooded-assassin",
                title = "Hooded Assassin Silver Blade",
                category = "Anime",
                tags = listOf("Anime", "Cyberpunk", "Assassin", "Katana", "Cyber", "Dark"),
                thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#0D0E15",
                downloads = 62100L,
                views = 524000L
            ),
            Wallpaper(
                id = "paper-04-bauhaus-geometry",
                title = "Bauhaus Minimal Geometry",
                category = "Minimal",
                tags = listOf("Minimal", "Bauhaus", "Geometric", "Pill", "Modern", "Abstract"),
                thumbnailUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#EAB308",
                downloads = 28700L,
                views = 215000L
            ),
            Wallpaper(
                id = "paper-05-samurai-katana-moon",
                title = "Samurai Crimson Katana Moon",
                category = "Anime",
                tags = listOf("Anime", "Samurai", "Crimson", "Katana", "Moon", "Warrior"),
                thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#990000",
                downloads = 74500L,
                views = 689000L
            ),
            Wallpaper(
                id = "paper-06-silver-hair-lunar",
                title = "Silver Haired Cyber Maiden",
                category = "Anime",
                tags = listOf("Anime", "Cyberpunk", "Portrait", "Silver Hair", "Cyborg", "Futuristic"),
                thumbnailUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#1E1E2D",
                downloads = 83200L,
                views = 760000L
            ),
            Wallpaper(
                id = "paper-07-silver-hair-teal-eyes",
                title = "Silver Haired Teal Gaze",
                category = "Anime",
                tags = listOf("Anime", "Cyberpunk", "Teal Eyes", "Silver Hair", "Dark", "Portrait"),
                thumbnailUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#0F172A",
                downloads = 59800L,
                views = 492000L
            ),
            Wallpaper(
                id = "paper-08-crimson-plume-knight",
                title = "Crimson Plume Obsidian Knight",
                category = "Anime",
                tags = listOf("Anime", "Knight", "Crimson Plume", "Armor", "Sword", "Moon"),
                thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#B91C1C",
                downloads = 67300L,
                views = 580000L
            ),
            Wallpaper(
                id = "paper-09-metallic-origami-crane",
                title = "Metallic Origami Crane",
                category = "Minimal",
                tags = listOf("Minimal", "Origami", "Crane", "Geometric", "Chrome", "AMOLED"),
                thumbnailUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#E2E8F0",
                downloads = 41200L,
                views = 345000L
            ),
            Wallpaper(
                id = "paper-10-liquid-chrome-sculpture",
                title = "Liquid Chrome Infinity Sculpture",
                category = "AMOLED",
                tags = listOf("AMOLED", "Liquid Chrome", "Sculpture", "Mercury", "Fluid", "Red Glow"),
                thumbnailUrl = "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#C02026",
                downloads = 53800L,
                views = 461000L
            ),
            Wallpaper(
                id = "paper-11-wired-earphones-orbit",
                title = "Wired Audio Circular Orbit",
                category = "Minimal",
                tags = listOf("Minimal", "Audio", "Earphones", "Orbit", "Industrial", "Dark"),
                thumbnailUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#EF4444",
                downloads = 32400L,
                views = 289000L
            ),
            Wallpaper(
                id = "paper-12-biometric-fingerprint",
                title = "Biometric Fingerprint Red Center",
                category = "Minimal",
                tags = listOf("Minimal", "Fingerprint", "Biometric", "Tech", "Red Center", "AMOLED"),
                thumbnailUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#DC2626",
                downloads = 36900L,
                views = 310000L
            ),
            Wallpaper(
                id = "paper-13-transparent-cassette",
                title = "Transparent Audio Cassette",
                category = "Minimal",
                tags = listOf("Minimal", "Cassette", "Retro", "Transparent", "Audio", "Vintage"),
                thumbnailUrl = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#E5E7EB",
                downloads = 45100L,
                views = 390000L
            ),
            Wallpaper(
                id = "paper-14-exploded-watch-mechanism",
                title = "Exploded Chronograph Watch Mechanism",
                category = "Minimal",
                tags = listOf("Minimal", "Watch", "Gears", "Horology", "Mechanism", "Engineering"),
                thumbnailUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1522335789203-aabd1fc54bc9?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#DC2626",
                downloads = 51200L,
                views = 440000L
            ),
            Wallpaper(
                id = "paper-15-glass-butterfly-crimson",
                title = "Glass Butterfly Red Accents",
                category = "AMOLED",
                tags = listOf("AMOLED", "Butterfly", "Glass", "Crimson", "Ethereal", "Wings"),
                thumbnailUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#E11D48",
                downloads = 58900L,
                views = 512000L
            ),
            Wallpaper(
                id = "paper-16-concentric-metallic-rings",
                title = "Concentric Metallic Aperture Rings",
                category = "Minimal",
                tags = listOf("Minimal", "Aperture", "Concentric", "Rings", "Precision", "Optics"),
                thumbnailUrl = "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#EF4444",
                downloads = 34500L,
                views = 298000L
            ),
            Wallpaper(
                id = "paper-17-circular-speaker-grille",
                title = "Circular Acoustic Speaker Grille",
                category = "Minimal",
                tags = listOf("Minimal", "Acoustic", "Speaker", "Grille", "Audio", "Hardware"),
                thumbnailUrl = "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#DC2626",
                downloads = 29400L,
                views = 245000L
            ),
            Wallpaper(
                id = "paper-18-crescent-moon-orbit",
                title = "Crescent Moon Minimal Orbit",
                category = "AMOLED",
                tags = listOf("AMOLED", "Moon", "Crescent", "Orbit", "Space", "Minimal"),
                thumbnailUrl = "https://images.unsplash.com/photo-1532693322450-2cb5c511067d?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1532693322450-2cb5c511067d?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#D1D5DB",
                downloads = 47800L,
                views = 389000L
            ),
            Wallpaper(
                id = "paper-19-black-feather-red-spine",
                title = "Black Feather Red Spine",
                category = "AMOLED",
                tags = listOf("AMOLED", "Feather", "Filigree", "Crimson Spine", "Minimal", "Ethereal"),
                thumbnailUrl = "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1544967082-d9d25d867d66?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#EF4444",
                downloads = 42100L,
                views = 360000L
            ),
            Wallpaper(
                id = "paper-20-red-solar-eclipse",
                title = "Red Solar Eclipse Minimal",
                category = "AMOLED",
                tags = listOf("AMOLED", "Eclipse", "Solar", "Corona", "Red Sun", "Space"),
                thumbnailUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#B91C1C",
                downloads = 56200L,
                views = 478000L
            ),
            Wallpaper(
                id = "paper-21-red-moon-misty-forest",
                title = "Red Moon Misty Mountain Forest",
                category = "Nature",
                tags = listOf("Nature", "Red Moon", "Forest", "Fog", "Mountains", "Dark Pines"),
                thumbnailUrl = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?w=450&auto=format&fit=crop&q=80",
                fullResUrl = "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?w=2160&auto=format&fit=crop&q=95",
                dominantColorHex = "#B91C1C",
                downloads = 61400L,
                views = 530000L
            )
        )
    }
}
