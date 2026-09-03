package com.example.camera.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HdrAuto
import androidx.compose.material.icons.filled.HdrOff
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Waves
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
import com.example.camera.model.VideoHdrMode
import com.example.camera.model.VideoHdrState

@Composable
fun VideoHdrControlBar(
    hdrState: VideoHdrState,
    onModeChanged: (VideoHdrMode) -> Unit,
    onIntensityChanged: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.90f),
                        Color.Black.copy(alpha = 0.97f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("video_hdr_control_bar")
    ) {
        // 1. Header: Title, Live Status Badge, and Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (hdrState.isHdrActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else Color.White.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hdrState.isHdrActive) Icons.Default.HdrOn else Icons.Default.HdrOff,
                        contentDescription = "HDR Mode Icon",
                        tint = if (hdrState.isHdrActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "REAL-TIME VIDEO HDR",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        if (hdrState.isHdrActive) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = borderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "LIVE ISP",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = hdrState.statusDescription,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .testTag("hdr_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close HDR Panel",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 2. Mode Selector: OFF / AUTO / MANUAL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            VideoHdrMode.entries.forEach { mode ->
                val isSelected = hdrState.mode == mode
                val bg by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "hdrModeBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.8f),
                    label = "hdrModeTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { onModeChanged(mode) }
                        .padding(vertical = 8.dp)
                        .testTag("hdr_mode_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val icon = when (mode) {
                            VideoHdrMode.OFF -> Icons.Default.HdrOff
                            VideoHdrMode.AUTO -> Icons.Default.HdrAuto
                            VideoHdrMode.MANUAL -> Icons.Default.Tune
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = mode.label,
                            tint = textColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = mode.label,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Mode Content
        when (hdrState.mode) {
            VideoHdrMode.OFF -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "HDR disabled. Sensor uses standard linear tonemapping.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { onModeChanged(VideoHdrMode.AUTO) },
                        modifier = Modifier.testTag("enable_hdr_button")
                    ) {
                        Text("Enable AUTO", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }

            VideoHdrMode.AUTO -> {
                // Auto Telemetry & Optimization Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Intelligent Scene & Noise Optimization",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HdrMetricChip(
                            label = "Shadow Boost",
                            value = "+${(hdrState.shadowLift * 100).toInt()}%",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        HdrMetricChip(
                            label = "Highlights",
                            value = "Safe -${(hdrState.highlightProtection * 100).toInt()}%",
                            tint = Color(0xFF64B5F6)
                        )
                        HdrMetricChip(
                            label = "Noise Reduction",
                            value = if (hdrState.noiseReductionStrength > 0.45f) "Aggressive S+T" else "Adaptive Clean",
                            tint = if (hdrState.noiseReductionStrength > 0.45f) Color(0xFF81C784) else Color(0xFFFFB74D)
                        )
                        HdrMetricChip(
                            label = "Motion Aware",
                            value = if (hdrState.isMotionDetected) "Fast Edge" else "Consecutive HQ",
                            tint = Color(0xFFBA68C8)
                        )
                    }
                }
            }

            VideoHdrMode.MANUAL -> {
                // Manual Intensity Slider (0 to 100)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HDR Dynamic Range Intensity",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${hdrState.manualIntensity}%",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = hdrState.manualIntensity.toFloat(),
                        onValueChange = { onIntensityChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        steps = 99,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hdr_intensity_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    // Quick presets (25%, 50%, 75%, 100%)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(25, 50, 75, 100).forEach { preset ->
                            val isPresetSelected = hdrState.manualIntensity == preset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPresetSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        width = 0.5.dp,
                                        color = if (isPresetSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onIntensityChanged(preset) }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$preset%",
                                    color = if (isPresetSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Explanatory note
                    Text(
                        text = "Dynamic range processing expands shadows & preserves highlights. Noise reduction remains independently adaptive to actual scene ISO.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HdrMetricChip(
    label: String,
    value: String,
    tint: Color
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = borderStroke(0.5.dp, tint.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 9.sp
            )
            Text(
                text = value,
                color = tint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun VideoHdrFloatingButton(
    hdrState: VideoHdrState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.testTag("hdr_floating_open_button"),
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.70f),
        border = borderStroke(
            width = 1.dp,
            color = if (hdrState.isHdrActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else Color.White.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (hdrState.isHdrActive) Icons.Default.HdrOn else Icons.Default.HdrOff,
                contentDescription = "Video HDR Button",
                tint = if (hdrState.isHdrActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = when (hdrState.mode) {
                    VideoHdrMode.OFF -> "HDR: OFF"
                    VideoHdrMode.AUTO -> "HDR: AUTO"
                    VideoHdrMode.MANUAL -> "HDR: ${hdrState.manualIntensity}%"
                },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)

