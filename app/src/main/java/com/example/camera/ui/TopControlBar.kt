package com.example.camera.ui

import androidx.compose.foundation.BorderStroke
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
    videoResolution: CameraResolution? = null,
    videoFps: Int = 30,
    photoMegapixelMode: PhotoMegapixelMode = PhotoMegapixelMode.M12,
    cinemaConfig: CinemaConfig = CinemaConfig(),
    onCinemaSettingsClick: () -> Unit = {},
    onVideoQualityClick: () -> Unit = {},
    onVideoSettingsClick: () -> Unit = {},
    onToggleMegapixelMode: () -> Unit = {},
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
            // Left Group: Settings, Flash & Timer
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
                        .background(Color(0xB21E1E22))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
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
                if (cameraMode != CameraMode.VIDEO && cameraMode != CameraMode.CINEMA) {
                    IconButton(
                        onClick = onTimerClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xB21E1E22))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
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

            // Center Group: Liquid Frosted Pill
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video Mode: Sleek Resolution · FPS Pill (HDR system completely removed)
                if (cameraMode == CameraMode.VIDEO) {
                    val resLabel = when {
                        videoResolution?.width == 3840 || videoResolution?.height == 3840 -> "4K"
                        videoResolution?.width == 7680 || videoResolution?.height == 7680 -> "8K"
                        videoResolution?.width == 1920 || videoResolution?.height == 1920 -> "1080"
                        videoResolution?.width == 1280 || videoResolution?.height == 1280 -> "720"
                        else -> "4K"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xB21E1E22))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                            .clickable { onVideoSettingsClick() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("video_resolution_fps_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$resLabel · $videoFps",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Cinema Mode: Sleek Quality Option Pill ONLY (cinema icon removed, tap here to open all cinema settings)
                if (cameraMode == CameraMode.CINEMA) {
                    val resLabel = when {
                        cinemaConfig.selectedResolution?.width == 3840 || cinemaConfig.selectedResolution?.height == 3840 -> "4K"
                        cinemaConfig.selectedResolution?.width == 1920 || cinemaConfig.selectedResolution?.height == 1920 -> "1080"
                        else -> "4K"
                    }
                    val bitLabel = if (cinemaConfig.logBitDepth == LogBitDepth.BIT_10) "10-BIT" else "8-BIT"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF26210A))
                            .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(16.dp))
                            .clickable { onCinemaSettingsClick() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("cinema_resolution_fps_pill"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$resLabel · ${cinemaConfig.videoFps} · $bitLabel",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Photo Mode: 12M / 50M Ultra HD Option Pill
                if (cameraMode == CameraMode.PHOTO) {
                    val is50M = photoMegapixelMode == PhotoMegapixelMode.M50
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (is50M) Color(0xFFFFD54F).copy(alpha = 0.25f) else Color(0xB21E1E22),
                        border = BorderStroke(
                            1.dp,
                            if (is50M) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleMegapixelMode() }
                            .testTag("photo_mp_toggle_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = photoMegapixelMode.label,
                                color = if (is50M) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.90f),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            if (is50M) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ULTRA",
                                    color = Color(0xFFFFD54F),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // Photo Mode: RAW toggle chip (if supported)
                if (cameraMode == CameraMode.PHOTO && supportsRaw) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isRawEnabled) Color(0xFFFFB300).copy(alpha = 0.25f) else Color(0xB21E1E22),
                        border = BorderStroke(
                            1.dp,
                            if (isRawEnabled) Color(0xFFFFB300) else Color.White.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onRawClick() }
                            .testTag("raw_toggle_chip")
                    ) {
                        Text(
                            text = "RAW",
                            color = if (isRawEnabled) Color(0xFFFFB300) else Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Right Group: Grid & Settings Button (Always positioned on far right)
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
                        .background(Color(0xB21E1E22))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                        .testTag("grid_button")
                ) {
                    Icon(
                        imageVector = if (gridType == GridType.NONE) Icons.Outlined.GridOff else Icons.Outlined.GridOn,
                        contentDescription = "Grid: ${gridType.title}",
                        tint = if (gridType == GridType.NONE) Color.White.copy(alpha = 0.75f) else Color(0xFF64FFDA),
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Settings Button (Always on the top right)
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xB21E1E22))
                        .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
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
