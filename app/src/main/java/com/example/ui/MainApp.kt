package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AdminUploadScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.WallpaperDetailScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainApp(
    viewModel: WallpaperViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val favoriteWallpapers by viewModel.favoriteWallpapers.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFF7F8FC))) {
        if (!uiState.isOnboardingCompleted) {
            // Screen 1: Welcome / Onboarding Screen
            OnboardingScreen(
                wallpapers = uiState.wallpapers,
                onStartExplore = { viewModel.completeOnboarding() }
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
                containerColor = Color(0xFFF7F8FC)
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
                                    onSelectCategory = { viewModel.selectCategory(it) },
                                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                                    onWallpaperClick = { viewModel.openWallpaperDetail(it) },
                                    onFavoriteClick = { viewModel.toggleFavorite(it) },
                                    onOpenAdminUpload = { viewModel.navigateToScreen(AppScreen.ADMIN_UPLOAD) }
                                )
                            }
                            AppScreen.FAVORITES -> {
                                FavoritesScreen(
                                    favorites = favoriteWallpapers,
                                    favoriteIds = favoriteIds,
                                    onWallpaperClick = { viewModel.openWallpaperDetail(it) },
                                    onFavoriteClick = { viewModel.toggleFavorite(it) },
                                    onNavigateToExplore = { viewModel.navigateToScreen(AppScreen.EXPLORE) }
                                )
                            }
                            AppScreen.ADMIN_UPLOAD -> {
                                AdminUploadScreen(
                                    uploadState = uploadState,
                                    categories = viewModel.categories,
                                    onImageSelected = { viewModel.onUploadImageSelected(it) },
                                    onTitleChanged = { viewModel.onUploadTitleChanged(it) },
                                    onCategoryChanged = { viewModel.onUploadCategoryChanged(it) },
                                    onTagsChanged = { viewModel.onUploadTagsChanged(it) },
                                    onSubmitUpload = { viewModel.submitUpload() },
                                    onResetForm = { viewModel.resetUploadForm() },
                                    onNavigateBack = { viewModel.navigateToScreen(AppScreen.EXPLORE) }
                                )
                            }
                        }
                    }

                    // Floating Glassmorphic Bottom Navigation Dock (Matching Screen 2)
                    if (uiState.selectedWallpaper == null) {
                        FloatingBottomNavDock(
                            currentScreen = uiState.currentScreen,
                            onTabSelected = { viewModel.navigateToScreen(it) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 18.dp)
                        )
                    }
                }
            }
        }

        // Full Screen Wallpaper Detail Screen (Matching Screen 3)
        uiState.selectedWallpaper?.let { selected ->
            WallpaperDetailScreen(
                wallpaper = selected,
                isFavorite = favoriteIds.contains(selected.id),
                previewMode = uiState.previewMode,
                isApplying = uiState.isApplyingWallpaper,
                isDownloading = uiState.isDownloading,
                onBackClick = { viewModel.closeWallpaperDetail() },
                onPreviewModeChange = { viewModel.setPreviewMode(it) },
                onToggleFavorite = { viewModel.toggleFavorite(selected) },
                onApplyWallpaper = { viewModel.applyWallpaper(it) },
                onDownloadToGallery = { viewModel.downloadWallpaperToGallery() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Floating glassmorphic dock matching Screen 2 in the reference design.
 */
@Composable
private fun FloatingBottomNavDock(
    currentScreen: AppScreen,
    onTabSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xDD25223A), // Translucent frosted dark glass
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        shadowElevation = 14.dp,
        modifier = modifier
            .height(58.dp)
            .width(230.dp)
            .clip(RoundedCornerShape(32.dp))
            .testTag("floating_dock")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
        ) {
            // Tab 1: Home (white pill highlight when selected, dark icon)
            DockTabItem(
                icon = if (currentScreen == AppScreen.EXPLORE) Icons.Filled.Home else Icons.Outlined.Home,
                isSelected = currentScreen == AppScreen.EXPLORE,
                onClick = { onTabSelected(AppScreen.EXPLORE) }
            )

            // Tab 2: Categories / Grid (or Admin Upload)
            DockTabItem(
                icon = if (currentScreen == AppScreen.ADMIN_UPLOAD) Icons.Filled.GridView else Icons.Outlined.GridView,
                isSelected = currentScreen == AppScreen.ADMIN_UPLOAD,
                onClick = { onTabSelected(AppScreen.ADMIN_UPLOAD) }
            )

            // Tab 3: Favorites (Heart)
            DockTabItem(
                icon = if (currentScreen == AppScreen.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                isSelected = currentScreen == AppScreen.FAVORITES,
                onClick = { onTabSelected(AppScreen.FAVORITES) }
            )
        }
    }
}

@Composable
private fun DockTabItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(44.dp)
            .width(if (isSelected) 60.dp else 46.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color(0xFF14151C) else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(22.dp)
        )
    }
}
