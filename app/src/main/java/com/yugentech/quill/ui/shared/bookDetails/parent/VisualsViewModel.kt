package com.yugentech.quill.ui.shared.bookDetails.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.quill.database.dao.VisualDao
import com.yugentech.quill.database.entity.VisualEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class VisualsViewModel(
    private val visualDao: VisualDao
) : ViewModel() {

    private val _visuals = MutableStateFlow<List<VisualEntity>>(emptyList())
    val visuals = _visuals.asStateFlow()

    fun loadVisuals(bookId: String) {
        viewModelScope.launch {
            visualDao.getVisualsForBookFlow(bookId).collect { visuals ->
                _visuals.value = visuals
            }
        }
    }

    fun deleteVisual(visual: VisualEntity) {
        viewModelScope.launch {
            visualDao.deleteVisual(visual.id)
            File(visual.imagePath).delete()
        }
    }
}
