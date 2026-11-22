package com.yugentech.quill.ui.info.insights.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.insights.engine.AiraStats
import com.yugentech.theme.tokens.spacing

/** Reading with Aira: questions asked, the book explored most, and visuals created. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiraCard(aira: AiraStats, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)) {
        EngagementRow(
            icon = Icons.Outlined.AutoAwesome,
            badge = MaterialShapes.Cookie9Sided.toShape(),
            container = colors.primaryContainer,
            content = colors.onPrimaryContainer,
            title = if (aira.questions == 0) stringResource(R.string.no_questions_yet)
            else pluralStringResource(R.plurals.questions_asked, aira.questions, aira.questions),
            subtitle = stringResource(R.string.total_conversations)
        )
        if (aira.mostExploredTitle != null) {
            EngagementRow(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                badge = MaterialShapes.Clover4Leaf.toShape(),
                container = colors.secondaryContainer,
                content = colors.onSecondaryContainer,
                title = aira.mostExploredTitle,
                subtitle = stringResource(R.string.most_explored)
            )
        }
        if (aira.visuals > 0) {
            EngagementRow(
                icon = Icons.Outlined.Image,
                badge = MaterialShapes.Flower.toShape(),
                container = colors.tertiaryContainer,
                content = colors.onTertiaryContainer,
                title = pluralStringResource(R.plurals.insights_visuals_created, aira.visuals, aira.visuals),
                subtitle = stringResource(R.string.visuals_subtitle)
            )
        }
    }
}

@Composable
private fun EngagementRow(icon: ImageVector, badge: Shape, container: Color, content: Color, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
    ) {
        ShapeBadge(icon, badge, container, content, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
