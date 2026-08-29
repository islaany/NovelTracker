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
 * Small tag pill.
 *
 * States:
 *  - [filled]  = a MAIN tag: solid tinted background + colored border (prominent).
 *  - [selected] = a SUB tag: light tint, no border.
 *  - default   = an unselected chip (used in filter rows / sub-tag pool).
 * Pass [onClick] to make it tappable (filter chips, tag picker).
 */
@Composable
fun TagChip(
    name: String,
    colorHex: String? = null,
    selected: Boolean = false,
    filled: Boolean = false,
    onClick: (() -> Unit)? = null,
    /** Small trailing action, e.g. "✕" remove or "↑" promote to main tag. */
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val color = runCatching { colorHex?.let { Color(android.graphics.Color.parseColor(it)) } }
        .getOrDefault(null)

    val shape = RoundedCornerShape(50)
    val bg = when {
        filled && color != null -> color.copy(alpha = 0.16f)
        selected && color != null -> color.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val border = when {
        filled && color != null -> Modifier.border(1.5.dp, color, shape)
        selected && color != null -> Modifier.border(1.dp, color.copy(alpha = 0.5f), shape)
        else -> Modifier
    }
    val dotColor = color ?: MaterialTheme.colorScheme.primary
    val textColor = when {
        filled && color != null -> color.copy(alpha = 0.92f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(border)
            .background(bg, shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
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
            color = textColor
        )
        if (actionLabel != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.then(
                    if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier
                )
            )
        }
    }
}
