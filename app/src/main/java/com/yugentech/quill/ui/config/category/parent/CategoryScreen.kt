package com.yugentech.quill.ui.config.category.parent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R
import com.yugentech.quill.category.viewmodel.CategoryViewModel
import com.yugentech.quill.database.model.Category
import com.yugentech.quill.ui.config.category.components.AddCategorySheet
import com.yugentech.quill.ui.config.category.components.CategoryDialogType
import com.yugentech.quill.ui.config.category.components.DeleteCategoryDialog
import com.yugentech.quill.ui.config.category.components.DragDropList
import com.yugentech.quill.ui.config.category.components.PinnedShelfItem
import com.yugentech.quill.ui.config.category.components.RenameCategoryDialog
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.ui.common.components.ResumeFab

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    categoryViewModel: CategoryViewModel,
    openAddSheetOnEntry: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    val categories by categoryViewModel.categories.collectAsState()
    val isLoaded by categoryViewModel.isLoaded.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val isFabExpanded by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction < 0.5f }
    }

    var activeDialog by remember { mutableStateOf(if (openAddSheetOnEntry) CategoryDialogType.Add else CategoryDialogType.None) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    val closeDialog = {
        activeDialog = CategoryDialogType.None
        selectedCategory = null
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.categories))
                        Text(
                            text = stringResource(R.string.categories_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            // The Library's FAB: scales in with the screen, and folds to its icon while the
            // list is scrolled (the top bar collapsed)
            ResumeFab(
                visible = true,
                isScrollingDown = !isFabExpanded,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                    activeDialog = CategoryDialogType.Add
                },
                icon = Icons.Default.Add,
                label = stringResource(R.string.add_category)
            )
        }
    ) { innerPadding ->
        val contentState = when {
            !isLoaded -> CategoryContent.Loading
            categories.none { !it.isSystem } -> CategoryContent.Empty
            else -> CategoryContent.List
        }

        val contentTransition = updateTransition(contentState, label = "categoriesContent")

        contentTransition.AnimatedContent(
            transitionSpec = {
                when {
                    // First load: show whatever's there at once, no animation on entry
                    initialState == CategoryContent.Loading ->
                        EnterTransition.None togetherWith ExitTransition.None

                    // The first category was added: the illustration shrinks and fades away
                    // while the pinned Shelf and the new category rise in one after the other
                    targetState == CategoryContent.List ->
                        EnterTransition.None togetherWith (
                            fadeOut(tween(EMPTY_EXIT_MS)) +
                                scaleOut(tween(EMPTY_EXIT_MS, easing = EmphasizedAccelerate), targetScale = 0.88f)
                            )

                    // The last category was deleted: the same moves played backwards. The
                    // category sinks away, then the Shelf, and as the Shelf goes the
                    // illustration grows back in
                    else ->
                        (fadeIn(tween(EMPTY_EXIT_MS, delayMillis = EMPTY_RETURN_DELAY_MS)) +
                            scaleIn(
                                tween(EMPTY_EXIT_MS, delayMillis = EMPTY_RETURN_DELAY_MS, easing = EmphasizedDecelerate),
                                initialScale = 0.88f
                            )) togetherWith ExitTransition.None
                }.using(SizeTransform(clip = false))
            }
        ) { state ->
            when (state) {
                CategoryContent.Loading -> Box(Modifier.fillMaxSize())

                CategoryContent.Empty -> CategoriesEmptyState(innerPadding)

                CategoryContent.List -> {
                    // Stagger the rows only between the list and the empty state: rising in
                    // (Shelf first) when coming from it, sinking out (category first) when going
                    // to it. While a transition runs, currentState is the state it's leaving
                    val fromEmpty = contentTransition.currentState == CategoryContent.Empty
                    val toEmpty = contentTransition.targetState == CategoryContent.Empty
                    val shelfMotion = Modifier.animateEnterExit(
                        enter = if (fromEmpty) riseIn(delayMillis = 120) else EnterTransition.None,
                        exit = if (toEmpty) sinkOut(delayMillis = 100) else ExitTransition.None
                    )
                    val listMotion = Modifier.animateEnterExit(
                        enter = if (fromEmpty) riseIn(delayMillis = 220) else EnterTransition.None,
                        exit = if (toEmpty) sinkOut(delayMillis = 0) else ExitTransition.None
                    )

                    // The last rows that had a category in them: when the last one is deleted,
                    // the list on its way out keeps showing it, so it can sink away rather than
                    // vanish the moment it's gone from the database
                    var shownCategories by remember { mutableStateOf(categories) }
                    if (categories.any { !it.isSystem }) shownCategories = categories

                    CategoriesList(
                        categories = shownCategories,
                        innerPadding = innerPadding,
                        shelfModifier = shelfMotion,
                        listModifier = listMotion,
                        onReorderFinished = { newOrder -> categoryViewModel.updateOrder(newOrder) },
                        onRename = { category ->
                            selectedCategory = category
                            activeDialog = CategoryDialogType.Rename
                        },
                        onDelete = { category ->
                            selectedCategory = category
                            activeDialog = CategoryDialogType.Delete
                        }
                    )
                }
            }
        }
    }

    when (activeDialog) {
        CategoryDialogType.Add -> {
            AddCategorySheet(
                onDismiss = closeDialog,
                onConfirm = { name ->
                    categoryViewModel.addCategory(name)
                    closeDialog()
                }
            )
        }

        CategoryDialogType.Rename -> {
            selectedCategory?.let { category ->
                RenameCategoryDialog(
                    initialName = category.name,
                    onDismiss = closeDialog,
                    onConfirm = { newName ->
                        categoryViewModel.renameCategory(category.copy(name = newName))
                        closeDialog()
                    }
                )
            }
        }

        CategoryDialogType.Delete -> {
            selectedCategory?.let { category ->
                DeleteCategoryDialog(
                    categoryName = category.name,
                    onDismiss = closeDialog,
                    onConfirm = {
                        categoryViewModel.deleteCategory(category)
                        closeDialog()
                    }
                )
            }
        }

        CategoryDialogType.None -> Unit
    }
}

