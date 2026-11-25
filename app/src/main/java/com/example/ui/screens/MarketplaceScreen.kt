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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.VerifiedCatalogRepository
import com.example.model.CompatibilityStatus
import com.example.model.MarketplaceSearchChannel
import com.example.model.ProductCategory
import com.example.model.ScanComponentDoc
import com.example.model.ScanDoc
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.HairlineDivider
import com.example.ui.components.NoticeCallout
import com.example.ui.components.PartlyTopBar
import com.example.ui.components.TechnicalSpecRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarketplaceScreen(
    scan: ScanDoc,
    component: ScanComponentDoc,
    onSavePartChannel: (MarketplaceSearchChannel) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val uriHandler = LocalUriHandler.current
    val category = ProductCategory.fromRaw(scan.category)
    val allChannels = remember(scan, component) {
        VerifiedCatalogRepository.buildSearchChannels(
            brand = scan.productBrand,
            model = scan.productModel,
            category = category,
            component = component
        )
    }

    var filterOemOnly by remember { mutableStateOf(false) }
    var filterVerifiedOnly by remember { mutableStateOf(false) }
    var selectedCompareIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDemoPartnerFormat by remember { mutableStateOf(false) }

    val filteredChannels = remember(allChannels, filterOemOnly, filterVerifiedOnly) {
        allChannels.filter { channel ->
            val oemOk = !filterOemOnly || channel.originalOrAftermarket.contains("OEM", ignoreCase = true) ||
                channel.originalOrAftermarket.contains("Original", ignoreCase = true)
            val verOk = !filterVerifiedOnly || channel.compatibilityStatus == CompatibilityStatus.VERIFIED_COMPATIBLE
            oemOk && verOk
        }
    }

    val comparedChannels = allChannels.filter { it.id in selectedCompareIds }
    val sampleDemoListings = remember(component) {
        VerifiedCatalogRepository.getSampleDemoListings(component)
    }

    Scaffold(
        topBar = {
            PartlyTopBar(
                title = "Replacement Marketplace",
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
                    .widthIn(max = 720.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = component.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${scan.productBrand} ${scan.productModel} • ${component.dimensions.ifBlank { "Measure before ordering" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    NoticeCallout(
                        title = "Verified Supplier Search Portals",
                        message = "Partly never invents live prices, stock counts, or sellers. Use the verified search links below to check live availability, or toggle the labeled sample preview to inspect partner API comparison formatting."
                    )
                }

                // Filters
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = filterOemOnly,
                            onClick = { filterOemOnly = !filterOemOnly },
                            label = { Text("Original / OEM Spec") },
                            modifier = Modifier.testTag("filter_oem_chip")
                        )
                        FilterChip(
                            selected = filterVerifiedOnly,
                            onClick = { filterVerifiedOnly = !filterVerifiedOnly },
                            label = { Text("Verified Compatible Only") },
                            modifier = Modifier.testTag("filter_verified_chip")
                        )
                    }
                }

                // Side-by-side comparison card when 2+ channels selected
                if (comparedChannels.size >= 2) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("marketplace_comparison_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Outlined.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Side-by-Side Comparison (${comparedChannels.size})", style = MaterialTheme.typography.titleMedium)
                                }
                                HairlineDivider()
                                comparedChannels.forEach { ch ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(ch.partnerName, style = MaterialTheme.typography.titleSmall)
                                        TechnicalSpecRow("Type", ch.originalOrAftermarket)
                                        TechnicalSpecRow("Material", ch.materialSpec)
                                        TechnicalSpecRow("Dimensions", ch.dimensionSpec, monospaceValue = true)
                                        HairlineDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                // Authorized Search Channel Cards
                if (filteredChannels.isEmpty()) {
                    item {
                        NoticeCallout(
                            title = "No channels match current filters",
                            message = "Clear a filter chip to view all authorized supplier search portals for ${component.name}.",
                            isWarning = true
                        )
                    }
                } else {
                    items(filteredChannels, key = { it.id }) { channel ->
                        val isSelectedForCompare = channel.id in selectedCompareIds
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("marketplace_channel_${channel.id}")
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = channel.channelType.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = channel.partnerName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = isSelectedForCompare,
                                            onCheckedChange = { checked ->
                                                selectedCompareIds = if (checked && selectedCompareIds.size < 3) {
                                                    selectedCompareIds + channel.id
                                                } else {
                                                    selectedCompareIds - channel.id
                                                }
                                            }
                                        )
                                        Text("Compare", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                CompatibilityBadge(status = channel.compatibilityStatus)

                                TechnicalSpecRow("Grade", channel.originalOrAftermarket)
                                TechnicalSpecRow("Search Query", channel.searchQuery, monospaceValue = true)
                                Text(
                                    text = channel.specNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { uriHandler.openUri(channel.searchUrl) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("open_supplier_${channel.id}"),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("Search Live Catalog")
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                    OutlinedButton(
                                        onClick = { onSavePartChannel(channel) },
                                        modifier = Modifier.height(48.dp),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Icon(Icons.Outlined.BookmarkAdd, contentDescription = "Save part reference", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Clearly labeled Demonstration Format Preview toggle
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Preview Sample Partner API Format", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Demonstration data only — shows how partner pricing & stock render once API keys are configured.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = showDemoPartnerFormat,
                                onCheckedChange = { showDemoPartnerFormat = it },
                                modifier = Modifier.testTag("toggle_demo_marketplace")
                            )
                        }
                    }
                }

                if (showDemoPartnerFormat) {
                    items(sampleDemoListings, key = { it.id }) { demo ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "DEMO SAMPLE • NOT LIVE INVENTORY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$${"%.2f".format(demo.priceUsd)}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                Text(demo.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${demo.sellerName} (${demo.sellerRating}★) • ${demo.deliveryEstimate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${if (demo.isOriginalOem) "OEM" else "Aftermarket"} • ${if (demo.isRefurbished) "Refurbished" else "New"} • ${demo.material}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
