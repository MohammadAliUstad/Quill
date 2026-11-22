package com.yugentech.quill.ui.access.onboarding.parent

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.R
import com.yugentech.quill.ui.access.common.components.AuraTheme
import com.yugentech.quill.ui.access.onboarding.components.OnboardingContent

/**
 * Quill's first run, laid out and animated as Sessions': four short, interactive scenes rather
 * than a feature list. A famous first line writes itself onto a page that turns at a tap,
 * classics slide onto a shelf, Aira answers a
 * question without spoiling the book, and a reading heatmap fills in.
 *
 * It sits on the still aura the nav host draws behind it (shared with the sign-in screens).
 * Story-style bars at the top fill as the reader swipes; Skip leaves at any point, a back button
 * appears beside the main button from the second scene, and the main button moves on, then
 * [finishLabel] finishes on the last scene. Settings opens it again later, with "Done" in place of
 * the first run's label.
 */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    finishLabel: String = stringResource(R.string.onboarding_start_reading)
) {
    // The aura's colours, which the nav host draws behind this screen
    AuraTheme { OnboardingContent(onFinish, finishLabel) }
}
