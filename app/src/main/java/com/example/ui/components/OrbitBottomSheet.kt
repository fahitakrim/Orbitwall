package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Orbit Bottom-Sheet-First Architecture Component
 *
 * Core Specifications:
 * - Bauhaus Functionalism + Nothing OS Minimalism + Material 3 Expressive Fluidity
 * - Container: #18181A (Deep Dark Galaxy)
 * - Shape: Top 28dp rounded corners, bottom 0dp (Edge-to-edge)
 * - Handlebar: Center minimalist pill drag handle (36dp × 4dp, rounded 2dp, #3E3E44)
 * - Scrim: Deep black 70% opacity
 */
val OrbitGalaxyColor = Color(0xFF18181A)
val OrbitSurfaceDarkColor = Color(0xFF141416)
val OrbitRedColor = Color(0xFFE5252A)
val OrbitHairlineBorderColor = Color(0xFF26262A)
val OrbitHandlebarColor = Color(0xFF3E3E44)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    OrbitModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 28.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        ),
        containerColor = OrbitGalaxyColor,
        scrimColor = Color.Black.copy(alpha = 0.70f),
        dragHandle = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(OrbitHandlebarColor)
                )
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
            content = content
        )
    }
}
