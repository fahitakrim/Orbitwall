package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import com.example.data.SplitBackendRepository
import com.example.data.local.AppDatabase
import com.example.model.AppUpdateInfo
import com.example.model.Wallpaper
import com.example.model.toFavoriteEntity
import com.example.model.toWallpaper
import com.example.ui.theme.AppThemeMode
import com.example.util.WallpaperDestination
import com.example.util.WallpaperSystemHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    val isPreviewingOnboarding: Boolean = false,
    val showInAppUpdateDialog: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val hapticEnabled: Boolean = true,
    val cacheSizeMb: Double = 0.0,
    val visibleLimit: Int = 24,
    val totalDownloads: Long = 0L,
    val isAdmin: Boolean = false,
    val isAdminConsoleOpen: Boolean = false,
    val featuredWallpaperIds: Set<String> = emptySet(),
    val onboardingWallpaperIds: Set<String> = emptySet(),
    val promotedWallpaperIds: List<String> = emptyList(),
    val demotedWallpaperIds: List<String> = emptyList(),
    val deletedWallpaperIds: Set<String> = emptySet(),
    val globalTotalDownloadsOverride: Long? = null
) {
    val featuredWallpapers: List<Wallpaper>
        get() = wallpapers.filter { featuredWallpaperIds.contains(it.id) }

    val onboardingWallpapers: List<Wallpaper>
        get() {
            val selected = wallpapers.filter { onboardingWallpaperIds.contains(it.id) }
            return if (selected.isNotEmpty()) {
                if (selected.size < 9) {
                    val remaining = wallpapers.filter { it.id !in onboardingWallpaperIds }
                    selected + remaining.take(9 - selected.size)
                } else selected
            } else wallpapers.take(12)
        }

    val totalAppDownloads: Long
        get() {
            if (globalTotalDownloadsOverride != null && globalTotalDownloadsOverride >= 0L) {
                return globalTotalDownloadsOverride
            }
            val catalogSum = wallpapers.sumOf { it.downloads }
            return if (catalogSum > 0L) catalogSum else totalDownloads
        }
}

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

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdates = MutableStateFlow(false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

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
        val isAdmin = prefs.getBoolean("is_admin", false)
        val savedDeleted = prefs.getStringSet("deleted_ids", emptySet()) ?: emptySet()
        val defaultFeatured = setOf(
            "paper-2k-prism-smoke-cube",
            "paper-2k-black-hole-singularity",
            "paper-2k-cherry-blossom-maiden",
            "paper-2k-crimson-eclipse-orbit",
            "paper-obito-split-panels",
            "paper-itachi-crimson-smoke"
        )
        val savedFeatured = prefs.getStringSet("featured_ids", defaultFeatured) ?: defaultFeatured

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
        val savedOnboarding = prefs.getStringSet("onboarding_ids", defaultOnboarding) ?: defaultOnboarding

        val savedPromotedStr = prefs.getString("promoted_ids", "") ?: ""
        val savedPromoted = if (savedPromotedStr.isNotBlank()) savedPromotedStr.split(",").filter { it.isNotBlank() } else emptyList()

        val savedDemotedStr = prefs.getString("demoted_ids", "") ?: ""
        val savedDemoted = if (savedDemotedStr.isNotBlank()) savedDemotedStr.split(",").filter { it.isNotBlank() } else emptyList()

        val globalOverride = if (prefs.contains("global_total_downloads_override")) {
            prefs.getLong("global_total_downloads_override", 0L)
        } else null

        _uiState.update {
            it.copy(
                themeMode = theme,
                hapticEnabled = haptic,
                isAdmin = isAdmin,
                deletedWallpaperIds = savedDeleted,
                featuredWallpaperIds = savedFeatured,
                onboardingWallpaperIds = savedOnboarding,
                promotedWallpaperIds = savedPromoted,
                demotedWallpaperIds = savedDemoted,
                globalTotalDownloadsOverride = globalOverride
            )
        }

        calculateCacheSize()
        loadWallpapers(category = "All")
        checkForUpdates()
    }

    private fun applyCustomRankings(
        list: List<Wallpaper>,
        promoted: List<String>,
        demoted: List<String>
    ): List<Wallpaper> {
        if (promoted.isEmpty() && demoted.isEmpty()) return list
        val promotedSet = promoted.toSet()
        val demotedSet = demoted.toSet()

        val promotedItems = promoted.mapNotNull { id -> list.find { it.id == id } }
        val demotedItems = demoted.mapNotNull { id -> list.find { it.id == id } }
        val middleItems = list.filter { it.id !in promotedSet && it.id !in demotedSet }

        return promotedItems + middleItems + demotedItems
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
            val deleted = _uiState.value.deletedWallpaperIds
            val result = splitRepository.fetchWallpapers(
                category = catFilter,
                searchQuery = query,
                forceRefresh = forceRefresh,
                deletedIds = deleted
            )
            result.onSuccess { rawList ->
                // Filter out any deleted wallpapers
                val nonDeleted = if (deleted.isNotEmpty()) rawList.filter { it.id !in deleted } else rawList

                // Augment with local download tracking increments or overrides
                val isResetInflated = prefs.getBoolean("reset_inflated_stats", false)
                val listWithLocalCounts = nonDeleted.map { wp ->
                    val overrideKey = "dl_override_${wp.id}"
                    if (prefs.contains(overrideKey)) {
                        wp.copy(downloads = prefs.getLong(overrideKey, 0L))
                    } else if (isResetInflated) {
                        val realDl = com.example.data.DownloadTracker.getLocalDownloadCount(getApplication(), wp.id)
                        wp.copy(downloads = realDl)
                    } else {
                        val localInc = com.example.data.DownloadTracker.getLocalDownloadCount(getApplication(), wp.id)
                        if (localInc > 0) wp.copy(downloads = wp.downloads + localInc) else wp
                    }
                }

                // In the ALL page, sort by download count (Highest downloads first)
                val sortedList = if (catFilter == null || catFilter.equals("All", ignoreCase = true)) {
                    listWithLocalCounts.sortedByDescending { it.downloads }
                } else {
                    listWithLocalCounts
                }

                // Apply custom rankings (Promoted to top, Demoted to bottom)
                val rankedList = applyCustomRankings(
                    list = sortedList,
                    promoted = _uiState.value.promotedWallpaperIds,
                    demoted = _uiState.value.demotedWallpaperIds
                )

                val totalDl = com.example.data.DownloadTracker.getTotalDownloads(getApplication())
                _uiState.update { it.copy(wallpapers = rankedList, isLoading = false, totalDownloads = totalDl) }
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
                    val rankedList = applyCustomRankings(
                        list = resortList,
                        promoted = state.promotedWallpaperIds,
                        demoted = state.demotedWallpaperIds
                    )
                    val selected = if (state.selectedWallpaper?.id == wallpaper.id) {
                        state.selectedWallpaper.copy(downloads = state.selectedWallpaper.downloads + 1)
                    } else state.selectedWallpaper
                    state.copy(
                        wallpapers = rankedList,
                        selectedWallpaper = selected,
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

    // --- Admin Console & Management Handlers ---

    fun loginAdmin(username: String, code: String): Boolean {
        if (verifyAdminCode(code)) {
            prefs.edit().putBoolean("is_admin", true).apply()
            _uiState.update { it.copy(isAdmin = true, isAdminConsoleOpen = true) }
            viewModelScope.launch {
                val displayName = if (username.isNotBlank()) username else "Admin"
                _userMessage.emit("Welcome $displayName! Admin mode unlocked.")
            }
            return true
        }
        return false
    }

    private fun verifyAdminCode(code: String): Boolean {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(code.trim().toByteArray(Charsets.UTF_8))
            val hex = digest.joinToString("") { "%02x".format(it) }
            hex.equals("1e64cf2187204083be72d32fd5d3d99319e9927d42591ce983a33342e084e539", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun logoutAdmin() {
        prefs.edit().putBoolean("is_admin", false).apply()
        _uiState.update { it.copy(isAdmin = false, isAdminConsoleOpen = false) }
        viewModelScope.launch {
            _userMessage.emit("Logged out of Admin Mode")
        }
    }

    fun openAdminConsole() {
        _uiState.update { it.copy(isAdminConsoleOpen = true) }
    }

    fun closeAdminConsole() {
        _uiState.update { it.copy(isAdminConsoleOpen = false) }
    }

    fun toggleFeaturedWallpaper(wallpaperId: String) {
        val current = _uiState.value.featuredWallpaperIds.toMutableSet()
        val wasFeatured = current.contains(wallpaperId)
        if (wasFeatured) {
            current.remove(wallpaperId)
        } else {
            current.add(wallpaperId)
        }
        prefs.edit().putStringSet("featured_ids", current).apply()
        _uiState.update { it.copy(featuredWallpaperIds = current) }
        viewModelScope.launch {
            val title = _uiState.value.wallpapers.find { it.id == wallpaperId }?.title ?: "Wallpaper"
            _userMessage.emit(if (wasFeatured) "Unpinned '$title' from Featured" else "Pinned '$title' to Featured on Home")
        }
    }

    fun toggleOnboardingWallpaper(wallpaperId: String) {
        val current = _uiState.value.onboardingWallpaperIds.toMutableSet()
        val wasOnboarding = current.contains(wallpaperId)
        if (wasOnboarding) {
            current.remove(wallpaperId)
        } else {
            current.add(wallpaperId)
        }
        prefs.edit().putStringSet("onboarding_ids", current).apply()
        _uiState.update { it.copy(onboardingWallpaperIds = current) }
        viewModelScope.launch {
            val title = _uiState.value.wallpapers.find { it.id == wallpaperId }?.title ?: "Wallpaper"
            _userMessage.emit(
                if (wasOnboarding) "Removed '$title' from Welcome Screen"
                else "Added '$title' to Welcome Screen (${current.size} active)"
            )
        }
    }

    fun setPreviewingOnboarding(previewing: Boolean) {
        _uiState.update { it.copy(isPreviewingOnboarding = previewing) }
    }

    fun promoteWallpaper(wallpaperId: String) {
        val currentPromoted = _uiState.value.promotedWallpaperIds.toMutableList()
        val currentDemoted = _uiState.value.demotedWallpaperIds.toMutableList()
        currentDemoted.remove(wallpaperId)

        val wasPromoted = currentPromoted.contains(wallpaperId)
        if (wasPromoted) {
            currentPromoted.remove(wallpaperId)
        } else {
            currentPromoted.remove(wallpaperId)
            currentPromoted.add(0, wallpaperId)
        }

        prefs.edit()
            .putString("promoted_ids", currentPromoted.joinToString(","))
            .putString("demoted_ids", currentDemoted.joinToString(","))
            .apply()

        val reordered = applyCustomRankings(
            list = _uiState.value.wallpapers,
            promoted = currentPromoted,
            demoted = currentDemoted
        )

        _uiState.update {
            it.copy(
                promotedWallpaperIds = currentPromoted,
                demotedWallpaperIds = currentDemoted,
                wallpapers = reordered
            )
        }

        viewModelScope.launch {
            val title = _uiState.value.wallpapers.find { it.id == wallpaperId }?.title ?: "Wallpaper"
            _userMessage.emit(
                if (wasPromoted) "Reset position for '$title'"
                else "Promoted '$title' to the top of the feed ▲"
            )
        }
    }

    fun demoteWallpaper(wallpaperId: String) {
        val currentPromoted = _uiState.value.promotedWallpaperIds.toMutableList()
        val currentDemoted = _uiState.value.demotedWallpaperIds.toMutableList()
        currentPromoted.remove(wallpaperId)

        val wasDemoted = currentDemoted.contains(wallpaperId)
        if (wasDemoted) {
            currentDemoted.remove(wallpaperId)
        } else {
            currentDemoted.remove(wallpaperId)
            currentDemoted.add(wallpaperId)
        }

        prefs.edit()
            .putString("promoted_ids", currentPromoted.joinToString(","))
            .putString("demoted_ids", currentDemoted.joinToString(","))
            .apply()

        val reordered = applyCustomRankings(
            list = _uiState.value.wallpapers,
            promoted = currentPromoted,
            demoted = currentDemoted
        )

        _uiState.update {
            it.copy(
                promotedWallpaperIds = currentPromoted,
                demotedWallpaperIds = currentDemoted,
                wallpapers = reordered
            )
        }

        viewModelScope.launch {
            val title = _uiState.value.wallpapers.find { it.id == wallpaperId }?.title ?: "Wallpaper"
            _userMessage.emit(
                if (wasDemoted) "Reset position for '$title'"
                else "Demoted '$title' to the bottom of the feed ▼"
            )
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showInAppUpdateDialog = false) }
    }

    fun updateWallpaperDownloadCount(wallpaperId: String, newCount: Long) {
        val cleanCount = maxOf(0L, newCount)
        prefs.edit().putLong("dl_override_$wallpaperId", cleanCount).apply()
        com.example.data.DownloadTracker.setLocalDownloadCount(getApplication(), wallpaperId, cleanCount)

        _uiState.update { state ->
            val updatedList = state.wallpapers.map { wp ->
                if (wp.id == wallpaperId) wp.copy(downloads = cleanCount) else wp
            }
            val rankedList = applyCustomRankings(
                list = updatedList,
                promoted = state.promotedWallpaperIds,
                demoted = state.demotedWallpaperIds
            )
            val selected = if (state.selectedWallpaper?.id == wallpaperId) {
                state.selectedWallpaper.copy(downloads = cleanCount)
            } else state.selectedWallpaper
            state.copy(wallpapers = rankedList, selectedWallpaper = selected)
        }

        viewModelScope.launch {
            val title = _uiState.value.wallpapers.find { it.id == wallpaperId }?.title ?: "Wallpaper"
            _userMessage.emit("Updated downloads for '$title' to ${String.format(java.util.Locale.US, "%,d", cleanCount)}")
        }
    }

    fun updateGlobalTotalDownloads(newTotal: Long?) {
        if (newTotal == null || newTotal < 0L) {
            prefs.edit().remove("global_total_downloads_override").apply()
            _uiState.update { it.copy(globalTotalDownloadsOverride = null) }
            viewModelScope.launch {
                val current = _uiState.value.totalAppDownloads
                _userMessage.emit("Total downloads synced to catalog sum (${String.format(java.util.Locale.US, "%,d", current)})")
            }
        } else {
            prefs.edit().putLong("global_total_downloads_override", newTotal).apply()
            com.example.data.DownloadTracker.setTotalDownloads(getApplication(), newTotal)
            _uiState.update { it.copy(globalTotalDownloadsOverride = newTotal) }
            viewModelScope.launch {
                _userMessage.emit("Total app downloads set to ${String.format(java.util.Locale.US, "%,d", newTotal)}")
            }
        }
    }

    fun resetInflatedDownloadStats() {
        prefs.edit().putBoolean("reset_inflated_stats", true).apply()
        prefs.edit().remove("global_total_downloads_override").apply()
        com.example.data.DownloadTracker.resetAllStats(getApplication())

        val allPrefs = prefs.all
        val editor = prefs.edit()
        allPrefs.keys.forEach { key ->
            if (key.startsWith("dl_override_")) {
                editor.remove(key)
            }
        }
        editor.apply()

        _uiState.update { it.copy(globalTotalDownloadsOverride = null) }
        loadWallpapers(forceRefresh = true)

        viewModelScope.launch {
            _userMessage.emit("All inflated download stats reset to verified real metrics (0+)")
        }
    }

    fun deleteWallpaper(wallpaperId: String) {
        val currentDeleted = _uiState.value.deletedWallpaperIds.toMutableSet()
        currentDeleted.add(wallpaperId)
        prefs.edit().putStringSet("deleted_ids", currentDeleted).apply()

        // Remove from featured if it was featured
        val currentFeatured = _uiState.value.featuredWallpaperIds.toMutableSet()
        currentFeatured.remove(wallpaperId)
        prefs.edit().putStringSet("featured_ids", currentFeatured).apply()

        // Remove from onboarding if it was in onboarding
        val currentOnboarding = _uiState.value.onboardingWallpaperIds.toMutableSet()
        currentOnboarding.remove(wallpaperId)
        prefs.edit().putStringSet("onboarding_ids", currentOnboarding).apply()

        // Remove from promoted/demoted rankings
        val currentPromoted = _uiState.value.promotedWallpaperIds.toMutableList()
        currentPromoted.remove(wallpaperId)
        val currentDemoted = _uiState.value.demotedWallpaperIds.toMutableList()
        currentDemoted.remove(wallpaperId)
        prefs.edit()
            .putString("promoted_ids", currentPromoted.joinToString(","))
            .putString("demoted_ids", currentDemoted.joinToString(","))
            .apply()

        // Remove from repository in-memory catalog
        splitRepository.deleteWallpaper(wallpaperId)

        val deletedTitle = _uiState.value.wallpapers.find { it.id == wallpaperId }?.title ?: "Wallpaper"

        // If currently viewing deleted wallpaper in detail, close it
        if (_uiState.value.selectedWallpaper?.id == wallpaperId) {
            closeWallpaperDetail()
        }

        // Update uiState immediately
        _uiState.update {
            it.copy(
                deletedWallpaperIds = currentDeleted,
                featuredWallpaperIds = currentFeatured,
                onboardingWallpaperIds = currentOnboarding,
                promotedWallpaperIds = currentPromoted,
                demotedWallpaperIds = currentDemoted,
                wallpapers = it.wallpapers.filter { wp -> wp.id != wallpaperId }
            )
        }

        viewModelScope.launch {
            favoriteDao.deleteFavoriteById(wallpaperId)
            _userMessage.emit("Deleted '$deletedTitle' from catalog")
        }
    }

    fun submitAdminUpload() {
        val state = _uploadState.value
        val uri = state.selectedImageUri
        if (uri == null) {
            _uploadState.update { it.copy(statusMessage = "Please select an image first") }
            return
        }
        if (state.title.isBlank()) {
            _uploadState.update { it.copy(statusMessage = "Please enter a wallpaper title") }
            return
        }

        viewModelScope.launch {
            _uploadState.update { it.copy(isUploading = true, statusMessage = "Optimizing and uploading...") }
            val tagsList = state.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val result = splitRepository.uploadWallpaperAsAdmin(
                context = getApplication(),
                imageUri = uri,
                title = state.title.trim(),
                category = state.category,
                tags = tagsList
            )
            result.onSuccess { newWp ->
                _uploadState.update {
                    it.copy(
                        isUploading = false,
                        isSuccess = true,
                        statusMessage = "Wallpaper uploaded successfully!"
                    )
                }
                _uiState.update {
                    it.copy(wallpapers = listOf(newWp) + it.wallpapers)
                }
                _userMessage.emit("New wallpaper published to catalog!")
            }.onFailure { err ->
                _uploadState.update {
                    it.copy(
                        isUploading = false,
                        isSuccess = false,
                        statusMessage = "Upload failed: ${err.message}"
                    )
                }
            }
        }
    }

    // --- Dynamic In-App Update Service ---

    fun checkForUpdates(onComplete: ((AppUpdateInfo) -> Unit)? = null) {
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            val update = withContext(Dispatchers.IO) {
                fetchLatestReleaseFromGitHub()
            }
            _isCheckingUpdates.value = false
            _updateInfo.value = update
            if (update.isUpdateAvailable) {
                _uiState.update { it.copy(showInAppUpdateDialog = true) }
            }
            onComplete?.invoke(update)
        }
    }

    fun dismissInAppUpdateDialog() {
        _uiState.update { it.copy(showInAppUpdateDialog = false) }
    }

    private fun fetchLatestReleaseFromGitHub(): AppUpdateInfo {
        val currentVersion = com.example.BuildConfig.VERSION_NAME.removePrefix("v").trim()
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // 1. Primary: Official GitHub Releases API
        try {
            val request = okhttp3.Request.Builder()
                .url("https://api.github.com/repos/fahitakrim/Orbitwall/releases/latest")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Orbitwall-App")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = org.json.JSONObject(response.body?.string() ?: "{}")
                    val tagName = json.optString("tag_name", "v$currentVersion").removePrefix("v").trim()
                    val releaseName = json.optString("name", "Orbitwall v$tagName")
                    val releaseBody = json.optString("body", "Includes bug fixes and performance improvements.")
                    val assets = json.optJSONArray("assets")
                    var downloadUrl = ""
                    var fileSizeFormatted = ""
                    if (assets != null && assets.length() > 0) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                val size = asset.optLong("size", 0L)
                                if (size > 0) {
                                    fileSizeFormatted = String.format(java.util.Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
                                }
                                break
                            }
                        }
                    }
                    if (downloadUrl.isEmpty()) {
                        downloadUrl = "https://github.com/fahitakrim/Orbitwall/releases/download/v$tagName/Orbitwall-v$tagName.apk"
                    }

                    val isNewer = isVersionNewer(latest = tagName, current = currentVersion)
                    return AppUpdateInfo(
                        isUpdateAvailable = isNewer,
                        latestVersion = "v$tagName",
                        currentVersion = "v$currentVersion",
                        releaseTitle = releaseName,
                        releaseNotes = releaseBody,
                        downloadUrl = downloadUrl,
                        fileSizeFormatted = fileSizeFormatted
                    )
                }
            }
        } catch (_: Exception) {}

        // 2. Secondary fallback: High-speed CDN version.json (Rate-limit immune)
        try {
            val cdnRequest = okhttp3.Request.Builder()
                .url("https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/version.json")
                .build()
            client.newCall(cdnRequest).execute().use { res ->
                if (res.isSuccessful) {
                    val json = org.json.JSONObject(res.body?.string() ?: "{}")
                    val tagName = json.optString("version", currentVersion).removePrefix("v").trim()
                    val title = json.optString("title", "Orbitwall v$tagName")
                    val body = json.optString("body", "Bug fixes and improvements.")
                    val dlUrl = json.optString("download_url", "https://github.com/fahitakrim/Orbitwall/releases/download/v$tagName/Orbitwall-v$tagName.apk")
                    val size = json.optString("file_size", "")
                    val isNewer = isVersionNewer(latest = tagName, current = currentVersion)
                    return AppUpdateInfo(
                        isUpdateAvailable = isNewer,
                        latestVersion = "v$tagName",
                        currentVersion = "v$currentVersion",
                        releaseTitle = title,
                        releaseNotes = body,
                        downloadUrl = dlUrl,
                        fileSizeFormatted = size
                    )
                }
            }
        } catch (_: Exception) {}

        return fallbackUpdateInfo(currentVersion)
    }

    private fun fallbackUpdateInfo(currentVersion: String): AppUpdateInfo {
        return AppUpdateInfo(
            isUpdateAvailable = false,
            latestVersion = "v$currentVersion",
            currentVersion = "v$currentVersion",
            releaseTitle = "Orbitwall v$currentVersion",
            releaseNotes = "You are running the latest version of Orbitwall.",
            downloadUrl = "https://github.com/fahitakrim/Orbitwall/releases"
        )
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}

