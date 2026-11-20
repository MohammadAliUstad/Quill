package com.yugentech.quill.ui.access.subscriptions.parent

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import com.yugentech.quill.billing.viewmodel.RestoreResult
import com.yugentech.quill.billing.viewmodel.SubscriptionViewModel
import com.yugentech.quill.ui.main.components.QuillDialog
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.quill.ui.access.subscriptions.components.SubscribeBottomBar
import com.yugentech.quill.ui.access.subscriptions.components.SubscribedContent
import com.yugentech.quill.ui.access.subscriptions.components.UnsubscribedContent
import androidx.annotation.StringRes
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource
import com.yugentech.quill.localization.AppStrings

/** Why Restore didn't bring Pro back. Only an OK button: there's nothing to choose. */
@Composable
private fun RestoreResultDialog(result: RestoreResult, onDismiss: () -> Unit) {
    val notFound = result == RestoreResult.NotFound
    QuillDialog(
        icon = if (notFound) Icons.Outlined.Info else Icons.Outlined.ErrorOutline,
        title = stringResource(if (notFound) R.string.restore_not_found_title else R.string.restore_failed_title),
        message = stringResource(
            when (result) {
                RestoreResult.NotFound -> R.string.restore_not_found_message
                RestoreResult.ConnectionFailed -> R.string.billing_connect_failed
                RestoreResult.NeedsAccount -> R.string.guest_subscribe_needs_account
                RestoreResult.NotSignedIn -> R.string.billing_login_restore
            }
        ),
        primaryLabel = stringResource(R.string.ok),
        onPrimary = onDismiss,
        onDismiss = onDismiss
    )
}

data class PlanOption(
    val basePlanId: String,
    @param:StringRes val label: Int,
    val tag: String? = null
)

val planOptions = listOf(
    PlanOption("monthly-base", R.string.plan_monthly),
    PlanOption("yearly-base", R.string.plan_yearly)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    subscriptionViewModel: SubscriptionViewModel
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as? Activity

    val isPro by subscriptionViewModel.isPro.collectAsStateWithLifecycle()
    val subProducts by subscriptionViewModel.subProducts.collectAsStateWithLifecycle()
    val isRestoring by subscriptionViewModel.isRestoring.collectAsStateWithLifecycle()
    val restoreResult by subscriptionViewModel.restoreResult.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var selectedPlanIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        subscriptionViewModel.events.collect { event ->
            val message = when (event) {
                is BillingEvent.PurchasePending -> AppStrings.get(R.string.billing_payment_pending)
                is BillingEvent.Error -> event.message
                else -> null
            }
            message?.let { snackbarHostState.showSnackbar(it) }
        }
    }

    // How a restore ended, in a dialog rather than a passing snackbar
    restoreResult?.let { result ->
        RestoreResultDialog(result = result, onDismiss = subscriptionViewModel::dismissRestoreResult)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        // Positioned inside the Scaffold to appear correctly above the bottom bar
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 4.dp)
            ) { data ->
                Snackbar(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    // Force vertical center alignment using a Box wrapper
                    Box(
                        modifier = Modifier
                            .heightIn(min = 32.dp), // Ensures consistency with Snackbar min-height
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        topBar = {
            if (isPro) {
                TopAppBar(
                    title = {
                        Column {
                            Text(text = stringResource(R.string.subscription))
                            Text(
                                text = stringResource(R.string.premium_purchased),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    // A fixed bar on a full screen: no scroll behaviour (the one shared with the
                    // large bar could leave it collapsed and tinted) and no fill of its own, so
                    // the screen's background runs straight up under the status bar
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            } else {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(text = stringResource(R.string.quill_pro))
                            Text(
                                text = stringResource(R.string.subscription_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        },
        bottomBar = {
            if (!isPro) {
                SubscribeBottomBar(
                    selectedPlanIndex = selectedPlanIndex,
                    isRestoring = isRestoring,
                    onSubscribeClick = {
                        activity?.let {
                            subscriptionViewModel.subscribe(it, planOptions[selectedPlanIndex].basePlanId)
                        }
                    },
                    onRestoreClick = { subscriptionViewModel.restorePurchases() }
                )
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = isPro,
            transitionSpec = {
                // Reduced duration to 250ms for a snappier feel
                val duration = 250
                (slideInVertically(
                    initialOffsetY = { 40 }, // Reduced offset for a tighter transition
                    animationSpec = tween(duration)
                ) + fadeIn(animationSpec = tween(duration))).togetherWith(
                    slideOutVertically(
                        targetOffsetY = { -40 },
                        animationSpec = tween(duration)
                    ) + fadeOut(animationSpec = tween(duration))
                )
            },
            label = "subscription_state",
            modifier = Modifier.fillMaxSize()
        ) { isSubscribed ->
            if (isSubscribed) {
                SubscribedContent(paddingValues = paddingValues, onBack = onBack)
            } else {
                UnsubscribedContent(
                    paddingValues = paddingValues,
                    selectedPlanIndex = selectedPlanIndex,
                    subProducts = subProducts,
                    onPlanSelected = { selectedPlanIndex = it }
                )
            }
        }
    }
}