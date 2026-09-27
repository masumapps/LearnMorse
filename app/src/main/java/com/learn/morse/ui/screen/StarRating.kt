package com.learn.morse.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

val StarColor = Color(0xFFFFB300)

/** Three stars, the first [stars] filled. With [animate], earned stars pop in one after another. */
@Composable
fun StarRating(stars: Int, modifier: Modifier = Modifier, size: Dp = 16.dp, animate: Boolean = false) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(size / 8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val earned = index < stars
            val scale = remember { Animatable(if (animate && earned) 0f else 1f) }
            if (animate && earned) {
                LaunchedEffect(Unit) {
                    delay(300L + index * 250L)
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                }
            }
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    },
                tint = if (earned) StarColor else MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
