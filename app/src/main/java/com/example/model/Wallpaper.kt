package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Domain model representing a wallpaper item.
 * Strictly separates thumbnailUrl (lightweight WebP for grids)
 * and fullResUrl (high-res bitmap for preview, download, and applying).
 */
@JsonClass(generateAdapter = true)
data class Wallpaper(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "category") val category: String,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "thumbnail_url") val thumbnailUrl: String,
    @Json(name = "full_res_url") val fullResUrl: String,
    @Json(name = "dominant_color_hex") val dominantColorHex: String = "#1E1B4B",
    @Json(name = "downloads") val downloads: Long = 0L,
    @Json(name = "views") val views: Long = 120000L,
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity for storing local favorite wallpapers on-device.
 */
@Entity(tableName = "favorite_wallpapers")
@TypeConverters(Converters::class)
data class FavoriteWallpaperEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val tags: List<String>,
    val thumbnailUrl: String,
    val fullResUrl: String,
    val dominantColorHex: String,
    val downloads: Long,
    val views: Long = 120000L,
    val createdAt: Long,
    val savedAtTimestamp: Long = System.currentTimeMillis()
)

/**
 * Extension mapper from domain Wallpaper to Room Favorite entity.
 */
fun Wallpaper.toFavoriteEntity(savedAt: Long = System.currentTimeMillis()): FavoriteWallpaperEntity =
    FavoriteWallpaperEntity(
        id = id,
        title = title,
        category = category,
        tags = tags,
        thumbnailUrl = thumbnailUrl,
        fullResUrl = fullResUrl,
        dominantColorHex = dominantColorHex,
        downloads = downloads,
        views = views,
        createdAt = createdAt,
        savedAtTimestamp = savedAt
    )

/**
 * Extension mapper from Room Favorite entity back to domain Wallpaper.
 */
fun FavoriteWallpaperEntity.toWallpaper(): Wallpaper =
    Wallpaper(
        id = id,
        title = title,
        category = category,
        tags = tags,
        thumbnailUrl = thumbnailUrl,
        fullResUrl = fullResUrl,
        dominantColorHex = dominantColorHex,
        downloads = downloads,
        views = views,
        createdAt = createdAt
    )

/**
 * Room TypeConverter for converting List<String> tags to and from comma-separated Strings.
 */
class Converters {
    @TypeConverter
    fun fromTagsList(tags: List<String>?): String {
        return tags?.joinToString(separator = ",") ?: ""
    }

    @TypeConverter
    fun toTagsList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
