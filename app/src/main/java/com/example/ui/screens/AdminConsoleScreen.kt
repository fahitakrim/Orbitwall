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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import com.example.ui.components.OrbitModalBottomSheet
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
import java.util.Locale
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
    UPLOAD,
    CLOUD_SYNC
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminConsoleScreen(
    wallpapers: List<Wallpaper>,
    featuredWallpaperIds: Set<String>,
    onboardingWallpaperIds: Set<String> = emptySet(),
    promotedWallpaperIds: List<String> = emptyList(),
    demotedWallpaperIds: List<String> = emptyList(),
    totalDownloads: Long = 0L,
    uploadState: UploadUiState,
    categories: List<String>,
    onToggleFeatured: (String) -> Unit,
    onToggleOnboarding: (String) -> Unit = {},
    onPromoteWallpaper: (String) -> Unit = {},
    onDemoteWallpaper: (String) -> Unit = {},
    onUpdateWallpaperDownloads: (wallpaperId: String, newCount: Long) -> Unit = { _, _ -> },
    onUpdateGlobalDownloads: (Long?) -> Unit = {},
    onResetInflatedDownloads: () -> Unit = {},
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
    onSyncToGitHub: (token: String, onComplete: (Result<String>) -> Unit) -> Unit = { _, _ -> },
    onPullFromGitHub: () -> Unit = {},
    initialGitHubToken: String = "",
    onSaveGitHubToken: (String) -> Unit = {},
    initialTelemetryUrl: String = "",
    onSaveTelemetryUrl: (String) -> Unit = {},
    onRefreshTelemetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    var currentTab by remember { mutableStateOf(AdminTab.MANAGE) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("ALL") }
    var wallpaperToDelete by remember { mutableStateOf<Wallpaper?>(null) }
    var wallpaperToEditDownloads by remember { mutableStateOf<Wallpaper?>(null) }
    var showGlobalTotalDialog by remember { mutableStateOf(false) }

    var gitHubTokenInput by remember { mutableStateOf(initialGitHubToken) }
    var telemetryUrlInput by remember { mutableStateOf(initialTelemetryUrl) }
    var isSyncingToGitHub by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }

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
    val filteredWallpapers = remember(wallpapers, activeFilter, searchQuery, featuredWallpaperIds, onboardingWallpaperIds, promotedWallpaperIds, demotedWallpaperIds) {
        wallpapers.filter { wp ->
            val matchesFilter = when (activeFilter) {
                "ALL" -> true
                "PROMOTED" -> promotedWallpaperIds.contains(wp.id)
                "DEMOTED" -> demotedWallpaperIds.contains(wp.id)
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

                Spacer(modifier = Modifier.width(8.dp))

                // Tab 3: Cloud Sync
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (currentTab == AdminTab.CLOUD_SYNC) MaterialTheme.colorScheme.onBackground else Color.Transparent)
                        .clickable { currentTab = AdminTab.CLOUD_SYNC }
                        .padding(vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (currentTab == AdminTab.CLOUD_SYNC) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SYNC",
                            fontFamily = DotMatrixFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = if (currentTab == AdminTab.CLOUD_SYNC) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- Content View ---
        when (currentTab) {
            AdminTab.MANAGE -> {
                // 1. App Downloads & Catalog Metrics Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { showGlobalTotalDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "TOTAL APP DOWNLOADS",
                                    fontFamily = DotMatrixFont,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "CALIBRATE ⚙",
                                        fontFamily = DotMatrixFont,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = NothingRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val catalogDownloads = wallpapers.sumOf { it.downloads }
                                val displayTotal = if (totalDownloads > 0L) totalDownloads else catalogDownloads
                                Text(
                                    text = String.format(Locale.US, "%,d", displayTotal),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = DotMatrixFont,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "RANKING & CATALOG",
                                fontFamily = DotMatrixFont,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "▲ ${promotedWallpaperIds.size}",
                                    fontFamily = DotMatrixFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "•",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "▼ ${demotedWallpaperIds.size}",
                                    fontFamily = DotMatrixFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9100)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "•",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${wallpapers.size} WALLS",
                                    fontFamily = DotMatrixFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Search & Filter Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
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
                        "PROMOTED" to "▲ PROMOTED (${promotedWallpaperIds.size})",
                        "DEMOTED" to "▼ DEMOTED (${demotedWallpaperIds.size})",
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
                            val isProm = key == "PROMOTED"
                            val isDem = key == "DEMOTED"

                            val bg = when {
                                isSelected && isDup -> NothingRed
                                isSelected && isFeat -> Color(0xFFFFD700)
                                isSelected && isWelcome -> Color(0xFF00E676)
                                isSelected && isProm -> Color(0xFF00E5FF)
                                isSelected && isDem -> Color(0xFFFF9100)
                                isSelected -> MaterialTheme.colorScheme.onBackground
                                isDup -> NothingRed.copy(alpha = 0.12f)
                                isFeat -> Color(0xFFFFD700).copy(alpha = 0.12f)
                                isWelcome -> Color(0xFF00E676).copy(alpha = 0.14f)
                                isProm -> Color(0xFF00E5FF).copy(alpha = 0.14f)
                                isDem -> Color(0xFFFF9100).copy(alpha = 0.14f)
                                else -> MaterialTheme.colorScheme.surface
                            }

                            val textColor = when {
                                isSelected && isDup -> Color.White
                                isSelected && isFeat -> Color.Black
                                isSelected && isWelcome -> Color.Black
                                isSelected && isProm -> Color.Black
                                isSelected && isDem -> Color.Black
                                isSelected -> MaterialTheme.colorScheme.background
                                isDup -> NothingRed
                                isFeat -> Color(0xFFFFD700)
                                isWelcome -> Color(0xFF00E676)
                                isProm -> Color(0xFF00E5FF)
                                isDem -> Color(0xFFFF9100)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            val borderColor = when {
                                isSelected -> bg
                                isDup -> NothingRed.copy(alpha = 0.45f)
                                isFeat -> Color(0xFFFFD700).copy(alpha = 0.45f)
                                isWelcome -> Color(0xFF00E676).copy(alpha = 0.5f)
                                isProm -> Color(0xFF00E5FF).copy(alpha = 0.5f)
                                isDem -> Color(0xFFFF9100).copy(alpha = 0.5f)
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
                            val isPromoted = promotedWallpaperIds.contains(wp.id)
                            val isDemoted = demotedWallpaperIds.contains(wp.id)
                            val isCandidateDuplicate = candidateDuplicateIds.contains(wp.id)

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (isPromoted || isDemoted) 1.5.dp else 1.dp,
                                    when {
                                        isPromoted -> Color(0xFF00E5FF).copy(alpha = 0.7f)
                                        isDemoted -> Color(0xFFFF9100).copy(alpha = 0.7f)
                                        isCandidateDuplicate -> NothingRed.copy(alpha = 0.45f)
                                        else -> MaterialTheme.colorScheme.outline
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Wallpaper Thumbnail with Badges
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

                                            // Status Badge: Promoted (Cyan ▲) or Demoted (Orange ▼)
                                            if (isPromoted) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(4.dp)
                                                        .size(18.dp)
                                                        .background(Color(0xFF00E5FF), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowUpward,
                                                        contentDescription = "Promoted",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            } else if (isDemoted) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(4.dp)
                                                        .size(18.dp)
                                                        .background(Color(0xFFFF9100), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowDownward,
                                                        contentDescription = "Demoted",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }

                                            // Status Badge: Featured (Gold ★)
                                            if (isFeatured) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(18.dp)
                                                        .background(Color(0xFFFFD700), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Featured",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }

                                            // Status Badge: Onboarding Welcome (Green ✓)
                                            if (isOnboarding) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(4.dp)
                                                        .size(18.dp)
                                                        .background(Color(0xFF00E676), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Welcome Screen",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Wallpaper Info & Metrics
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = wp.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

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

                                                // Download Count Chip
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                                                        .clickable { wallpaperToEditDownloads = wp }
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Download,
                                                            contentDescription = null,
                                                            tint = Color(0xFF00E5FF),
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = String.format(Locale.US, "%,d", wp.downloads),
                                                            fontFamily = DotMatrixFont,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF00E5FF)
                                                        )
                                                    }
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

                                            Spacer(modifier = Modifier.height(4.dp))

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
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Connected Segmented Button Group (Takrim Aesthetic Pillar 2: Asymmetric Radii & Hairline Borders)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // 1. Promote Button (Cyan) - Outer Left
                                        Button(
                                            onClick = { onPromoteWallpaper(wp.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isPromoted) Color(0xFF00E5FF) else MaterialTheme.colorScheme.surface,
                                                contentColor = if (isPromoted) Color.Black else Color(0xFF00E5FF)
                                            ),
                                            border = BorderStroke(1.dp, if (isPromoted) Color(0xFF00E5FF) else Color(0xFF26262A)),
                                            shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isPromoted) "TOP ✓" else "PROMOTE",
                                                fontFamily = DotMatrixFont,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // 2. Demote Button (Orange) - Inner Segment
                                        Button(
                                            onClick = { onDemoteWallpaper(wp.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDemoted) Color(0xFFFF9100) else MaterialTheme.colorScheme.surface,
                                                contentColor = if (isDemoted) Color.Black else Color(0xFFFF9100)
                                            ),
                                            border = BorderStroke(1.dp, if (isDemoted) Color(0xFFFF9100) else Color(0xFF26262A)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isDemoted) "LOW ✓" else "DEMOTE",
                                                fontFamily = DotMatrixFont,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // 3. Welcome Screen Toggle Button (Green) - Inner Segment
                                        Button(
                                            onClick = { onToggleOnboarding(wp.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isOnboarding) Color(0xFF00E676) else MaterialTheme.colorScheme.surface,
                                                contentColor = if (isOnboarding) Color.Black else MaterialTheme.colorScheme.onSurface
                                            ),
                                            border = BorderStroke(1.dp, if (isOnboarding) Color(0xFF00E676) else Color(0xFF26262A)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isOnboarding) Icons.Default.CheckCircle else Icons.Default.Wallpaper,
                                                contentDescription = null,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isOnboarding) "WELCOME ✓" else "+ WELCOME",
                                                fontFamily = DotMatrixFont,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // 4. Feature Toggle Button (Gold) - Inner Segment
                                        Button(
                                            onClick = { onToggleFeatured(wp.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isFeatured) Color(0xFFFFD700) else MaterialTheme.colorScheme.surface,
                                                contentColor = if (isFeatured) Color.Black else MaterialTheme.colorScheme.onSurface
                                            ),
                                            border = BorderStroke(1.dp, if (isFeatured) Color(0xFFFFD700) else Color(0xFF26262A)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isFeatured) Icons.Default.Star else Icons.Default.StarOutline,
                                                contentDescription = null,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isFeatured) "FEATURED" else "PIN",
                                                fontFamily = DotMatrixFont,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // 5. Download Count Calibration Button (Cyan Outline) - Inner Segment
                                        Button(
                                            onClick = { wallpaperToEditDownloads = wp },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surface,
                                                contentColor = Color(0xFF00E5FF)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFF26262A)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "COUNT ⬇",
                                                fontFamily = DotMatrixFont,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // 6. Delete Button (Red) - Outer Right
                                        Button(
                                            onClick = { wallpaperToDelete = wp },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NothingRed.copy(alpha = 0.12f),
                                                contentColor = NothingRed
                                            ),
                                            border = BorderStroke(1.dp, NothingRed.copy(alpha = 0.35f)),
                                            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                modifier = Modifier.size(11.dp)
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

            AdminTab.CLOUD_SYNC -> {
                val syncScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(syncScrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: GitHub Cloud Sync Card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = NothingRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GLOBAL ADMIN CLOUD SYNC",
                                    fontFamily = DotMatrixFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Push your featured picks, onboarding showcase, rankings, and deleted wallpapers directly to GitHub (admin_config.json). Every user's device around the world automatically fetches these settings.",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = gitHubTokenInput,
                                onValueChange = {
                                    gitHubTokenInput = it
                                    onSaveGitHubToken(it)
                                },
                                label = { Text("GitHub Personal Access Token", fontSize = 11.sp) },
                                placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NothingRed,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (syncStatusMessage != null) {
                                Text(
                                    text = syncStatusMessage!!,
                                    fontSize = 11.sp,
                                    color = if (syncStatusMessage!!.contains("live", ignoreCase = true) || syncStatusMessage!!.contains("success", ignoreCase = true) || syncStatusMessage!!.contains("Pulled", ignoreCase = true)) Color(0xFF00E676) else NothingRed,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onPullFromGitHub()
                                        syncStatusMessage = "Pulled latest config from GitHub."
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("PULL CONFIG", fontFamily = DotMatrixFont, fontSize = 10.sp, letterSpacing = 1.sp)
                                }

                                Button(
                                    onClick = {
                                        if (gitHubTokenInput.isBlank()) {
                                            syncStatusMessage = "Please enter your GitHub token first."
                                            return@Button
                                        }
                                        isSyncingToGitHub = true
                                        syncStatusMessage = "Syncing admin_config.json to GitHub..."
                                        onSyncToGitHub(gitHubTokenInput) { result ->
                                            isSyncingToGitHub = false
                                            result.onSuccess { msg ->
                                                syncStatusMessage = msg
                                            }.onFailure { err ->
                                                syncStatusMessage = "Error: ${err.message}"
                                            }
                                        }
                                    },
                                    enabled = !isSyncingToGitHub,
                                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = Color.White),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1.2f).height(46.dp)
                                ) {
                                    if (isSyncingToGitHub) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SYNCING...", fontFamily = DotMatrixFont, fontSize = 10.sp)
                                    } else {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("PUSH TO CLOUD", fontFamily = DotMatrixFont, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Section 2: Google Sheets Telemetry Card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DataUsage,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GOOGLE SHEETS TELEMETRY",
                                    fontFamily = DotMatrixFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Track unified downloads in real time. Deploy the provided Google Apps Script in your spreadsheet, paste the Web App URL here, and every wallpaper download will be logged automatically.",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = telemetryUrlInput,
                                onValueChange = {
                                    telemetryUrlInput = it
                                },
                                label = { Text("Google Apps Script Webhook URL", fontSize = 11.sp) },
                                placeholder = { Text("https://script.google.com/macros/s/.../exec", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onRefreshTelemetry()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("REFRESH STATS", fontFamily = DotMatrixFont, fontSize = 10.sp, letterSpacing = 1.sp)
                                }

                                Button(
                                    onClick = {
                                        onSaveTelemetryUrl(telemetryUrlInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text("SAVE WEBHOOK", fontFamily = DotMatrixFont, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Delete Confirmation Dialog ---
    // --- Delete Confirmation Bottom Sheet (Takrim Aesthetic) ---
    wallpaperToDelete?.let { targetWp ->
        OrbitModalBottomSheet(
            onDismissRequest = { wallpaperToDelete = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(NothingRed, CircleShape))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "DELETE WALLPAPER",
                            fontFamily = DotMatrixFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.6.sp,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingRed.copy(alpha = 0.15f))
                            .border(1.dp, NothingRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "PERMANENT",
                            fontFamily = DotMatrixFont,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF141416),
                    border = BorderStroke(1.dp, Color(0xFF26262A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Are you sure you want to permanently remove \"${targetWp.title}\" (${targetWp.category}) from your catalog feed?",
                            fontSize = 13.sp,
                            color = Color(0xFFE0E0E6),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CATALOG ID // ${targetWp.id}",
                            fontFamily = DotMatrixFont,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { wallpaperToDelete = null },
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                        border = BorderStroke(1.dp, Color(0xFF26262A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("CANCEL", fontFamily = DotMatrixFont, fontSize = 11.sp, letterSpacing = 1.sp)
                    }

                    Button(
                        onClick = {
                            val id = targetWp.id
                            wallpaperToDelete = null
                            onDeleteWallpaper(id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = Color.White),
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("CONFIRM DELETE", fontFamily = DotMatrixFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }

    // --- Edit Individual Wallpaper Download Count Bottom Sheet ---
    wallpaperToEditDownloads?.let { wp ->
        var countInput by remember(wp) { mutableStateOf(wp.downloads.toString()) }
        OrbitModalBottomSheet(
            onDismissRequest = { wallpaperToEditDownloads = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF00E5FF), CircleShape))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "CALIBRATE DOWNLOADS",
                            fontFamily = DotMatrixFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.6.sp,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "METRICS",
                            fontFamily = DotMatrixFont,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Edit download metadata for \"${wp.title}\"",
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF141416),
                    border = BorderStroke(1.dp, Color(0xFF26262A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = countInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 10) {
                                    countInput = input
                                }
                            },
                            label = { Text("Download Count", fontSize = 11.sp, color = Color(0xFF8E8E93)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color(0xFF26262A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "QUICK PRESETS",
                            fontFamily = DotMatrixFont,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFF8E8E93)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val presets = listOf(0L, 50L, 100L, 500L, 1200L, 5000L, 10000L)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.forEach { preset ->
                                val label = if (preset == 0L) "0 (Real)" else if (preset >= 1000) "${preset / 1000}K" else "$preset"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .clickable { countInput = preset.toString() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = DotMatrixFont,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { wallpaperToEditDownloads = null },
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                        border = BorderStroke(1.dp, Color(0xFF26262A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("CANCEL", fontFamily = DotMatrixFont, fontSize = 11.sp, letterSpacing = 1.sp)
                    }

                    Button(
                        onClick = {
                            val newCount = countInput.toLongOrNull() ?: wp.downloads
                            wallpaperToEditDownloads = null
                            onUpdateWallpaperDownloads(wp.id, newCount)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("SAVE COUNT", fontFamily = DotMatrixFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }

    // --- Edit Global Total Downloads Bottom Sheet ---
    if (showGlobalTotalDialog) {
        val catalogSum = wallpapers.sumOf { it.downloads }
        val currentTotal = if (totalDownloads > 0L) totalDownloads else catalogSum
        var totalInput by remember { mutableStateOf(currentTotal.toString()) }

        OrbitModalBottomSheet(
            onDismissRequest = { showGlobalTotalDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(NothingRed, CircleShape))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TOTAL DOWNLOADS CALIBRATION",
                            fontFamily = DotMatrixFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF26262A))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "GLOBAL",
                            fontFamily = DotMatrixFont,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0B0B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Calibrate global app downloads. Current catalog sum is ${String.format(Locale.US, "%,d", catalogSum)}.",
                    fontSize = 12.sp,
                    color = Color(0xFF8E8E93)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF141416),
                    border = BorderStroke(1.dp, Color(0xFF26262A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = totalInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 12) {
                                    totalInput = input
                                }
                            },
                            label = { Text("Total App Downloads", fontSize = 11.sp, color = Color(0xFF8E8E93)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NothingRed,
                                unfocusedBorderColor = Color(0xFF26262A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "QUICK ACTIONS",
                            fontFamily = DotMatrixFont,
                            fontSize = 9.sp,
                            letterSpacing = 1.sp,
                            color = Color(0xFF8E8E93)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF26262A))
                                    .clickable { totalInput = catalogSum.toString() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "CATALOG SUM (${String.format(Locale.US, "%,d", catalogSum)})",
                                    fontFamily = DotMatrixFont,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            listOf(0L, 500L, 1000L, 5000L, 10000L).forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF00E5FF).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .clickable { totalInput = p.toString() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (p == 0L) "0 (Real)" else if (p >= 1000) "${p / 1000}K" else "$p",
                                        fontFamily = DotMatrixFont,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Prominent Reset Button for Inflated Stats
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NothingRed.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, NothingRed.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGlobalTotalDialog = false
                                    onResetInflatedDownloads()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = NothingRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "RESET TO REAL METRICS (0+)",
                                        fontFamily = DotMatrixFont,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingRed
                                    )
                                    Text(
                                        text = "Wipes synthetic mock metrics. Only real downloads tracked.",
                                        fontSize = 9.sp,
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showGlobalTotalDialog = false },
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                        border = BorderStroke(1.dp, Color(0xFF26262A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("CANCEL", fontFamily = DotMatrixFont, fontSize = 11.sp, letterSpacing = 1.sp)
                    }

                    Button(
                        onClick = {
                            val newTotal = totalInput.toLongOrNull()
                            showGlobalTotalDialog = false
                            onUpdateGlobalDownloads(newTotal)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NothingRed, contentColor = Color.White),
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("APPLY TOTAL", fontFamily = DotMatrixFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
    }
}
