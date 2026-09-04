package com.example.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import com.example.camera.model.*

@Composable
fun TopControlBar(
    cameraMode: CameraMode,
    flashMode: FlashMode,
    timerMode: TimerMode,
    gridType: GridType,
    isRawEnabled: Boolean,
    supportsRaw: Boolean,
    storageStats: StorageStats = StorageStats(),
    videoQuality: VideoQualityOption = VideoQualityOption.UHD_4K_30,
    hdrState: VideoHdrState = VideoHdrState(),
    onVideoQualityClick: () -> Unit = {},
    onHdrClick: () -> Unit = {},
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onGridClick: () -> Unit,
    onRawClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.70f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Flash & Timer (Photo Mode)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash Button
                IconButton(
                    onClick = onFlashClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("flash_button")
                ) {
                    val (flashIcon, flashColor) = when (flashMode) {
                        FlashMode.OFF -> Icons.Outlined.FlashOff to Color.White.copy(alpha = 0.75f)
                        FlashMode.AUTO -> Icons.Outlined.FlashAuto to Color(0xFFFFD54F)
                        FlashMode.ON -> Icons.Outlined.FlashOn to Color(0xFFFFD54F)
                        FlashMode.TORCH -> Icons.Outlined.Highlight to Color(0xFFFFB300)
                    }
                    Icon(
                        imageVector = flashIcon,
                        contentDescription = "Flash: ${flashMode.title}",
                        tint = flashColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Timer Button (Photo / Portrait Modes)
                if (cameraMode != CameraMode.VIDEO) {
                    IconButton(
                        onClick = onTimerClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .testTag("timer_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (timerMode == TimerMode.OFF) Icons.Outlined.TimerOff else Icons.Outlined.Timer,
                                contentDescription = "Timer: ${timerMode.label}",
                                tint = if (timerMode == TimerMode.OFF) Color.White.copy(alpha = 0.75f) else Color(0xFFFFD54F),
                                modifier = Modifier.size(19.dp)
                            )
                            if (timerMode != TimerMode.OFF) {
                                Text(
                                    text = timerMode.label,
                                    color = Color(0xFFFFD54F),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .offset(x = 8.dp, y = 6.dp)
                                        .background(Color.Black, shape = CircleShape)
                                        .padding(1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Center Group: Mode-Specific Liquid Frosted Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video Mode: Direct HDR & Video Quality Chips
                if (cameraMode == CameraMode.VIDEO) {
                    // HDR Chip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hdrState.isHdrActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else Color.Black.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (hdrState.isHdrActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                            else Color.White.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onHdrClick() }
                            .testTag("top_hdr_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (hdrState.isHdrActive) Icons.Default.HdrOn else Icons.Default.HdrOff,
                                contentDescription = "Video HDR",
                                tint = if (hdrState.isHdrActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (hdrState.mode) {
                                    VideoHdrMode.OFF -> "HDR OFF"
                                    VideoHdrMode.AUTO -> "HDR AUTO"
                                    VideoHdrMode.MANUAL -> "HDR ${hdrState.manualIntensity}%"
                                },
                                color = if (hdrState.isHdrActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Video Quality Chip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFE53935).copy(alpha = 0.22f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFFF5252).copy(alpha = 0.75f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onVideoQualityClick() }
                            .testTag("video_quality_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF5252))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = videoQuality.badgeLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Photo Mode: RAW toggle chip (if supported)
                if (cameraMode == CameraMode.PHOTO && supportsRaw) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isRawEnabled) Color(0xFFFFB300).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isRawEnabled) Color(0xFFFFB300) else Color.White.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onRawClick() }
                            .testTag("raw_toggle_chip")
                    ) {
                        Text(
                            text = "RAW",
                            color = if (isRawEnabled) Color(0xFFFFB300) else Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Right Group: Grid & Settings Button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Grid Button
                IconButton(
                    onClick = onGridClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("grid_button")
                ) {
                    Icon(
                        imageVector = if (gridType == GridType.NONE) Icons.Outlined.GridOff else Icons.Outlined.GridOn,
                        contentDescription = "Grid: ${gridType.title}",
                        tint = if (gridType == GridType.NONE) Color.White.copy(alpha = 0.75f) else Color(0xFF64FFDA),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Settings Button (Always neatly placed at top-right corner)
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
