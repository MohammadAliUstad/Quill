package com.yugentech.quill.ui.config.editProfile.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.R
import com.yugentech.quill.ui.config.editProfile.components.AvatarSection
import com.yugentech.quill.ui.config.editProfile.components.DisplayNameSection
import com.yugentech.quill.ui.main.components.SectionHeader
import com.yugentech.quill.user.viewmodel.UserViewModel
import com.yugentech.theme.tokens.spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userViewModel: UserViewModel,
    userId: String,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by userViewModel.uiState.collectAsStateWithLifecycle()
    val user = uiState.user

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        userViewModel.loadUser(userId)
    }

    if (user != null) {

        var displayName by remember(user.userId) {
            mutableStateOf(user.name.orEmpty())
        }

        var selectedAvatarId by remember(user.userId) {
            mutableIntStateOf(user.avatarId ?: 0)
        }

        var validationError by remember { mutableStateOf<String?>(null) }

        val canSave = validationError == null && displayName.isNotBlank()

        LaunchedEffect(displayName) {
            validationError = when {
                displayName.isBlank() -> "Display name is required"
                displayName.length < 2 -> "At least 2 characters please"
                displayName.length > 20 -> "Keep it under 20 characters"
                !displayName.matches(Regex("^[a-zA-Z0-9\\\\s._-]+$")) -> "Letters, numbers, and basic symbols only"
                else -> null
            }
        }

        fun saveProfile() {
            if (canSave) {
                val updatedUser = user.copy(
                    name = displayName.trim(),
                    avatarId = selectedAvatarId
                )

                coroutineScope.launch {
                    userViewModel.upsertUser(updatedUser)
                }
                onNavigateBack()

            }
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets.statusBars,
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text("Edit Profile")
                            Text(
                                "Change your avatar and name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = MaterialTheme.spacing.m)
                    .imePadding()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                    SectionHeader(
                        icon = Icons.Default.Face,
                        title = "Choose Your Avatar"
                    )
                    AvatarSection(
                        selectedAvatarId = selectedAvatarId,
                        onAvatarSelected = { selectedAvatarId = it }
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
                ) {
                    SectionHeader(
                        icon = Icons.Default.Person,
                        title = "Display Name"
                    )

                    DisplayNameSection(
                        displayName = displayName,
                        onDisplayNameChange = {
                            if (it.length <= 20) {
                                displayName = it
                            }
                        },
                        validationError = validationError,
                        isSaving = uiState.isSaving,
                        canSave = canSave,
                        onSaveClick = { saveProfile() }
                    )
                }

                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}