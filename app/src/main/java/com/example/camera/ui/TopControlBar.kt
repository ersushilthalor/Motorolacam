package com.example.camera.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.*

/**
 * Master Top Control Bar matching the reference UI design.
 * Exactly 6 beautifully aligned elements across a pure dark glass bar:
 * [Flash]  [Timer/Action]  [Pill 1]  [Pill 2]  [Grid/Assist]  [Settings]
 */
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
    isAudioEnabled: Boolean = true,
    onAudioToggle: () -> Unit = {},
    portraitAperture: String = "f/1.8",
    onPortraitApertureClick: () -> Unit = {},
    onPortraitStyleClick: () -> Unit = {},
    onCinemaSettingsClick: () -> Unit = {},
    onCinemaEvChange: (Int) -> Unit = {},
    onVideoQualityClick: () -> Unit = {},
    onVideoSettingsClick: () -> Unit = {},
    onToggleMegapixelMode: () -> Unit = {},
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onGridClick: () -> Unit,
    onRawClick: () -> Unit,
    onSettingsClick: () -> Unit,
    layoutConfig: ModeLayoutConfig = ModeLayoutConfig(),
    modifier: Modifier = Modifier
) {
    val accentColor = layoutConfig.getComposeAccentColor()
    val iconSize = layoutConfig.topControlsIconSizeDp.dp
    val buttonSize = (layoutConfig.topControlsIconSizeDp + 18).dp.coerceAtLeast(38.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("master_top_control_bar")
    ) {
        val flashButton = @Composable {
            IconButton(
                onClick = onFlashClick,
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(Color(0xB21A1A1E))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .testTag("flash_button")
            ) {
                val (flashIcon, flashColor) = when (flashMode) {
                    FlashMode.OFF -> Icons.Outlined.FlashOff to Color.White.copy(alpha = 0.85f)
                    FlashMode.AUTO -> Icons.Outlined.FlashAuto to accentColor
                    FlashMode.ON -> Icons.Outlined.FlashOn to accentColor
                    FlashMode.TORCH -> Icons.Outlined.Highlight to Color(0xFFFFB300)
                }
                Icon(
                    imageVector = flashIcon,
                    contentDescription = "Flash: ${flashMode.title}",
                    tint = flashColor,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        val timerAudioButton = @Composable {
            when (cameraMode) {
                CameraMode.VIDEO, CameraMode.CINEMA -> {
                    IconButton(
                        onClick = onAudioToggle,
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .testTag("video_audio_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isAudioEnabled) Icons.Outlined.Mic else Icons.Outlined.MicOff,
                            contentDescription = if (isAudioEnabled) "Audio On" else "Audio Muted",
                            tint = if (isAudioEnabled) Color.White.copy(alpha = 0.85f) else Color(0xFFFF6B6B),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
                else -> {
                    IconButton(
                        onClick = onTimerClick,
                        modifier = Modifier
                            .size(buttonSize)
                            .clip(CircleShape)
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                            .testTag("timer_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (timerMode == TimerMode.OFF) Icons.Outlined.TimerOff else Icons.Outlined.Timer,
                                contentDescription = "Timer: ${timerMode.label}",
                                tint = if (timerMode == TimerMode.OFF) Color.White.copy(alpha = 0.85f) else accentColor,
                                modifier = Modifier.size(iconSize)
                            )
                            if (timerMode != TimerMode.OFF) {
                                Text(
                                    text = timerMode.label,
                                    color = accentColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    softWrap = false,
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
        }

        val primaryBadge = @Composable {
            when (cameraMode) {
                CameraMode.PHOTO -> {
                    val is50M = photoMegapixelMode == PhotoMegapixelMode.M50
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (is50M) accentColor.copy(alpha = 0.2f) else Color(0xB21A1A1E))
                            .border(
                                1.dp,
                                if (is50M) accentColor else Color.White.copy(alpha = 0.22f),
                                RoundedCornerShape(17.dp)
                            )
                            .clickable { onToggleMegapixelMode() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = photoMegapixelMode.label,
                            color = if (is50M) accentColor else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.PORTRAIT -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, accentColor, RoundedCornerShape(17.dp))
                            .clickable { onPortraitApertureClick() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = portraitAperture,
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.VIDEO -> {
                    val resLabel = when {
                        videoResolution?.width == 3840 || videoResolution?.height == 3840 -> "4K"
                        videoResolution?.width == 7680 || videoResolution?.height == 7680 -> "8K"
                        videoResolution?.width == 1920 || videoResolution?.height == 1920 -> "1080"
                        videoResolution?.width == 1280 || videoResolution?.height == 1280 -> "720"
                        else -> "4K"
                    }
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
                            .clickable { onVideoSettingsClick() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = resLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.CINEMA -> {
                    val resLabel = when {
                        cinemaConfig.selectedResolution?.width == 3840 || cinemaConfig.selectedResolution?.height == 3840 -> "4K"
                        cinemaConfig.selectedResolution?.width == 1920 || cinemaConfig.selectedResolution?.height == 1920 -> "1080"
                        else -> "4K"
                    }
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = resLabel,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.MORE -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MORE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.NIGHT -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0x33FFB300))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(17.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NIGHT HDR",
                            color = Color(0xFFFFB300),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.DOLLY_ZOOM -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .border(1.dp, accentColor, RoundedCornerShape(17.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DOLLY",
                            color = accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.DUAL_VIDEO -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0x33E53935))
                            .border(1.dp, Color(0xFFE53935), RoundedCornerShape(17.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DUAL CAM",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        val secondaryBadge = @Composable {
            when (cameraMode) {
                CameraMode.PHOTO -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(if (isRawEnabled) accentColor.copy(alpha = 0.2f) else Color(0xB21A1A1E))
                            .border(
                                1.dp,
                                if (isRawEnabled) accentColor else Color.White.copy(alpha = 0.22f),
                                RoundedCornerShape(17.dp)
                            )
                            .clickable { onRawClick() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RAW",
                            color = if (isRawEnabled) accentColor else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.PORTRAIT -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
                            .clickable { onPortraitStyleClick() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "BOKEH",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.VIDEO -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
                            .clickable { onVideoSettingsClick() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$videoFps",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.CINEMA -> {
                    val bitLabel = if (cinemaConfig.logBitDepth == LogBitDepth.BIT_10) "10b" else "8b"
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, accentColor, RoundedCornerShape(17.dp))
                            .clickable { onCinemaSettingsClick() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LOG $bitLabel",
                            color = accentColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.MORE -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PRO",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.NIGHT -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(17.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MULTI-FUSION",
                            color = Color(0xFFFFB300),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.DOLLY_ZOOM -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AUTO TRACK",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                CameraMode.DUAL_VIDEO -> {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Color(0xB21A1A1E))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PIP / SPLIT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        val gridAssistButton = @Composable {
            if (cameraMode == CameraMode.CINEMA) {
                val evVal = cinemaConfig.exposureCompensation
                val evString = when {
                    evVal > 0 -> "+${evVal / 3f}"
                    evVal < 0 -> "${evVal / 3f}"
                    else -> "±0.0"
                }
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(if (evVal != 0) accentColor.copy(alpha = 0.2f) else Color(0xB21A1A1E))
                        .border(
                            1.dp,
                            if (evVal != 0) accentColor else Color.White.copy(alpha = 0.22f),
                            RoundedCornerShape(17.dp)
                        )
                        .clickable {
                            val nextEv = when (evVal) {
                                0 -> 3
                                3 -> 6
                                6 -> -6
                                -6 -> -3
                                -3 -> 0
                                else -> 0
                            }
                            onCinemaEvChange(nextEv)
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EV $evString",
                        color = if (evVal != 0) accentColor else Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                IconButton(
                    onClick = onGridClick,
                    modifier = Modifier
                        .size(buttonSize)
                        .clip(CircleShape)
                        .background(Color(0xB21A1A1E))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .testTag("grid_button")
                ) {
                    Icon(
                        imageVector = if (gridType == GridType.NONE) Icons.Outlined.GridOff else Icons.Outlined.GridOn,
                        contentDescription = "Grid: ${gridType.title}",
                        tint = if (gridType == GridType.NONE) Color.White.copy(alpha = 0.85f) else accentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }

        val proExpButton = @Composable {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xB21A1A1E))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(17.dp))
                    .clickable { onSettingsClick() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PRO",
                    color = accentColor,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        val settingsButton = @Composable {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(Color(0xB21A1A1E))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // Render Top Controls according to layoutConfig
        val visibleItems = layoutConfig.topControlsOrder.filterNot { layoutConfig.hiddenTopControls.contains(it) }

        val horizontalArrangement = when (layoutConfig.topBarAlignment) {
            TopBarAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
            TopBarAlignment.CENTER -> Arrangement.spacedBy(layoutConfig.topControlsSpacingDp.dp, Alignment.CenterHorizontally)
            TopBarAlignment.COMPACT_LEFT -> Arrangement.spacedBy(layoutConfig.topControlsSpacingDp.dp, Alignment.Start)
            TopBarAlignment.COMPACT_RIGHT -> Arrangement.spacedBy(layoutConfig.topControlsSpacingDp.dp, Alignment.End)
        }

        val shouldScroll = visibleItems.size > 5
        val topScrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (shouldScroll) Modifier.horizontalScroll(topScrollState) else Modifier
                ),
            horizontalArrangement = if (shouldScroll) Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally) else horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleItems.forEach { item ->
                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (item) {
                        TopControlItem.FLASH -> flashButton()
                        TopControlItem.TIMER -> timerAudioButton()
                        TopControlItem.GRID -> gridAssistButton()
                        TopControlItem.RESOLUTION -> primaryBadge()
                        TopControlItem.RAW -> secondaryBadge()
                        TopControlItem.PRO_EXP -> proExpButton()
                        TopControlItem.SETTINGS -> settingsButton()
                    }
                }
            }
        }
    }
}

