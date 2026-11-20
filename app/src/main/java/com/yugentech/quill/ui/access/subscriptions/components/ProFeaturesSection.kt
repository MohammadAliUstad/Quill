package com.yugentech.quill.ui.access.subscriptions.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

/**
 * The free and Pro tiers side by side as cards, then what a query costs. [freeLabel] and
 * [proLabel] sit above the tier names: "Current plan" and "Upgrade to" on the subscription
 * screen; a guest, who has no plan yet, gets labels about making an account instead.
 */
@Composable
fun ProFeaturesSection(
    freeLabel: String = stringResource(R.string.tier_label_current),
    proLabel: String = stringResource(R.string.tier_label_upgrade)
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.s)
    ) {
        // Free tier card
        QueryTierCard(
            icon = {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = freeLabel,
            tierName = stringResource(R.string.tier_basic),
            subtitle = stringResource(R.string.tier_basic_subtitle),
            description = stringResource(R.string.tier_basic_body),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            borderColor = MaterialTheme.colorScheme.outlineVariant
        )

        // Pro tier card
        QueryTierCard(
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            label = proLabel,
            tierName = stringResource(R.string.quill_pro),
            subtitle = stringResource(R.string.tier_pro_subtitle),
            description = stringResource(R.string.tier_pro_body),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        // Quota cost note: applies to both tiers
        Text(
            text = stringResource(R.string.query_cost_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            // Further from the cards than they are from each other: a note on both, not part of the Pro one
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.xs,
                end = MaterialTheme.spacing.xs,
                top = MaterialTheme.spacing.s
            )
        )
    }
}

@Composable
private fun QueryTierCard(
    icon: @Composable () -> Unit,
    label: String,
    tierName: String,
    subtitle: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(MaterialTheme.corners.extraLarge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.l), // Standardize padding for the entire content block
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m) // Spacing between the header row and description
        ) {
            // Header Row: Icon and Title Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.l)
            ) {
                icon()
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Text(
                        text = tierName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Description Text: Positioned directly below the header row
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}