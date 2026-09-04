package com.example.camera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.model.*

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
    videoFps: Int,
    videoBitrate: VideoBitrateOption,
    isVideoStabilizationEnabled: Boolean,
    isAudioEnabled: Boolean,
    isRawEnabled: Boolean,
    saveSelfieAsPreviewed: Boolean = true,
    hdrState: com.example.camera.model.VideoHdrState = com.example.camera.model.VideoHdrState(),
    viewfinderResolution: ViewfinderResolution = ViewfinderResolution.NORMAL,
    onLensSelected: (LensInfo) -> Unit = {},
    onForceDeepScan: () -> Unit = {},
    onPhotoResolutionSelected: (CameraResolution) -> Unit,
    onVideoResolutionSelected: (CameraResolution) -> Unit,
    onViewfinderResolutionSelected: (ViewfinderResolution) -> Unit = {},
    onVideoFpsSelected: (Int) -> Unit,
    onVideoBitrateSelected: (VideoBitrateOption) -> Unit,
    onStabilizationToggle: (Boolean) -> Unit,
    onAudioToggle: () -> Unit,
    onRawToggle: () -> Unit,
    onSaveSelfieAsPreviewedToggle: (Boolean) -> Unit = {},
    onHdrModeSelected: (com.example.camera.model.VideoHdrMode) -> Unit = {},
    onHdrIntensityChanged: (Int) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF141416),
        contentColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f))
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Camera Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Section: Photo Resolutions
            if (cameraMode == CameraMode.PHOTO) {
                item {
                    SettingsSectionHeader(title = "Photo Resolution (Camera2 Output)")
                }

                items(capabilities.supportedPhotoResolutions) { res ->
                    val isSelected = selectedPhotoResolution?.width == res.width && selectedPhotoResolution?.height == res.height
                    ResolutionItem(
                        resolution = res,
                        isSelected = isSelected,
                        onClick = {
                            onPhotoResolutionSelected(res)
                        }
                    )
                }

                if (capabilities.supportsRaw) {
                    item {
                        SettingsToggleRow(
                            title = "RAW (DNG) Sensor Capture",
                            subtitle = "Saves uncompressed RAW DNG alongside full-res JPEG",
                            isChecked = isRawEnabled,
                            onToggle = { onRawToggle() }
                        )
                    }
                }
            }

            // Section: Selfie & Mirroring Preferences
            item {
                SettingsSectionHeader(title = "Selfie & Orientation")
                SettingsToggleRow(
                    title = "Selfie as Previewed Without Flipping",
                    subtitle = "Saves front camera photos & videos with exact preview orientation without unwanted flipping",
                    isChecked = saveSelfieAsPreviewed,
                    onToggle = { onSaveSelfieAsPreviewedToggle(!saveSelfieAsPreviewed) }
                )
            }

            // Section: Viewfinder Preview Resolution
            item {
                SettingsSectionHeader(title = "Viewfinder Resolution")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ViewfinderResolution.entries.forEach { resOption ->
                        val isSelected = viewfinderResolution == resOption
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFFFD54F).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFFFD54F) else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onViewfinderResolutionSelected(resOption) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = resOption.label,
                                        color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    Text(
                                        text = resOption.description,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Video Configuration
            if (cameraMode == CameraMode.VIDEO) {
                item {
                    SettingsSectionHeader(title = "Video Resolution")
                }

                items(capabilities.supportedVideoResolutions) { res ->
                    val isSelected = selectedVideoResolution?.width == res.width && selectedVideoResolution?.height == res.height
                    ResolutionItem(
                        resolution = res,
                        isSelected = isSelected,
                        onClick = {
                            onVideoResolutionSelected(res)
                        }
                    )
                }

                // Frame Rate
                item {
                    SettingsSectionHeader(title = "Frame Rate (FPS)")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        capabilities.supportedFpsRanges.forEach { fps ->
                            val isSelected = videoFps == fps
                            FilterChip(
                                selected = isSelected,
                                onClick = { onVideoFpsSelected(fps) },
                                label = { Text("$fps FPS", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFD54F),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Real-Time Video HDR
                item {
                    SettingsSectionHeader(title = "Real-Time Video HDR")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Intelligently balances shadows, highlights, and contrast in real time on both live viewfinder and recorded video. Features live adaptive spatial + temporal noise reduction.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        // Mode Selector Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.camera.model.VideoHdrMode.entries.forEach { mode ->
                                val isSelected = hdrState.mode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onHdrModeSelected(mode) },
                                    label = { Text(mode.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFD54F),
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }

                        if (hdrState.mode == com.example.camera.model.VideoHdrMode.MANUAL) {
                            Column(modifier = Modifier.padding(top = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("HDR Dynamic Range Intensity", color = Color.White, fontSize = 12.sp)
                                    Text("${hdrState.manualIntensity}%", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = hdrState.manualIntensity.toFloat(),
                                    onValueChange = { onHdrIntensityChanged(it.toInt()) },
                                    valueRange = 0f..100f,
                                    steps = 99,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFFFD54F),
                                        activeTrackColor = Color(0xFFFFD54F),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                }

                // Bitrate
                item {
                    SettingsSectionHeader(title = "Video Bitrate")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        VideoBitrateOption.entries.forEach { option ->
                            val isSelected = videoBitrate == option
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFFFD54F).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFFFD54F) else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onVideoBitrateSelected(option) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option.title,
                                        color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Video Stabilization
                item {
                    val hasStab = capabilities.supportsEis || capabilities.supportsOis
                    SettingsToggleRow(
                        title = "Video Stabilization",
                        subtitle = if (hasStab) {
                            "Hardware EIS: ${if (capabilities.supportsEis) "Active" else "None"}, OIS: ${if (capabilities.supportsOis) "Active" else "None"}"
                        } else {
                            "Not supported by device hardware"
                        },
                        isChecked = isVideoStabilizationEnabled && hasStab,
                        enabled = hasStab,
                        onToggle = { onStabilizationToggle(!isVideoStabilizationEnabled) }
                    )
                }

                // Audio Recording
                item {
                    SettingsToggleRow(
                        title = "Record Audio",
                        subtitle = "High-fidelity AAC 48kHz stereo microphone capture",
                        isChecked = isAudioEnabled,
                        onToggle = { onAudioToggle() }
                    )
                }
            }

            // Section: Hardware Diagnostic / Capabilities
            item {
                SettingsSectionHeader(title = "Hardware Capabilities (Camera2 Verified)")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CapabilityItem("Manual Sensor Control", if (capabilities.supportsManualSensor) "Supported" else "Limited")
                        CapabilityItem("RAW Sensor Capture", if (capabilities.supportsRaw) "Supported (DNG)" else "Not Available")
                        CapabilityItem("ISO Sensitivity Range", "${capabilities.minIso} – ${capabilities.maxIso}")
                        CapabilityItem("Exposure Time Range", "${capabilities.minExposureTimeNs / 1_000_000f}ms – ${capabilities.maxExposureTimeNs / 1_000_000_000f}s")
                        CapabilityItem("Exposure Comp Steps", "${capabilities.minExposureCompensation}..${capabilities.maxExposureCompensation} (step ${capabilities.exposureCompensationStep})")
                        CapabilityItem("Optical Stabilization (OIS)", if (capabilities.supportsOis) "Supported" else "No")
                        CapabilityItem("Electronic Stabilization (EIS)", if (capabilities.supportsEis) "Supported" else "No")
                        CapabilityItem("Max Digital Zoom", "%.1fx".format(capabilities.maxZoom))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color(0xFFFFD54F),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ResolutionItem(
    resolution: CameraResolution,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFFFD54F).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFFFD54F) else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = resolution.displayLabel,
                    color = if (isSelected) Color(0xFFFFD54F) else Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "Ratio: ${resolution.aspectRatioLabel} · Output: ${resolution.width} x ${resolution.height}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
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
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFFFFD54F),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun CapabilityItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
