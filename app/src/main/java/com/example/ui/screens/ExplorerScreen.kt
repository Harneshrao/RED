package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CompatibilityStatus
import com.example.model.ProductCategory
import com.example.model.SavedItemType
import com.example.model.ScanComponentDoc
import com.example.model.ScanDoc
import com.example.model.VerificationProvenance
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.ConfidencePill
import com.example.ui.components.ExplodedViewCanvas
import com.example.ui.components.HairlineDivider
import com.example.ui.components.NoticeCallout
import com.example.ui.components.PartlyTopBar
import com.example.ui.components.ProvenanceTag
import com.example.ui.components.TechnicalSpecRow
import com.example.ui.viewmodel.UiState

@Composable
fun ExplorerScreen(
    scanState: UiState<ScanDoc?>,
    componentsState: UiState<List<ScanComponentDoc>>,
    thumbnailBitmap: Bitmap?,
    onToggleComponentVisibility: (scanId: String, componentId: String, hidden: Boolean) -> Unit,
    onOpenComponentDetails: (componentId: String) -> Unit,
    onOpenMarketplace: (componentId: String) -> Unit,
    onOpenRepairGuide: (componentId: String) -> Unit,
    onSaveProductBookmark: (ScanDoc) -> Unit,
    onCaptureAnotherAngle: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Visual, 1 = List, 2 = Technical
    var selectedComponentId by remember { mutableStateOf<String?>(null) }

    val scan = (scanState as? UiState.Success)?.data
    val components = (componentsState as? UiState.Success)?.data.orEmpty()

    LaunchedEffect(components) {
        if (selectedComponentId == null && components.isNotEmpty()) {
            selectedComponentId = components.first().id
        }
    }

    val selectedComponent = components.firstOrNull { it.id == selectedComponentId } ?: components.firstOrNull()

    Scaffold(
        topBar = {
            PartlyTopBar(
                title = scan?.let { "${it.productBrand} ${it.productModel}".trim() } ?: "Component Explorer",
                onBack = onBack,
                actions = {
                    if (scan != null) {
                        IconButton(
                            onClick = { onSaveProductBookmark(scan) },
                            modifier = Modifier.testTag("explorer_save_product_button")
                        ) {
                            Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save product")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            scanState is UiState.Loading || componentsState is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            scanState is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NoticeCallout(
                        title = "Unable to load product",
                        message = scanState.message,
                        isWarning = true
                    )
                }
            }
            scan == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NoticeCallout(
                        title = "No active scan selected",
                        message = "Select a recent scan from Home or capture a new product photograph."
                    )
                }
            }
            else -> {
                val category = ProductCategory.fromRaw(scan.category)
                val provenance = VerificationProvenance.fromRaw(scan.dataVerificationStatus)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val isWide = maxWidth >= 760.dp

                    if (isWide) {
                        // Tablet / Foldable Canonical List-Detail + Interactive Pane
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProductOverviewHeaderCard(
                                    scan = scan,
                                    thumbnailBitmap = thumbnailBitmap,
                                    onCaptureAnotherAngle = onCaptureAnotherAngle
                                )
                                ExplodedViewCanvas(
                                    category = category,
                                    provenance = provenance,
                                    components = components,
                                    selectedComponentId = selectedComponent?.id,
                                    onSelectComponent = { selectedComponentId = it.id },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (selectedComponent != null) {
                                    item {
                                        SelectedComponentPanel(
                                            component = selectedComponent,
                                            onOpenDetails = { onOpenComponentDetails(selectedComponent.id) },
                                            onOpenMarketplace = { onOpenMarketplace(selectedComponent.id) },
                                            onOpenRepairGuide = { onOpenRepairGuide(selectedComponent.id) }
                                        )
                                    }
                                }
                                item {
                                    Text(
                                        text = "ALL COMPONENTS (${components.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                items(components, key = { it.id }) { comp ->
                                    ComponentListCard(
                                        component = comp,
                                        selected = comp.id == selectedComponent?.id,
                                        onSelect = { selectedComponentId = comp.id },
                                        onOpenDetails = { onOpenComponentDetails(comp.id) },
                                        onToggleHidden = {
                                            onToggleComponentVisibility(scan.id, comp.id, !comp.hiddenInViewer)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Handheld Mobile Layout
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                ProductOverviewHeaderCard(
                                    scan = scan,
                                    thumbnailBitmap = thumbnailBitmap,
                                    onCaptureAnotherAngle = onCaptureAnotherAngle
                                )
                            }

                            item {
                                val tabLabels = listOf("Visual View", "Components (${components.size})", "Technical")
                                SingleChoiceSegmentedButtonRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("explorer_view_tabs")
                                ) {
                                    tabLabels.forEachIndexed { idx, label ->
                                        SegmentedButton(
                                            selected = selectedTab == idx,
                                            onClick = { selectedTab = idx },
                                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabLabels.size)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            when (selectedTab) {
                                0 -> {
                                    item {
                                        ExplodedViewCanvas(
                                            category = category,
                                            provenance = provenance,
                                            components = components,
                                            selectedComponentId = selectedComponent?.id,
                                            onSelectComponent = { selectedComponentId = it.id },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(410.dp)
                                        )
                                    }
                                    if (selectedComponent != null) {
                                        item {
                                            SelectedComponentPanel(
                                                component = selectedComponent,
                                                onOpenDetails = { onOpenComponentDetails(selectedComponent.id) },
                                                onOpenMarketplace = { onOpenMarketplace(selectedComponent.id) },
                                                onOpenRepairGuide = { onOpenRepairGuide(selectedComponent.id) }
                                            )
                                        }
                                    }
                                }

                                1 -> {
                                    items(components, key = { it.id }) { comp ->
                                        ComponentListCard(
                                            component = comp,
                                            selected = comp.id == selectedComponent?.id,
                                            onSelect = { selectedComponentId = comp.id },
                                            onOpenDetails = { onOpenComponentDetails(comp.id) },
                                            onToggleHidden = {
                                                onToggleComponentVisibility(scan.id, comp.id, !comp.hiddenInViewer)
                                            }
                                        )
                                    }
                                }

                                else -> {
                                    item {
                                        TechnicalOverviewPanel(scan = scan, provenance = provenance)
                                    }
                                }
                            }

                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductOverviewHeaderCard(
    scan: ScanDoc,
    thumbnailBitmap: Bitmap?,
    onCaptureAnotherAngle: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap.asImageBitmap(),
                        contentDescription = "Captured product photograph",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = scan.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${scan.productBrand} ${scan.productModel}".trim(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ConfidencePill(score = scan.confidenceScore)
                        Text(
                            text = "${scan.componentCount} parts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (scan.needsAdditionalAngle) {
                NoticeCallout(
                    title = "Additional angle improves verification",
                    message = scan.suggestedNextAngle.ifBlank { "Capture rear markings or side profile to verify model specifications." },
                    isWarning = true
                )
                OutlinedButton(
                    onClick = onCaptureAnotherAngle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Another Photo Angle")
                }
            }
        }
    }
}

@Composable
private fun SelectedComponentPanel(
    component: ScanComponentDoc,
    onOpenDetails: () -> Unit,
    onOpenMarketplace: () -> Unit,
    onOpenRepairGuide: () -> Unit
) {
    val compatStatus = CompatibilityStatus.fromRaw(component.compatibilityStatus)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_component_panel")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SELECTED COMPONENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                CompatibilityBadge(status = compatStatus)
            }

            Text(
                text = component.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = component.primaryFunction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HairlineDivider()

            TechnicalSpecRow(label = "Material", value = component.material.ifBlank { "Verify in person" })
            TechnicalSpecRow(
                label = "Dimensions",
                value = component.dimensions.ifBlank { "Measurement required" },
                monospaceValue = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenMarketplace,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("selected_find_parts_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Replacements")
                }
                OutlinedButton(
                    onClick = onOpenDetails,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("selected_details_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Full Specs")
                }
            }

            OutlinedButton(
                onClick = onOpenRepairGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("selected_repair_guide_button"),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Step-by-Step Repair Guide")
            }
        }
    }
}

@Composable
private fun ComponentListCard(
    component: ScanComponentDoc,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenDetails: () -> Unit,
    onToggleHidden: () -> Unit
) {
    val compat = CompatibilityStatus.fromRaw(component.compatibilityStatus)
    Surface(
        onClick = {
            onSelect()
            onOpenDetails()
        },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("component_list_item_${component.categoryKey}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onToggleHidden,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (component.hiddenInViewer) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (component.hiddenInViewer) "Show in exploded view" else "Hide in exploded view",
                    tint = if (component.hiddenInViewer) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${if (component.visibleInPhoto) "Visible in photo" else "Internal sub-assembly"} • ${component.dimensions}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                CompatibilityBadge(status = compat)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Inspect component",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TechnicalOverviewPanel(
    scan: ScanDoc,
    provenance: VerificationProvenance
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Technical Identification",
                    style = MaterialTheme.typography.titleMedium
                )
                ProvenanceTag(provenance = provenance)
            }
            HairlineDivider()
            TechnicalSpecRow("Brand", scan.productBrand)
            TechnicalSpecRow("Model", scan.productModel)
            TechnicalSpecRow("Category", scan.category)
            TechnicalSpecRow("Generation / Era", scan.generationOrEra.ifBlank { "Unknown" })
            TechnicalSpecRow("Architecture", scan.movementOrPowerType.ifBlank { "Unknown" })
            TechnicalSpecRow("Observed Damage", scan.damageNotes.ifBlank { "None observed" })
            TechnicalSpecRow("Missing Parts", scan.missingComponentNotes.ifBlank { "None observed" })
        }
    }
}
