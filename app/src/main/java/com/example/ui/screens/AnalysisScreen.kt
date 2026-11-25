package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AnalysisStage
import com.example.ui.components.NoticeCallout
import com.example.ui.components.PartlyTopBar
import com.example.ui.theme.VerifiedGreen
import com.example.ui.viewmodel.AnalysisUiState

@Composable
fun AnalysisScreen(
    analysisState: AnalysisUiState,
    onRetry: () -> Unit,
    onCaptureAnotherAngle: () -> Unit,
    onProceedToOverview: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            PartlyTopBar(
                title = "Component Intelligence",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                when (analysisState) {
                    is AnalysisUiState.Idle,
                    is AnalysisUiState.Processing -> {
                        val currentStage = (analysisState as? AnalysisUiState.Processing)?.stage
                            ?: AnalysisStage.INGESTING
                        val progress = (analysisState as? AnalysisUiState.Processing)?.progress ?: 0.1f

                        Text(
                            text = "Analyzing product architecture",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = currentStage.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                        )

                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                AnalysisStage.entries.forEach { stage ->
                                    val done = stage.stepIndex < currentStage.stepIndex
                                    val active = stage == currentStage
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        when {
                                            done -> Icon(
                                                imageVector = Icons.Outlined.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = VerifiedGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            active -> CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                            else -> Icon(
                                                imageVector = Icons.Outlined.RadioButtonUnchecked,
                                                contentDescription = "Pending",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = stage.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (active || done) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is AnalysisUiState.Completed -> {
                        val pct = (analysisState.confidenceScore * 100).toInt()
                        NoticeCallout(
                            title = if (analysisState.confidenceScore < 0.65) {
                                "Low identification confidence ($pct%)"
                            } else {
                                "Additional angle recommended ($pct%)"
                            },
                            message = analysisState.suggestedNextAngle.ifBlank {
                                "Internal or rear reference markings were not clearly visible. Adding another angle helps verify exact compatibility."
                            },
                            isWarning = true
                        )

                        Button(
                            onClick = onCaptureAnotherAngle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("analysis_add_angle_button")
                        ) {
                            Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Capture Additional Angle")
                        }

                        OutlinedButton(
                            onClick = { onProceedToOverview(analysisState.scanId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("analysis_continue_anyway_button")
                        ) {
                            Text("Inspect Current Results")
                        }
                    }

                    is AnalysisUiState.Error -> {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text(
                                        text = "Recognition Interrupted",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = analysisState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("analysis_retry_button")
                        ) {
                            Text("Retry Analysis")
                        }
                        OutlinedButton(
                            onClick = onCaptureAnotherAngle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Retake Photograph")
                        }
                    }
                }
            }
        }
    }
}
