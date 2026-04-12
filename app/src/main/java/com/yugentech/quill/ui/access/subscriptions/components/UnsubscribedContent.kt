package com.yugentech.quill.ui.access.subscriptions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.yugentech.quill.ui.access.subscriptions.parent.planOptions

@Composable
fun UnsubscribedContent(
    paddingValues: PaddingValues,
    selectedPlanIndex: Int,
    subProducts: List<ProductDetails>,
    onPlanSelected: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. The Teaser Hero
        item { AiraHero() }

        // 2. The Core Comparison (Your existing cards)
        item { ProFeaturesSection() }

        // 3. The Value Add (New Feature List)
        item { FeatureHighlights() }

        // 4. The Selection Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select a Plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                PlanToggle(selectedPlanIndex = selectedPlanIndex, onPlanSelected = onPlanSelected)
                PriceCard(selectedPlan = planOptions[selectedPlanIndex], subProducts = subProducts)
            }
        }
    }
}