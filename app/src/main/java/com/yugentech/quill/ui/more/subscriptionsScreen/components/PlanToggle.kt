package com.yugentech.quill.ui.more.subscriptionsScreen.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yugentech.quill.ui.more.subscriptionsScreen.parent.planOptions

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlanToggle(
    selectedPlanIndex: Int,
    onPlanSelected: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        planOptions.forEachIndexed { index, plan ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = planOptions.size),
                onClick = { onPlanSelected(index) },
                selected = index == selectedPlanIndex,
                label = { Text(plan.label) }
            )
        }
    }
}