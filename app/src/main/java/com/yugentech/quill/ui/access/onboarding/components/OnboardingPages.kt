package com.yugentech.quill.ui.access.onboarding.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TouchApp
import com.yugentech.quill.R

// The onboarding flow, in order. The pager and progress indicator size themselves from this
// list, so adding or removing a page only needs a change here.
val onboardingPages = listOf(
    PageContent(
        title = "Your Library,\nBeautifully Kept",
        description = "Bring your own books or discover thousands of free classics, all gathered in one calm, beautifully organized library.",
        highlights = listOf(
            FeatureHighlight(Icons.Default.FileUpload, "Import EPUBs from your device"),
            FeatureHighlight(Icons.Default.Public, "Free classics from Project Gutenberg & Standard Ebooks"),
            FeatureHighlight(Icons.Default.Category, "Organize books into your own categories")
        ),
        imageRes = R.drawable.new_beginnings
    ),

    PageContent(
        title = "Read It\nYour Way",
        description = "A reader that adapts to you. Tune the page, set the mood, and keep the passages that move you.",
        highlights = listOf(
            FeatureHighlight(Icons.Default.FormatSize, "Your fonts, spacing & page themes"),
            FeatureHighlight(Icons.Default.MusicNote, "Ambient sounds & a warm night light"),
            FeatureHighlight(Icons.Default.Highlight, "Highlight passages and revisit them anytime")
        ),
        imageRes = R.drawable.feliz
    ),

    PageContent(
        title = "Meet\nAira",
        description = "Your AI reading companion. She reads along with you, answers from the book itself, and never spoils what's ahead.",
        highlights = listOf(
            FeatureHighlight(Icons.AutoMirrored.Filled.MenuBook, "Answers grounded in the actual text"),
            FeatureHighlight(Icons.Default.Lock, "Spoiler-locked to your reading progress"),
            FeatureHighlight(Icons.AutoMirrored.Filled.Chat, "Chat about plots, worlds & characters")
        ),
        imageRes = R.drawable.aira_smile_big
    ),

    PageContent(
        title = "Ask Without\nLeaving the Page",
        description = "Select any word or passage and Aira is right there. No switching apps, no losing your place.",
        highlights = listOf(
            FeatureHighlight(Icons.Default.TouchApp, "Define words & explain tricky passages"),
            FeatureHighlight(Icons.Default.Face, "Recall who a character is and their journey"),
            FeatureHighlight(Icons.Default.AutoAwesome, "Turn scenes into painted illustrations")
        ),
        imageRes = R.drawable.whoa
    ),

    PageContent(
        title = "Build a\nReading Habit",
        description = "Every page counts. See your reading habits come to life and get gentle nudges to keep your streak going.",
        highlights = listOf(
            FeatureHighlight(Icons.Default.Insights, "Reading insights, heatmaps & peak hours"),
            FeatureHighlight(Icons.Default.NotificationsActive, "Daily reminders to keep your streak"),
            FeatureHighlight(Icons.Default.Palette, "Color themes, dark & AMOLED modes")
        ),
        imageRes = R.drawable.growth
    )
)
