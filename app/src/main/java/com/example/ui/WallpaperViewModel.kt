package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.example.data.SplitBackendRepository
import com.example.data.local.AppDatabase
import com.example.model.Wallpaper
import com.example.model.toFavoriteEntity
import com.example.model.toWallpaper
import com.example.ui.theme.AppThemeMode
import com.example.util.WallpaperDestination
import com.example.util.WallpaperSystemHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Preview overlay modes for the full screen wallpaper detail view.
 */
enum class PreviewOverlayMode {
    LOCK_SCREEN,
    HOME_SCREEN,
    CLEAN
}

/**
 * Navigation destination tabs for the main app container:
 * 1. EXPLORE (Home)
 * 2. FAVORITES (Heart)
 * 3. SETTINGS (Gear)
 */
enum class AppScreen {
    EXPLORE,
    FAVORITES,
    SETTINGS
}

data class WallpaperUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val isSearchBarActive: Boolean = false,
    val selectedWallpaper: Wallpaper? = null,
    val previewMode: PreviewOverlayMode = PreviewOverlayMode.CLEAN,
    val isApplyingWallpaper: Boolean = false,
    val isDownloading: Boolean = false,
    val currentScreen: AppScreen = AppScreen.EXPLORE,
    val isOnboardingCompleted: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val hapticEnabled: Boolean = true,
    val cacheSizeMb: Double = 0.0,
    val visibleLimit: Int = 24,
    val totalDownloads: Long = 0L
)

