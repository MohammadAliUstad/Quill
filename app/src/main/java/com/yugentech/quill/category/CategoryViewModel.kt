package com.yugentech.quill.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.room.entities.toDomain
import com.yugentech.theme.tokens.AppConstants.FIVE
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryModel>> = repository.getUserCategories()
        .map { entities ->
            entities.map { categoryEntity ->
                categoryEntity.toDomain()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(FIVE),
            initialValue = emptyList()
        )

    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.insertCategory(name)
        }
    }

    fun renameCategory(category: CategoryModel) {
        viewModelScope.launch {
            repository.updateCategory(category.toEntity())
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            repository.deleteCategory(name)
        }
    }

    fun updateOrder(reorderedList: List<CategoryModel>) {
        viewModelScope.launch {
            repository.updateCategories(reorderedList.map { it.toEntity() })
        }
    }
}