package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Wallpaper

/**
 * Parses hex color strings safely with dark fallback.
 */
fun parseHexColor(hexString: String): Color {
    return try {
        val clean = hexString.removePrefix("#")
        val colorInt = if (clean.length == 6) {
            android.graphics.Color.parseColor("#FF$clean")
        } else {
            android.graphics.Color.parseColor("#$clean")
        }
        Color(colorInt)
    } catch (_: Exception) {
        Color(0xFF1E1B4B)
    }
}

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    onCardClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dominantColor = remember(wallpaper.dominantColorHex) {
        parseHexColor(wallpaper.dominantColorHex)
    }

    // Dynamic natural staggered grid rhythm (e.g. 0.65 to 0.76)
    val cardAspectRatio = remember(wallpaper.id) {
        val hash = wallpaper.id.hashCode()
        if (hash % 3 == 0) 0.68f else if (hash % 2 == 0) 0.74f else 0.80f
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cardAspectRatio)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onCardClick)
            .testTag("wallpaper_card_${wallpaper.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dominantColor.copy(alpha = 0.25f))
        ) {
            // Bandwidth Protection: Strictly load 450px thumbnail in grid
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(wallpaper.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = wallpaper.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("wallpaper_thumb_${wallpaper.id}")
            )

            // Floating Favorite Heart Button at Bottom-Right (as shown in reference UI)
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(38.dp)
                    .background(
                        color = if (isFavorite) Color(0xFFE11D48) else Color.Black.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (isFavorite) Color(0xFFE11D48) else Color.White.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
                    .testTag("favorite_btn_${wallpaper.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Save favorite",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
