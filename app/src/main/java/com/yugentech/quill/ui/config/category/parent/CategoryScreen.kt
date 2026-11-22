package com.yugentech.quill.ui.config.category.parent

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    categoryViewModel: CategoryViewModel,
    openAddSheetOnEntry: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    val categories by categoryViewModel.categories.collectAsState()
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
        if (categories.none { !it.isSystem }) {
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
        } else {
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
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                    )
                }

                DragDropList(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = innerPadding.calculateBottomPadding() + 8.dp),
                    items = draggableCategories,
                    onReorderFinished = { newOrder ->
                        categoryViewModel.updateOrder(newOrder)
                    },
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
