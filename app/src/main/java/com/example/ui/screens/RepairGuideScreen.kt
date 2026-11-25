package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.VerifiedCatalogRepository
import com.example.model.ProductCategory
import com.example.model.RepairGuideInfo
import com.example.model.ScanComponentDoc
import com.example.model.ScanDoc
import com.example.ui.components.HairlineDivider
import com.example.ui.components.PartlyTopBar
import com.example.ui.theme.LikelyAmber
import com.example.ui.theme.LikelyAmberBg

@Composable
fun RepairGuideScreen(
    scan: ScanDoc,
    component: ScanComponentDoc,
    onSaveRepairProject: (RepairGuideInfo) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val category = ProductCategory.fromRaw(scan.category)
    val guide = remember(category, component.categoryKey, component.name) {
        VerifiedCatalogRepository.getRepairGuide(category, component.categoryKey, component.name)
    }

    var checkedTools by remember { mutableStateOf<Set<String>>(emptySet()) }

    Scaffold(
        topBar = {
            PartlyTopBar(
                title = "Repair Guide",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = guide.sourceAttribution.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${guide.componentName} Replacement",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = guide.difficulty.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(
                                    text = guide.estimatedMinutesRange,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Problem Identification
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Problem Identification", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = guide.problemIdentification,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Safety Warnings
                if (guide.safetyWarnings.isNotEmpty()) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = LikelyAmberBg,
                            border = BorderStroke(1.dp, LikelyAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = LikelyAmber)
                                    Text("Safety Warnings", style = MaterialTheme.typography.titleMedium)
                                }
                                guide.safetyWarnings.forEach { warning ->
                                    Text(
                                        text = "• $warning",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Required Tools Checklist
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Required Tools", style = MaterialTheme.typography.titleMedium)
                            }
                            HairlineDivider()
                            guide.requiredTools.forEach { tool ->
                                val checked = tool in checkedTools
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            checkedTools = if (checked) checkedTools - tool else checkedTools + tool
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            checkedTools = if (it) checkedTools + tool else checkedTools - tool
                                        }
                                    )
                                    Text(
                                        text = tool,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // Step-by-Step Instructions
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Replacement Instructions", style = MaterialTheme.typography.titleMedium)
                            HairlineDivider()
                            guide.steps.forEachIndexed { idx, step ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = String.format("%02d", idx + 1),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = { onSaveRepairProject(guide) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_repair_project_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Repair Project to Library")
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
