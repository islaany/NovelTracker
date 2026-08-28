package com.huqi.noveltracker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/**
 * Gradient cover placeholder used when a novel has no cover image.
 * Shows the first character of the title on a soft primary→accent gradient,
 * so the list/detail still looks like a "book shelf" instead of empty boxes.
 */
@Composable
fun CoverPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 92.dp
) {
    val char = title.firstOrNull()?.toString() ?: "?"
    val brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
        )
    )
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(10.dp))
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            color = Color.White,
            fontSize = if (width.value < 80f) 30.sp else 44.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
