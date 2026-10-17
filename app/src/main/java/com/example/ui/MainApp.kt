package com.example.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun MainApp(
    viewModel: WallpaperViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteWallpapers by viewModel.favoriteWallpapers.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val view = LocalView.current

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

    MyApplicationTheme(darkTheme = uiState.themeMode == AppThemeMode.DARK) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!uiState.isOnboardingCompleted) {
                // Screen 1: Welcome / Onboarding Screen
                OnboardingScreen(
                    wallpapers = uiState.wallpapers,
                    onStartExplore = {
                        triggerHaptic()
                        viewModel.completeOnboarding()
                    }
                )
            } else {
                // Main App Experience
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
                        Crossfade(
                            targetState = uiState.currentScreen,
                            label = "main_screen_fade"
                        ) { screen ->
                            when (screen) {
                                AppScreen.EXPLORE -> {
                                    HomeScreen(
                                        wallpapers = uiState.wallpapers,
                                        isLoading = uiState.isLoading,
                                        categories = viewModel.categories,
                                        selectedCategory = uiState.selectedCategory,
                                        searchQuery = uiState.searchQuery,
                                        favoriteIds = favoriteIds,
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
                                        }
                                    )
                                }
                                AppScreen.FAVORITES -> {
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
                                            viewModel.navigateToScreen(AppScreen.EXPLORE)
                                        }
                                    )
                                }
                                AppScreen.SETTINGS -> {
                                    SettingsScreen(
                                        currentTheme = uiState.themeMode,
                                        isHapticEnabled = uiState.hapticEnabled,
                                        cacheSizeMb = uiState.cacheSizeMb,
                                        onThemeChange = { viewModel.setThemeMode(it) },
                                        onHapticToggle = { viewModel.setHapticEnabled(it) },
                                        onClearCache = { viewModel.clearImageCache() }
                                    )
                                }
                            }
                        }

                        // Floating Glassmorphic Bottom Dock (3 tabs: Home, Favorites, Settings with dividers)
                        if (uiState.selectedWallpaper == null) {
                            NothingBottomNavDock(
                                currentScreen = uiState.currentScreen,
                                isDarkMode = uiState.themeMode == AppThemeMode.DARK,
                                onTabSelected = {
                                    triggerHaptic()
                                    viewModel.navigateToScreen(it)
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
                    isFavorite = favoriteIds.contains(selected.id),
                    previewMode = uiState.previewMode,
                    isApplying = uiState.isApplyingWallpaper,
                    isDownloading = uiState.isDownloading,
                    onBackClick = {
                        triggerHaptic()
                        viewModel.closeWallpaperDetail()
                    },
                    onPreviewModeChange = {
                        triggerHaptic()
                        viewModel.setPreviewMode(it)
                    },
                    onToggleFavorite = {
                        triggerHaptic()
                        viewModel.toggleFavorite(selected)
                    },
                    onApplyWallpaper = {
                        triggerHaptic()
                        viewModel.applyWallpaper(it)
                    },
                    onDownloadToGallery = {
                        triggerHaptic()
                        viewModel.downloadWallpaperToGallery()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Nothing OS 3-Tab Floating Pill Navigation Dock:
 * - Home: Contrast pill highlight
 * - Divider line (|)
 * - Favorites: Red (#E51C23) pill highlight
 * - Divider line (|)
 * - Settings: Blue (#1E6BF1) pill highlight
 */
@Composable
private fun NothingBottomNavDock(
    currentScreen: AppScreen,
    isDarkMode: Boolean,
    onTabSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = if (isDarkMode) NothingDarkDock else NothingLightDock,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 16.dp,
        modifier = modifier
            .height(58.dp)
            .width(250.dp)
            .clip(RoundedCornerShape(32.dp))
            .testTag("floating_dock")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            // Tab 1: Home (Contrast White / Dark Pill)
            DockTabItem(
                icon = if (currentScreen == AppScreen.EXPLORE) Icons.Filled.Home else Icons.Outlined.Home,
                isSelected = currentScreen == AppScreen.EXPLORE,
                activePillColor = if (isDarkMode) Color.White else MaterialTheme.colorScheme.onBackground,
                activeIconColor = if (isDarkMode) Color.Black else MaterialTheme.colorScheme.background,
                onClick = { onTabSelected(AppScreen.EXPLORE) }
            )

            // Thin Vertical Divider Line (|)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            )

            // Tab 2: Favorites (Red #E51C23 Pill)
            DockTabItem(
                icon = if (currentScreen == AppScreen.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                isSelected = currentScreen == AppScreen.FAVORITES,
                activePillColor = NothingRed,
                activeIconColor = Color.White,
                onClick = { onTabSelected(AppScreen.FAVORITES) }
            )

            // Thin Vertical Divider Line (|)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            )

            // Tab 3: Settings (Blue #1E6BF1 Pill)
            DockTabItem(
                icon = if (currentScreen == AppScreen.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                isSelected = currentScreen == AppScreen.SETTINGS,
                activePillColor = NothingBlue,
                activeIconColor = Color.White,
                onClick = { onTabSelected(AppScreen.SETTINGS) }
            )
        }
    }
}

@Composable
private fun DockTabItem(
    icon: ImageVector,
    isSelected: Boolean,
    activePillColor: Color,
    activeIconColor: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(44.dp)
            .width(if (isSelected) 58.dp else 46.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) activePillColor else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) activeIconColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )
    }
}
