package com.yugentech.quill.ui.about.moreApps.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.ui.about.moreApps.components.ClosingCard
import com.yugentech.quill.ui.about.moreApps.components.HajimeFeatures
import com.yugentech.quill.ui.about.moreApps.components.HajimeHeroSection
import com.yugentech.quill.ui.about.moreApps.components.MoreAppsIntro
import com.yugentech.quill.ui.about.moreApps.components.MyntFeatures
import com.yugentech.quill.ui.about.moreApps.components.MyntHeroSection
import com.yugentech.quill.ui.about.moreApps.components.RyoriFeatures
import com.yugentech.quill.ui.about.moreApps.components.RyoriHeroSection
import com.yugentech.quill.ui.about.moreApps.components.SESSIONS_PACKAGE
import com.yugentech.quill.ui.about.moreApps.components.SectionDivider
import com.yugentech.quill.ui.about.moreApps.components.SessionsFeatures
import com.yugentech.quill.ui.about.moreApps.components.SessionsHeroSection
import com.yugentech.quill.ui.about.moreApps.components.openPlayStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreAppsScreen(
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.more_more_from_us)) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { MoreAppsIntro() }

            item { MyntHeroSection() }
            item { MyntFeatures() }
            item { SectionDivider() }

            item { HajimeHeroSection() }
            item { HajimeFeatures() }
            item { SectionDivider() }

            item { SessionsHeroSection(onDownloadClick = { openPlayStore(context, SESSIONS_PACKAGE) }) }
            item { SessionsFeatures() }
            item { SectionDivider() }

            item { RyoriHeroSection() }
            item { RyoriFeatures() }
            item { SectionDivider() }

            item { ClosingCard() }
        }
    }
}
