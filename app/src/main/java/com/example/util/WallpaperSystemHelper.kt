package com.example.util

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * Wallpaper destination target for WallpaperManager.
 */
enum class WallpaperDestination(val flag: Int, val displayName: String) {
    HOME_SCREEN(WallpaperManager.FLAG_SYSTEM, "Home Screen"),
    LOCK_SCREEN(WallpaperManager.FLAG_LOCK, "Lock Screen"),
    BOTH(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK, "Both Screens")
}

/**
 * Dedicated system utility for:
 * 1. Downloading full-resolution bitmaps and applying them to Home, Lock, or Both screens via WallpaperManager.
 * 2. Saving wallpapers to the device gallery using Android MediaStore Scoped Storage.
 */
object WallpaperSystemHelper {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Downloads bitmap from [fullResUrl] and applies it to the selected destination using WallpaperManager.
     */
    suspend fun applyWallpaper(
        context: Context,
        fullResUrl: String,
        destination: WallpaperDestination
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            val appContext = context.applicationContext
            val wallpaperManager = WallpaperManager.getInstance(appContext)

            bitmap = downloadBitmap(appContext, fullResUrl)
                ?: return@withContext Result.failure(IllegalStateException("Failed to download or decode wallpaper image"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    wallpaperManager.setBitmap(bitmap, null, true, destination.flag)
                } catch (e: Exception) {
                    // Fallback to general setBitmap if specific flag is rejected by OEM implementation
                    wallpaperManager.setBitmap(bitmap)
                }
            } else {
                @Suppress("DEPRECATION")
                wallpaperManager.setBitmap(bitmap)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                bitmap?.recycle()
            } catch (_: Exception) {}
        }
    }

    /**
     * Downloads the full-resolution wallpaper and saves it into the device's Pictures/AuraWall
     * directory via MediaStore (Scoped Storage compliant for Android 10+).
     */
    suspend fun saveToGallery(
        context: Context,
        fullResUrl: String,
        title: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val appContext = context.applicationContext
            val safeTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
            val fileName = "AuraWall_${safeTitle}_${System.currentTimeMillis()}.jpg"

            val imageBytes = downloadBytes(appContext, fullResUrl)
                ?: return@withContext Result.failure(IllegalStateException("Failed to download image data"))

            val resolver = appContext.contentResolver

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AuraWall")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), contentValues)
                    ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry"))

                resolver.openOutputStream(imageUri)?.use { outputStream: OutputStream ->
                    outputStream.write(imageBytes)
                    outputStream.flush()
                } ?: return@withContext Result.failure(IllegalStateException("Failed to open output stream"))

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                Result.success(imageUri)
            } else {
                // Legacy Android 9 and lower
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val auraWallDir = File(picturesDir, "AuraWall").apply { if (!exists()) mkdirs() }
                val targetFile = File(auraWallDir, fileName)

                FileOutputStream(targetFile).use { outputStream ->
                    outputStream.write(imageBytes)
                    outputStream.flush()
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    @Suppress("DEPRECATION")
                    put(MediaStore.Images.Media.DATA, targetFile.absolutePath)
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: Uri.fromFile(targetFile)

                Result.success(imageUri)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads image as decoded Bitmap, supporting both content URIs and remote URLs.
     */
    private fun downloadBitmap(context: Context, urlOrUri: String): Bitmap? {
        return try {
            if (urlOrUri.startsWith("content://") || urlOrUri.startsWith("file://") || urlOrUri.startsWith("android.resource://")) {
                val uri = Uri.parse(urlOrUri)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } else {
                val request = Request.Builder().url(urlOrUri).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { inputStream: InputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Downloads raw bytes of the image file, supporting both content URIs and remote URLs.
     */
    private fun downloadBytes(context: Context, urlOrUri: String): ByteArray? {
        return try {
            if (urlOrUri.startsWith("content://") || urlOrUri.startsWith("file://") || urlOrUri.startsWith("android.resource://")) {
                val uri = Uri.parse(urlOrUri)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.readBytes()
                }
            } else {
                val request = Request.Builder().url(urlOrUri).build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
