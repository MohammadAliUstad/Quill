package com.yugentech.quill.ui.more.subscriptions.parent

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.billing.SubscriptionViewModel
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.quill.ui.more.subscriptions.components.SubscribeBottomBar
import com.yugentech.quill.ui.more.subscriptions.components.SubscribedContent
import com.yugentech.quill.ui.more.subscriptions.components.UnsubscribedContent
import kotlinx.coroutines.delay

data class PlanOption(
    val basePlanId: String,
    val label: String,
    val tag: String? = null
)

val planOptions = listOf(
    PlanOption("monthly-base", "Monthly"),
    PlanOption("yearly-base", "Yearly")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    subscriptionViewModel: SubscriptionViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isPro by subscriptionViewModel.isPro.collectAsStateWithLifecycle()
    val subProducts by subscriptionViewModel.subProducts.collectAsStateWithLifecycle()
    val isRestoring by subscriptionViewModel.isRestoring.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var selectedPlanIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        subscriptionViewModel.events.collect { event ->
            val message = when (event) {
                is BillingEvent.SubscriptionActivated -> "Welcome to Quill Pro!"
                is BillingEvent.NoSubscriptionFound -> "No active subscription found."
                is BillingEvent.Error -> event.message
                else -> null
            }

            if (event is BillingEvent.SubscriptionActivated) {
                delay(2000)
            }
            message?.let { snackbarHostState.showSnackbar(it) }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                // Custom Snackbar Implementation
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = MaterialTheme.shapes.medium,
                    actionColor = MaterialTheme.colorScheme.primary,
                    dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    snackbarData = data
                )
            }
        },
        topBar = {
            if (isPro) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Subscription"
                            )
                            Text(
                                text = "Premium Purchased",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                // LargeTopAppBar for the scrollable unsubscribed screen
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(text = "Quill Pro")
                            Text(
                                text = "Unlock Aira's full potential",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "subscription_state"
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