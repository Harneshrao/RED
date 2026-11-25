package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.ProductCategory
import com.example.ui.theme.VerifiedGreen
import kotlin.math.abs

private const val CAM_TAG = "CameraCaptureScreen"

@Composable
fun CameraCaptureScreen(
    capturedBitmaps: List<Bitmap>,
    categoryHint: ProductCategory?,
    onAddBitmap: (Bitmap) -> Unit,
    onReplaceBitmap: (Int, Bitmap) -> Unit,
    onRemoveBitmap: (Int) -> Unit,
    onAnalyzeClicked: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

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
                onAddBitmap(bmp)
            }
        }
    }

    var flashEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    // Real accelerometer stability & tilt telemetry
    var stabilityLabel by remember { mutableStateOf("Hold steady • Align inside frame") }
    var isSteady by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastMag = 9.8f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = kotlin.math.sqrt(x * x + y * y + z * z)
                val delta = abs(mag - lastMag)
                lastMag = mag
                isSteady = delta < 0.85f
                stabilityLabel = if (isSteady) {
                    "Steady • Ready to capture"
                } else {
                    "Stabilize device for sharp component detail"
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accel != null) {
            sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // If user tapped a thumbnail to crop/rotate before AI analysis
    val cropTargetIdx = editingIndex
    if (cropTargetIdx != null && cropTargetIdx in capturedBitmaps.indices) {
        ImageCropReviewOverlay(
            bitmap = capturedBitmaps[cropTargetIdx],
            onApply = { adjusted ->
                onReplaceBitmap(cropTargetIdx, adjusted)
                editingIndex = null
            },
            onCancel = { editingIndex = null }
        )
        return
    }

    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0C0E))
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        runCatching {
                            val provider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCaptureRef = imageCapture
                            provider.unbindAll()
                            cameraRef = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        }.onFailure { err ->
                            Log.e(CAM_TAG, "Failed to bind CameraX", err)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                update = {
                    cameraRef?.cameraControl?.enableTorch(flashEnabled)
                    cameraRef?.cameraControl?.setZoomRatio(zoomRatio)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Precision optical framing reticle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxSize = minOf(size.width, size.height) * 0.68f
                val left = (size.width - boxSize) / 2f
                val top = (size.height - boxSize) / 2f - 30.dp.toPx()
                val corner = 24.dp.toPx()
                val reticleColor = if (isSteady) Color.White.copy(alpha = 0.88f) else Color(0xFFF59E0B)

                // Top-left corner
                drawLine(reticleColor, Offset(left, top), Offset(left + corner, top), strokeWidth = 3f)
                drawLine(reticleColor, Offset(left, top), Offset(left, top + corner), strokeWidth = 3f)
                // Top-right corner
                drawLine(reticleColor, Offset(left + boxSize - corner, top), Offset(left + boxSize, top), strokeWidth = 3f)
                drawLine(reticleColor, Offset(left + boxSize, top), Offset(left + boxSize, top + corner), strokeWidth = 3f)
                // Bottom-left corner
                drawLine(reticleColor, Offset(left, top + boxSize), Offset(left + corner, top + boxSize), strokeWidth = 3f)
                drawLine(reticleColor, Offset(left, top + boxSize - corner), Offset(left, top + boxSize), strokeWidth = 3f)
                // Bottom-right corner
                drawLine(reticleColor, Offset(left + boxSize - corner, top + boxSize), Offset(left + boxSize, top + boxSize), strokeWidth = 3f)
                drawLine(reticleColor, Offset(left + boxSize, top + boxSize - corner), Offset(left + boxSize, top + boxSize), strokeWidth = 3f)

                drawRect(
                    color = reticleColor.copy(alpha = 0.16f),
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize),
                    style = Stroke(1f)
                )
            }
        } else {
            // Clean Camera Permission Prompt
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Camera Access",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.camera_permission_rationale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("grant_camera_button")
                        ) {
                            Text(stringResource(R.string.grant_camera_permission))
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
                                .testTag("camera_fallback_gallery_button")
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_upload_gallery))
                        }
                    }
                }
            }
        }

        // Top Bar Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.testTag("camera_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = Color.Black.copy(alpha = 0.62f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSteady) VerifiedGreen else Color(0xFFF59E0B))
                    )
                    Text(
                        text = categoryHint?.displayName ?: stabilityLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            if (hasCameraPermission) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { zoomRatio = if (zoomRatio < 1.5f) 2f else 1f },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.testTag("camera_zoom_button")
                    ) {
                        Text(
                            text = if (zoomRatio >= 1.5f) "2x" else "1x",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }
                    Surface(
                        onClick = { flashEnabled = !flashEnabled },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.testTag("camera_flash_button")
                    ) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                            contentDescription = "Toggle flash",
                            tint = if (flashEnabled) Color(0xFFFACC15) else Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Bottom Capture & Multi-Angle Tray
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.72f))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (capturedBitmaps.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${capturedBitmaps.size}/4 angles captured • Tap photo to crop",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(capturedBitmaps) { idx, bmp ->
                        Box(modifier = Modifier.size(64.dp)) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Angle ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.small)
                                    .border(1.dp, Color.White.copy(alpha = 0.45f), MaterialTheme.shapes.small)
                                    .clickable { editingIndex = idx }
                            )
                            Surface(
                                onClick = { onRemoveBitmap(idx) },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Remove angle",
                                    tint = Color.White,
                                    modifier = Modifier.padding(3.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Tip: Capture front dial/face first, then add side or rear caseback angles for higher accuracy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.testTag("camera_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = "Upload from gallery",
                        tint = Color.White,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                // Shutter button
                if (hasCameraPermission) {
                    Surface(
                        onClick = {
                            val capture = imageCaptureRef ?: return@Surface
                            capture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bmp = imageProxyToBitmap(image)
                                        image.close()
                                        if (bmp != null) onAddBitmap(bmp)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e(CAM_TAG, "Photo capture failed", exception)
                                    }
                                }
                            )
                        },
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(4.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("camera_shutter_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFF171717), CircleShape)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(72.dp))
                }

                Button(
                    onClick = onAnalyzeClicked,
                    enabled = capturedBitmaps.isNotEmpty(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("camera_analyze_button")
                ) {
                    Text("Analyze")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ImageCropReviewOverlay(
    bitmap: Bitmap,
    onApply: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var rotationQuarterTurns by remember { mutableIntStateOf(0) }
    var cropZoom by remember { mutableFloatStateOf(1f) }

    val previewBitmap = remember(bitmap, rotationQuarterTurns, cropZoom) {
        val matrix = Matrix().apply { postRotate((rotationQuarterTurns * 90).toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (cropZoom <= 1.02f) {
            rotated
        } else {
            val cropW = (rotated.width / cropZoom).toInt().coerceAtLeast(100)
            val cropH = (rotated.height / cropZoom).toInt().coerceAtLeast(100)
            val x = ((rotated.width - cropW) / 2).coerceAtLeast(0)
            val y = ((rotated.height - cropH) / 2).coerceAtLeast(0)
            Bitmap.createBitmap(rotated, x, y, cropW, cropH)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101114))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Outlined.Close, contentDescription = "Cancel crop", tint = Color.White)
            }
            Text(
                text = "Crop & Rotate Framing",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            IconButton(onClick = { rotationQuarterTurns = (rotationQuarterTurns + 1) % 4 }) {
                Icon(Icons.Outlined.RotateRight, contentDescription = "Rotate 90 degrees", tint = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Cropped product preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Crop, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text(
                    text = "Center Zoom ${"%.1f".format(cropZoom)}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                Slider(
                    value = cropZoom,
                    onValueChange = { cropZoom = it },
                    valueRange = 1f..2.2f,
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = { onApply(previewBitmap) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Apply Framing")
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes.firstOrNull()?.buffer ?: return null
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val rotation = image.imageInfo.rotationDegrees
    if (rotation == 0) return raw
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
}
