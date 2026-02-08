package com.yugentech.quill.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.room.entities.UserCategoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    // STATE: All categories (including system categories: Favorites, Uncategorized)
    val allCategories: StateFlow<List<UserCategoryEntity>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // STATE: Only user-created categories (excludes Favorites & Uncategorized)
    val userCategories: StateFlow<List<UserCategoryEntity>> = repository.getUserCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // STATE: For showing error/success messages
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // INITIALIZATION: Seed default categories on first launch
    init {
        viewModelScope.launch {
            repository.initializeDefaultCategories()
        }
    }

    // ACTION: Create new category with validation
    fun createCategory(name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _message.value = "Category name cannot be empty"
            return
        }

        viewModelScope.launch {
            val result = repository.createCategory(cleanName)

            result.onSuccess {
                _message.value = "Category '$cleanName' created"
            }.onFailure { error ->
                _message.value = error.message ?: "Failed to create category"
            }
        }
    }

    // ACTION: Delete category (moves books to Uncategorized)
    fun deleteCategory(name: String) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(name)
                _message.value = "Category '$name' deleted"
            } catch (e: Exception) {
                _message.value = "Failed to delete category: ${e.message}"
            }
        }
    }

    // ACTION: Reorder categories (drag and drop)
    fun updateCategoryOrder(newOrder: List<UserCategoryEntity>) {
        viewModelScope.launch {
            try {
                // Re-assign sortOrder indices based on the new list order
                // Keep system categories (Favorites: 1, Uncategorized: 999) intact
                val updates = newOrder.mapIndexed { index, category ->
                    if (category.isSystem) {
                        category // Don't change system category sortOrder
                    } else {
                        // User categories get sortOrder 2, 3, 4, etc.
                        category.copy(sortOrder = index + 2)
                    }
                }
                repository.updateCategories(updates)
            } catch (e: Exception) {
                _message.value = "Failed to reorder categories: ${e.message}"
            }
        }
    }

    // ACTION: Clear message after showing it
    fun clearMessage() {
        _message.value = null
    }

    // HELPER: Check if a category name is valid
    fun isValidCategoryName(name: String): Boolean {
        val cleanName = name.trim()
        return cleanName.isNotBlank() &&
                cleanName != "Favorites" &&
                cleanName != "Uncategorized"
    }

    // HELPER: Get category count (useful for UI)
    fun getCategoryCount(): Int = userCategories.value.size
}