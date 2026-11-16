package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.WaveCard
import com.example.ui.components.WaveEmptyState
import com.example.ui.components.WaveHeroCard
import com.example.ui.components.WaveSectionLabel
import com.example.ui.theme.WaveTheme
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.FocusIndigo
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.CustomPageItem
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.RoutineViewModel

@Composable
fun CustomPageScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val activePage by viewModel.activeCustomPage.collectAsStateWithLifecycle()
    val page = activePage ?: CustomPageItem(
        id = "default",
        title = "Custom Page",
        description = "Personal workspace for daily goals and routines"
    )

    var newGoalText by remember { mutableStateOf("") }
    val checkedItems = remember { mutableStateListOf<Int>() }

    val totalGoals = page.items.size
    val completedGoals = checkedItems.size
    val progressPct = if (totalGoals > 0) completedGoals.toFloat() / totalGoals.toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("custom_page_screen_${page.id}"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Page Hero Header
        item {
            WaveHeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_page_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Page Icon",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = page.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = page.description.ifBlank { "Personal Goals & Milestones" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WaveTheme.colors.onSurfaceMuted,
                                    maxLines = 2
                                )
                            }
                        }

                        // Close button to close the panel and return to Routine
                        IconButton(
                            onClick = { viewModel.setTab(NavTab.ROUTINE) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("close_milestones_panel_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Goals Progress ($completedGoals/$totalGoals completed)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(progressPct * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progressPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Add Goal Section
        item {
            WaveCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_custom_goal_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Add Workspace Goal / Milestone",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newGoalText,
                            onValueChange = { newGoalText = it },
                            placeholder = { Text("e.g. Finish quarterly project review", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_goal_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (newGoalText.isNotBlank()) {
                                    viewModel.addCustomPageGoal(page.id, newGoalText)
                                    newGoalText = ""
                                }
                            },
                            enabled = newGoalText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("submit_custom_goal_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }
                }
            }
        }

        // Checklist of Goals for this page
        item {
            WaveSectionLabel(text = "Milestones & Checklist")
        }

        if (page.items.isEmpty()) {
            item {
                WaveEmptyState(
                    emoji = "📌",
                    text = "No goals added yet. Use the input above to add targets and daily milestones."
                )
            }
        } else {
            itemsIndexed(page.items) { index, itemText ->
                val isChecked = checkedItems.contains(index)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isChecked) SuccessGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isChecked) SuccessGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline
                    ),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isChecked) checkedItems.remove(index) else checkedItems.add(index)
                        }
                        .testTag("custom_goal_item_$index")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isChecked) "Completed" else "Incomplete",
                            tint = if (isChecked) SuccessGreen else WaveTheme.colors.onSurfaceMuted,
                            modifier = Modifier.size(22.dp)
                        )

                        Text(
                            text = itemText,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.5.sp),
                            fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                            color = if (isChecked) WaveTheme.colors.onSurfaceMuted else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { viewModel.setTab(NavTab.ROUTINE) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("close_milestones_bottom_button")
            ) {
                Text("Close Milestones", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
