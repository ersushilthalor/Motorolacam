package com.example.camera.model

enum class LogBitDepth(val label: String, val bitDepth: Int) {
    OFF("Off", 8),
    BIT_8("8 bit", 8),
    BIT_10("10 bit", 10)
}

enum class CinemaColorProfile(
    val label: String,
    val description: String,
    val gammaName: String
) {
    FLAT_LOG("Flat (LOG)", "Logarithmic dynamic range curve for color grading", "Flat Log"),
    S_LOG3("S-Log3", "Sony S-Log3 cine curve preserving 14+ stops of dynamic range", "S-Log3"),
    C_LOG3("C-Log3", "Canon Log 3 filmic contrast curve with smooth highlight rolloff", "C-Log3"),
    V_LOG("V-Log", "Panasonic V-Log logarithmic transfer curve for maximum latitude", "V-Log"),
    REC_709("Rec.709", "ITU-R BT.709 standard broadcast display gamma", "BT.709"),
    REC_2020("Rec.2020", "ITU-R BT.2020 wide color gamut transfer curve", "BT.2020"),
    HLG("HLG", "ITU-R BT.2100 Hybrid Log-Gamma HDR profile", "HLG")
}

enum class CinemaColorSpace(val label: String) {
    REC_709("REC.709"),
    REC_2020("REC.2020"),
    DCI_P3("DCI-P3")
}

enum class ZebraThreshold(val label: String, val thresholdIre: Int) {
    OFF("Off", 0),
    IRE_70("70", 70),
    IRE_100("100", 100)
}

data class CinemaConfig(
    val logBitDepth: LogBitDepth = LogBitDepth.BIT_10,
    val colorProfile: CinemaColorProfile = CinemaColorProfile.FLAT_LOG,
    val colorSpace: CinemaColorSpace = CinemaColorSpace.REC_709,
    val isFocusPeakingEnabled: Boolean = false,
    val isWaveformEnabled: Boolean = false,
    val zebraThreshold: ZebraThreshold = ZebraThreshold.IRE_70,
    val videoFps: Int = 24, // 24 fps cinematic standard
    val selectedResolution: CameraResolution? = null
)

data class CinemaHardwareCapabilities(
    val supportsContrastCurve: Boolean = false,
    val supports10BitRecording: Boolean = false,
    val supportsHevc10Bit: Boolean = false,
    val supportsDynamicRangeProfiles: Boolean = false,
    val supportedFpsList: List<Int> = listOf(24, 30, 60),
    val supportedResolutions: List<CameraResolution> = emptyList(),
    val isHardwareLogSupported: Boolean = false,
    val is10BitAvailableOnHAL: Boolean = false
)
