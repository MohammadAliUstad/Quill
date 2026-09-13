package com.yugentech.quill.ui.access.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.yugentech.quill.ui.access.onboarding.components.OnboardingPage
import com.yugentech.quill.ui.access.onboarding.components.onboardingPages
import com.yugentech.theme.service.HapticService
import com.yugentech.theme.tokens.components
import com.yugentech.theme.tokens.dimensions.AppAnimations
import com.yugentech.theme.tokens.icons
import com.yugentech.theme.tokens.spacing
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val haptic = koinInject<HapticService>()
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val lastPage = onboardingPages.lastIndex
    val scope = rememberCoroutineScope()
    val spacing = MaterialTheme.spacing

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.l, vertical = spacing.xl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val showSkip = pagerState.currentPage < lastPage
                AnimatedVisibility(
                    visible = showSkip,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(
                        onClick = {
                            haptic.performHaptic()
                            onFinish()
                        },
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text("Skip", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (!showSkip) Spacer(Modifier.width(spacing.none))

                Button(
                    onClick = {
                        haptic.performHaptic()
                        scope.launch {
                            if (pagerState.currentPage < lastPage) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onFinish()
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = PaddingValues(horizontal = spacing.l, vertical = spacing.sm)
                ) {
                    // Standard fade between "Next" and "Get Started"; AnimatedContent's default
                    // size transform also smoothly resizes the button to fit the longer label.
                    AnimatedContent(
                        targetState = pagerState.currentPage == lastPage,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "OnboardingButtonLabel"
                    ) { isLastPage ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLastPage) "Get Started" else "Next",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.width(spacing.s))
                            Icon(
                                imageVector = if (isLastPage)
                                    Icons.Default.Check
                                else
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.icons.smallMedium)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val targetProgress = (pagerState.currentPage + 1) / onboardingPages.size.toFloat()

            val animatedProgress by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = tween(
                    durationMillis = AppAnimations.Durations.Delay,
                    easing = AppAnimations.Easings.Standard
                ),
                label = "OnboardingProgress"
            )

            LinearWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.m, bottom = spacing.s)
                    .height(MaterialTheme.components.onboardingIndicatorHeight),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(
                    content = onboardingPages[page],
                    isVisible = (pagerState.currentPage == page)
                )
            }
        }
    }
}
