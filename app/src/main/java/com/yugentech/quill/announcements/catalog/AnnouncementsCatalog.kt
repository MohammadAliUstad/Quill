package com.yugentech.quill.announcements.catalog

import com.yugentech.quill.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Translate
import com.yugentech.quill.announcements.model.Announcement
import com.yugentech.quill.announcements.model.AnnouncementAction
import com.yugentech.quill.announcements.model.AnnouncementStyle
import com.yugentech.quill.navigation.screen.AppScreen

/**
 * Central registry of all release announcements.
 *
 * Whenever you ship an update:
 * 1. Add your new [Announcement] entries to [ALL_ANNOUNCEMENTS].
 * 2. Set [Announcement.minVersionCode] to the new version code (or a range if relevant).
 * 3. Each announcement will be presented to existing users exactly once.
 *    Fresh installs automatically skip older announcements so new users aren't spammed.
 */
object AnnouncementsCatalog {

    /**
     * All registered announcements across releases.
     * Keep older announcements here as a permanent record; the system only shows
     * ones matching the current app version that haven't been seen yet.
     */
    val ALL_ANNOUNCEMENTS: List<Announcement> = listOf(
        // Version 5.0.0 (versionCode 15)
        Announcement(
            id = "v5_0_0_whats_new",
            minVersionCode = 15,
            maxVersionCode = 15,
            priority = 100,
            style = AnnouncementStyle.DIALOG,
            title = R.string.announcement_v5_title,
            message = R.string.announcement_v5_message,
            icon = Icons.Default.AutoAwesome,
            primaryAction = AnnouncementAction.OpenWhatsNew,
            primaryActionLabel = R.string.announcement_see_whats_new,
            dismissLabel = R.string.announcement_maybe_later
        ),
        Announcement(
            id = "v5_0_0_language_tip",
            minVersionCode = 15,
            maxVersionCode = 15,
            priority = 50,
            style = AnnouncementStyle.DIALOG,
            title = R.string.announcement_language_title,
            message = R.string.announcement_language_message,
            icon = Icons.Default.Translate,
            primaryAction = AnnouncementAction.OpenRoute(AppScreen.Language.route),
            primaryActionLabel = R.string.announcement_choose_language,
            dismissLabel = R.string.announcement_got_it
        )
    )
}
