package com.example.ui.screens

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.VerifiedCatalogRepository
import com.example.model.ProductCategory
import com.example.model.SavedItemDoc
import com.example.model.ScanDoc
import com.example.ui.components.ConfidencePill
import com.example.ui.components.NoticeCallout
import com.example.ui.viewmodel.UiState

private data class CategoryTile(
    val category: ProductCategory,
    val title: String,
    val subtitle: String,
    val imageRes: Int
)

@Composable
fun HomeScreen(
    scansState: UiState<List<ScanDoc>>,
    savedItemsState: UiState<List<SavedItemDoc>>,
    onOpenCamera: (ProductCategory?) -> Unit,
    onGalleryImagePicked: (android.graphics.Bitmap) -> Unit,
    onOpenScan: (String) -> Unit,
    onLoadReferenceAssembly: (ProductCategory) -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            }.onSuccess { bmp ->
                onGalleryImagePicked(bmp)
            }
        }
    }

    val categories = remember {
        listOf(
            CategoryTile(
                category = ProductCategory.WATCHES,
                title = "Watches",
                subtitle = "12-part mechanical & quartz assemblies",
                imageRes = R.drawable.img_cat_watches
            ),
            CategoryTile(
                category = ProductCategory.ELECTRONICS,
                title = "Smartphones & Tech",
                subtitle = "Displays, cells, ports & fasteners",
                imageRes = R.drawable.img_cat_electronics
            ),
            CategoryTile(
                category = ProductCategory.APPLIANCES,
                title = "Appliances",
                subtitle = "Gaskets, shower screens & valves",
                imageRes = R.drawable.img_cat_appliances
            )
        )
    }

    val recentScans = (scansState as? UiState.Success)?.data.orEmpty()
    val savedItems = (savedItemsState as? UiState.Success)?.data.orEmpty()

    val catalogSearchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim()
            (VerifiedCatalogRepository.watchComponentTemplates.map { ProductCategory.WATCHES to it } +
                VerifiedCatalogRepository.electronicsComponentTemplates.map { ProductCategory.ELECTRONICS to it } +
                VerifiedCatalogRepository.applianceComponentTemplates.map { ProductCategory.APPLIANCES to it })
                .filter { (_, tpl) ->
                    tpl.name.contains(q, ignoreCase = true) ||
                        tpl.alternativeNames.any { it.contains(q, ignoreCase = true) } ||
                        tpl.primaryFunction.contains(q, ignoreCase = true)
                }
                .take(6)
        }
    }

    val filteredScans = remember(searchQuery, recentScans) {
        if (searchQuery.isBlank()) recentScans
        else recentScans.filter {
            it.productBrand.contains(searchQuery, ignoreCase = true) ||
                it.productModel.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Editorial Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PARTLY • VISUAL COMPONENT INTELLIGENCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.tagline_headline),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.tagline_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_field"),
                    placeholder = {
                        Text(
                            "Search scans, spring bars, gaskets, displays...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Instant Catalog Search Matches (when typing)
            if (catalogSearchResults.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "VERIFIED COMPONENT CATALOG MATCHES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        catalogSearchResults.forEach { (cat, tpl) ->
                            Surface(
                                onClick = { onLoadReferenceAssembly(cat) },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tpl.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${cat.displayName} • ${tpl.standardSizes.firstOrNull().orEmpty()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Open reference",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Primary Camera Hero Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Visual Component Scan",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Multi-angle capture with AI recognition & exploded view",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Watch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(24.dp)
                                )
                            }
                        }

                        Button(
                            onClick = { onOpenCamera(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("home_identify_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.action_identify_product),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("home_gallery_button"),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_upload_gallery),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Popular Categories + Reference Assembly Launcher
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Popular Categories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = { onLoadReferenceAssembly(ProductCategory.WATCHES) },
                            modifier = Modifier.testTag("explore_watch_demo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Watch Reference")
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(categories, key = { it.category.name }) { cat ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .width(232.dp)
                                    .clickable { onOpenCamera(cat.category) }
                                    .testTag("category_card_${cat.category.name.lowercase()}")
                            ) {
                                Column {
                                    Image(
                                        painter = painterResource(id = cat.imageRes),
                                        contentDescription = cat.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(124.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                    )
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = cat.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = cat.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Scan",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Preview Reference",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .clickable { onLoadReferenceAssembly(cat.category) }
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Recent Scans
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Scans",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (recentScans.isNotEmpty()) {
                            TextButton(onClick = onOpenLibrary) {
                                Text("View all (${recentScans.size})")
                            }
                        }
                    }

                    when (scansState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                        is UiState.Error -> {
                            NoticeCallout(
                                title = "Could not load scans",
                                message = scansState.message,
                                isWarning = true
                            )
                        }
                        is UiState.Success -> {
                            if (filteredScans.isEmpty()) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (searchQuery.isBlank()) "No scans yet" else "No matching scans",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Photograph a product or preview the verified Seiko SKX007 watch reference assembly to explore every component.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    filteredScans.take(5).forEach { scan ->
                                        Surface(
                                            onClick = { onOpenScan(scan.id) },
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("recent_scan_card_${scan.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                ConfidencePill(score = scan.confidenceScore)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Saved Products / Bookmarks Preview
            if (savedItems.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saved Library",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = onOpenLibrary) {
                                Text("Open (${savedItems.size})")
                            }
                        }
                        savedItems.take(3).forEach { item ->
                            Surface(
                                onClick = onOpenLibrary,
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
