package com.yugentech.quill.ui.common.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.R

/**
 * The name shown for a shelf. "Favorites" and "My Shelf" are the built-in shelves' ids (see
 * SeeAllViewModel) and "Shelf" the default category's stored name, all shown in the app's
 * language; any other is the reader's own category name.
 */
@Composable
fun shelfTitle(id: String): String = when (id) {
    "Favorites" -> stringResource(R.string.shelf_favorites)
    "My Shelf" -> stringResource(R.string.shelf_my_shelf)
    "Shelf" -> stringResource(R.string.shelf_default)
    else -> id
}
