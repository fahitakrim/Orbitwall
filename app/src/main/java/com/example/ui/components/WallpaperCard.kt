package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.Wallpaper
import com.example.ui.theme.DotMatrixFont
import com.example.ui.theme.NothingRed

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
        Color(0xFF151517)
    }
}

/**
 * Nothing OS styled wallpaper card:
 * - 1.dp technical stroke border matching current theme outline
 * - Monospace / Dot font title & category
 * - Circular favorite heart button in the corner
 */
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

    // Dynamic natural staggered grid rhythm (e.g. 0.68f to 0.80f)
    val cardAspectRatio = remember(wallpaper.id) {
        val hash = wallpaper.id.hashCode()
        if (hash % 3 == 0) 0.68f else if (hash % 2 == 0) 0.74f else 0.80f
    }

    val cardShape = RoundedCornerShape(20.dp)

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cardAspectRatio)
            .clip(cardShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, cardShape)
            .clickable(onClick = onCardClick)
            .testTag("wallpaper_card_${wallpaper.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dominantColor.copy(alpha = 0.15f))
        ) {
            // High efficiency 450px thumbnail streaming from jsDelivr CDN
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

            // Scrim gradient for text legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(96.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            // Bottom metadata info & favorite button
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = wallpaper.title,
                        fontFamily = DotMatrixFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(NothingRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = wallpaper.category.uppercase(),
                            fontFamily = DotMatrixFont,
                            fontWeight = FontWeight.Normal,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                // Circular favorite button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFavorite) NothingRed else Color.Black.copy(alpha = 0.45f)
                        )
                        .border(
                            1.dp,
                            if (isFavorite) NothingRed else Color.White.copy(alpha = 0.35f),
                            CircleShape
                        )
                        .clickable(onClick = onFavoriteClick)
                        .testTag("favorite_btn_${wallpaper.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
