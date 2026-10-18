package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Wallpaper
import com.example.ui.UploadUiState
import com.example.ui.theme.DotMatrixFont
import com.example.ui.theme.NothingRed

enum class AdminTab {
    MANAGE,
    UPLOAD
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminConsoleScreen(
    wallpapers: List<Wallpaper>,
    featuredWallpaperIds: Set<String>,
    onboardingWallpaperIds: Set<String> = emptySet(),
    uploadState: UploadUiState,
    categories: List<String>,
    onToggleFeatured: (String) -> Unit,
    onToggleOnboarding: (String) -> Unit = {},
    onPreviewWelcomeScreen: () -> Unit = {},
    onDeleteWallpaper: (String) -> Unit,
    onImageSelected: (Uri?) -> Unit,
    onTitleChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onTagsChanged: (String) -> Unit,
    onSubmitUpload: () -> Unit,
    onResetUploadForm: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    var currentTab by remember { mutableStateOf(AdminTab.MANAGE) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("ALL") }
    var wallpaperToDelete by remember { mutableStateOf<Wallpaper?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onImageSelected(uri)
    }

    // Helper: Identify candidate duplicate wallpapers (e.g. matching keywords, cars, towers, dunes, waves, etc.)
    val duplicateKeywords = listOf("car", "smoke", "burnout", "drift", "tower", "skyscraper", "waves", "layers", "paper", "cabin", "deer", "cat", "anime girl", "eyes")
    val candidateDuplicateIds = remember(wallpapers) {
        val dupes = mutableSetOf<String>()
        for (i in wallpapers.indices) {
            for (j in i + 1 until wallpapers.size) {
                val w1 = wallpapers[i]
                val w2 = wallpapers[j]
                val t1 = w1.title.lowercase()
                val t2 = w2.title.lowercase()

                // Check keyword overlap
                val matchesKeyword = duplicateKeywords.any { kw -> t1.contains(kw) && t2.contains(kw) }
                // Check similar IDs (e.g. brutalist-tower-a and brutalist-tower-b)
                val baseId1 = w1.id.replace(Regex("-[ab0-9]+$"), "")
                val baseId2 = w2.id.replace(Regex("-[ab0-9]+$"), "")
                val idMatch = baseId1 == baseId2

                if (matchesKeyword || idMatch) {
                    dupes.add(w1.id)
                    dupes.add(w2.id)
                }
            }
        }
        dupes
    }

    // Filter wallpapers based on filter tab and search query
    val filteredWallpapers = remember(wallpapers, activeFilter, searchQuery, featuredWallpaperIds, onboardingWallpaperIds) {
        wallpapers.filter { wp ->
            val matchesFilter = when (activeFilter) {
                "ALL" -> true
                "WELCOME" -> onboardingWallpaperIds.contains(wp.id)
                "FEATURED" -> featuredWallpaperIds.contains(wp.id)
                "DUPLICATES" -> candidateDuplicateIds.contains(wp.id)
                else -> wp.category.equals(activeFilter, ignoreCase = true)
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                wp.title.lowercase().contains(q) ||
                        wp.category.lowercase().contains(q) ||
                        wp.id.lowercase().contains(q) ||
                        wp.tags.any { it.lowercase().contains(q) }
            }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("admin_console_screen")
    ) {
        // --- Top Header ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ADMIN CONSOLE",
                                fontFamily = DotMatrixFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 1.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•",
                                color = NothingRed,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "AUTHENTICATED // ROOT SESSION",
                            fontFamily = DotMatrixFont,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Preview Welcome Screen Button
                    OutlinedButton(
                        onClick = onPreviewWelcomeScreen,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00E676)
                        ),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Preview Welcome",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PREVIEW",
                            fontFamily = DotMatrixFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Logout Button
                    OutlinedButton(
                        onClick = onLogout,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = NothingRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LOGOUT",
                            fontFamily = DotMatrixFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                    }
                }
            }
        }

        // --- Navigation Tabs: MANAGE CATALOG vs UPLOAD WALLPAPER ---
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Tab 1: Manage
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (currentTab == AdminTab.MANAGE) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                        .clickable { currentTab = AdminTab.MANAGE }
                        .padding(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MANAGE (${wallpapers.size})",
                            fontFamily = DotMatrixFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = if (currentTab == AdminTab.MANAGE) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tab 2: Upload
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (currentTab == AdminTab.UPLOAD) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                        .clickable { currentTab = AdminTab.UPLOAD }
                        .padding(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = if (currentTab == AdminTab.UPLOAD) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UPLOAD",
                            fontFamily = DotMatrixFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = if (currentTab == AdminTab.UPLOAD) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- Content View ---
        when (currentTab) {
            AdminTab.MANAGE -> {
                // Search & Filter Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search catalog or duplicate name...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips Row
                    val filterOptions = listOf(
                        "ALL" to "ALL (${wallpapers.size})",
                        "WELCOME" to "🚀 WELCOME (${onboardingWallpaperIds.size})",
                        "DUPLICATES" to "⚠️ DUPLICATES (${candidateDuplicateIds.size})",
                        "FEATURED" to "⭐ FEATURED (${featuredWallpaperIds.size})",
                        "AMOLED" to "AMOLED",
                        "ANIME" to "ANIME",
                        "MINIMAL" to "MINIMAL",
                        "NATURE" to "NATURE"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filterOptions) { (key, label) ->
                            val isSelected = activeFilter == key
                            val isDup = key == "DUPLICATES"
                            val isFeat = key == "FEATURED"
                            val isWelcome = key == "WELCOME"

                            val bg = when {
                                isSelected && isDup -> NothingRed
                                isSelected && isFeat -> Color(0xFFFFD700)
                                isSelected && isWelcome -> Color(0xFF00E676)
                                isSelected -> MaterialTheme.colorScheme.onBackground
                                isDup -> NothingRed.copy(alpha = 0.12f)
                                isFeat -> Color(0xFFFFD700).copy(alpha = 0.12f)
                                isWelcome -> Color(0xFF00E676).copy(alpha = 0.14f)
                                else -> MaterialTheme.colorScheme.surface
                            }

                            val textColor = when {
                                isSelected && isDup -> Color.White
                                isSelected && isFeat -> Color.Black
                                isSelected && isWelcome -> Color.Black
                                isSelected -> MaterialTheme.colorScheme.background
                                isDup -> NothingRed
                                isFeat -> Color(0xFFFFD700)
                                isWelcome -> Color(0xFF00E676)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            val borderColor = when {
                                isSelected -> bg
                                isDup -> NothingRed.copy(alpha = 0.45f)
                                isFeat -> Color(0xFFFFD700).copy(alpha = 0.45f)
                                isWelcome -> Color(0xFF00E676).copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.outline
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable { activeFilter = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = DotMatrixFont,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }

                // Wallpapers List
                if (filteredWallpapers.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO WALLPAPERS MATCH CRITERIA",
                                fontFamily = DotMatrixFont,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredWallpapers, key = { it.id }) { wp ->
                            val isFeatured = featuredWallpaperIds.contains(wp.id)
                            val isOnboarding = onboardingWallpaperIds.contains(wp.id)
                            val isCandidateDuplicate = candidateDuplicateIds.contains(wp.id)

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCandidateDuplicate) NothingRed.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Wallpaper Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(width = 68.dp, height = 96.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.DarkGray)
                                    ) {
                                        AsyncImage(
                                            model = wp.thumbnailUrl,
                                            contentDescription = wp.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (isOnboarding) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(4.dp)
                                                    .size(16.dp)
                                                    .background(Color(0xFF00E676), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }

                                        if (isFeatured) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(16.dp)
                                                    .background(Color(0xFFFFD700), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Wallpaper Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = wp.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Category Chip
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = wp.category.uppercase(),
                                                    fontFamily = DotMatrixFont,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Dominant Color Dot
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(
                                                        try {
                                                             Color(android.graphics.Color.parseColor(wp.dominantColorHex))
                                                        } catch (_: Exception) { Color.Gray },
                                                        CircleShape
                                                    )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Text(
                                            text = wp.id,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (isCandidateDuplicate) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "⚠️ POSSIBLE DUPLICATE",
                                                fontFamily = DotMatrixFont,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingRed
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Action Buttons: Welcome, Feature & Delete
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Welcome Screen Toggle Button
                                        Button(
                                            onClick = { onToggleOnboarding(wp.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isOnboarding) Color(0xFF00E676) else MaterialTheme.colorScheme.surface,
                                                contentColor = if (isOnboarding) Color.Black else MaterialTheme.colorScheme.onSurface
                                            ),
                                            border = if (!isOnboarding) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isOnboarding) Icons.Default.CheckCircle else Icons.Default.Wallpaper,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isOnboarding) "WELCOME ✓" else "+ WELCOME",
                                                fontFamily = DotMatrixFont,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Feature Toggle Button
                                        Button(
                                            onClick = { onToggleFeatured(wp.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isFeatured) Color(0xFFFFD700) else MaterialTheme.colorScheme.surface,
                                                contentColor = if (isFeatured) Color.Black else MaterialTheme.colorScheme.onSurface
                                            ),
                                            border = if (!isFeatured) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFeatured) Icons.Default.Star else Icons.Default.StarOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isFeatured) "FEATURED" else "PIN",
                                                fontFamily = DotMatrixFont,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Delete Button
                                        Button(
                                            onClick = { wallpaperToDelete = wp },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NothingRed.copy(alpha = 0.12f),
                                                contentColor = NothingRed
                                            ),
                                            border = BorderStroke(1.dp, NothingRed.copy(alpha = 0.35f)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "DELETE",
                                                fontFamily = DotMatrixFont,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminTab.UPLOAD -> {
                // Upload Form View
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .padding(bottom = 60.dp)
                ) {
                    // Bandwidth Notice
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.DataUsage,
                                contentDescription = null,
                                tint = NothingRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Edge Cloud Storage Architecture",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "High-res images are uploaded to the GitHub release repository with lightweight 720px thumbnails streamed via jsDelivr edge CDN.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Image Picker
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        if (uploadState.selectedImageUri != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = uploadState.selectedImageUri,
                                    contentDescription = "Selected Wallpaper",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color.Black.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(bottomStart = 16.dp),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Change", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(NothingRed.copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = NothingRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "TAP TO SELECT 2K/4K IMAGE",
                                    fontFamily = DotMatrixFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title Input
                    OutlinedTextField(
                        value = uploadState.title,
                        onValueChange = onTitleChanged,
                        label = { Text("Wallpaper Title", fontSize = 12.sp) },
                        placeholder = { Text("e.g. Crimson Eclipse Singularity") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Selection Chips
                    Text(
                        text = "CATEGORY",
                        fontFamily = DotMatrixFont,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val uploadCategories = listOf("AMOLED", "Anime", "Minimal", "Nature")
                        uploadCategories.forEach { cat ->
                            val isSelected = uploadState.category.equals(cat, ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .clickable { onCategoryChanged(cat) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = cat.uppercase(),
                                    fontFamily = DotMatrixFont,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tags Input
                    OutlinedTextField(
                        value = uploadState.tags,
                        onValueChange = onTagsChanged,
                        label = { Text("Tags (Comma-separated)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. AMOLED, Red, Dark, 4K") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NothingRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Status Message Feedback
                    if (uploadState.statusMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uploadState.statusMessage,
                            color = if (uploadState.isSuccess) Color(0xFF10B981) else NothingRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = onSubmitUpload,
                        enabled = !uploadState.isUploading && uploadState.selectedImageUri != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NothingRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uploadState.isUploading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "OPTIMIZING & UPLOADING...", fontFamily = DotMatrixFont, fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "PUBLISH WALLPAPER", fontFamily = DotMatrixFont, fontSize = 12.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    }

    // --- Delete Confirmation Dialog ---
    wallpaperToDelete?.let { targetWp ->
        AlertDialog(
            onDismissRequest = { wallpaperToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(NothingRed, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DELETE WALLPAPER?",
                        fontFamily = DotMatrixFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Permanently remove \"${targetWp.title}\" (${targetWp.category}) from your app catalog feed?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ID: ${targetWp.id}",
                        fontFamily = DotMatrixFont,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = targetWp.id
                        wallpaperToDelete = null
                        onDeleteWallpaper(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "DELETE", fontFamily = DotMatrixFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { wallpaperToDelete = null },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(text = "CANCEL", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
