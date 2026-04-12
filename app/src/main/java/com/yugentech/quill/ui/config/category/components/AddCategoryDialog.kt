package com.yugentech.quill.ui.config.category.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties
import com.yugentech.theme.tokens.AppConstants.EMPTY
import com.yugentech.theme.tokens.AppConstants.FAVOURITES
import com.yugentech.theme.tokens.AppConstants.SHELF
import com.yugentech.theme.tokens.corners

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(EMPTY) }

    val isReserved =
        text.equals(SHELF, ignoreCase = true) || text.equals(FAVOURITES, ignoreCase = true)
    val isBlank = text.isBlank()
    val isError = isReserved || (text.isNotEmpty() && isBlank)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.84f),
        title = { Text("New Collection") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Category Name") },
                isError = isError,
                // Pass null when there's no error to remove the reserved bottom spacing
                supportingText = if (isReserved) {
                    { Text("Name reserved for system") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                // textStyle removed to use the default normal text size
                shape = RoundedCornerShape(MaterialTheme.corners.medium),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    errorContainerColor = MaterialTheme.colorScheme.errorContainer,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text.trim()) },
                enabled = !isError && !isBlank
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
}