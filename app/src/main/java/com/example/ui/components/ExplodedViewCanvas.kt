package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProductCategory
import com.example.model.ScanComponentDoc
import com.example.model.VerificationProvenance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private data class ProjectedLayer(
    val component: ScanComponentDoc,
    val center: Offset,
    val hitRadius: Float
)

@Composable
fun ExplodedViewCanvas(
    category: ProductCategory,
    provenance: VerificationProvenance,
    components: List<ScanComponentDoc>,
    selectedComponentId: String?,
    onSelectComponent: (ScanComponentDoc) -> Unit,
    modifier: Modifier = Modifier
) {
    var explodeTarget by remember { mutableFloatStateOf(0.82f) }
    val explodeFactor by animateFloatAsState(
        targetValue = explodeTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "explode_spring"
    )

    var yawDeg by remember { mutableFloatStateOf(24f) }
    var pitchTilt by remember { mutableFloatStateOf(0.52f) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val visibleComponents = remember(components) {
        components.filter { !it.hiddenInViewer }.sortedBy { it.layerOrder }
    }

    val textMeasurer = rememberTextMeasurer()
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)

    var projectedLayers by remember { mutableStateOf<List<ProjectedLayer>>(emptyList()) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = surfaceColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                surfaceColor,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (zoom != 1f) {
                                zoomScale = (zoomScale * zoom).coerceIn(0.65f, 2.2f)
                            } else {
                                // Drag tilts/rotates isometric view, two-finger or edge pans
                                yawDeg = (yawDeg + pan.x * 0.28f).coerceIn(-65f, 65f)
                                pitchTilt = (pitchTilt + pan.y * 0.0018f).coerceIn(0.28f, 0.85f)
                            }
                            if (zoomScale > 1.05f) {
                                panOffset = Offset(
                                    (panOffset.x + pan.x * 0.4f).coerceIn(-220f, 220f),
                                    (panOffset.y + pan.y * 0.4f).coerceIn(-220f, 220f)
                                )
                            }
                        }
                    }
                    .pointerInput(visibleComponents) {
                        detectTapGestures { tapOffset ->
                            val hit = projectedLayers
                                .asReversed()
                                .minByOrNull { layer ->
                                    hypot(tapOffset.x - layer.center.x, tapOffset.y - layer.center.y)
                                }
                            if (hit != null && hypot(tapOffset.x - hit.center.x, tapOffset.y - hit.center.y) <= hit.hitRadius) {
                                onSelectComponent(hit.component)
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawTechnicalGrid(gridLineColor)

                    if (visibleComponents.isEmpty()) return@Canvas

                    val count = visibleComponents.size
                    val centerX = size.width * 0.5f + panOffset.x
                    val centerY = size.height * 0.5f + panOffset.y
                    val yawRad = yawDeg * (PI / 180.0).toFloat()
                    val horizontalShiftPerLayer = sin(yawRad) * 18f * explodeFactor * zoomScale
                    val maxSpan = size.height * 0.72f
                    val layerStepY = if (count > 1) {
                        (maxSpan / (count - 1).coerceAtLeast(1)) * explodeFactor
                    } else 0f

                    val baseRadius = (minOf(size.width, size.height) * 0.19f) * zoomScale

                    // Central vertical alignment axis hairline
                    drawLine(
                        color = accentColor.copy(alpha = 0.25f),
                        start = Offset(
                            centerX - horizontalShiftPerLayer * (count - 1) * 0.5f,
                            centerY - layerStepY * (count - 1) * 0.5f - baseRadius * 0.5f
                        ),
                        end = Offset(
                            centerX + horizontalShiftPerLayer * (count - 1) * 0.5f,
                            centerY + layerStepY * (count - 1) * 0.5f + baseRadius * 0.5f
                        ),
                        strokeWidth = 1.5f
                    )

                    val newProjected = mutableListOf<ProjectedLayer>()

                    // Draw bottom-to-top so upper crystal/bezel overlaps lower caseback cleanly
                    visibleComponents.reversed().forEach { comp ->
                        val idx = visibleComponents.indexOf(comp)
                        val offsetIndex = idx - (count - 1) / 2f
                        val layerCenter = Offset(
                            x = centerX + offsetIndex * horizontalShiftPerLayer,
                            y = centerY + offsetIndex * layerStepY
                        )
                        val isSelected = comp.id == selectedComponentId ||
                            (selectedComponentId == null && idx == 0)

                        drawAssemblyPart(
                            category = category,
                            component = comp,
                            center = layerCenter,
                            baseRadius = baseRadius,
                            pitchTilt = pitchTilt,
                            yawDeg = yawDeg,
                            isSelected = isSelected,
                            accentColor = accentColor,
                            onSurface = onSurface,
                            mutedColor = mutedColor,
                            surfaceColor = surfaceColor
                        )

                        if (isSelected) {
                            drawCalloutLabel(
                                componentName = comp.name,
                                center = layerCenter,
                                radius = baseRadius,
                                canvasWidth = size.width,
                                accentColor = accentColor,
                                surfaceColor = surfaceColor,
                                textMeasurer = textMeasurer,
                                textStyle = labelStyle
                            )
                        }

                        newProjected.add(
                            ProjectedLayer(
                                component = comp,
                                center = layerCenter,
                                hitRadius = baseRadius * 1.15f
                            )
                        )
                    }
                    projectedLayers = newProjected.reversed()
                }

                // Top overlay controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProvenanceTag(provenance = provenance)

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = surfaceColor.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    yawDeg = 24f
                                    pitchTilt = 0.52f
                                    zoomScale = 1.0f
                                    panOffset = Offset.Zero
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("reset_view_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CenterFocusStrong,
                                    contentDescription = "Reset 3D camera",
                                    modifier = Modifier.size(18.dp),
                                    tint = onSurface
                                )
                            }
                        }
                    }
                }

                // Bottom-left gesture hint
                Text(
                    text = "Drag to tilt/rotate • Pinch to zoom • Tap part",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // Explode / Assemble precision slider bar
            HairlineDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Layers,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (explodeTarget < 0.15f) "ASSEMBLED" else "EXPLODED ${(explodeTarget * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurface
                )
                Slider(
                    value = explodeTarget,
                    onValueChange = { explodeTarget = it },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("explode_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor
                    )
                )
                Surface(
                    onClick = { explodeTarget = if (explodeTarget > 0.4f) 0f else 0.85f },
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.testTag("toggle_explode_button")
                ) {
                    Text(
                        text = if (explodeTarget > 0.4f) "Assemble" else "Explode",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawTechnicalGrid(lineColor: Color) {
    val step = 40.dp.toPx()
    var x = step
    while (x < size.width) {
        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.8f)
        x += step
    }
    var y = step
    while (y < size.height) {
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
        y += step
    }
}

private fun DrawScope.drawAssemblyPart(
    category: ProductCategory,
    component: ScanComponentDoc,
    center: Offset,
    baseRadius: Float,
    pitchTilt: Float,
    yawDeg: Float,
    isSelected: Boolean,
    accentColor: Color,
    onSurface: Color,
    mutedColor: Color,
    surfaceColor: Color
) {
    val rx = baseRadius
    val ry = baseRadius * pitchTilt
    val strokeColor = if (isSelected) accentColor else onSurface.copy(alpha = 0.78f)
    val strokeW = if (isSelected) 3.2f else 1.8f
    val fillHighlight = if (isSelected) accentColor.copy(alpha = 0.16f) else surfaceColor.copy(alpha = 0.88f)

    if (isSelected) {
        drawOval(
            color = accentColor.copy(alpha = 0.12f),
            topLeft = Offset(center.x - rx * 1.18f, center.y - ry * 1.18f),
            size = Size(rx * 2.36f, ry * 2.36f)
        )
    }

    val key = component.categoryKey.lowercase()
    when {
        key == "watch_crystal" -> {
            drawOval(
                color = Color(0xFF93C5FD).copy(alpha = if (isSelected) 0.35f else 0.22f),
                topLeft = Offset(center.x - rx * 0.92f, center.y - ry * 0.92f),
                size = Size(rx * 1.84f, ry * 1.84f)
            )
            drawOval(
                color = strokeColor,
                topLeft = Offset(center.x - rx * 0.92f, center.y - ry * 0.92f),
                size = Size(rx * 1.84f, ry * 1.84f),
                style = Stroke(strokeW)
            )
            // Specular highlight
            drawLine(
                color = Color.White.copy(alpha = 0.75f),
                start = Offset(center.x - rx * 0.45f, center.y - ry * 0.35f),
                end = Offset(center.x + rx * 0.15f, center.y + ry * 0.25f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }
        key == "bezel" -> {
            drawOval(
                color = fillHighlight,
                topLeft = Offset(center.x - rx * 1.05f, center.y - ry * 1.05f),
                size = Size(rx * 2.1f, ry * 2.1f)
            )
            drawOval(
                color = strokeColor,
                topLeft = Offset(center.x - rx * 1.05f, center.y - ry * 1.05f),
                size = Size(rx * 2.1f, ry * 2.1f),
                style = Stroke(strokeW)
            )
            drawOval(
                color = strokeColor,
                topLeft = Offset(center.x - rx * 0.78f, center.y - ry * 0.78f),
                size = Size(rx * 1.56f, ry * 1.56f),
                style = Stroke(1.6f)
            )
            for (deg in 0 until 360 step 30) {
                val rad = (deg + yawDeg * 0.4f) * (PI / 180.0).toFloat()
                drawLine(
                    color = strokeColor,
                    start = Offset(center.x + cos(rad) * rx * 0.80f, center.y + sin(rad) * ry * 0.80f),
                    end = Offset(center.x + cos(rad) * rx * 1.02f, center.y + sin(rad) * ry * 1.02f),
                    strokeWidth = 1.6f
                )
            }
        }
        key == "dial" -> {
            drawOval(
                color = if (isSelected) accentColor.copy(alpha = 0.2f) else onSurface.copy(alpha = 0.14f),
                topLeft = Offset(center.x - rx * 0.86f, center.y - ry * 0.86f),
                size = Size(rx * 1.72f, ry * 1.72f)
            )
            drawOval(
                color = strokeColor,
                topLeft = Offset(center.x - rx * 0.86f, center.y - ry * 0.86f),
                size = Size(rx * 1.72f, ry * 1.72f),
                style = Stroke(strokeW)
            )
            // Hour indices & date aperture
            for (i in 0 until 12) {
                val rad = (i * 30f + yawDeg * 0.3f) * (PI / 180.0).toFloat()
                drawCircle(
                    color = strokeColor,
                    radius = if (i % 3 == 0) 3.2f else 2f,
                    center = Offset(center.x + cos(rad) * rx * 0.68f, center.y + sin(rad) * ry * 0.68f)
                )
            }
            drawRect(
                color = strokeColor,
                topLeft = Offset(center.x + rx * 0.36f, center.y - ry * 0.12f),
                size = Size(rx * 0.22f, ry * 0.24f),
                style = Stroke(1.5f)
            )
        }
        key == "hands" -> {
            drawCircle(color = strokeColor, radius = 5f, center = center)
            drawLine(
                color = strokeColor,
                start = center,
                end = Offset(center.x - rx * 0.42f, center.y - ry * 0.36f),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = strokeColor,
                start = center,
                end = Offset(center.x + rx * 0.58f, center.y - ry * 0.42f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = accentColor,
                start = Offset(center.x - rx * 0.16f, center.y + ry * 0.14f),
                end = Offset(center.x + rx * 0.65f, center.y + ry * 0.32f),
                strokeWidth = 1.8f,
                cap = StrokeCap.Round
            )
        }
        key == "crown" -> {
            // Stem + knurled crown
            drawLine(
                color = strokeColor,
                start = Offset(center.x - rx * 0.35f, center.y),
                end = Offset(center.x + rx * 0.55f, center.y),
                strokeWidth = 3f
            )
            drawRoundRect(
                color = fillHighlight,
                topLeft = Offset(center.x + rx * 0.55f, center.y - ry * 0.32f),
                size = Size(rx * 0.26f, ry * 0.64f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(center.x + rx * 0.55f, center.y - ry * 0.32f),
                size = Size(rx * 0.26f, ry * 0.64f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(strokeW)
            )
        }
        key == "watch_case" -> {
            // Sculpted lugs + main case
            val lugW = rx * 0.52f
            val lugH = ry * 0.42f
            drawRoundRect(
                color = fillHighlight,
                topLeft = Offset(center.x - lugW, center.y - ry * 1.24f),
                size = Size(lugW * 2f, ry * 2.48f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(center.x - lugW, center.y - ry * 1.24f),
                size = Size(lugW * 2f, ry * 2.48f),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(strokeW)
            )
            drawOval(
                color = surfaceColor,
                topLeft = Offset(center.x - rx * 1.02f, center.y - ry * 0.96f),
                size = Size(rx * 2.04f, ry * 1.92f)
            )
            drawOval(
                color = strokeColor,
                topLeft = Offset(center.x - rx * 1.02f, center.y - ry * 0.96f),
                size = Size(rx * 2.04f, ry * 1.92f),
                style = Stroke(strokeW)
            )
            drawOval(
                color = mutedColor.copy(alpha = 0.5f),
                topLeft = Offset(center.x - rx * 0.72f, center.y - ry * 0.68f),
                size = Size(rx * 1.44f, ry * 1.36f),
                style = Stroke(1.4f)
            )
        }
        key == "movement" -> {
            drawOval(
                color = Color(0xFFD97706).copy(alpha = if (isSelected) 0.25f else 0.14f),
                topLeft = Offset(center.x - rx * 0.80f, center.y - ry * 0.80f),
                size = Size(rx * 1.60f, ry * 1.60f)
            )
            drawOval(
                color = strokeColor,
                topLeft = Offset(center.x - rx * 0.80f, center.y - ry * 0.80f),
                size = Size(rx * 1.60f, ry * 1.60f),
                style = Stroke(strokeW)
            )
            // Gear wheels & ruby bearing
            drawCircle(strokeColor, radius = rx * 0.22f, center = Offset(center.x - rx * 0.28f, center.y - ry * 0.14f), style = Stroke(1.6f))
            drawCircle(strokeColor, radius = rx * 0.16f, center = Offset(center.x + rx * 0.28f, center.y + ry * 0.14f), style = Stroke(1.6f))
            drawCircle(Color(0xFFDC2626), radius = 3.5f, center = Offset(center.x, center.y))
        }
        key == "case_back" -> {
            drawOval(
                color = fillHighlight,
                topLeft = Offset(center.x - rx * 0.90f, center.y - ry * 0.90f),
                size = Size(rx * 1.80f, ry * 1.80f)
            )
            drawOval(
                color = strokeColor,
                topLeft = Offset(center.x - rx * 0.90f, center.y - ry * 0.90f),
                size = Size(rx * 1.80f, ry * 1.80f),
                style = Stroke(strokeW)
            )
            drawOval(
                color = accentColor.copy(alpha = 0.65f),
                topLeft = Offset(center.x - rx * 0.72f, center.y - ry * 0.72f),
                size = Size(rx * 1.44f, ry * 1.44f),
                style = Stroke(2.2f)
            )
        }
        key == "strap" -> {
            val strapW = rx * 0.76f
            val segH = ry * 0.36f
            listOf(-ry * 0.9f, -ry * 0.45f, ry * 0.1f, ry * 0.55f).forEach { yOff ->
                drawRoundRect(
                    color = fillHighlight,
                    topLeft = Offset(center.x - strapW * 0.5f, center.y + yOff),
                    size = Size(strapW, segH),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(center.x - strapW * 0.5f, center.y + yOff),
                    size = Size(strapW, segH),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(strokeW)
                )
            }
        }
        key == "spring_bar" -> {
            listOf(-ry * 0.48f, ry * 0.48f).forEach { yOff ->
                drawLine(
                    color = strokeColor,
                    start = Offset(center.x - rx * 0.65f, center.y + yOff),
                    end = Offset(center.x + rx * 0.65f, center.y + yOff),
                    strokeWidth = if (isSelected) 5.5f else 4f,
                    cap = StrokeCap.Round
                )
                drawCircle(accentColor, radius = 4f, center = Offset(center.x - rx * 0.45f, center.y + yOff))
                drawCircle(accentColor, radius = 4f, center = Offset(center.x + rx * 0.45f, center.y + yOff))
            }
        }
        key == "clasp" -> {
            drawRoundRect(
                color = fillHighlight,
                topLeft = Offset(center.x - rx * 0.55f, center.y - ry * 0.38f),
                size = Size(rx * 1.1f, ry * 0.76f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(center.x - rx * 0.55f, center.y - ry * 0.38f),
                size = Size(rx * 1.1f, ry * 0.76f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(strokeW)
            )
            drawLine(
                color = strokeColor,
                start = Offset(center.x - rx * 0.3f, center.y),
                end = Offset(center.x + rx * 0.3f, center.y),
                strokeWidth = 2f
            )
        }
        key == "fasteners" || key == "enclosure_fasteners" -> {
            listOf(-rx * 0.5f, 0f, rx * 0.5f).forEach { xOff ->
                drawCircle(fillHighlight, radius = 8f, center = Offset(center.x + xOff, center.y))
                drawCircle(strokeColor, radius = 8f, center = Offset(center.x + xOff, center.y), style = Stroke(strokeW))
                drawLine(
                    color = strokeColor,
                    start = Offset(center.x + xOff - 5f, center.y),
                    end = Offset(center.x + xOff + 5f, center.y),
                    strokeWidth = 1.6f
                )
            }
        }
        category == ProductCategory.ELECTRONICS -> {
            // Isometric rounded slab for smartphone modules
            val w = rx * 1.4f
            val h = ry * 1.1f
            drawRoundRect(
                color = fillHighlight,
                topLeft = Offset(center.x - w * 0.5f, center.y - h * 0.5f),
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(center.x - w * 0.5f, center.y - h * 0.5f),
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(strokeW)
            )
            drawRect(
                color = accentColor.copy(alpha = 0.5f),
                topLeft = Offset(center.x - w * 0.25f, center.y - h * 0.25f),
                size = Size(w * 0.5f, h * 0.5f),
                style = Stroke(1.5f)
            )
        }
        else -> {
            // Appliances / General precision ring or assembly block
            drawOval(
                color = fillHighlight,
                topLeft = Offset(center.x - rx * 0.85f, center.y - ry * 0.85f),
                size = Size(rx * 1.7f, ry * 1.7f)
            )
            drawOval(
                color = strokeColor,
                topLeft = Offset(center.x - rx * 0.85f, center.y - ry * 0.85f),
                size = Size(rx * 1.7f, ry * 1.7f),
                style = Stroke(strokeW)
            )
            drawOval(
                color = strokeColor.copy(alpha = 0.6f),
                topLeft = Offset(center.x - rx * 0.45f, center.y - ry * 0.45f),
                size = Size(rx * 0.9f, ry * 0.9f),
                style = Stroke(1.6f)
            )
        }
    }
}

private fun DrawScope.drawCalloutLabel(
    componentName: String,
    center: Offset,
    radius: Float,
    canvasWidth: Float,
    accentColor: Color,
    surfaceColor: Color,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle
) {
    val label = componentName.take(24)
    val measured = textMeasurer.measure(label, textStyle)
    val padH = 14f
    val padV = 8f
    val boxW = measured.size.width + padH * 2
    val boxH = measured.size.height + padV * 2

    val placeRight = center.x < canvasWidth * 0.55f
    val anchorX = if (placeRight) center.x + radius * 0.85f else center.x - radius * 0.85f
    val boxLeft = if (placeRight) {
        (anchorX + 18f).coerceAtMost(canvasWidth - boxW - 12f)
    } else {
        (anchorX - 18f - boxW).coerceAtLeast(12f)
    }
    val boxTop = (center.y - boxH * 0.5f).coerceIn(12f, size.height - boxH - 12f)

    // Leader line
    val path = Path().apply {
        moveTo(center.x, center.y)
        lineTo(if (placeRight) boxLeft else boxLeft + boxW, boxTop + boxH * 0.5f)
    }
    drawPath(path, color = accentColor, style = Stroke(2f))
    drawCircle(color = accentColor, radius = 4.5f, center = center)

    drawRoundRect(
        color = surfaceColor,
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = accentColor,
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(1.8f)
    )
    drawText(
        textLayoutResult = measured,
        color = accentColor,
        topLeft = Offset(boxLeft + padH, boxTop + padV)
    )
}
