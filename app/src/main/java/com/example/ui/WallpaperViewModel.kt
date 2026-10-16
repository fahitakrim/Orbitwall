package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SplitBackendRepository
import com.example.data.local.AppDatabase
import com.example.model.Wallpaper
import com.example.model.toFavoriteEntity
import com.example.model.toWallpaper
import com.example.util.WallpaperDestination
import com.example.util.WallpaperSystemHelper
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
 * Navigation destination tabs for the main app container.
 */
enum class AppScreen {
    EXPLORE,
    FAVORITES,
    ADMIN_UPLOAD
}

data class WallpaperUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCategory: String = "Recents",
    val searchQuery: String = "",
    val isSearchBarActive: Boolean = false,
    val selectedWallpaper: Wallpaper? = null,
    val previewMode: PreviewOverlayMode = PreviewOverlayMode.LOCK_SCREEN,
    val isApplyingWallpaper: Boolean = false,
    val isDownloading: Boolean = false,
    val currentScreen: AppScreen = AppScreen.EXPLORE,
    val isOnboardingCompleted: Boolean = false
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

    private val splitRepository = SplitBackendRepository()
    private val database = AppDatabase.getInstance(application)
    private val favoriteDao = database.favoriteWallpaperDao()

    private val _uiState = MutableStateFlow(WallpaperUiState())
    val uiState: StateFlow<WallpaperUiState> = _uiState.asStateFlow()

    private val _uploadState = MutableStateFlow(UploadUiState())
    val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

    // One-time transient events for Snackbars and notifications
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val categories = listOf(
        "Recents",
        "Trending",
        "Popular",
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
        loadWallpapers(category = "Recents")
    }

    fun loadWallpapers(category: String? = _uiState.value.selectedCategory, query: String? = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val catFilter = when (category) {
                "Recents", "Trending", "Popular", "All", null -> null
                else -> category
            }
            val result = splitRepository.fetchWallpapers(
                category = catFilter,
                searchQuery = query
            )
            result.onSuccess { list ->
                val sortedList = when (category) {
                    "Trending" -> list.sortedByDescending { it.views }
                    "Popular" -> list.sortedByDescending { it.downloads }
                    else -> list
                }
                _uiState.update { it.copy(wallpapers = sortedList, isLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                _userMessage.emit("Displaying curated 4K wallpapers collection.")
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
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

    fun applyWallpaper(destination: WallpaperDestination) {
        val wallpaper = _uiState.value.selectedWallpaper ?: return
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

    fun downloadWallpaperToGallery() {
        val wallpaper = _uiState.value.selectedWallpaper ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true) }
            val result = WallpaperSystemHelper.saveToGallery(
                context = getApplication(),
                fullResUrl = wallpaper.fullResUrl,
                title = wallpaper.title
            )
            _uiState.update { it.copy(isDownloading = false) }

            result.onSuccess {
                _userMessage.emit("Saved in 4K resolution to Pictures/AuraWall!")
            }.onFailure { error ->
                _userMessage.emit("Download failed: ${error.localizedMessage ?: "Storage error"}")
            }
        }
    }

    // --- Admin Upload Handlers ---

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

    fun submitUpload() {
        val state = _uploadState.value
        val uri = state.selectedImageUri ?: run {
            viewModelScope.launch { _userMessage.emit("Please pick an image from gallery first") }
            return
        }

        if (state.title.isBlank()) {
            viewModelScope.launch { _userMessage.emit("Please enter a title for the wallpaper") }
            return
        }

        val tagList = state.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        viewModelScope.launch {
            _uploadState.update { it.copy(isUploading = true, statusMessage = "Compressing 450px WebP thumbnail & uploading to GitHub + jsDelivr CDN...") }

            val result = splitRepository.uploadWallpaperAsAdmin(
                context = getApplication(),
                imageUri = uri,
                title = state.title.trim(),
                category = state.category,
                tags = tagList
            )

            result.onSuccess { createdWallpaper ->
                _uploadState.update {
                    it.copy(
                        isUploading = false,
                        isSuccess = true,
                        statusMessage = "Uploaded successfully! Added to ${createdWallpaper.category}."
                    )
                }
                _userMessage.emit("Wallpaper uploaded and live in catalog!")
                // Refresh catalog list
                loadWallpapers()
            }.onFailure { error ->
                _uploadState.update {
                    it.copy(
                        isUploading = false,
                        isSuccess = false,
                        statusMessage = "Upload error: ${error.localizedMessage}"
                    )
                }
                _userMessage.emit("Upload failed: ${error.localizedMessage}")
            }
        }
    }

    fun resetUploadForm() {
        _uploadState.value = UploadUiState()
    }
}
