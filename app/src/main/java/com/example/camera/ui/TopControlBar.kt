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
    storageStats: StorageStats,
    videoQuality: VideoQualityOption = VideoQualityOption.UHD_4K_30,
    onVideoQualityClick: () -> Unit = {},
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onGridClick: () -> Unit,
    onRawClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.75f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Storage Chip & Video Quality / RAW Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Storage Capacity Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.testTag("storage_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SdStorage,
                        contentDescription = "Storage",
                        tint = Color(0xFF64FFDA),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val storageText = if (cameraMode == CameraMode.PHOTO) {
                        "%.1f GB Free · ~%s img".format(
                            storageStats.freeGb,
                            if (storageStats.estimatedPhotos > 9999) "${storageStats.estimatedPhotos / 1000}k" else storageStats.estimatedPhotos.toString()
                        )
                    } else {
                        "%.1f GB Free · ~%dh %dm".format(
                            storageStats.freeGb,
                            storageStats.estimatedVideoMinutes / 60,
                            storageStats.estimatedVideoMinutes % 60
                        )
                    }
                    Text(
                        text = storageText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Video Mode: Direct Video Quality Chip (4K 30, 4K 60, 1080p 30, etc.)
            if (cameraMode == CameraMode.VIDEO) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE53935).copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFFF5252).copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onVideoQualityClick() }
                        .testTag("video_quality_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5252))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = videoQuality.badgeLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Photo Mode: RAW toggle chip (if supported)
            if (cameraMode == CameraMode.PHOTO && supportsRaw) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isRawEnabled) Color(0xFFFFB300).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRawEnabled) Color(0xFFFFB300) else Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onRawClick() }
                        .testTag("raw_toggle_chip")
                ) {
                    Text(
                        text = "RAW",
                        color = if (isRawEnabled) Color(0xFFFFB300) else Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Action Icons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Button
            IconButton(
                onClick = onFlashClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .testTag("flash_button")
            ) {
                val (flashIcon, flashColor) = when (flashMode) {
                    FlashMode.OFF -> Icons.Outlined.FlashOff to Color.White.copy(alpha = 0.7f)
                    FlashMode.AUTO -> Icons.Outlined.FlashAuto to Color(0xFFFFD54F)
                    FlashMode.ON -> Icons.Outlined.FlashOn to Color(0xFFFFD54F)
                    FlashMode.TORCH -> Icons.Outlined.Highlight to Color(0xFFFFB300)
                }
                Icon(
                    imageVector = flashIcon,
                    contentDescription = "Flash: ${flashMode.title}",
                    tint = flashColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Timer Button
            IconButton(
                onClick = onTimerClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .testTag("timer_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (timerMode == TimerMode.OFF) Icons.Outlined.TimerOff else Icons.Outlined.Timer,
                        contentDescription = "Timer: ${timerMode.label}",
                        tint = if (timerMode == TimerMode.OFF) Color.White.copy(alpha = 0.7f) else Color(0xFFFFD54F),
                        modifier = Modifier.size(20.dp)
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

            // Grid Button
            IconButton(
                onClick = onGridClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .testTag("grid_button")
            ) {
                Icon(
                    imageVector = if (gridType == GridType.NONE) Icons.Outlined.GridOff else Icons.Outlined.GridOn,
                    contentDescription = "Grid: ${gridType.title}",
                    tint = if (gridType == GridType.NONE) Color.White.copy(alpha = 0.7f) else Color(0xFF64FFDA),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
