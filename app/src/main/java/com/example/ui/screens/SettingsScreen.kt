package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NothingLogo4Dot
import com.example.ui.components.NothingSectionHeader
import com.example.ui.components.TechnicalCircleGraphic
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.DotMatrixFont
import com.example.ui.theme.NothingBlue
import com.example.ui.theme.NothingRed

@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    isHapticEnabled: Boolean,
    cacheSizeMb: Double,
    onThemeChange: (AppThemeMode) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    fun triggerHaptic() {
        if (isHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp)
            .testTag("settings_screen")
    ) {
        // --- Header with Technical Circle Graphic ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            TechnicalCircleGraphic(
                modifier = Modifier
                    .size(95.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp, y = (-4).dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                accentColor = NothingBlue.copy(alpha = 0.35f)
            )

            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SETTINGS",
                        fontFamily = DotMatrixFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = 2.5.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = NothingRed,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "PREFERENCES & SYSTEM CONFIGURATION",
                    fontFamily = DotMatrixFont,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 1. APPEARANCE • Section Card ---
        SettingsCard(title = "APPEARANCE") {
            // Theme Mode Toggle (Dark / Light)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "THEME",
                        fontFamily = DotMatrixFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentTheme == AppThemeMode.DARK) "Dark Mode (#0B0B0C)" else "Light Mode (#F2F2F5)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Segmented Pill (Dark / Light)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        ThemePillButton(
                            text = "DARK",
                            isSelected = currentTheme == AppThemeMode.DARK,
                            onClick = {
                                triggerHaptic()
                                onThemeChange(AppThemeMode.DARK)
                            }
                        )
                        ThemePillButton(
                            text = "LIGHT",
                            isSelected = currentTheme == AppThemeMode.LIGHT,
                            onClick = {
                                triggerHaptic()
                                onThemeChange(AppThemeMode.LIGHT)
                            }
                        )
                    }
                }
            }

            SettingsDivider()

            // Haptic Feedback Toggle (Off / On)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HAPTIC FEEDBACK",
                        fontFamily = DotMatrixFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vibration on touch interactions",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        ThemePillButton(
                            text = "OFF",
                            isSelected = !isHapticEnabled,
                            onClick = {
                                onHapticToggle(false)
                            }
                        )
                        ThemePillButton(
                            text = "ON",
                            isSelected = isHapticEnabled,
                            onClick = {
                                triggerHaptic()
                                onHapticToggle(true)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. STORAGE • Section Card ---
        SettingsCard(title = "STORAGE") {
            // Image Cache Row with Clear Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "IMAGE CACHE",
                        fontFamily = DotMatrixFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format("%.1f MB used (Coil buffer)", cacheSizeMb),
                        fontFamily = DotMatrixFont,
                        fontSize = 11.sp,
                        color = if (cacheSizeMb > 0.1) NothingRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Red Clear Pill Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NothingRed)
                        .clickable {
                            triggerHaptic()
                            onClearCache()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CLEAR",
                            fontFamily = DotMatrixFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    }
                }
            }

            SettingsDivider()

            // Download Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DOWNLOAD LOCATION",
                        fontFamily = DotMatrixFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pictures/Orbitwall",
                        fontFamily = DotMatrixFont,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. ABOUT • Section Card ---
        SettingsCard(title = "ABOUT") {
            // App Branding Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                NothingLogo4Dot(
                    boxSize = 44.dp,
                    dotSize = 5.dp,
                    backgroundColor = MaterialTheme.colorScheme.background,
                    borderColor = MaterialTheme.colorScheme.outline,
                    contrastDotColor = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ORBITWALL",
                            fontFamily = DotMatrixFont,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "v1.0.0",
                                fontFamily = DotMatrixFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Nothing OS Minimalist 4K Wallpaper Hub",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsDivider()

            // Developer Info & Social Links
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DEVELOPER",
                    fontFamily = DotMatrixFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Takrim",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Social Profile Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Facebook Profile Button
                    SocialPillButton(
                        label = "FACEBOOK",
                        accentColor = Color(0xFF1877F2),
                        onClick = {
                            triggerHaptic()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/profile.php?id=61592810150809"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // X (Twitter) Profile Button
                    SocialPillButton(
                        label = "X (TWITTER)",
                        accentColor = MaterialTheme.colorScheme.onBackground,
                        onClick = {
                            triggerHaptic()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/TakrimFahim"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.uppercase(),
                    fontFamily = DotMatrixFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.8.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "•",
                    color = NothingRed,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun ThemePillButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) NothingRed else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontFamily = DotMatrixFont,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SocialPillButton(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(accentColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontFamily = DotMatrixFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
