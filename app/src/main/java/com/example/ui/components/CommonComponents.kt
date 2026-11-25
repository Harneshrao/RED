package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CompatibilityStatus
import com.example.model.ConfidenceLevel
import com.example.model.VerificationProvenance
import com.example.ui.theme.IncompatibleRed
import com.example.ui.theme.IncompatibleRedBg
import com.example.ui.theme.LikelyAmber
import com.example.ui.theme.LikelyAmberBg
import com.example.ui.theme.UnknownSlate
import com.example.ui.theme.UnknownSlateBg
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartlyTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun CompatibilityBadge(
    status: CompatibilityStatus,
    modifier: Modifier = Modifier
) {
    val (fg, bg, icon) = when (status) {
        CompatibilityStatus.VERIFIED_COMPATIBLE -> Triple(
            VerifiedGreen,
            VerifiedGreenBg,
            Icons.Outlined.Verified
        )
        CompatibilityStatus.LIKELY_COMPATIBLE -> Triple(
            LikelyAmber,
            LikelyAmberBg,
            Icons.Outlined.CheckCircle
        )
        CompatibilityStatus.COMPATIBILITY_UNKNOWN -> Triple(
            UnknownSlate,
            UnknownSlateBg,
            Icons.Outlined.HelpOutline
        )
        CompatibilityStatus.NOT_COMPATIBLE -> Triple(
            IncompatibleRed,
            IncompatibleRedBg,
            Icons.Outlined.ErrorOutline
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = fg
            )
        }
    }
}

@Composable
fun ConfidencePill(
    score: Double,
    modifier: Modifier = Modifier
) {
    val level = ConfidenceLevel.fromScore(score)
    val pct = (score * 100).toInt().coerceIn(0, 100)
    val dotColor = when (level) {
        ConfidenceLevel.HIGH -> VerifiedGreen
        ConfidenceLevel.MEDIUM -> LikelyAmber
        ConfidenceLevel.LOW -> IncompatibleRed
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = "$pct% ${level.label.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProvenanceTag(
    provenance: VerificationProvenance,
    modifier: Modifier = Modifier
) {
    val isVerified = provenance == VerificationProvenance.VERIFIED_DATASET
    val tint = if (isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isVerified) Icons.Outlined.Verified else Icons.Outlined.Info,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = provenance.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TechnicalSpecRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    monospaceValue: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = if (monospaceValue) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
fun NoticeCallout(
    title: String,
    message: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = if (isWarning) LikelyAmber else MaterialTheme.colorScheme.primary
    val bg = if (isWarning) LikelyAmberBg else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isWarning) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}
