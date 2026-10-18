package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Wallpaper
import com.example.ui.PreviewOverlayMode
import com.example.ui.theme.DotMatrixFont
import com.example.ui.theme.NothingRed
import com.example.util.WallpaperDestination


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperDetailScreen(
    wallpaper: Wallpaper,
    wallpapers: List<Wallpaper> = emptyList(),
    favoriteIds: Set<String> = emptySet(),
    previewMode: PreviewOverlayMode,
    isApplying: Boolean,
    isDownloading: Boolean,
    onBackClick: () -> Unit,
    onPreviewModeChange: (PreviewOverlayMode) -> Unit,
    onToggleFavorite: (Wallpaper) -> Unit = {},
    onApplyWallpaper: (Wallpaper, WallpaperDestination) -> Unit = { _, _ -> },
    onDownloadToGallery: (Wallpaper) -> Unit = {},
    onWallpaperChanged: (Wallpaper) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    var showApplySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayList = remember(wallpapers, wallpaper) {
        if (wallpapers.isNotEmpty()) wallpapers else listOf(wallpaper)
    }
    val initialIndex = remember(displayList, wallpaper) {
        displayList.indexOfFirst { it.id == wallpaper.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { displayList.size }
    )
    val currentWallpaper = displayList.getOrNull(pagerState.currentPage) ?: wallpaper
    val isCurrentFavorite = favoriteIds.contains(currentWallpaper.id)

    LaunchedEffect(pagerState.currentPage) {
        val active = displayList.getOrNull(pagerState.currentPage)
        if (active != null) {
            onWallpaperChanged(active)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("wallpaper_detail_screen")
    ) {
        // Horizontal Pager for Swipe between Wallpapers
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("full_res_pager")
        ) { page ->
            val pageWallpaper = displayList[page]
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(pageWallpaper.fullResUrl)
                    .allowHardware(true)
                    .allowRgb565(false)
                    .crossfade(true)
                    .placeholder(android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor(pageWallpaper.dominantColorHex)))
                    .error(android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor(pageWallpaper.dominantColorHex)))
                    .build(),
                contentDescription = pageWallpaper.title,
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("full_res_image")
            )
        }

        // Simulated Overlay based on active mode
        when (previewMode) {
            PreviewOverlayMode.LOCK_SCREEN -> {
                LockScreenOverlay()
            }
            PreviewOverlayMode.HOME_SCREEN -> {
                HomeScreenOverlay()
            }
            PreviewOverlayMode.CLEAN -> {
                // Clean mode: wallpaper only
            }
        }

        // Top Bar: Circular Back Button & Preview Mode Switcher Pill
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = statusBarPadding + 12.dp, start = 18.dp, end = 18.dp)
        ) {
            // Circular Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Top Center: Preview Mode Switcher Pill
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    .padding(4.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                DetailModeTab(
                    title = "Lock",
                    isSelected = previewMode == PreviewOverlayMode.LOCK_SCREEN,
                    onClick = { onPreviewModeChange(PreviewOverlayMode.LOCK_SCREEN) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                DetailModeTab(
                    title = "Home",
                    isSelected = previewMode == PreviewOverlayMode.HOME_SCREEN,
                    onClick = { onPreviewModeChange(PreviewOverlayMode.HOME_SCREEN) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                DetailModeTab(
                    title = "Clean",
                    isSelected = previewMode == PreviewOverlayMode.CLEAN,
                    onClick = { onPreviewModeChange(PreviewOverlayMode.CLEAN) }
                )
            }

            // Balancing placeholder (matches back button size to keep center switcher centered)
            Spacer(modifier = Modifier.size(46.dp))
        }

        // Minimal Floating Action Dock (3 buttons: Download | Set Wallpaper | Favorite)
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xDD121214), // Dark translucent Nothing matte glass
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
            shadowElevation = 20.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarPadding + 20.dp)
                .height(56.dp)
                .width(204.dp)
                .clip(RoundedCornerShape(28.dp))
                .testTag("detail_bottom_dock")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp)
            ) {
                // 1. Download to Gallery Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isDownloading) { onDownloadToGallery(currentWallpaper) }
                        .testTag("detail_download_pill")
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Save to Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Thin Vertical Divider (|)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )

                // 2. Direct Set / Apply Wallpaper Pill
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(40.dp)
                        .width(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable(enabled = !isApplying) { showApplySheet = true }
                        .testTag("detail_apply_checkmark")
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Set Wallpaper",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Thin Vertical Divider (|)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )

                // 3. Favorite Button (Heart)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onToggleFavorite(currentWallpaper) }
                        .testTag("detail_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isCurrentFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isCurrentFavorite) NothingRed else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Apply Destination Modal Bottom Sheet (Nothing OS Theme)
        if (showApplySheet) {
            ModalBottomSheet(
                onDismissRequest = { showApplySheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF151517),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "APPLY WALLPAPER",
                            fontFamily = DotMatrixFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            color = NothingRed,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose screen destination to set wallpaper",
                        fontSize = 13.sp,
                        color = Color(0xFF8E8E93)
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    ApplyDestinationItem(
                        title = "Home Screen",
                        subtitle = "Apply to main launcher screen",
                        icon = Icons.Default.Home,
                        onClick = {
                            showApplySheet = false
                            onApplyWallpaper(currentWallpaper, WallpaperDestination.HOME_SCREEN)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApplyDestinationItem(
                        title = "Lock Screen",
                        subtitle = "Apply to device lock screen",
                        icon = Icons.Default.Lock,
                        onClick = {
                            showApplySheet = false
                            onApplyWallpaper(currentWallpaper, WallpaperDestination.LOCK_SCREEN)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApplyDestinationItem(
                        title = "Both Screens",
                        subtitle = "Apply to Home and Lock screens",
                        icon = Icons.Default.Smartphone,
                        onClick = {
                            showApplySheet = false
                            onApplyWallpaper(currentWallpaper, WallpaperDestination.BOTH)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailModeTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ApplyDestinationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E22),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E2E34)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF8E8E93))
            }
        }
    }
}

@Composable
private fun LockScreenOverlay() {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var isFlashlightOn by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Subtle lock indicator at top center (No clock / watch)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarPadding + 76.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Bottom Left Corner: Flashlight Shortcut Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = navBarPadding + 23.dp)
                .size(50.dp)
                .background(
                    if (isFlashlightOn) Color(0xFFFFD600) else Color.Black.copy(alpha = 0.55f),
                    CircleShape
                )
                .border(
                    1.dp,
                    if (isFlashlightOn) Color(0xFFFFEA00) else Color.White.copy(alpha = 0.25f),
                    CircleShape
                )
                .clickable { isFlashlightOn = !isFlashlightOn }
        ) {
            Icon(
                imageVector = Icons.Default.FlashlightOn,
                contentDescription = "Flashlight",
                tint = if (isFlashlightOn) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Bottom Right Corner: Camera Shortcut Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = navBarPadding + 23.dp)
                .size(50.dp)
                .background(
                    if (isCameraActive) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.55f),
                    CircleShape
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                .clickable { isCameraActive = !isCameraActive }
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera",
                tint = if (isCameraActive) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun HomeScreenOverlay() {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = statusBarPadding + 76.dp)
                .height(46.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "G",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search...",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarPadding + 96.dp)
        ) {
            SimulatedDockIcon(icon = Icons.Default.Phone, label = "Phone", iconColor = Color(0xFF22C55E))
            SimulatedDockIcon(icon = Icons.Default.Search, label = "Browser", iconColor = Color(0xFF3B82F6))
            SimulatedDockIcon(icon = Icons.Default.CameraAlt, label = "Camera", iconColor = Color(0xFFEAB308))
            SimulatedDockIcon(icon = Icons.Default.Wallpaper, label = "Photos", iconColor = Color(0xFFA855F7))
        }
    }
}

@Composable
private fun SimulatedDockIcon(
    icon: ImageVector,
    label: String,
    iconColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .background(iconColor.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}
