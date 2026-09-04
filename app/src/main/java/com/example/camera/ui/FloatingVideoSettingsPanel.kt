package com.example.camera.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.CameraResolution

/**
 * Floating Liquid Frosted Glass Resolution & Frame Panel
 * Exactly styled as the reference screenshot.
 */
@Composable
fun FloatingVideoSettingsPanel(
    isOpen: Boolean,
    currentResolution: CameraResolution?,
    currentFps: Int,
    onResolutionSelected: (CameraResolution) -> Unit,
    onFpsSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEA25262B),
                            Color(0xD91A1B1F)
                        )
                    )
                )
                .border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("floating_video_settings_panel")
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Resolution Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Resolution",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(90.dp)
                    )

                    val resolutionOptions = listOf(
                        "720p" to CameraResolution(1280, 720),
                        "1080p" to CameraResolution(1920, 1080),
                        "4K" to CameraResolution(3840, 2160),
                        "8K" to CameraResolution(7680, 4320)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        resolutionOptions.forEach { (label, res) ->
                            val isSelected = currentResolution?.let {
                                (it.width == res.width && it.height == res.height) ||
                                (it.width == res.height && it.height == res.width)
                            } ?: (label == "4K")

                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFFFF7A00) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .clickable { onResolutionSelected(res) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                    .testTag("res_option_$label")
                            )
                        }
                    }
                }

                // Frame Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Frame",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(90.dp)
                    )

                    val fpsOptions = listOf(
                        "30fps" to 30,
                        "60fps" to 60,
                        "120" to 120
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        fpsOptions.forEach { (label, fps) ->
                            val isSelected = currentFps == fps

                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFFFF7A00) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .clickable { onFpsSelected(fps) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                                    .testTag("fps_option_$label")
                            )
                        }
                    }
                }
            }
        }
    }
}
