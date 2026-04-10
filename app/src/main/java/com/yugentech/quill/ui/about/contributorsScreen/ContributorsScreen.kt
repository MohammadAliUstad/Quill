package com.yugentech.quill.ui.about.contributorsScreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// ContributorsScreen
//
// Credits every person who shaped Quill — from core team to beta testers.
// Full Material 3 color roles, LargeFluentTopAppBar, no hardcoded colors.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContributorsScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Contributors",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Opening quote
            OpeningBanner()

            Spacer(modifier = Modifier.height(32.dp))

            // ── Core Team ────────────────────────────────────────────────────
            SectionHeader(
                icon = Icons.Outlined.Star,
                label = "Core Team"
            )
            Spacer(modifier = Modifier.height(12.dp))
            CoreTeamSection()

            Spacer(modifier = Modifier.height(28.dp))

            // ── Beta Testers ─────────────────────────────────────────────────
            SectionHeader(
                icon = Icons.Outlined.BugReport,
                label = "Beta Testers"
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "These readers put Quill through its paces before anyone else — their feedback shaped everything.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            TestersSection()

            Spacer(modifier = Modifier.height(28.dp))

            // ── Open Source ───────────────────────────────────────────────────
            SectionHeader(
                icon = Icons.Outlined.Code,
                label = "Built On Open Source"
            )
            Spacer(modifier = Modifier.height(12.dp))
            OpenSourceSection()

            Spacer(modifier = Modifier.height(28.dp))

            // ── Acknowledgements ──────────────────────────────────────────────
            SectionHeader(
                icon = Icons.Outlined.Favorite,
                label = "Special Thanks"
            )
            Spacer(modifier = Modifier.height(12.dp))
            AcknowledgementsCard()

            Spacer(modifier = Modifier.height(28.dp))

            // ── Footer ────────────────────────────────────────────────────────
            FooterCard()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Opening Banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OpeningBanner() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Great software is never built alone.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Quill exists because of the people who believed in it, tested it relentlessly, and pushed it further than one person ever could.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 21.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Core Team
// ─────────────────────────────────────────────────────────────────────────────

private data class CoreMember(
    val initials: String,
    val name: String,
    val role: String,
    val detail: String,
    val slot: Int  // 0=primary, 1=secondary, 2=tertiary
)

@Composable
private fun CoreTeamSection() {
    val team = listOf(
        CoreMember(
            initials = "M",
            name = "Mohammad",
            role = "Founder & Android Developer",
            detail = "Designed and built Quill from the ground up — architecture, UI, the RAG pipeline, and everything in between.",
            slot = 0
        ),
        CoreMember(
            initials = "OS",
            name = "Omar Shaikh",
            role = "AI Engineer",
            detail = "Shaped the intelligence behind Aira — from prompt design to model integration and response quality.",
            slot = 1
        ),
        CoreMember(
            initials = "JB",
            name = "Junaid Bagwan",
            role = "Chai",
            detail = "Gave Chai Toss to everyone on time making sure everyone was energized!",
            slot = 2
        ),
        CoreMember(
            initials = "MS",
            name = "Mubarish Shaikh",
            role = "CEO",
            detail = "Constantly pestered everyone and was a menace throughout the project, means true bhai true. And one day his dream was become CEO. So I made him one.",
            slot = 0
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        team.forEach { member ->
            CoreMemberCard(member)
        }
    }
}

@Composable
private fun CoreMemberCard(member: CoreMember) {
    val containerColor = when (member.slot) {
        0 -> MaterialTheme.colorScheme.primaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val onContainerColor = when (member.slot) {
        0 -> MaterialTheme.colorScheme.onPrimaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val avatarColor = when (member.slot) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val onAvatarColor = when (member.slot) {
        0 -> MaterialTheme.colorScheme.onPrimary
        1 -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onTertiary
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = avatarColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = member.initials,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onAvatarColor
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = onContainerColor
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = avatarColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = member.role,
                        style = MaterialTheme.typography.labelSmall,
                        color = onContainerColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = member.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainerColor.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Beta Testers
// ─────────────────────────────────────────────────────────────────────────────

private data class Tester(val name: String, val initials: String)

@Composable
private fun TestersSection() {
    val testers = listOf(
        Tester("Hasan Ustad",       "HU"),
        Tester("Umar Mujawar",      "UM"),
        Tester("Zaid Kalim",        "ZK"),
        Tester("Faizan Qadri",      "FQ"),
        Tester("Deep Patel",        "DP"),
        Tester("Shahbaz Luqman",    "SL"),
        Tester("Sehan Devadi",      "SD"),
        Tester("Shaizan Sayyed",    "SS"),
        Tester("Ethan Caldwell",    "EC"),
        Tester("Marcus Webb",       "MW"),
        Tester("Sophie Hartman",    "SH"),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Avatars row — first 8
            Row(
                horizontalArrangement = Arrangement.spacedBy((-10).dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.error,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.error,
                )
                val onColors = listOf(
                    MaterialTheme.colorScheme.onPrimary,
                    MaterialTheme.colorScheme.onSecondary,
                    MaterialTheme.colorScheme.onTertiary,
                    MaterialTheme.colorScheme.onError,
                    MaterialTheme.colorScheme.onPrimary,
                    MaterialTheme.colorScheme.onSecondary,
                    MaterialTheme.colorScheme.onTertiary,
                    MaterialTheme.colorScheme.onError,
                )
                testers.take(8).forEachIndexed { i, tester ->
                    Surface(
                        shape = CircleShape,
                        color = colors[i % colors.size],
                        modifier = Modifier.size(38.dp),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = tester.initials,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = onColors[i % onColors.size]
                            )
                        }
                    }
                }
                // +N overflow badge
                if (testers.size > 8) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.size(38.dp),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "+${testers.size - 8}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Full name list as chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                testers.forEach { tester ->
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = tester.name,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Open Source Acknowledgements
// ─────────────────────────────────────────────────────────────────────────────

private data class OssItem(val name: String, val purpose: String)

@Composable
private fun OpenSourceSection() {
    val libs = listOf(
        OssItem("Readium SDK",          "EPUB parsing and rendering"),
        OssItem("ONNX Runtime Android", "On-device embedding inference"),
        OssItem("BAAI/bge-small-en",    "Text embedding model"),
        OssItem("Google Gemini",        "Aira's conversational intelligence"),
        OssItem("Room Database",        "Local vector and message storage"),
        OssItem("Jetpack Compose",      "Entire UI layer"),
        OssItem("Koin",                 "Dependency injection"),
        OssItem("WorkManager",          "Background book indexing"),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        libs.forEachIndexed { index, lib ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (index % 2 == 0)
                    MaterialTheme.colorScheme.surfaceContainerLow
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lib.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = lib.purpose,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Acknowledgements Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AcknowledgementsCard() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AckRow(
                icon = Icons.Outlined.MenuBook,
                text = "To every author whose work lives inside Quill — your words made building this worth it."
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AckRow(
                icon = Icons.Outlined.School,
                text = "To the open-source community for making powerful tools accessible to independent developers."
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AckRow(
                icon = Icons.Outlined.People,
                text = "To our early users — your patience during the rough builds and your honest feedback built the foundation."
            )
        }
    }
}

@Composable
private fun AckRow(icon: ImageVector, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = FontStyle.Italic,
            lineHeight = 21.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Footer Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FooterCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Made with care in 2026",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Quill is an independent project by Yugen Tech.\nNo investors. No compromises. Just books.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth(0.35f)
            )
            Text(
                text = "Version 1.0 · Quill · Yugen Tech",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f)
            )
        }
    }
}