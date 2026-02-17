package com.yugentech.quill.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.room.entities.CategoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    val userCategories: StateFlow<List<CategoryEntity>> = repository.getUserCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.insertCategory(name)
        }
    }

    fun renameCategory(categoryModel: CategoryModel, newName: String) {
        viewModelScope.launch {
            repository.updateCategory(categoryModel.toEntity())
        }
    }

    fun updateCategoryOrder(categories: List<CategoryEntity>) {
        viewModelScope.launch {
            repository.updateCategories(categories)
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategory(name)
        }
    }
}