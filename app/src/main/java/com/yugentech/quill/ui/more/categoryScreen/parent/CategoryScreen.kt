package com.yugentech.quill.ui.more.categoryScreen.parent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.yugentech.quill.database.model.Category
import com.yugentech.quill.category.viewmodel.CategoryViewModel
import com.yugentech.quill.ui.more.categoryScreen.components.AddCategoryDialog
import com.yugentech.quill.ui.more.categoryScreen.components.DragDropList

enum class CategoryDialogType {
    None, Add, Rename, Delete
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    categoryViewModel: CategoryViewModel
) {
    val categories by categoryViewModel.categories.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val isFabExpanded by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction < 0.5f }
    }

    var activeDialog by remember { mutableStateOf(CategoryDialogType.None) }
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
                        Text("Collections")
                        Text(
                            text = "Organize your library",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { activeDialog = CategoryDialogType.Add },
                expanded = isFabExpanded,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Category") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { innerPadding ->
        DragDropList(
            modifier = Modifier.padding(innerPadding),
            items = categories,
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

    when (activeDialog) {
        CategoryDialogType.Add -> {
            AddCategoryDialog(
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
                        categoryViewModel.deleteCategory(category.name)
                        closeDialog()
                    }
                )
            }
        }

        CategoryDialogType.None -> Unit
    }
}

@Composable
fun DeleteCategoryDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Category") },
        text = {
            Text("Are you sure you want to delete \"$categoryName\"? All books in this category will be moved back to the default Shelf.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RenameCategoryDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialName) }
    val isReserved = text.equals("Shelf", ignoreCase = true) ||
            text.equals("Favorites", ignoreCase = true)
    val isInvalid = text.isBlank() || isReserved

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Category") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("New Name") },
                isError = isReserved,
                supportingText = {
                    if (isReserved) Text("Name reserved for system")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = !isInvalid && text != initialName
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}