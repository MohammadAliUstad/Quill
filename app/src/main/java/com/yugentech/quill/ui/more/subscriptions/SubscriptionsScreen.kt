package com.yugentech.quill.ui.more.subscriptions

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yugentech.quill.billing.SubscriptionViewModel
import com.yugentech.quill.domain.BillingEvent
import com.yugentech.theme.tokens.corners
import com.yugentech.theme.tokens.spacing
import org.koin.androidx.compose.koinViewModel

// Maps Play Console base plan IDs to UI labels
private data class PlanOption(
    val basePlanId: String,
    val label: String,
    val tag: String? = null
)

private val planOptions = listOf(
    PlanOption("monthly-base", "Monthly"),
    PlanOption("yearly-base", "Yearly", "Best Value")
)

private val features = listOf(
    "Unlimited Aira AI queries",
    "Your personal AI reading companion",
    "Ask Aira about any passage",
    "Deep book insights and analysis",
    "Priority support"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val subProducts by viewModel.subProducts.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Default to yearly (index 1) since it's the better value
    var selectedPlanIndex by remember { mutableIntStateOf(1) }

    // Collect one-shot billing events and show snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is BillingEvent.SubscriptionActivated -> "Welcome to Quill Pro!"
                is BillingEvent.UserCancelled -> null
                is BillingEvent.Error -> event.message
                else -> null
            }
            message?.let { snackbarHostState.showSnackbar(it) }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MediumTopAppBar(
                title = { Text("Quill Pro", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            // Hide bottom bar if user is already subscribed
            if (!isPro) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(MaterialTheme.spacing.m)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                activity?.let {
                                    viewModel.subscribe(
                                        it,
                                        planOptions[selectedPlanIndex].basePlanId
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(MaterialTheme.corners.large)
                        ) {
                            Text("Get Started", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(MaterialTheme.spacing.s))
                        TextButton(
                            onClick = { viewModel.restorePurchases() },
                            enabled = !isRestoring
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(MaterialTheme.spacing.xs))
                            }
                            Text(
                                text = if (isRestoring) "Restoring..." else "Restore Purchases",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(
                            text = "Cancel anytime. Terms and conditions apply.",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = isPro,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "subscription_state"
        ) { isSubscribed ->
            if (isSubscribed) {
                // ── Subscribed State ──────────────────────────────────────────
                SubscribedContent(
                    paddingValues = paddingValues,
                    onBack = onBack
                )
            } else {
                // ── Unsubscribed State ────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(MaterialTheme.spacing.m),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.m)
                ) {
                    // Header
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MaterialTheme.spacing.m),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(MaterialTheme.spacing.m))
                            Text(
                                text = "Read Deeper with Aira",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(MaterialTheme.spacing.xs))
                            Text(
                                text = "Your AI reading companion, always by your side",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Plan toggle
                    item {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            planOptions.forEachIndexed { index, plan ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = planOptions.size
                                    ),
                                    onClick = { selectedPlanIndex = index },
                                    selected = index == selectedPlanIndex,
                                    label = { Text(plan.label) }
                                )
                            }
                        }
                    }

                    // Price card — pulls real price from Play ProductDetails
                    item {
                        val selectedPlan = planOptions[selectedPlanIndex]
                        val product = subProducts.firstOrNull()
                        val formattedPrice = remember(product, selectedPlan.basePlanId) {
                            product?.subscriptionOfferDetails
                                ?.firstOrNull { it.basePlanId == selectedPlan.basePlanId }
                                ?.pricingPhases
                                ?.pricingPhaseList
                                ?.firstOrNull()
                                ?.formattedPrice
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(MaterialTheme.corners.extraLarge)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MaterialTheme.spacing.l),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = selectedPlan.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                                Text(
                                    // Show real price from Play or a loading placeholder
                                    text = formattedPrice ?: "—",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (selectedPlan.basePlanId == "monthly-base")
                                        "billed monthly" else "billed annually",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (selectedPlan.tag != null) {
                                    Spacer(Modifier.height(MaterialTheme.spacing.s))
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(selectedPlan.tag) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            labelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        border = null
                                    )
                                }
                            }
                        }
                    }

                    // Features header
                    item {
                        Text(
                            text = "What's Included",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs)
                        )
                    }

                    // Feature rows
                    items(features) { feature ->
                        Row(
                            modifier = Modifier.padding(vertical = MaterialTheme.spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(MaterialTheme.spacing.m))
                            Text(feature, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    item { Spacer(Modifier.height(MaterialTheme.spacing.xl)) }
                }
            }
        }
    }
}

// ── Subscribed Content ────────────────────────────────────────────────────────

@Composable
private fun SubscribedContent(
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(MaterialTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(MaterialTheme.spacing.l))
        Text(
            text = "You're a Pro Reader",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(MaterialTheme.spacing.s))
        Text(
            text = "Aira and all Pro features are fully unlocked. Enjoy your reading journey.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MaterialTheme.spacing.xl))
        FilledTonalButton(onClick = onBack) {
            Text("Back to Reading")
        }
        Spacer(Modifier.height(MaterialTheme.spacing.s))
        Text(
            text = "Manage your subscription in Google Play",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}