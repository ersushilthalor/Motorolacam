package com.example.camera.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.BokehStyle
import com.example.camera.model.PortraitConfig
import com.example.camera.model.PortraitProcessingState

@Composable
fun PortraitControlBar(
    config: PortraitConfig,
    processingState: PortraitProcessingState,
    onBlurStrengthChanged: (Float) -> Unit,
    onApertureSelected: (String) -> Unit,
    onBokehStyleSelected: (BokehStyle) -> Unit,
    onToggleFaceEnhancement: () -> Unit,
    onToggleSkinTone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val apertures = listOf("f/0.95", "f/1.2", "f/1.4", "f/1.8", "f/2.4", "f/2.8")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.88f),
                        Color.Black.copy(alpha = 0.96f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Processing Progress Banner (when portrait depth calculation is running)
        AnimatedVisibility(
            visible = processingState.isProcessing,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = Color(0xFFFFD54F).copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { processingState.progress },
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFFFFD54F),
                        strokeWidth = 2.5.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = processingState.statusText.ifEmpty { "AI Portrait Processing..." },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { processingState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .height(3.dp)
                                .clip(CircleShape),
                            color = Color(0xFFFFD54F),
                            trackColor = Color.White.copy(alpha = 0.15f)
                        )
                    }
                    Text(
                        text = "${(processingState.progress * 100).toInt()}%",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Row 1: Aperture Simulation Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aperture:",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 4.dp)
            )

            apertures.forEach { aperture ->
                val isSelected = config.simulatedAperture == aperture
                val bgColor by animateColorAsState(
                    if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.08f),
                    label = "apertureBg"
                )
                val textColor by animateColorAsState(
                    if (isSelected) Color.Black else Color.White,
                    label = "apertureText"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(bgColor)
                        .clickable { onApertureSelected(aperture) }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .testTag("aperture_chip_$aperture"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = aperture,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Blur Strength Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BlurOn,
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Depth Blur: ${config.blurStrength.toInt()}%",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(110.dp)
            )
            Slider(
                value = config.blurStrength,
                onValueChange = onBlurStrengthChanged,
                valueRange = 0f..100f,
                modifier = Modifier
                    .weight(1f)
                    .testTag("portrait_blur_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFFD54F),
                    activeTrackColor = Color(0xFFFFD54F),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 3: Bokeh Style & Enhancement Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bokeh Style Chips (Natural vs Strong Bokeh)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BokehStyle.values().forEach { style ->
                    val isSelected = config.bokehStyle == style
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFFFD54F).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onBokehStyleSelected(style) }
                            .testTag("bokeh_style_${style.name.lowercase()}")
                    ) {
                        Text(
                            text = style.label,
                            color = if (isSelected) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Quick Enhancement Toggles
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Face enhancement toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (config.faceEnhancement) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (config.faceEnhancement) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleFaceEnhancement() }
                        .testTag("toggle_face_enhancement")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = if (config.faceEnhancement) Color(0xFF00E676) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Face",
                            color = if (config.faceEnhancement) Color(0xFF00E676) else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = if (config.faceEnhancement) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Skin tone correction toggle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (config.skinToneCorrection) Color(0xFFFFAB40).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (config.skinToneCorrection) Color(0xFFFFAB40) else Color.White.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleSkinTone() }
                        .testTag("toggle_skin_tone")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = if (config.skinToneCorrection) Color(0xFFFFAB40) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Tone",
                            color = if (config.skinToneCorrection) Color(0xFFFFAB40) else Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = if (config.skinToneCorrection) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
