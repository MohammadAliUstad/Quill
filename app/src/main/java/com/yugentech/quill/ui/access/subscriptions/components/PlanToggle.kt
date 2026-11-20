package com.yugentech.quill.ui.access.subscriptions.components

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yugentech.quill.ui.access.subscriptions.parent.planOptions
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlanToggle(
    selectedPlanIndex: Int,
    onPlanSelected: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        planOptions.forEachIndexed { index, plan ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = planOptions.size),
                onClick = {
                    if (index != selectedPlanIndex) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onPlanSelected(index)
                },
                selected = index == selectedPlanIndex,
                label = { Text(stringResource(plan.label)) }
            )
        }
    }
}