package com.yugentech.quill.ui.config.category.parent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yugentech.quill.R
import com.yugentech.quill.category.viewmodel.CategoryViewModel
import com.yugentech.quill.database.model.Category
import com.yugentech.quill.ui.config.category.components.AddCategoryDialog
import com.yugentech.quill.ui.config.category.components.CategoryDialogType
import com.yugentech.quill.ui.config.category.components.DeleteCategoryDialog
import com.yugentech.quill.ui.config.category.components.DragDropList
import com.yugentech.quill.ui.config.category.components.RenameCategoryDialog

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
                        Text("Categories")
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
            FloatingActionButton(
                onClick = { activeDialog = CategoryDialogType.Add },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(0.dp),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(64.dp)
                    .widthIn(min = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category",
                        modifier = Modifier.size(26.dp)
                    )

                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = fadeIn(animationSpec = tween(200)) + expandHorizontally(
                            animationSpec = tween(200),
                            expandFrom = Alignment.Start
                        ),
                        exit = fadeOut(animationSpec = tween(200)) + shrinkHorizontally(
                            animationSpec = tween(200),
                            shrinkTowards = Alignment.Start
                        )
                    ) {
                        Text(
                            text = "Add Category",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (categories.isEmpty()) {
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
                        contentDescription = "No categories",
                        modifier = Modifier.size(240.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "No categories yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap the button below to create\nyour first category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            DragDropList(
                modifier = Modifier.padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 8.dp
                ),
                items = categories,
                onReorderFinished = { newOrder ->
                    categoryViewModel.updateOrder(newOrder)
                },
                onRename = { category ->
                    selectedCategory = category
                    activeDialog =
                        CategoryDialogType.Rename
                },
                onDelete = { category ->
                    selectedCategory = category
                    activeDialog =
                        CategoryDialogType.Delete
                }
            )
        }
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
                        categoryViewModel.deleteCategory(category)
                        closeDialog()
                    }
                )
            }
        }

        CategoryDialogType.None -> Unit
    }
}