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
import com.example.util.WallpaperDestination
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperDetailScreen(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    previewMode: PreviewOverlayMode,
    isApplying: Boolean,
    isDownloading: Boolean,
    onBackClick: () -> Unit,
    onPreviewModeChange: (PreviewOverlayMode) -> Unit,
    onToggleFavorite: () -> Unit,
    onApplyWallpaper: (WallpaperDestination) -> Unit,
    onDownloadToGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    var showApplySheet by remember { mutableStateOf(false) }
    var isGlassCardExpanded by remember { mutableStateOf(true) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val formattedViews = remember(wallpaper.views) {
        NumberFormat.getNumberInstance(Locale.US).format(wallpaper.views)
    }
    val formattedDownloads = remember(wallpaper.downloads) {
        NumberFormat.getNumberInstance(Locale.US).format(wallpaper.downloads)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("wallpaper_detail_screen")
    ) {
        // Fullscreen Wallpaper Image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(wallpaper.fullResUrl)
                .crossfade(true)
                .placeholder(android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor(wallpaper.dominantColorHex)))
                .build(),
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .testTag("full_res_image")
        )

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

        // Top Bar: Circular Back Button & Circular Favorite Button (Matching Screen 3)
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

            // Circular Heart Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("detail_favorite_button")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFE11D48) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom Frosted Glassmorphic Card (Matching Screen 3)
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = navBarPadding + 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xCC181726), // Frosted dark glass
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_glass_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Wallpaper Title
                    Text(
                        text = wallpaper.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Metric Chips: Views | Downloads | Featured in Category
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricFrostedChip(
                            topText = formattedViews,
                            bottomText = "Views",
                            modifier = Modifier.weight(1f)
                        )
                        MetricFrostedChip(
                            topText = formattedDownloads,
                            bottomText = "Downloads",
                            modifier = Modifier.weight(1f)
                        )
                        MetricFrostedChip(
                            topText = wallpaper.category,
                            bottomText = "Featured in",
                            modifier = Modifier.weight(1.1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Horizontal Scrollable Tag Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val tagsList = if (wallpaper.tags.isNotEmpty()) wallpaper.tags else listOf("4K", "Wallpaper", "UltraHD")
                        items(tagsList) { tag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bottom Action Dock: Gallery Save Button | Cycle Mode (>>>) | Apply Checkmark Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Gallery Save Pill Button
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(enabled = !isDownloading) { onDownloadToGallery() }
                                .testTag("detail_download_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            ) {
                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        color = Color.Black,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Save to Gallery",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 2. Cycle Mode / Arrows Indicator `> > >`
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable {
                                    val nextMode = when (previewMode) {
                                        PreviewOverlayMode.LOCK_SCREEN -> PreviewOverlayMode.HOME_SCREEN
                                        PreviewOverlayMode.HOME_SCREEN -> PreviewOverlayMode.CLEAN
                                        PreviewOverlayMode.CLEAN -> PreviewOverlayMode.LOCK_SCREEN
                                    }
                                    onPreviewModeChange(nextMode)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 18.dp)
                            ) {
                                Text(
                                    text = "> > >",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // 3. Apply / Checkmark Action Pill Button
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .clickable(enabled = !isApplying) { showApplySheet = true }
                                .testTag("detail_apply_checkmark")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 22.dp)
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
                        }
                    }
                }
            }
        }

        // Apply Destination Modal Bottom Sheet
        if (showApplySheet) {
            ModalBottomSheet(
                onDismissRequest = { showApplySheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF161524),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                ) {
                    Text(
                        text = "Apply 4K Wallpaper",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose screen destination to set wallpaper",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    ApplyDestinationItem(
                        title = "Home Screen",
                        subtitle = "Apply to main launcher screen",
                        icon = Icons.Default.Home,
                        onClick = {
                            showApplySheet = false
                            onApplyWallpaper(WallpaperDestination.HOME_SCREEN)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApplyDestinationItem(
                        title = "Lock Screen",
                        subtitle = "Apply to device lock screen",
                        icon = Icons.Default.Lock,
                        onClick = {
                            showApplySheet = false
                            onApplyWallpaper(WallpaperDestination.LOCK_SCREEN)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ApplyDestinationItem(
                        title = "Both Screens",
                        subtitle = "Apply to Home and Lock screens",
                        icon = Icons.Default.Smartphone,
                        onClick = {
                            showApplySheet = false
                            onApplyWallpaper(WallpaperDestination.BOTH)
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
private fun MetricFrostedChip(
    topText: String,
    bottomText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(
                text = topText,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = bottomText,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
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
        color = Color(0xFF222034),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
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
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
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
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun LockScreenOverlay() {
    val currentTime = remember {
        val sdf = SimpleDateFormat("h:mm", Locale.getDefault())
        sdf.format(Date())
    }
    val currentDate = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 90.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = currentTime,
                fontSize = 70.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Text(
                text = currentDate,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 240.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashlightOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(2.dp))
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeScreenOverlay() {
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
                .padding(top = 90.dp)
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
                .padding(bottom = 240.dp)
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
