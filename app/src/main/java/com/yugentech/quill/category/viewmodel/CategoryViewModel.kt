package com.yugentech.quill.category.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.model.Category
import com.yugentech.quill.category.repository.CategoryRepository
import com.yugentech.quill.database.mapper.toDomainModel
import com.yugentech.quill.database.mapper.toEntity
import com.yugentech.theme.tokens.AppConstants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.getUserCategories()
        .map { entities ->
            entities.map { categoryEntity ->
                categoryEntity.toDomainModel()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(AppConstants.FIVE),
            initialValue = emptyList()
        )

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.insertCategory(name)
        }
    }

    fun renameCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category.toEntity())
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategory(name)
        }
    }

    fun updateOrder(reorderedList: List<Category>) {
        viewModelScope.launch {
            repository.updateCategories(reorderedList.map { it.toEntity() })
        }
    }
}