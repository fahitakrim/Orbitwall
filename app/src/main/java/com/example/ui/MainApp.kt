package com.example.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AdminConsoleScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WallpaperDetailScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NothingBlue
import com.example.ui.theme.NothingDarkDock
import com.example.ui.theme.NothingLightDock
import com.example.ui.theme.NothingRed
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainApp(
    viewModel: WallpaperViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsStateWithLifecycle()
    val favoriteWallpapers by viewModel.favoriteWallpapers.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun triggerHaptic() {
        if (uiState.hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = when (uiState.currentScreen) {
            AppScreen.EXPLORE -> 0
            AppScreen.FAVORITES -> 1
            AppScreen.SETTINGS -> 2
        },
        pageCount = { 3 }
    )

    // Sync Pager state -> ViewModel navigation state with haptic feedback
    LaunchedEffect(pagerState.currentPage) {
        val targetScreen = when (pagerState.currentPage) {
            0 -> AppScreen.EXPLORE
            1 -> AppScreen.FAVORITES
            else -> AppScreen.SETTINGS
        }
        if (uiState.currentScreen != targetScreen) {
            triggerHaptic()
            viewModel.navigateToScreen(targetScreen)
        }
    }

    // Sync ViewModel navigation state -> Pager scroll
    LaunchedEffect(uiState.currentScreen) {
        val targetPage = when (uiState.currentScreen) {
            AppScreen.EXPLORE -> 0
            AppScreen.FAVORITES -> 1
            AppScreen.SETTINGS -> 2
        }
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    MyApplicationTheme(darkTheme = uiState.themeMode == AppThemeMode.DARK) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!uiState.isOnboardingCompleted || uiState.isPreviewingOnboarding) {
                // Screen 1: Welcome / Onboarding Screen
                OnboardingScreen(
                    wallpapers = uiState.onboardingWallpapers,
                    onStartExplore = {
                        triggerHaptic()
                        if (uiState.isPreviewingOnboarding) {
                            viewModel.setPreviewingOnboarding(false)
                        } else {
                            viewModel.completeOnboarding()
                        }
                    },
                    isPreview = uiState.isPreviewingOnboarding,
                    onExitPreview = {
                        triggerHaptic()
                        viewModel.setPreviewingOnboarding(false)
                    }
                )
            } else {
                // Main App Experience with Full-Screen Swipeable HorizontalPager
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier.padding(bottom = 85.dp)
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> {
                                    HomeScreen(
                                        wallpapers = uiState.wallpapers,
                                        featuredWallpapers = uiState.featuredWallpapers,
                                        isLoading = uiState.isLoading,
                                        categories = viewModel.categories,
                                        selectedCategory = uiState.selectedCategory,
                                        searchQuery = uiState.searchQuery,
                                        favoriteIds = favoriteIds,
                                        visibleLimit = uiState.visibleLimit,
                                        onSelectCategory = {
                                            triggerHaptic()
                                            viewModel.selectCategory(it)
                                        },
                                        onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                        onWallpaperClick = {
                                            triggerHaptic()
                                            viewModel.openWallpaperDetail(it)
                                        },
                                        onFavoriteClick = {
                                            triggerHaptic()
                                            viewModel.toggleFavorite(it)
                                        },
                                        onRefresh = {
                                            triggerHaptic()
                                            viewModel.refreshWallpapers()
                                        },
                                        onLoadMore = {
                                            triggerHaptic()
                                            viewModel.loadMoreWallpapers()
                                        }
                                    )
                                }
                                1 -> {
                                    FavoritesScreen(
                                        favorites = favoriteWallpapers,
                                        favoriteIds = favoriteIds,
                                        onWallpaperClick = {
                                            triggerHaptic()
                                            viewModel.openWallpaperDetail(it)
                                        },
                                        onFavoriteClick = {
                                            triggerHaptic()
                                            viewModel.toggleFavorite(it)
                                        },
                                        onNavigateToExplore = {
                                            triggerHaptic()
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(0)
                                            }
                                        }
                                    )
                                }
                                2 -> {
                                    SettingsScreen(
                                        currentTheme = uiState.themeMode,
                                        isHapticEnabled = uiState.hapticEnabled,
                                        cacheSizeMb = uiState.cacheSizeMb,
                                        updateInfo = updateInfo,
                                        isCheckingUpdates = isCheckingUpdates,
                                        onCheckForUpdates = { onComplete ->
                                            viewModel.checkForUpdates(onComplete)
                                        },
                                        onThemeChange = { viewModel.setThemeMode(it) },
                                        onHapticToggle = { viewModel.setHapticEnabled(it) },
                                        onClearCache = { viewModel.clearImageCache() },
                                        isAdmin = uiState.isAdmin,
                                        onAdminLogin = { username, code ->
                                            viewModel.loginAdmin(username, code)
                                        },
                                        onOpenAdminConsole = {
                                            viewModel.openAdminConsole()
                                        },
                                        onAdminLogout = {
                                            viewModel.logoutAdmin()
                                        }
                                    )
                                }
                            }
                        }

                        // Floating Glassmorphic Bottom Dock (Symmetric Concentric Capsule with Gesture Swipe)
                        if (uiState.selectedWallpaper == null && !uiState.isAdminConsoleOpen) {
                            NothingBottomNavDock(
                                pagerState = pagerState,
                                isDarkMode = uiState.themeMode == AppThemeMode.DARK,
                                onTabSelected = { targetPage ->
                                    triggerHaptic()
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(targetPage)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 18.dp)
                            )
                        }
                    }
                }
            }

            // Full Screen Wallpaper Detail Screen
            uiState.selectedWallpaper?.let { selected ->
                WallpaperDetailScreen(
                    wallpaper = selected,
                    wallpapers = uiState.wallpapers,
                    favoriteIds = favoriteIds,
                    previewMode = uiState.previewMode,
                    isApplying = uiState.isApplyingWallpaper,
                    isDownloading = uiState.isDownloading,
                    isAdmin = uiState.isAdmin,
                    featuredWallpaperIds = uiState.featuredWallpaperIds,
                    onboardingWallpaperIds = uiState.onboardingWallpaperIds,
                    onBackClick = {
                        triggerHaptic()
                        viewModel.closeWallpaperDetail()
                    },
                    onPreviewModeChange = {
                        triggerHaptic()
                        viewModel.setPreviewMode(it)
                    },
                    onToggleFavorite = { wp ->
                        triggerHaptic()
                        viewModel.toggleFavorite(wp)
                    },
                    onApplyWallpaper = { wp, destination ->
                        triggerHaptic()
                        viewModel.applyWallpaper(destination, wp)
                    },
                    onDownloadToGallery = { wp ->
                        triggerHaptic()
                        viewModel.downloadWallpaperToGallery(wp)
                    },
                    onWallpaperChanged = { wp ->
                        viewModel.setSelectedWallpaper(wp)
                    },
                    onToggleFeatured = { wp ->
                        viewModel.toggleFeaturedWallpaper(wp.id)
                    },
                    onToggleOnboarding = { wp ->
                        viewModel.toggleOnboardingWallpaper(wp.id)
                    },
                    onDeleteWallpaper = { wp ->
                        viewModel.deleteWallpaper(wp.id)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Full Screen Admin Console Overlay
            if (uiState.isAdminConsoleOpen) {
                AdminConsoleScreen(
                    wallpapers = uiState.wallpapers,
                    featuredWallpaperIds = uiState.featuredWallpaperIds,
                    onboardingWallpaperIds = uiState.onboardingWallpaperIds,
                    uploadState = uploadState,
                    categories = viewModel.categories,
                    onToggleFeatured = { id -> viewModel.toggleFeaturedWallpaper(id) },
                    onToggleOnboarding = { id -> viewModel.toggleOnboardingWallpaper(id) },
                    onPreviewWelcomeScreen = { viewModel.setPreviewingOnboarding(true) },
                    onDeleteWallpaper = { id -> viewModel.deleteWallpaper(id) },
                    onImageSelected = { uri -> viewModel.onUploadImageSelected(uri) },
                    onTitleChanged = { title -> viewModel.onUploadTitleChanged(title) },
                    onCategoryChanged = { cat -> viewModel.onUploadCategoryChanged(cat) },
                    onTagsChanged = { tags -> viewModel.onUploadTagsChanged(tags) },
                    onSubmitUpload = { viewModel.submitAdminUpload() },
                    onResetUploadForm = { viewModel.resetUploadForm() },
                    onLogout = { viewModel.logoutAdmin() },
                    onNavigateBack = { viewModel.closeAdminConsole() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Nothing OS Symmetrical Concentric Capsule Pill Navigation Dock:
 * - Outer capsule: 56dp height with 28dp radius
 * - Inner margin: 4dp padding all around
 * - Inner sliding indicator: 48dp height with 24dp radius (perfect concentric curvature R_inner = R_outer - padding)
 * - Symmetrically blends with outer capsule edges on left (Home) and right (Settings)
 * - Swipe gestures on dock itself and full screen seamlessly shift active pill
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NothingBottomNavDock(
    pagerState: PagerState,
    isDarkMode: Boolean,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var totalDragDelta by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        totalDragDelta += delta
    }

    // Continuous page position: [0.0f .. 2.0f]
    val continuousPage = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, 2f)

    val homePillColor = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onBackground
    val activeIndicatorColor = when {
        continuousPage <= 1f -> lerp(homePillColor, NothingRed, continuousPage)
        else -> lerp(NothingRed, NothingBlue, continuousPage - 1f)
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (isDarkMode) NothingDarkDock else NothingLightDock,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 16.dp,
        modifier = modifier
            .height(56.dp)
            .width(240.dp)
            .clip(RoundedCornerShape(28.dp))
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    val threshold = 30f
                    if (velocity < -200f || totalDragDelta < -threshold) {
                        // Swiped left -> move right (next tab)
                        val target = (pagerState.currentPage + 1).coerceAtMost(2)
                        onTabSelected(target)
                    } else if (velocity > 200f || totalDragDelta > threshold) {
                        // Swiped right -> move left (prev tab)
                        val target = (pagerState.currentPage - 1).coerceAtLeast(0)
                        onTabSelected(target)
                    }
                    totalDragDelta = 0f
                }
            )
            .testTag("floating_dock")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalWidth = maxWidth
                val slotWidth = totalWidth / 3f

                // 1. Thin Vertical Divider Lines (|) Layer
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(18.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                // 2. Sliding Indicator Pill Layer (Concentric 24dp radius matching 28dp outer - 4dp padding)
                val indicatorOffset = slotWidth * continuousPage
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(slotWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(activeIndicatorColor)
                )

                // 3. Tab Icons & Interactive Click Targets Layer
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 0: Home
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(0) }
                    ) {
                        val dist0 = (continuousPage - 0f).absoluteValue.coerceIn(0f, 1f)
                        val iconTint = lerp(
                            start = if (isDarkMode) Color.Black else MaterialTheme.colorScheme.background,
                            stop = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            fraction = (dist0 * 2f).coerceIn(0f, 1f)
                        )
                        Icon(
                            imageVector = if (continuousPage < 0.5f) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Tab 1: Favorites
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(1) }
                    ) {
                        val dist1 = (continuousPage - 1f).absoluteValue.coerceIn(0f, 1f)
                        val iconTint = lerp(
                            start = Color.White,
                            stop = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            fraction = (dist1 * 2f).coerceIn(0f, 1f)
                        )
                        Icon(
                            imageVector = if (continuousPage in 0.5f..1.5f) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorites",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Tab 2: Settings
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(2) }
                    ) {
                        val dist2 = (continuousPage - 2f).absoluteValue.coerceIn(0f, 1f)
                        val iconTint = lerp(
                            start = Color.White,
                            stop = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            fraction = (dist2 * 2f).coerceIn(0f, 1f)
                        )
                        Icon(
                            imageVector = if (continuousPage > 1.5f) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
