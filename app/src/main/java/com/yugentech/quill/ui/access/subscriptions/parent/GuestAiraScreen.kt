package com.yugentech.quill.ui.access.subscriptions.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.quota.model.QuotaLimits
import com.yugentech.quill.ui.access.onboarding.components.AiraScene
import com.yugentech.quill.ui.access.subscriptions.components.FeatureHighlights
import com.yugentech.quill.ui.access.subscriptions.components.ProFeaturesSection
import com.yugentech.quill.ui.main.components.SectionHeader
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing

/**
 * What a guest sees instead of the subscription screen. Aira needs an account (her answers come
 * from the cloud, and her daily allowance belongs to one), so there's nothing to buy yet.
 *
 * From the top: the onboarding's Aira scene, sample questions and answers playing in turn; the
 * subscription screen's sections, the free and Pro tiers (labelled for someone who has no plan
 * yet) and what Aira can do; then how to get her, step by step, ending with the free allowance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestAiraScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Scaffold(
        topBar = {
            // No title: the scene below names her, as it does in the onboarding
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = innerPadding.calculateTopPadding())
                .padding(bottom = navBarPadding.calculateBottomPadding())
        ) {
            // The example chat: her portrait, the spoiler lock and the sample exchanges
            AiraScene(active = true)

            Spacer(Modifier.height(MaterialTheme.spacing.l))

            Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.m)) {
                // What an account brings: the subscription screen's tier cards
                SectionHeader(
                    icon = Icons.Default.WorkspacePremium,
                    title = stringResource(R.string.guest_aira_section_plans),
                    startPadding = 0.dp
                )
                ProFeaturesSection(
                    freeLabel = stringResource(R.string.guest_tier_label_free),
                    proLabel = stringResource(R.string.guest_tier_label_pro)
                )

                Spacer(Modifier.height(MaterialTheme.spacing.s))

                // What she can do: the subscription screen's feature list
                SectionHeader(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.guest_aira_section_features),
                    startPadding = 0.dp
                )
                FeatureHighlights()

                Spacer(Modifier.height(MaterialTheme.spacing.l))

                GetAiraCard()
            }
        }
    }
}

/**
 * The way to Aira from guest mode. Why she needs an account, then the three steps there in
 * order: keep the library, leave guest mode, sign up. The last step ends on the free allowance,
 * so the card closes on what they get rather than on what they have to do.
 */
@Composable
private fun GetAiraCard() {
    val colors = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(MaterialTheme.corners.extraLarge),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.l)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colors.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(MaterialTheme.spacing.m))
                Text(
                    text = stringResource(R.string.guest_aira_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.m))

            Text(
                text = stringResource(R.string.guest_aira_reason),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.l),
                color = colors.outlineVariant.copy(alpha = 0.6f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)) {
                Step(number = 1, text = stringResource(R.string.guest_aira_step_export))
                Step(number = 2, text = stringResource(R.string.guest_aira_step_exit))
                Step(
                    number = 3,
                    text = stringResource(R.string.guest_aira_step_signup, QuotaLimits.FREE),
                    last = true
                )
            }
        }
    }
}

/** One step: its number in a small circle, then what to do. The last one is in the primary colour. */
@Composable
private fun Step(number: Int, text: String, last: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (last) colors.primary else colors.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (last) colors.onPrimary else colors.onSecondaryContainer
            )
        }
        Spacer(Modifier.width(MaterialTheme.spacing.m))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (last) FontWeight.SemiBold else FontWeight.Normal,
            color = if (last) colors.primary else colors.onSurface,
            // Centred on the number's circle for a one-line step
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp)
        )
    }
}
