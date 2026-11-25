package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CenterFocusWeak
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R

private data class OnboardingStep(
    val number: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
    val imageRes: Int
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                number = "01",
                title = "Photograph your product.",
                body = "Point your camera at a watch, electronic device, or appliance. Partly guides framing and captures multiple angles when internal details matter.",
                icon = Icons.Outlined.CenterFocusWeak,
                imageRes = R.drawable.img_cat_watches
            ),
            OnboardingStep(
                number = "02",
                title = "Explore every component.",
                body = "Inspect an interactive exploded assembly. Tap any part to learn its name, function, material, and what measurements to verify.",
                icon = Icons.Outlined.Layers,
                imageRes = R.drawable.img_onboarding_exploded
            ),
            OnboardingStep(
                number = "03",
                title = "Find the right replacement.",
                body = "See honest compatibility confidence, step-by-step repair tools and safety guidance, and direct links to verified parts catalogs.",
                icon = Icons.Outlined.Build,
                imageRes = R.drawable.img_cat_electronics
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentIndex]

    Scaffold(
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
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PARTLY • ${step.number}/03",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier.testTag("onboarding_skip_button")
                    ) {
                        Text("Skip")
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = step.imageRes),
                            contentDescription = step.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(MaterialTheme.shapes.large)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = step.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        steps.indices.forEach { idx ->
                            val active = idx == currentIndex
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (active) 24.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (currentIndex < steps.lastIndex) {
                                currentIndex++
                            } else {
                                onFinished()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("onboarding_next_button"),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = if (currentIndex < steps.lastIndex) "Continue" else "Start Identifying",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
