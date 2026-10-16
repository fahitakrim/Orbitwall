package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.FavoriteWallpaperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteWallpaperDao {

    @Query("SELECT * FROM favorite_wallpapers ORDER BY savedAtTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteWallpaperEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_wallpapers WHERE id = :wallpaperId)")
    fun isFavorite(wallpaperId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_wallpapers WHERE id = :wallpaperId)")
    suspend fun isFavoriteDirect(wallpaperId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(entity: FavoriteWallpaperEntity)

    @Delete
    suspend fun deleteFavorite(entity: FavoriteWallpaperEntity)

    @Query("DELETE FROM favorite_wallpapers WHERE id = :wallpaperId")
    suspend fun deleteFavoriteById(wallpaperId: String)
}
