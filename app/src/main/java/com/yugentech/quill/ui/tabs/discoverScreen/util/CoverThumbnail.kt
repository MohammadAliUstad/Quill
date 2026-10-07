package com.yugentech.quill.ui.tabs.discoverScreen.util

import com.yugentech.quill.database.model.Book

/**
 * Standard Ebooks serves a 1400x2100 (~350 KB) `cover.jpg` and a 350x525 (~40 KB)
 * `cover-thumbnail.jpg` side by side. Small cards and blurred backgrounds don't need the
 * full cover, so swap to the thumbnail. Other URLs are returned unchanged.
 */
fun String.toCoverThumbnailUrl(): String =
    if (contains("standardebooks.org") && endsWith("/cover.jpg")) {
        removeSuffix("cover.jpg") + "cover-thumbnail.jpg"
    } else {
        this
    }

val Book.thumbnailCoverUrl: String?
    get() = coverUrl?.toCoverThumbnailUrl()
