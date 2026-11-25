package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CompatibilityStatus
import com.example.model.SavedItemDoc
import com.example.model.SavedItemType
import com.example.model.ScanDoc
import com.example.ui.components.CompatibilityBadge
import com.example.ui.components.ConfidencePill
import com.example.ui.components.NoticeCallout
import com.example.ui.viewmodel.UiState

private enum class LibraryFilter(val label: String) {
    ALL("All"),
    SCANS("Scans"),
    PRODUCTS("Products"),
    COMPONENTS("Components"),
    REPAIRS("Repairs")
}

@Composable
fun SavedLibraryScreen(
    scansState: UiState<List<ScanDoc>>,
    savedItemsState: UiState<List<SavedItemDoc>>,
    onOpenScan: (String) -> Unit,
    onDeleteScan: (String) -> Unit,
    onRemoveSavedItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(LibraryFilter.ALL) }

    val scans = (scansState as? UiState.Success)?.data.orEmpty()
    val savedItems = (savedItemsState as? UiState.Success)?.data.orEmpty()

    val filteredBookmarks = remember(activeFilter, savedItems) {
        when (activeFilter) {
            LibraryFilter.ALL -> savedItems
            LibraryFilter.SCANS -> emptyList()
            LibraryFilter.PRODUCTS -> savedItems.filter { SavedItemType.fromRaw(it.itemType) == SavedItemType.PRODUCT }
            LibraryFilter.COMPONENTS -> savedItems.filter {
                val t = SavedItemType.fromRaw(it.itemType)
                t == SavedItemType.COMPONENT || t == SavedItemType.REPLACEMENT_PART
            }
            LibraryFilter.REPAIRS -> savedItems.filter { SavedItemType.fromRaw(it.itemType) == SavedItemType.REPAIR_PROJECT }
        }
    }

    val showScans = activeFilter == LibraryFilter.ALL || activeFilter == LibraryFilter.SCANS

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SAVED LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Your Scans & Saved Parts",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LibraryFilter.entries) { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("library_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (scansState is UiState.Loading && savedItemsState is UiState.Loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                if (scansState is UiState.Error) {
                    item {
                        NoticeCallout("Scans sync error", scansState.message, isWarning = true)
                    }
                }
                if (savedItemsState is UiState.Error) {
                    item {
                        NoticeCallout("Saved items sync error", savedItemsState.message, isWarning = true)
                    }
                }

                if (filteredBookmarks.isNotEmpty()) {
                    item {
                        Text(
                            text = "BOOKMARKED ITEMS (${filteredBookmarks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(filteredBookmarks, key = { it.id }) { item ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = SavedItemType.fromRaw(item.itemType).label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.subtitle.isNotBlank()) {
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    CompatibilityBadge(status = CompatibilityStatus.fromRaw(item.compatibilityStatus))
                                }
                                IconButton(
                                    onClick = { onRemoveSavedItem(item.id) },
                                    modifier = Modifier.testTag("delete_saved_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Remove saved item",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (showScans && scans.isNotEmpty()) {
                    item {
                        Text(
                            text = "SCAN HISTORY (${scans.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    items(scans, key = { it.id }) { scan ->
                        Surface(
                            onClick = { onOpenScan(scan.id) },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = "${scan.productBrand} ${scan.productModel}".trim(),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${scan.category} • ${scan.componentCount} components",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    ConfidencePill(score = scan.confidenceScore)
                                }
                                IconButton(
                                    onClick = { onDeleteScan(scan.id) },
                                    modifier = Modifier.testTag("delete_scan_${scan.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete scan",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredBookmarks.isEmpty() && (!showScans || scans.isEmpty())) {
                    item {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text("Nothing saved here yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Identify a product with the camera or bookmark components, replacement portals, and repair guides as you explore.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
