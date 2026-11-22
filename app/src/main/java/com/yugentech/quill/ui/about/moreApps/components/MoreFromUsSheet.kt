package com.yugentech.quill.ui.about.moreApps.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.theme.SheetTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One app in the sheet. The icon is drawn in a 56dp tile, so each is scaled from its 88dp one.
 * A null [packageName] means the app isn't out yet.
 */
private class SheetEntry(
    val name: String,
    val tagline: String,
    val packageName: String?,
    val iconBackground: Color,
    val animationMillis: Long,
    val icon: @Composable (isAnimating: Boolean) -> Unit
)

/**
 * The short version of the More apps screen, opened from the More tab's About section: each app's
 * icon, name and tagline with a Play Store button, then a link to the full screen. Tapping an icon
 * plays its animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreFromUsSheet(onDismiss: () -> Unit, onSeeAll: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val apps = listOf(
        SheetEntry("Mynt", stringResource(R.string.mynt_tagline), null, MyntIconBackground, 1300) {
            AnimatedMyntIcon(isAnimating = it, modifier = Modifier.requiredSize(84.dp))
        },
        SheetEntry("Hajime", stringResource(R.string.hajime_tagline), null, HajimeIconBackground, 1660) {
            AnimatedHajimeIcon(isAnimating = it, modifier = Modifier.requiredSize(100.dp))
        },
        SheetEntry("Sessions", stringResource(R.string.sessions_tagline), SESSIONS_PACKAGE, SessionsIconBackground, 900) {
            AnimatedSessionsIcon(isAnimating = it, modifier = Modifier.requiredSize(100.dp))
        },
        SheetEntry("Ryori", stringResource(R.string.ryori_tagline), null, RyoriIconBackground, 1200) {
            AnimatedRyoriIcon(isAnimating = it, modifier = Modifier.requiredSize(56.dp))
        }
    )

    SheetTheme {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            // Sessions' sheet handle: a small pill with roomy padding, so it's easy to grab.
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.more_more_from_us),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.more_apps_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Column(
                    modifier = Modifier.padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    apps.forEachIndexed { index, app ->
                        SheetAppRow(
                            app = app,
                            index = index,
                            count = apps.size,
                            onGetApp = app.packageName?.let { pkg -> { openPlayStore(context, pkg) } }
                        )
                    }
                }

                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        // Slides the sheet away first, so it doesn't vanish under the new screen.
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            onDismiss()
                            onSeeAll()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp)
                ) {
                    Text(stringResource(R.string.see_all_features))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetAppRow(app: SheetEntry, index: Int, count: Int, onGetApp: (() -> Unit)?) {
    val haptic = LocalHapticFeedback.current
    var isAnimating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val play: () -> Unit = {
        if (!isAnimating) {
            isAnimating = true
            scope.launch {
                delay(app.animationMillis)
                isAnimating = false
            }
        }
    }
    val large = 20.dp
    val small = 6.dp
    Surface(
        shape = RoundedCornerShape(
            topStart = if (index == 0) large else small,
            topEnd = if (index == 0) large else small,
            bottomStart = if (index == count - 1) large else small,
            bottomEnd = if (index == count - 1) large else small
        ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(app.iconBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            play()
                        }
                    )
            ) {
                app.icon(isAnimating)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onGetApp == null) {
                ComingSoonLabel()
            } else {
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onGetApp()
                    },
                    contentPadding = PaddingValues(horizontal = 18.dp)
                ) {
                    Text(stringResource(R.string.get))
                }
            }
        }
    }
}
