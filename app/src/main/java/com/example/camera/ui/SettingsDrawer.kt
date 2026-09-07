package com.example.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.*

/**
 * Modern Light Mode Settings Sheet.
 * All settings are organized logically by category:
 * - Photo Settings (50MP computational, RAW DNG, Selfie preview, Grid assist)
 * - Video Settings (Resolution, FPS, Bitrate, Stabilization EIS/OIS, Audio)
 * - Cinema & Log Settings (Log profile, 10-bit capture, Focus peaking, Zebra stripes, Waveform)
 * - Hardware & System (Viewfinder quality, Detected lenses, Deep scan, Camera2 diagnostics)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawer(
    isOpen: Boolean,
    cameraMode: CameraMode,
    capabilities: HardwareCapabilities,
    availableLenses: List<LensInfo> = emptyList(),
    selectedLens: LensInfo? = null,
    selectedPhotoResolution: CameraResolution?,
    selectedVideoResolution: CameraResolution?,
    photoMegapixelMode: PhotoMegapixelMode = PhotoMegapixelMode.M12,
    videoFps: Int,
    videoBitrate: VideoBitrateOption,
    isVideoStabilizationEnabled: Boolean,
    isAudioEnabled: Boolean,
    isRawEnabled: Boolean,
    saveSelfieAsPreviewed: Boolean = true,
    gridType: GridType = GridType.NONE,
    cinemaConfig: CinemaConfig = CinemaConfig(),
    cinemaCapabilities: CinemaHardwareCapabilities = CinemaHardwareCapabilities(),
    viewfinderResolution: ViewfinderResolution = ViewfinderResolution.NORMAL,
    onLensSelected: (LensInfo) -> Unit = {},
    onForceDeepScan: () -> Unit = {},
    onPhotoResolutionSelected: (CameraResolution) -> Unit,
    onPhotoMegapixelModeSelected: (PhotoMegapixelMode) -> Unit = {},
    onVideoResolutionSelected: (CameraResolution) -> Unit,
    onViewfinderResolutionSelected: (ViewfinderResolution) -> Unit = {},
    onVideoFpsSelected: (Int) -> Unit,
    onVideoBitrateSelected: (VideoBitrateOption) -> Unit,
    onStabilizationToggle: (Boolean) -> Unit,
    onAudioToggle: () -> Unit,
    onRawToggle: () -> Unit,
    onSaveSelfieAsPreviewedToggle: (Boolean) -> Unit = {},
    onGridTypeSelected: (GridType) -> Unit = {},
    onCinemaConfigChange: (CinemaConfig) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFFF6F8FA),
        contentColor = Color(0xFF1F2937),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFFD1D5DB))
        },
        modifier = modifier.testTag("settings_bottom_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "Organized by category",
                            fontSize = 12.5.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ==========================================
            // 1. PHOTO SETTINGS CATEGORY
            // ==========================================
            item {
                CategoryCard(
                    title = "Photo Settings",
                    icon = Icons.Outlined.CameraAlt,
                    badge = "Photo & Portrait"
                ) {
                    // 50MP Computational Mode Toggle
                    LightToggleRow(
                        title = "50MP Computational Mode",
                        subtitle = "Single-frame native capture with high-resolution AI upscaling & edge enhancement",
                        isChecked = photoMegapixelMode == PhotoMegapixelMode.M50,
                        onToggle = {
                            val next = if (photoMegapixelMode == PhotoMegapixelMode.M50) {
                                PhotoMegapixelMode.M12
                            } else {
                                PhotoMegapixelMode.M50
                            }
                            onPhotoMegapixelModeSelected(next)
                        }
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // RAW DNG Toggle
                    if (capabilities.supportsRaw) {
                        LightToggleRow(
                            title = "RAW (DNG) Sensor Capture",
                            subtitle = "Saves uncompressed 16-bit linear sensor DNG alongside JPEG",
                            isChecked = isRawEnabled,
                            onToggle = { onRawToggle() }
                        )
                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    }

                    // Selfie as Previewed Toggle
                    LightToggleRow(
                        title = "Save Selfie as Previewed",
                        subtitle = "Saves front camera captures with exact preview orientation without inversion",
                        isChecked = saveSelfieAsPreviewed,
                        onToggle = { onSaveSelfieAsPreviewedToggle(!saveSelfieAsPreviewed) }
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Framing Grid Assist Selector
                    Text(
                        text = "Framing Grid Assist",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GridType.entries.forEach { type ->
                            val isSelected = gridType == type
                            LightSelectPill(
                                label = type.title,
                                isSelected = isSelected,
                                onClick = { onGridTypeSelected(type) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. VIDEO SETTINGS CATEGORY
            // ==========================================
            item {
                CategoryCard(
                    title = "Video Settings",
                    icon = Icons.Outlined.Videocam,
                    badge = "Video Capture"
                ) {
                    // Video Stabilization Toggle
                    val hasStab = capabilities.supportsEis || capabilities.supportsOis
                    LightToggleRow(
                        title = "Video Stabilization (OIS/EIS)",
                        subtitle = if (hasStab) {
                            "Hardware OIS: ${if (capabilities.supportsOis) "Active" else "Off"}, EIS: ${if (capabilities.supportsEis) "Active" else "Off"}"
                        } else {
                            "Device hardware does not report stabilization"
                        },
                        isChecked = isVideoStabilizationEnabled && hasStab,
                        enabled = hasStab,
                        onToggle = { onStabilizationToggle(!isVideoStabilizationEnabled) }
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Audio Recording Toggle
                    LightToggleRow(
                        title = "Record Audio",
                        subtitle = "High-fidelity AAC 48kHz stereo microphone capture",
                        isChecked = isAudioEnabled,
                        onToggle = { onAudioToggle() }
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Frame Rate (FPS) Selector
                    Text(
                        text = "Frame Rate (FPS)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        capabilities.supportedFpsRanges.forEach { fps ->
                            val isSelected = videoFps == fps
                            LightSelectPill(
                                label = "$fps FPS",
                                isSelected = isSelected,
                                onClick = { onVideoFpsSelected(fps) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Video Bitrate Selector
                    Text(
                        text = "Video Encoding Bitrate",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VideoBitrateOption.entries.forEach { option ->
                            val isSelected = videoBitrate == option
                            LightSelectPill(
                                label = option.title,
                                isSelected = isSelected,
                                onClick = { onVideoBitrateSelected(option) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 3. CINEMA & LOG SETTINGS CATEGORY
            // ==========================================
            item {
                CategoryCard(
                    title = "Cinema & LOG Settings",
                    icon = Icons.Outlined.MovieCreation,
                    badge = "Professional"
                ) {
                    // 10-Bit Log Capture Toggle
                    val can10Bit = cinemaCapabilities.supports10BitRecording
                    LightToggleRow(
                        title = "10-Bit Log Color Depth",
                        subtitle = if (can10Bit) "10-bit HLG/HDR profile active" else "8-bit standard profile (Hardware limited)",
                        isChecked = cinemaConfig.logBitDepth == LogBitDepth.BIT_10 && can10Bit,
                        enabled = can10Bit,
                        onToggle = {
                            val nextDepth = if (cinemaConfig.logBitDepth == LogBitDepth.BIT_10) {
                                LogBitDepth.BIT_8
                            } else {
                                LogBitDepth.BIT_10
                            }
                            onCinemaConfigChange(cinemaConfig.copy(logBitDepth = nextDepth))
                        }
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Focus Peaking Assist Toggle
                    LightToggleRow(
                        title = "Focus Peaking Assist",
                        subtitle = "Highlights in-focus sharp edges with high-contrast color outline",
                        isChecked = cinemaConfig.isFocusPeakingEnabled,
                        onToggle = {
                            onCinemaConfigChange(cinemaConfig.copy(isFocusPeakingEnabled = !cinemaConfig.isFocusPeakingEnabled))
                        }
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Zebra Stripes Assist Toggle
                    val isZebraOn = cinemaConfig.zebraThreshold != ZebraThreshold.OFF
                    LightToggleRow(
                        title = "Zebra Exposure Stripes",
                        subtitle = "Overlays diagonal stripes on overexposed highlight zones",
                        isChecked = isZebraOn,
                        onToggle = {
                            val next = if (isZebraOn) ZebraThreshold.OFF else ZebraThreshold.IRE_70
                            onCinemaConfigChange(cinemaConfig.copy(zebraThreshold = next))
                        }
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Waveform Monitor Toggle
                    LightToggleRow(
                        title = "Waveform Luminance Monitor",
                        subtitle = "Real-time luminance IRE histogram monitor overlay",
                        isChecked = cinemaConfig.isWaveformEnabled,
                        onToggle = {
                            onCinemaConfigChange(cinemaConfig.copy(isWaveformEnabled = !cinemaConfig.isWaveformEnabled))
                        }
                    )

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Cinema Color Profile Selector
                    Text(
                        text = "Hardware Color Profile",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CinemaColorProfile.entries.forEach { profile ->
                            val isSelected = cinemaConfig.colorProfile == profile
                            LightOptionRow(
                                title = profile.label,
                                subtitle = profile.description,
                                isSelected = isSelected,
                                onClick = {
                                    onCinemaConfigChange(cinemaConfig.copy(colorProfile = profile))
                                }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 4. HARDWARE & DIAGNOSTICS CATEGORY
            // ==========================================
            item {
                CategoryCard(
                    title = "Hardware & Diagnostics",
                    icon = Icons.Outlined.Memory,
                    badge = "Camera2 HAL"
                ) {
                    // Viewfinder Quality
                    Text(
                        text = "Viewfinder Resolution",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ViewfinderResolution.entries.forEach { resOption ->
                            val isSelected = viewfinderResolution == resOption
                            LightSelectPill(
                                label = resOption.label,
                                isSelected = isSelected,
                                onClick = { onViewfinderResolutionSelected(resOption) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Hardware Lens Deep Scan
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Deep Scan Aux Lenses",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                text = "Bypass OEM package filter to discover ultra-wide, telephoto, and macro cameras",
                                fontSize = 11.5.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Button(
                            onClick = onForceDeepScan,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1A73E8),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("Scan", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    // Diagnostic Hardware Specs
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LightSpecItem("Manual Sensor Control", if (capabilities.supportsManualSensor) "Supported" else "Limited")
                        LightSpecItem("RAW Sensor Capture", if (capabilities.supportsRaw) "Supported (DNG)" else "Not Available")
                        LightSpecItem("Optical Stabilization (OIS)", if (capabilities.supportsOis) "Supported" else "No")
                        LightSpecItem("Electronic Stabilization (EIS)", if (capabilities.supportsEis) "Supported" else "No")
                        LightSpecItem("Max Digital Zoom", "%.1fx".format(capabilities.maxZoom))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ============================================================================
// LIGHT MODE COMPONENTS & STYLES
// ============================================================================

@Composable
private fun CategoryCard(
    title: String,
    icon: ImageVector,
    badge: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F0FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF3F4F6)
                ) {
                    Text(
                        text = badge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun LightToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    enabled: Boolean = true,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onToggle() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) Color(0xFF1F2937) else Color(0xFF9CA3AF),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF6B7280),
                fontSize = 11.5.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF1A73E8),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB)
            )
        )
    }
}

@Composable
private fun LightSelectPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF1A73E8) else Color(0xFFF3F4F6),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF1A73E8) else Color(0xFFE5E7EB)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF374151),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LightOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFE8F0FE) else Color(0xFFF9FAFB),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF1A73E8) else Color(0xFFE5E7EB)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isSelected) Color(0xFF1A73E8) else Color(0xFF1F2937),
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF6B7280),
                    fontSize = 11.5.sp
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LightSpecItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF6B7280),
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color(0xFF1F2937),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
