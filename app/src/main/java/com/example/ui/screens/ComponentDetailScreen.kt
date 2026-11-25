package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import com.example.model.CompatibilityStatus
import com.example.model.ProductCategory
import com.example.model.ReplacementDifficulty
import com.example.model.ScanComponentDoc
import com.example.model.ScanDoc
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.HairlineDivider
import com.example.ui.components.NoticeCallout
import com.example.ui.components.PartlyTopBar
import com.example.ui.components.TechnicalSpecRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentDetailScreen(
    scan: ScanDoc,
    component: ScanComponentDoc,
    onSaveComponentBookmark: () -> Unit,
    onVerifyMeasurement: (dimensions: String, status: CompatibilityStatus, reason: String) -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenRepairGuide: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val category = ProductCategory.fromRaw(scan.category)
    val template = remember(category, component.categoryKey, component.name) {
        VerifiedCatalogRepository.findTemplate(category, component.categoryKey, component.name)
    }
    val compatStatus = CompatibilityStatus.fromRaw(component.compatibilityStatus)
    val difficulty = ReplacementDifficulty.fromRaw(component.replacementDifficulty)

    var customMeasurement by remember(component.dimensions) {
        mutableStateOf(component.dimensions.takeIf { !it.contains("required", ignoreCase = true) } ?: "")
    }

    Scaffold(
        topBar = {
            PartlyTopBar(
                title = "Component Intelligence",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onSaveComponentBookmark,
                        modifier = Modifier.testTag("component_save_button")
                    ) {
                        Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save component")
                    }
                }
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${scan.productBrand} ${scan.productModel}".trim().uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = component.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        CompatibilityBadge(status = compatStatus)
                    }
                }

                if (component.alternativeNames.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ALSO KNOWN AS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                component.alternativeNames.forEach { alt ->
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = alt,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("What It Does", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = component.primaryFunction,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HairlineDivider()
                            TechnicalSpecRow("Material", component.material.ifBlank { "Verify in person" })
                            TechnicalSpecRow(
                                label = "Dimensions",
                                value = component.dimensions.ifBlank { "Measurement required" },
                                monospaceValue = true
                            )
                            TechnicalSpecRow(
                                label = "Part Number",
                                value = component.partNumber.ifBlank { "Unverified — check reference" },
                                monospaceValue = true
                            )
                            TechnicalSpecRow("Difficulty", difficulty.label)
                            TechnicalSpecRow(
                                label = "Compatibility Basis",
                                value = component.compatibilityReason
                            )
                        }
                    }
                }

                // Measurement Verification Card (prevents blind guesses)
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Straighten,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Measurement Requirements", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = template?.measurementGuidance
                                    ?: "Measure mounting width, outer/inner diameter, or connector pin count with calipers before ordering.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (template != null && template.standardSizes.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    template.standardSizes.forEach { sizeOption ->
                                        SuggestionChip(
                                            onClick = { customMeasurement = sizeOption },
                                            label = {
                                                Text(
                                                    text = sizeOption,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = customMeasurement,
                                onValueChange = { customMeasurement = it },
                                label = { Text("Enter measured dimension (e.g., 20 mm x 1.8 mm)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("measurement_input_field")
                            )

                            OutlinedButton(
                                onClick = {
                                    if (customMeasurement.isNotBlank()) {
                                        onVerifyMeasurement(
                                            customMeasurement.trim(),
                                            CompatibilityStatus.LIKELY_COMPATIBLE,
                                            "User-entered measurement ($customMeasurement) matched against standard ${component.name} sizing."
                                        )
                                    }
                                },
                                enabled = customMeasurement.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("confirm_measurement_button")
                            ) {
                                Text("Save Measured Specification")
                            }
                        }
                    }
                }

                if (compatStatus == CompatibilityStatus.COMPATIBILITY_UNKNOWN) {
                    item {
                        NoticeCallout(
                            title = "Compatibility not yet verified",
                            message = "Partly will never label a replacement compatible solely on visual similarity. Enter your exact physical measurement above or confirm the stamped model number first.",
                            isWarning = true
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onOpenMarketplace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("component_marketplace_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Find Compatible Replacements")
                        }

                        OutlinedButton(
                            onClick = onOpenRepairGuide,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("component_repair_guide_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open Repair & Replacement Guide")
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
