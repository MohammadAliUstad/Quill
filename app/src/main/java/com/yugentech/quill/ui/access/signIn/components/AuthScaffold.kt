package com.yugentech.quill.ui.access.signIn.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.ui.access.onboarding.components.AuraTheme
import com.yugentech.quill.ui.access.onboarding.components.OnPaper
import com.yugentech.theme.tokens.spacing

/** The form's widest, so it doesn't stretch across a tablet or a phone on its side. */
private val MaxFormWidth = 440.dp

/**
 * The frame both auth screens share, laid out as Hajime's, on the aura the nav host draws behind
 * them and the onboarding (as Amatyaa's auth screens sit on its blue): the screen's own animated
 * [header], a serif [greeting] with a short [accent] line and the [subtitle] under it, then the
 * screen's [content] on a floating card in the app's own colours. Each part rises in after the
 * one above it, with Amatyaa's entrance ([riseIn]). [overlay] sits above everything, for toasts.
 */
@Composable
fun AuthScaffold(
    header: @Composable () -> Unit,
    greeting: String,
    accent: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    // White on the shared aura, which the nav host draws behind this screen
    AuraTheme { AuthFrame(header, greeting, accent, subtitle, modifier, overlay, content) }
}

@Composable
private fun AuthFrame(
    header: @Composable () -> Unit,
    greeting: String,
    accent: String,
    subtitle: String,
    modifier: Modifier,
    overlay: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    // Amatyaa's entrance: everything starts hidden and, once the screen is up, each block rises
    // in after the one above it. Plays each time the screen opens (switching between sign-in and
    // sign-up too), while the shared background behind stays put.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val rise = with(LocalDensity.current) { 24.dp.toPx() }

    // Transparent: the aura behind is drawn once by the nav host, so only this content moves
    // when switching between sign-in and sign-up.
    Surface(modifier = modifier.fillMaxSize(), color = Color.Transparent, contentColor = colors.onBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            // The whole block (header, greeting, form, link) sits in the middle of the screen when
            // it fits, and scrolls when it doesn't (a short screen, or the keyboard up): the column
            // is at least the screen's height, its content centred within that.
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                val screenHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = screenHeight)
                        .padding(horizontal = MaterialTheme.spacing.l, vertical = MaterialTheme.spacing.l),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = MaterialTheme.spacing.s)
                            .riseIn(shown, order = 0, rise = rise)
                    ) { header() }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.displaySmall,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.riseIn(shown, order = 1, rise = rise)
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                        Text(
                            text = accent,
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            letterSpacing = 0.5.sp,
                            color = colors.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.riseIn(shown, order = 2, rise = rise)
                        )
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onBackground.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.riseIn(shown, order = 3, rise = rise)
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

                    // The form on a floating card in the app's own colours, as Amatyaa's sits on its blue
                    OnPaper {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .widthIn(max = MaxFormWidth)
                                .fillMaxWidth()
                                // The card rises further and a little quicker, as Amatyaa's does
                                .riseIn(shown, order = 4, rise = rise * 2, durationMs = CARD_ENTRANCE_MS)
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    start = MaterialTheme.spacing.m,
                                    end = MaterialTheme.spacing.m,
                                    top = MaterialTheme.spacing.l,
                                    bottom = MaterialTheme.spacing.s
                                ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                content = content
                            )
                        }
                    }
                }
            }

            overlay()
        }
    }
}

/** How long one block takes to rise in, and how far behind the block above it each one starts. */
private const val RISE_IN_MS = 550
private const val RISE_IN_STAGGER_MS = 90

/** The form card rises a little quicker than the text around it. */
private const val CARD_ENTRANCE_MS = 450

/** Material 3's emphasized-decelerate easing, for things arriving on screen. */
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * Amatyaa's entrance for its sign-in screens: fades in while rising [rise] px, [order] ×
 * [staggerMs] after [shown] turns true. ModulateAlpha fades each drawing directly instead of
 * through an offscreen buffer the size of the element, which would cut off a card's edges.
 */
@Composable
private fun Modifier.riseIn(
    shown: Boolean,
    order: Int,
    rise: Float,
    durationMs: Int = RISE_IN_MS,
    staggerMs: Int = RISE_IN_STAGGER_MS
): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMs, delayMillis = order * staggerMs, easing = EmphasizedDecelerate),
        label = "riseIn"
    )
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * rise
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

/** "[prompt] [action]" under the form, the action in bold accent, switching sign in and sign up. */
@Composable
fun AuthSwitchLink(prompt: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = buildAnnotatedString {
                append("$prompt ")
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) { append(action) }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A thin rule with [text] in the middle, between signing in with email and with Google. */
@Composable
fun AuthDivider(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
