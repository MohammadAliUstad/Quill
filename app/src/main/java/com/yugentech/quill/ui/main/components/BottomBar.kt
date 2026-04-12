package com.yugentech.quill.ui.main.components

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.yugentech.quill.R

@Composable
fun BottomBar(
    currentTab: QuillTab,
    onTabSelected: (QuillTab) -> Unit
) {
    NavigationBar {
        QuillTab.entries.forEach { tab ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { if (!isSelected) onTabSelected(tab) },
                icon = {
                    val resId = when (tab) {
                        QuillTab.Library -> R.drawable.anim_library_enter
                        QuillTab.Discover -> R.drawable.avd_explore_enter
                        QuillTab.Sources -> R.drawable.avd_feed_enter
                        QuillTab.Settings -> R.drawable.anim_more_enter
                    }
                    AnimatedNavIcon(
                        selected = isSelected,
                        avdRes = resId,
                        contentDescription = tab.title
                    )
                },
                label = { Text(tab.title) }
            )
        }
    }
}

@Composable
private fun AnimatedNavIcon(
    selected: Boolean,
    avdRes: Int,
    contentDescription: String
) {
    val image = AnimatedImageVector.animatedVectorResource(avdRes)
    Icon(
        painter = rememberAnimatedVectorPainter(
            animatedImageVector = image,
            atEnd = selected
        ),
        contentDescription = contentDescription
    )
}