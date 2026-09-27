package com.learn.morse.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws morse as shapes: round dots and pill-shaped dashes, a dash being 2.5 dots long.
 * A space (between the letters of a word) leaves a wider gap.
 */
@Composable
fun MorseSymbols(
    morse: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSize / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        morse.forEach { symbol ->
            if (symbol == ' ') {
                Spacer(Modifier.width(dotSize))
                return@forEach
            }
            Box(
                modifier = Modifier
                    .size(width = if (symbol == '-') dotSize * 2.5f else dotSize, height = dotSize)
                    .background(color, CircleShape)
            )
        }
    }
}
