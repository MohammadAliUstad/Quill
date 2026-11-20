package com.yugentech.theme.effects

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ismnoiet.borderbeam.BeamPalette
import com.ismnoiet.borderbeam.BeamShape
import com.ismnoiet.borderbeam.BeamStyle
import com.ismnoiet.borderbeam.BeamTiming
import com.ismnoiet.borderbeam.borderBeam

/** One lap of the beam around the field: unhurried, so it reads as "working", not "alert". */
private const val AIRA_BEAM_LAP_S = 3.0

/** How long the beam takes to fade in when Aira starts and out when she's done. */
private const val AIRA_BEAM_FADE_S = 0.45

/** The beam's brightness: kept below full so it glows softly rather than shines. */
private const val AIRA_BEAM_INTENSITY = 0.55

/**
 * The light that circles Aira's message field while she's answering (from sending until the
 * reply has finished streaming), the same in her chat and in the reader's peek bar.
 *
 * Drawn in the library's Gemini palette (blue, purple, red, orange, yellow, green), the same
 * in every theme. It fades in and out with [active], and its frame loop stops once it has faded
 * out, so an idle field costs nothing. With animations turned off in the system settings it
 * shows still.
 *
 * Meant for the capsule-shaped fields (CircleShape); it follows their outline exactly, however
 * many lines tall they grow.
 */
@Composable
fun Modifier.airaBeam(active: Boolean): Modifier = this.borderBeam(
    shape = BeamShape.capsule,
    style = BeamStyle.large,
    active = active,
    colors = BeamPalette.gemini,
    intensity = AIRA_BEAM_INTENSITY,
    timing = BeamTiming(rotateDuration = AIRA_BEAM_LAP_S, fade = AIRA_BEAM_FADE_S)
)
