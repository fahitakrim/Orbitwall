package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.DotMatrixFont

@Composable
fun OnboardingScreen(
    wallpapers: List<Wallpaper>,
    onStartExplore: () -> Unit,
    isPreview: Boolean = false,
    onExitPreview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userWallpapers = if (wallpapers.isNotEmpty()) {
        wallpapers.map { it.thumbnailUrl }
    } else {
        listOf(
            "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-akatsuki-moonlit-gathering.jpg",
            "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-minimal-capsule-stack.jpg",
            "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-itachi-red-moon.jpg",
            "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-solitary-red-boat.jpg",
            "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-eclipse-spheres-red-glow.jpg",
            "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-kakashi-sharingan-moon.jpg",
            "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-crimson-amber-fluid-flow.jpg",
            "https://cdn.jsdelivr.net/gh/fahitakrim/Orbitwall@main/cloud_catalog/thumbs/paper-cherry-blossom-black.jpg"
        )
    }

    val backgroundColor = Color(0xFFF7F8FC)

    if (isPreview) {
        BackHandler(enabled = true) {
            onExitPreview()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .testTag("onboarding_screen")
    ) {
        // Upper Angled 3D Wallpaper Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.58f)
                .offset(y = (-36).dp, x = 10.dp)
                .rotate(-8f)
                .clipToBounds()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Column 1
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    for (i in 0 until 3) {
                        val img = if (userWallpapers.isNotEmpty()) userWallpapers[i % userWallpapers.size] else null
                        OnboardingCardItem(imageUrl = img)
                    }
                }
                // Column 2
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = (-24).dp)
                ) {
                    for (i in 3 until 6) {
                        val img = if (userWallpapers.isNotEmpty()) userWallpapers[i % userWallpapers.size] else null
                        OnboardingCardItem(imageUrl = img)
                    }
                }
                // Column 3
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    for (i in 5 until 8) {
                        val img = if (userWallpapers.isNotEmpty()) userWallpapers[i % userWallpapers.size] else null
                        OnboardingCardItem(imageUrl = img)
                    }
                }
            }
        }

        // Gradient Scrim overlay fading cleanly into solid background without ghost overlaps
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.32f to Color.Transparent,
                            0.46f to backgroundColor.copy(alpha = 0.85f),
                            0.56f to backgroundColor,
                            1.0f to backgroundColor
                        )
                    )
                )
        )

        // Top Floating Pill for Preview Mode
        if (isPreview) {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xEE161720))
                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                    .clickable { onExitPreview() }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PREVIEW MODE // TAP TO EXIT",
                        fontFamily = DotMatrixFont,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Lower Content: Title, Subtitle, and "Start Explore" Pill Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 40.dp)
        ) {
            Text(
                text = "Explore 4K\nWallpapers",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF14151C),
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Explore, Create, Share\nUltra 4K Wallpapers Now!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF7A8092),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = if (isPreview) onExitPreview else onStartExplore,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF161720),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_explore_button")
            ) {
                Text(
                    text = if (isPreview) "Close Preview" else "Start Explore",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingCardItem(imageUrl: String?) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(22.dp))
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFE5E7F0), Color(0xFFD6DBE8))
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Wallpaper,
                    contentDescription = null,
                    tint = Color(0xFFA0A6B8),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
