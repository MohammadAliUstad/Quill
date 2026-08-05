package com.yugentech.quill.ui.about.aira.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.yugentech.quill.ui.about.aira.components.CapabilitiesCarousel
import com.yugentech.quill.ui.about.aira.components.ClosingCard
import com.yugentech.quill.ui.about.aira.components.HeroSection
import com.yugentech.quill.ui.about.aira.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiraAboutScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val surfaceColor = MaterialTheme.colorScheme.surface

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = (-scrollBehavior.state.contentOffset / 100f).coerceIn(0f, 1f)
                        }
                        .background(
                            Brush.verticalGradient(
                                0.0f to surfaceColor.copy(alpha = 0.9f),
                                0.4f to surfaceColor.copy(alpha = 0.7f),
                                0.7f to surfaceColor.copy(alpha = 0.30f),
                                1.0f to surfaceColor.copy(alpha = 0.0f)
                            )
                        )
                )

                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item { HeroSection() }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionLabel("Capabilities")
                    CapabilitiesCarousel()
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionLabel("Vision")
                    ClosingCard()
                }
            }
        }
    }
}
