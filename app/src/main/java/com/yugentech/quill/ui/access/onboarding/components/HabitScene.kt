package com.yugentech.quill.ui.access.onboarding.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.ui.access.common.components.SceneColumn
import com.yugentech.quill.ui.access.common.components.SceneText
import com.yugentech.quill.ui.access.common.components.paperColors
import com.yugentech.quill.ui.access.common.components.rememberEntrance
import com.yugentech.quill.ui.access.common.components.rise
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val HEAT_WEEKS = 12
private const val STREAK_DAYS = 12

/**
 * A reading heatmap filling in week by week, darker for longer reading days, then a streak
 * counting up.
 */
@Composable
internal fun HabitScene(active: Boolean) {
    // The card is in the app's own colours: its heatmap and streak use the accent colour
    val colors = paperColors()
    val haptic = LocalHapticFeedback.current
    val cardIn = rememberEntrance(active, 300)
    val fill = remember { Animatable(0f) }
    var streak by remember { mutableIntStateOf(0) }
    // How much was read each day: mostly something, more toward the recent weeks.
    val days = remember {
        val random = Random(11)
        List(HEAT_WEEKS * 7) { i ->
            val recent = i / (HEAT_WEEKS * 7f)
            if (random.nextFloat() < 0.25f - recent * 0.2f) 0f else (0.25f + random.nextFloat() * 0.5f + recent * 0.25f).coerceAtMost(1f)
        }
    }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        delay(600)
        fill.animateTo(HEAT_WEEKS.toFloat(), tween(1500, easing = FastOutSlowInEasing))
        for (day in 1..STREAK_DAYS) {
            streak = day
            if (day % 3 == 0) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
            delay(55)
        }
    }

    SceneColumn {
        SceneText(
            title = stringResource(R.string.onboarding_habit_title),
            body = stringResource(R.string.onboarding_habit_body),
            active = active,
            delayMs = 100
        )
        Spacer(Modifier.height(32.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = colors.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .rise({ cardIn.value }, 28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(HEAT_WEEKS) { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(7) { day ->
                                val amount = days[week * 7 + day]
                                // Each week's column lights up as the fill sweeps past it
                                val lit = (fill.value - week).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(colors.onSurfaceVariant.copy(alpha = 0.10f))
                                        .graphicsLayer { alpha = lit }
                                        .background(colors.primary.copy(alpha = amount))
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$streak",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = colors.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_day_streak),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}