private enum class CategoryContent { Loading, Empty, List }

private const val EMPTY_EXIT_MS = 220
private const val LIST_ENTER_MS = 480

// On the way back to empty, the illustration starts as the category finishes sinking away and
// overlaps the Shelf's exit, just as the Shelf's rise overlaps the illustration leaving
private const val EMPTY_RETURN_DELAY_MS = LIST_ENTER_MS

// Material 3 emphasized easings
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

/** Fades in while rising a short way from below, landing softly. */
private fun riseIn(delayMillis: Int): EnterTransition =
    fadeIn(tween(LIST_ENTER_MS, delayMillis = delayMillis)) +
        slideInVertically(tween(LIST_ENTER_MS, delayMillis = delayMillis, easing = EmphasizedDecelerate)) { it / 6 } +
        scaleIn(tween(LIST_ENTER_MS, delayMillis = delayMillis, easing = EmphasizedDecelerate), initialScale = 0.96f)

/** [riseIn] played backwards: fades out while sinking a short way down, picking up speed. */
private fun sinkOut(delayMillis: Int): ExitTransition =
    fadeOut(tween(LIST_ENTER_MS, delayMillis = delayMillis)) +
        slideOutVertically(tween(LIST_ENTER_MS, delayMillis = delayMillis, easing = EmphasizedAccelerate)) { it / 6 } +
        scaleOut(tween(LIST_ENTER_MS, delayMillis = delayMillis, easing = EmphasizedAccelerate), targetScale = 0.96f)

@Composable
private fun CategoriesList(
    categories: List<Category>,
    innerPadding: PaddingValues,
    shelfModifier: Modifier,
    listModifier: Modifier,
    onReorderFinished: (List<Category>) -> Unit,
    onRename: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    val shelfCategory = categories.firstOrNull { it.isSystem }
    val draggableCategories = categories.filter { !it.isSystem }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = innerPadding.calculateTopPadding())
    ) {
        shelfCategory?.let { shelf ->
            PinnedShelfItem(
                category = shelf,
                modifier = shelfModifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
        }

        DragDropList(
            modifier = listModifier
                .weight(1f)
                .padding(bottom = innerPadding.calculateBottomPadding() + 8.dp),
            items = draggableCategories,
            onReorderFinished = onReorderFinished,
            onRename = onRename,
            onDelete = onDelete
        )
    }
}

@Composable
private fun CategoriesEmptyState(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.offset(y = (-50).dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.empty_categories),
                contentDescription = stringResource(R.string.no_categories_cd),
                modifier = Modifier.size(240.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.no_categories_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.no_categories_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
