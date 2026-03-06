package com.yugentech.quill.ui.shared.airaScreen.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yugentech.quill.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiraAvatar(size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        LoadingIndicator(
            modifier = Modifier.size(size),
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Image(
            painter = painterResource(id = R.drawable.aira),
            contentDescription = "Aira Avatar",
            modifier = Modifier
                .size(size)
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}