package com.huqi.noveltracker.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Small tag pill. Pass [selected] + [onClick] to make it a toggle (used in filter row).
 */
@Composable
fun TagChip(
    name: String,
    colorHex: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val dotColor = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)

    val shape = RoundedCornerShape(50)
    val modifier = Modifier
        .clip(shape)
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
        .then(
            if (selected) Modifier.border(1.5.dp, dotColor, shape)
            else Modifier
        )
        .background(
            if (selected) dotColor.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant,
            shape
        )
        .padding(horizontal = 12.dp, vertical = 6.dp)

    Row(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