data class UploadUiState(
    val selectedImageUri: Uri? = null,
    val title: String = "",
    val category: String = "Nature",
    val tags: String = "Nature, 4K, Landscape",
    val isUploading: Boolean = false,
    val statusMessage: String? = null,
    val isSuccess: Boolean = false
)

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {

    private val splitRepository = SplitBackendRepository(context = application)
    private val database = AppDatabase.getInstance(application)
    private val favoriteDao = database.favoriteWallpaperDao()
    private val prefs = application.getSharedPreferences("orbitwall_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(WallpaperUiState())
    val uiState: StateFlow<WallpaperUiState> = _uiState.asStateFlow()

    private val _uploadState = MutableStateFlow(UploadUiState())
    val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

    // One-time transient events for Snackbars and notifications
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val categories = listOf(
        "All",
        "AMOLED",
        "Anime",
        "Minimal",
        "Nature"
    )

    // Reactive flow of favorite wallpapers from Room Database
    val favoriteWallpapers: StateFlow<List<Wallpaper>> = favoriteDao.getAllFavorites()
        .map { list -> list.map { it.toWallpaper() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive set of favorited IDs for O(1) heart icon checking in grids
    val favoriteIds: StateFlow<Set<String>> = favoriteDao.getAllFavorites()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    init {
        val savedTheme = prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val theme = try { AppThemeMode.valueOf(savedTheme) } catch (_: Exception) { AppThemeMode.DARK }
        val haptic = prefs.getBoolean("haptic_enabled", true)
        _uiState.update { it.copy(themeMode = theme, hapticEnabled = haptic) }

        calculateCacheSize()
        loadWallpapers(category = "All")
    }

    fun loadWallpapers(
        category: String? = _uiState.value.selectedCategory,
        query: String? = _uiState.value.searchQuery,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val catFilter = when (category) {
                "All", null -> null
                else -> category
            }
            val result = splitRepository.fetchWallpapers(
                category = catFilter,
                searchQuery = query,
                forceRefresh = forceRefresh
            )
            result.onSuccess { rawList ->
                // Augment with local download tracking increments
                val listWithLocalCounts = rawList.map { wp ->
                    val localInc = com.example.data.DownloadTracker.getLocalDownloadCount(getApplication(), wp.id)
                    if (localInc > 0) wp.copy(downloads = wp.downloads + localInc) else wp
                }

                // In the ALL page, sort by download count (Highest downloads first)
                val sortedList = if (catFilter == null || catFilter.equals("All", ignoreCase = true)) {
                    listWithLocalCounts.sortedByDescending { it.downloads }
                } else {
                    listWithLocalCounts
                }

                val totalDl = com.example.data.DownloadTracker.getTotalDownloads(getApplication())
                _uiState.update { it.copy(wallpapers = sortedList, isLoading = false, totalDownloads = totalDl) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                _userMessage.emit("Displaying curated Orbitwall collection.")
            }
        }
    }

    fun loadMoreWallpapers() {
        _uiState.update { it.copy(visibleLimit = it.visibleLimit + 24) }
    }

    fun refreshWallpapers() {
        loadWallpapers(forceRefresh = true)
        calculateCacheSize()
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category, visibleLimit = 24) }
        loadWallpapers(category = category, query = _uiState.value.searchQuery)
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        loadWallpapers(category = _uiState.value.selectedCategory, query = newQuery)
    }

    fun toggleSearchBar(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchBarActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            )
        }
        if (!active) {
            loadWallpapers(category = _uiState.value.selectedCategory, query = "")
        }
    }

    fun completeOnboarding() {
        _uiState.update { it.copy(isOnboardingCompleted = true) }
    }

    fun openWallpaperDetail(wallpaper: Wallpaper) {
        _uiState.update { it.copy(selectedWallpaper = wallpaper) }
    }

    fun closeWallpaperDetail() {
        _uiState.update { it.copy(selectedWallpaper = null) }
    }

    fun setPreviewMode(mode: PreviewOverlayMode) {
        _uiState.update { it.copy(previewMode = mode) }
    }

    fun navigateToScreen(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, selectedWallpaper = null) }
        if (screen == AppScreen.SETTINGS) {
            calculateCacheSize()
        }
    }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            val isFav = favoriteDao.isFavoriteDirect(wallpaper.id)
            if (isFav) {
                favoriteDao.deleteFavoriteById(wallpaper.id)
                _userMessage.emit("Removed from Favorites")
            } else {
                favoriteDao.insertFavorite(wallpaper.toFavoriteEntity())
                _userMessage.emit("Added to Favorites ❤️")
            }
        }
    }

    fun setSelectedWallpaper(wallpaper: Wallpaper) {
        _uiState.update { it.copy(selectedWallpaper = wallpaper) }
    }

    fun applyWallpaper(destination: WallpaperDestination, targetWallpaper: Wallpaper? = null) {
        val wallpaper = targetWallpaper ?: _uiState.value.selectedWallpaper ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingWallpaper = true) }
            val result = WallpaperSystemHelper.applyWallpaper(
                context = getApplication(),
                fullResUrl = wallpaper.fullResUrl,
                destination = destination
            )
            _uiState.update { it.copy(isApplyingWallpaper = false) }

            result.onSuccess {
                _userMessage.emit("Wallpaper applied to ${destination.displayName}!")
            }.onFailure { error ->
                _userMessage.emit("Failed to set wallpaper: ${error.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun downloadWallpaperToGallery(targetWallpaper: Wallpaper? = null) {
        val wallpaper = targetWallpaper ?: _uiState.value.selectedWallpaper ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            val result = WallpaperSystemHelper.saveToGallery(
                context = getApplication(),
                fullResUrl = wallpaper.fullResUrl,
                title = wallpaper.title
            )
            _uiState.update { it.copy(isDownloading = false) }

            result.onSuccess {
                com.example.data.DownloadTracker.recordDownload(
                    context = getApplication(),
                    wallpaperId = wallpaper.id,
                    wallpaperTitle = wallpaper.title
                )
                // Update in-memory wallpaper downloads count & total downloads
                _uiState.update { state ->
                    val updatedList = state.wallpapers.map {
                        if (it.id == wallpaper.id) it.copy(downloads = it.downloads + 1) else it
                    }
                    val resortList = if (state.selectedCategory.equals("All", ignoreCase = true)) {
                        updatedList.sortedByDescending { it.downloads }
                    } else {
                        updatedList
                    }
                    state.copy(
                        wallpapers = resortList,
                        totalDownloads = com.example.data.DownloadTracker.getTotalDownloads(getApplication())
                    )
                }
                _userMessage.emit("Saved in 4K resolution to Pictures/Orbitwall!")
            }.onFailure { error ->
                _userMessage.emit("Download failed: ${error.localizedMessage ?: "Storage error"}")
            }
        }
    }

    // --- Settings, Theme & Cache Management ---

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptic_enabled", enabled).apply()
        _uiState.update { it.copy(hapticEnabled = enabled) }
    }

    fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val cacheDir = app.cacheDir.resolve("orbitwall_image_cache")
            var bytes = 0L
            if (cacheDir.exists()) {
                cacheDir.walkTopDown().forEach { file ->
                    if (file.isFile) bytes += file.length()
                }
            }
            val sizeMb = bytes / (1024.0 * 1024.0)
            _uiState.update { it.copy(cacheSizeMb = sizeMb) }
        }
    }

    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    fun clearImageCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            try {
                // Clear Coil memory and disk caches
                val imageLoader = Coil.imageLoader(app)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()

                val cacheDir = app.cacheDir.resolve("orbitwall_image_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                    cacheDir.mkdirs()
                }
                _uiState.update { it.copy(cacheSizeMb = 0.0) }
                _userMessage.emit("Image cache cleared successfully")
            } catch (e: Exception) {
                _userMessage.emit("Failed to clear cache: ${e.message}")
            }
        }
    }

    // --- Admin Upload Handlers (Retained for Service/Repository compatibility) ---

    fun onUploadImageSelected(uri: Uri?) {
        _uploadState.update {
            it.copy(
                selectedImageUri = uri,
                statusMessage = null,
                isSuccess = false
            )
        }
    }

    fun onUploadTitleChanged(title: String) {
        _uploadState.update { it.copy(title = title) }
    }

    fun onUploadCategoryChanged(category: String) {
        _uploadState.update { it.copy(category = category) }
    }

    fun onUploadTagsChanged(tags: String) {
        _uploadState.update { it.copy(tags = tags) }
    }

    fun resetUploadForm() {
        _uploadState.value = UploadUiState()
    }
}
