package com.example.camera.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.camera.model.*

/**
 * Dedicated Cinema Mode Settings Window.
 *
 * Visually matches reference specification:
 * - Premium dark translucent panel with rounded corners and subtle border
 * - Top center CINEMA title with yellow accent bar
 * - Subtitle with film strip icon and (i) info modal
 * - LOG section: Real 8-bit / 10-bit recording selector (honest hardware HAL capability check)
 * - Color Profile dropdown: Flat (LOG), S-Log3, C-Log3, V-Log, Rec.709, Rec.2020, HLG
 * - Color Space dropdown: REC.709, REC.2020, DCI-P3
 * - Focus Assist: Peaking, Waveform, Zebras
 * - Zebras selector: Off, 70 IRE, 100 IRE
 * - Video Quality: Supported resolutions & cinema 24fps/30fps/60fps framerates
 */
@Composable
fun CinemaSettingsWindow(
    config: CinemaConfig,
    capabilities: CinemaHardwareCapabilities,
    onConfigChange: (CinemaConfig) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isColorProfileMenuOpen by remember { mutableStateOf(false) }
    var isColorSpaceMenuOpen by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF216161C))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("cinema_settings_window")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 370.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Title with Yellow Accent Pill
            Text(
                text = "CINEMA",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFFFD54F))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Subtitle row with Film Icon & Info Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Movie,
                        contentDescription = "Cinema Mode",
                        tint = Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Professional video with Log, colour profiles and cinematic controls.",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("cinema_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Information",
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 3. Row 1: LOG and Color Profile
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: LOG (Off, 8 bit, 10 bit)
                Column(modifier = Modifier.weight(1.05f)) {
                    Text(
                        text = "LOG",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF222228))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LogBitDepth.entries.forEach { bitDepth ->
                            val isSelected = config.logBitDepth == bitDepth
                            val isSupported = when (bitDepth) {
                                LogBitDepth.BIT_10 -> capabilities.supports10BitRecording
                                else -> true
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFFFFD54F) else Color.Transparent
                                    )
                                    .clickable(enabled = isSupported) {
                                        onConfigChange(config.copy(logBitDepth = bitDepth))
                                    }
                                    .padding(vertical = 7.dp)
                                    .testTag("log_option_${bitDepth.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bitDepth.label,
                                    color = when {
                                        !isSupported -> Color.White.copy(alpha = 0.30f)
                                        isSelected -> Color.Black
                                        else -> Color.White.copy(alpha = 0.85f)
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (!capabilities.supports10BitRecording) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "10-bit unavailable on this HAL",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 9.sp
                        )
                    }
                }

                // Right Column: Color Profile & Color Space Dropdowns
                Column(modifier = Modifier.weight(1.45f)) {
                    Text(
                        text = "Color Profile",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Profile Dropdown (Flat LOG, S-Log3, C-Log3, V-Log, Rec.709, Rec.2020, HLG)
                        Box(modifier = Modifier.weight(1.3f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF222228))
                                    .border(
                                        1.dp,
                                        if (isColorProfileMenuOpen) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { isColorProfileMenuOpen = true }
                                    .padding(horizontal = 8.dp, vertical = 7.dp)
                                    .testTag("color_profile_dropdown_trigger"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = config.colorProfile.label,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select profile",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isColorProfileMenuOpen,
                                onDismissRequest = { isColorProfileMenuOpen = false },
                                modifier = Modifier
                                    .background(Color(0xFF1C1C22))
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                    .width(170.dp)
                            ) {
                                CinemaColorProfile.entries.forEach { profile ->
                                    val isSelected = config.colorProfile == profile
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = profile.label,
                                                color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onConfigChange(config.copy(colorProfile = profile))
                                            isColorProfileMenuOpen = false
                                        },
                                        modifier = Modifier.testTag("color_profile_item_${profile.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // Color Space Dropdown (REC.709, REC.2020, DCI-P3)
                        Box(modifier = Modifier.weight(1.0f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF222228))
                                    .border(
                                        1.dp,
                                        if (isColorSpaceMenuOpen) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { isColorSpaceMenuOpen = true }
                                    .padding(horizontal = 7.dp, vertical = 7.dp)
                                    .testTag("color_space_dropdown_trigger"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = config.colorSpace.label,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select gamut",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isColorSpaceMenuOpen,
                                onDismissRequest = { isColorSpaceMenuOpen = false },
                                modifier = Modifier
                                    .background(Color(0xFF1C1C22))
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                            ) {
                                CinemaColorSpace.entries.forEach { space ->
                                    val isSelected = config.colorSpace == space
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = space.label,
                                                color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            onConfigChange(config.copy(colorSpace = space))
                                            isColorSpaceMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // RAW SENSOR LOG PIPELINE Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F1F26))
                    .border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("cinema_raw_pipeline_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (config.isRawSensorLogPipeline) Color(0xFFFFD54F) else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RAW SENSOR LOG PIPELINE",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Switch(
                        checked = config.isRawSensorLogPipeline,
                        onCheckedChange = { onConfigChange(config.copy(isRawSensorLogPipeline = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color(0xFFFFD54F),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color(0xFF2C2C34)
                        ),
                        modifier = Modifier
                            .scale(0.75f)
                            .testTag("cinema_raw_pipeline_switch")
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (config.isRawSensorLogPipeline)
                        "Direct Raw Sensor → Log curve. Consumer edge oversharpening & noise smearing bypassed. 100 Mbps intra-frame mastering."
                    else
                        "Standard ISP consumer filters active (consumer sharpening & temporal denoise applied).",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Row 2: Focus Assist & Zebras
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Focus Assist icons: Peaking [◉], Waveform 〰, Zebras ///
                Column(modifier = Modifier.weight(1.05f)) {
                    Text(
                        text = "Focus Assist",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Focus Peaking Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (config.isFocusPeakingEnabled) Color(0xFF353020) else Color(0xFF222228))
                                .border(
                                    1.dp,
                                    if (config.isFocusPeakingEnabled) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onConfigChange(config.copy(isFocusPeakingEnabled = !config.isFocusPeakingEnabled))
                                }
                                .testTag("focus_peaking_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            FocusBracketsIcon(tint = if (config.isFocusPeakingEnabled) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.85f))
                        }

                        // 2. Waveform Parade Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (config.isWaveformEnabled) Color(0xFF353020) else Color(0xFF222228))
                                .border(
                                    1.dp,
                                    if (config.isWaveformEnabled) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onConfigChange(config.copy(isWaveformEnabled = !config.isWaveformEnabled))
                                }
                                .testTag("waveform_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            WaveformIcon(tint = if (config.isWaveformEnabled) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.85f))
                        }

                        // 3. Zebras Active Visual Shortcut
                        val isZebraActive = config.zebraThreshold != ZebraThreshold.OFF
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isZebraActive) Color(0xFF353020) else Color(0xFF222228))
                                .border(
                                    1.dp,
                                    if (isZebraActive) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    val nextThreshold = when (config.zebraThreshold) {
                                        ZebraThreshold.OFF -> ZebraThreshold.IRE_70
                                        ZebraThreshold.IRE_70 -> ZebraThreshold.IRE_100
                                        ZebraThreshold.IRE_100 -> ZebraThreshold.OFF
                                    }
                                    onConfigChange(config.copy(zebraThreshold = nextThreshold))
                                }
                                .testTag("zebras_quick_toggle"),
                            contentAlignment = Alignment.Center
                        ) {
                            ZebraIcon(tint = if (isZebraActive) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.85f))
                        }
                    }
                }

                // Zebras Pills: Off, 70, 100
                Column(modifier = Modifier.weight(1.45f)) {
                    Text(
                        text = "Zebras",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF222228))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ZebraThreshold.entries.forEach { threshold ->
                            val isSelected = config.zebraThreshold == threshold
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFFD54F) else Color.Transparent)
                                    .clickable {
                                        onConfigChange(config.copy(zebraThreshold = threshold))
                                    }
                                    .padding(vertical = 7.dp)
                                    .testTag("zebra_threshold_${threshold.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = threshold.label,
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Video Quality Section: Resolution & FPS
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Quality",
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Hardware ISP indicator
                    Text(
                        text = if (capabilities.isHardwareLogSupported) "✓ Hardware ISP Active" else "✓ Adaptive Gamma Active",
                        color = Color(0xFFFFD54F),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Supported Resolutions
                    val targetResolutions = capabilities.supportedResolutions.ifEmpty {
                        listOf(CameraResolution(3840, 2160), CameraResolution(1920, 1080), CameraResolution(1280, 720))
                    }
                    val mainResolutions = targetResolutions
                        .filter { it.width in listOf(3840, 1920, 1280) }
                        .distinctBy { it.width }
                        .sortedByDescending { it.width }
                        .ifEmpty { targetResolutions.take(3) }

                    Row(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF222228))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        mainResolutions.forEach { res ->
                            val isSelected = (config.selectedResolution?.width == res.width) ||
                                    (config.selectedResolution == null && res.width == 1920)
                            val label = when (res.width) {
                                3840 -> "4K"
                                1920 -> "1080p"
                                1280 -> "720p"
                                else -> "${res.height}p"
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFFD54F) else Color.Transparent)
                                    .clickable {
                                        onConfigChange(config.copy(selectedResolution = res))
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // FPS Pills: 24 (Cinema), 30, 60
                    val targetFps = capabilities.supportedFpsList.ifEmpty { listOf(24, 30, 60) }
                    Row(
                        modifier = Modifier
                            .weight(1.0f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF222228))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        targetFps.forEach { fps ->
                            val isSelected = config.videoFps == fps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFFFD54F) else Color.Transparent)
                                    .clickable {
                                        onConfigChange(config.copy(videoFps = fps))
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${fps}fps",
                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.85f),
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Info Dialog
    if (showInfoDialog) {
        Dialog(onDismissRequest = { showInfoDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E1E26))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cinema Pipeline Architecture",
                            color = Color(0xFFFFD54F),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showInfoDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Optical Log Curves: Real 64-point tonemap contrast curves are injected directly into Camera2's hardware ISP. Both the live viewfinder and the recorded video receive genuine Log transfer encoding.\n\n" +
                                "• 8-bit vs 10-bit: True 10-bit HEVC Main 10 profile recording is enabled only when physically supported by your device's Camera2 DynamicRangeProfiles and hardware video encoder.\n\n" +
                                "• Focus & Exposure Tools: Focus peaking highlights high-frequency contrast edges. Zebras flag overexposed highlights at 70% (skin) or 100% (clipping) IRE.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Got it", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- Helper Custom Vectors matching Reference Screenshot ----------------

@Composable
fun FocusBracketsIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        val arm = w * 0.28f

        // Top-left bracket
        drawLine(tint, Offset(0f, arm), Offset(0f, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(0f, 0f), Offset(arm, 0f), strokeWidth = stroke, cap = StrokeCap.Round)

        // Top-right bracket
        drawLine(tint, Offset(w - arm, 0f), Offset(w, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w, 0f), Offset(w, arm), strokeWidth = stroke, cap = StrokeCap.Round)

        // Bottom-left bracket
        drawLine(tint, Offset(0f, h - arm), Offset(0f, h), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(0f, h), Offset(arm, h), strokeWidth = stroke, cap = StrokeCap.Round)

        // Bottom-right bracket
        drawLine(tint, Offset(w - arm, h), Offset(w, h), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w, h), Offset(w, h - arm), strokeWidth = stroke, cap = StrokeCap.Round)

        // Center target dot
        drawCircle(tint, radius = 1.6.dp.toPx(), center = Offset(w / 2f, h / 2f))
    }
}

@Composable
fun WaveformIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.6.dp.toPx()
        val path = Path().apply {
            moveTo(0f, size.height * 0.55f)
            lineTo(size.width * 0.25f, size.height * 0.55f)
            lineTo(size.width * 0.40f, size.height * 0.15f)
            lineTo(size.width * 0.60f, size.height * 0.85f)
            lineTo(size.width * 0.75f, size.height * 0.40f)
            lineTo(size.width, size.height * 0.40f)
        }
        drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun ZebraIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.8.dp.toPx()
        val w = size.width
        val h = size.height

        // 3 diagonal stripes
        drawLine(tint, Offset(0f, h * 0.7f), Offset(w * 0.7f, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(0f, h), Offset(w, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.3f, h), Offset(w, h * 0.3f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
