package com.yugentech.quill.ui.access.subscriptions.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.billingclient.api.ProductDetails
import com.yugentech.quill.billing.model.recurringPrice
import com.yugentech.quill.ui.access.subscriptions.parent.PlanOption
import com.yugentech.quill.R
import androidx.compose.ui.res.stringResource

@Composable
fun PriceCard(
    selectedPlan: PlanOption,
    subProducts: List<ProductDetails>
) {
    val product = subProducts.firstOrNull()
    val formattedPrice = remember(product, selectedPlan.basePlanId) {
        // The renewal price, not a trial's or an introductory one
        product?.recurringPrice(selectedPlan.basePlanId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formattedPrice ?: "—",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black
            )

            Text(
                text = if (selectedPlan.basePlanId == "monthly-base")
                    stringResource(R.string.billed_monthly) else stringResource(R.string.billed_annually),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}