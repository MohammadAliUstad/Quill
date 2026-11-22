package com.yugentech.quill.ui.access.guestAira.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.ui.about.aira.components.HeroSection
import com.yugentech.quill.ui.access.common.components.FeatureHighlights
import com.yugentech.quill.ui.access.common.components.ProFeaturesSection
import com.yugentech.quill.ui.common.components.rememberSmoothScrimBrush

/**
 * What a guest sees instead of the subscription screen. Aira needs an account (her answers come
 * from the cloud, and her daily allowance belongs to one), so there's nothing to buy yet.
 *
 * Meet Aira's hero (her changing portrait, name and tagline) above the subscription screen's
 * details, laid out as they are there: the free and Pro tiers (labelled for someone who has no
 * plan yet), then what Aira can do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestAiraScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val surfaceColor = MaterialTheme.colorScheme.surface

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // As on Meet Aira: a transparent bar the content scrolls under, with a soft scrim
            // fading in behind it once the content moves, so the back arrow stays readable
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = (-scrollBehavior.state.contentOffset / 100f).coerceIn(0f, 1f)
                        }
                        .background(rememberSmoothScrimBrush(surfaceColor))
                )

                // No title: the hero below names her, as on Meet Aira
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // The subscription screen's side and section spacing, the content running under the
            // top bar, and 8dp at the end (above the navigation bar)
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { HeroSection() }

            item {
                ProFeaturesSection(
                    freeLabel = stringResource(R.string.guest_tier_label_free),
                    proLabel = stringResource(R.string.guest_tier_label_pro)
                )
            }

            item { FeatureHighlights() }
        }
    }
}
