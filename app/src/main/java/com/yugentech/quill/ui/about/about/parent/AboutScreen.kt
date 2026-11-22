package com.yugentech.quill.ui.about.about.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.quill.ui.about.about.components.AppInfoCard
import com.yugentech.quill.ui.about.about.components.DonationDialog
import com.yugentech.quill.ui.about.about.components.ThankYouDialog
import com.yugentech.quill.ui.about.about.components.AboutContent
import com.yugentech.quill.ui.common.components.SectionHeader
import com.yugentech.quill.ui.common.components.ToastMessage
import com.yugentech.quill.ui.common.components.SettingsListItem
import com.yugentech.theme.tokens.AppConstants
import com.yugentech.theme.tokens.spacing
import org.koin.androidx.compose.koinViewModel
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.localization.AppStrings
import com.yugentech.quill.ui.about.about.viewmodel.AboutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToMoreApps: () -> Unit,
    viewModel: AboutViewModel = koinViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showDonationDialog by remember { mutableStateOf(false) }
    var showThankYouDialog by remember { mutableStateOf(false) }
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val pendingMessage = stringResource(R.string.billing_payment_pending)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BillingEvent.TipThankYou -> showThankYouDialog = true
                is BillingEvent.PurchasePending -> toastMessage = pendingMessage
                is BillingEvent.Error -> toastMessage = event.message
                else -> Unit
            }
        }
    }

    val supportItems = remember(context) {
        AboutContent.getSupportItems(
            context = context,
            onDonateClick = { showDonationDialog = true },
            onMoreAppsClick = onNavigateToMoreApps
        )
    }

    val communityItems = remember(context) {
        AboutContent.getCommunityItems(context)
    }

    val legalItems = remember(context) {
        AboutContent.getLegalItems(
            context = context,
            onNavigateToLicenses = onNavigateToLicenses
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.more_section_about))
                        Text(
                            stringResource(R.string.about_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerpadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerpadding.calculateTopPadding())
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
                contentPadding = PaddingValues(
                    bottom = innerpadding.calculateBottomPadding() + 8.dp,
                    start = 16.dp,
                    end = 16.dp
                )
            ) {
                item {
                    AppInfoCard()
                }

                item {
                    SectionHeader(
                        icon = Icons.Filled.Favorite,
                        title = stringResource(R.string.about_section_connect)
                    )
                }
                itemsIndexed(supportItems) { index, item ->
                    SettingsListItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        leadingIcon = item.icon,
                        index = index,
                        totalCount = supportItems.size,
                        onClick = item.onClick
                    )
                }

                item {
                    SectionHeader(
                        icon = Icons.Filled.ThumbUp,
                        title = stringResource(R.string.about_section_share)
                    )
                }

                itemsIndexed(communityItems) { index, item ->
                    SettingsListItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        leadingIcon = item.icon,
                        index = index,
                        totalCount = communityItems.size,
                        onClick = item.onClick
                    )
                }

                item {
                    SectionHeader(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.about_section_legal)
                    )
                }

                itemsIndexed(legalItems) { index, item ->
                    SettingsListItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        leadingIcon = item.icon,
                        index = index,
                        totalCount = legalItems.size,
                        onClick = item.onClick
                    )
                }
            }

            ToastMessage(
                message = toastMessage,
                onDismiss = { toastMessage = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (showDonationDialog) {
            DonationDialog(
                onDismiss = { showDonationDialog = false },
                onCoffeeClick = {
                    val activity = context.findActivity()
                    viewModel.buyCoffee(activity)
                    showDonationDialog = false
                },
                onLunchClick = {
                    val activity = context.findActivity()
                    viewModel.buyLunch(activity)
                    showDonationDialog = false
                },
                onWebClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, AppConstants.KOFI_URL.toUri())
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        toastMessage = AppStrings.get(R.string.could_not_open_link)
                    }
                    showDonationDialog = false
                }
            )
        }

        if (showThankYouDialog) {
            ThankYouDialog(
                onDismiss = { showThankYouDialog = false }
            )
        }
    }
}

fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found in context chain")
}