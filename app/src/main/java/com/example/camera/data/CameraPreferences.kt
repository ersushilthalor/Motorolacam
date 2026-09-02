package com.example.camera.data

import android.content.Context
import android.content.SharedPreferences
import com.example.camera.model.CameraMode
import com.example.camera.model.ColorProfile
import com.example.camera.model.FlashMode
import com.example.camera.model.FocusMode
import com.example.camera.model.GridType
import com.example.camera.model.TimerMode
import com.example.camera.model.VideoBitrateOption
import com.example.camera.model.WhiteBalanceMode

/**
 * Persists user camera and video settings across sessions.
 */
class CameraPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pro_camera_user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CAMERA_MODE = "pref_camera_mode"
        private const val KEY_FLASH_MODE = "pref_flash_mode"
        private const val KEY_TIMER_MODE = "pref_timer_mode"
        private const val KEY_GRID_TYPE = "pref_grid_type"
        private const val KEY_RAW_ENABLED = "pref_raw_enabled"
        private const val KEY_VIDEO_QUALITY = "pref_video_quality" // e.g. "4K 30", "1080p 30", "1080p 60", "720p 30"
        private const val KEY_VIDEO_WIDTH = "pref_video_width"
        private const val KEY_VIDEO_HEIGHT = "pref_video_height"
        private const val KEY_VIDEO_FPS = "pref_video_fps"
        private const val KEY_VIDEO_BITRATE = "pref_video_bitrate"
        private const val KEY_VIDEO_STABILIZATION = "pref_video_stabilization"
        private const val KEY_AUDIO_ENABLED = "pref_audio_enabled"
        private const val KEY_COLOR_PROFILE = "pref_color_profile"
        private const val KEY_WHITE_BALANCE = "pref_white_balance"
        private const val KEY_FOCUS_MODE = "pref_focus_mode"
        private const val KEY_LAST_FACING = "pref_last_facing"
        private const val KEY_PORTRAIT_BLUR = "pref_portrait_blur"
        private const val KEY_PORTRAIT_APERTURE = "pref_portrait_aperture"
    }

    var cameraMode: CameraMode
        get() {
            val name = prefs.getString(KEY_CAMERA_MODE, CameraMode.PHOTO.name) ?: CameraMode.PHOTO.name
            return try { CameraMode.valueOf(name) } catch (e: Exception) { CameraMode.PHOTO }
        }
        set(value) = prefs.edit().putString(KEY_CAMERA_MODE, value.name).apply()

    var flashMode: FlashMode
        get() {
            val name = prefs.getString(KEY_FLASH_MODE, FlashMode.OFF.name) ?: FlashMode.OFF.name
            return try { FlashMode.valueOf(name) } catch (e: Exception) { FlashMode.OFF }
        }
        set(value) = prefs.edit().putString(KEY_FLASH_MODE, value.name).apply()

    var timerMode: TimerMode
        get() {
            val name = prefs.getString(KEY_TIMER_MODE, TimerMode.OFF.name) ?: TimerMode.OFF.name
            return try { TimerMode.valueOf(name) } catch (e: Exception) { TimerMode.OFF }
        }
        set(value) = prefs.edit().putString(KEY_TIMER_MODE, value.name).apply()

    var gridType: GridType
        get() {
            val name = prefs.getString(KEY_GRID_TYPE, GridType.NONE.name) ?: GridType.NONE.name
            return try { GridType.valueOf(name) } catch (e: Exception) { GridType.NONE }
        }
        set(value) = prefs.edit().putString(KEY_GRID_TYPE, value.name).apply()

    var isRawEnabled: Boolean
        get() = prefs.getBoolean(KEY_RAW_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_RAW_ENABLED, value).apply()

    var videoWidth: Int
        get() = prefs.getInt(KEY_VIDEO_WIDTH, 3840) // Default 4K if supported, will gracefully adapt
        set(value) = prefs.edit().putInt(KEY_VIDEO_WIDTH, value).apply()

    var videoHeight: Int
        get() = prefs.getInt(KEY_VIDEO_HEIGHT, 2160)
        set(value) = prefs.edit().putInt(KEY_VIDEO_HEIGHT, value).apply()

    var videoFps: Int
        get() = prefs.getInt(KEY_VIDEO_FPS, 30)
        set(value) = prefs.edit().putInt(KEY_VIDEO_FPS, value).apply()

    var videoBitrate: VideoBitrateOption
        get() {
            val name = prefs.getString(KEY_VIDEO_BITRATE, VideoBitrateOption.AUTO.name) ?: VideoBitrateOption.AUTO.name
            return try { VideoBitrateOption.valueOf(name) } catch (e: Exception) { VideoBitrateOption.AUTO }
        }
        set(value) = prefs.edit().putString(KEY_VIDEO_BITRATE, value.name).apply()

    var isVideoStabilizationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIDEO_STABILIZATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIDEO_STABILIZATION, value).apply()

    var isAudioEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUDIO_ENABLED, value).apply()

    var colorProfile: ColorProfile
        get() {
            val name = prefs.getString(KEY_COLOR_PROFILE, ColorProfile.STANDARD.name) ?: ColorProfile.STANDARD.name
            return try { ColorProfile.valueOf(name) } catch (e: Exception) { ColorProfile.STANDARD }
        }
        set(value) = prefs.edit().putString(KEY_COLOR_PROFILE, value.name).apply()

    var whiteBalance: WhiteBalanceMode
        get() {
            val name = prefs.getString(KEY_WHITE_BALANCE, WhiteBalanceMode.AUTO.name) ?: WhiteBalanceMode.AUTO.name
            return try { WhiteBalanceMode.valueOf(name) } catch (e: Exception) { WhiteBalanceMode.AUTO }
        }
        set(value) = prefs.edit().putString(KEY_WHITE_BALANCE, value.name).apply()

    var focusMode: FocusMode
        get() {
            val name = prefs.getString(KEY_FOCUS_MODE, FocusMode.CONTINUOUS.name) ?: FocusMode.CONTINUOUS.name
            return try { FocusMode.valueOf(name) } catch (e: Exception) { FocusMode.CONTINUOUS }
        }
        set(value) = prefs.edit().putString(KEY_FOCUS_MODE, value.name).apply()

    var lastFacing: Int
        get() = prefs.getInt(KEY_LAST_FACING, android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK)
        set(value) = prefs.edit().putInt(KEY_LAST_FACING, value).apply()

    var portraitBlurStrength: Float
        get() = prefs.getFloat(KEY_PORTRAIT_BLUR, 60f)
        set(value) = prefs.edit().putFloat(KEY_PORTRAIT_BLUR, value).apply()

    var portraitAperture: String
        get() = prefs.getString(KEY_PORTRAIT_APERTURE, "f/1.4") ?: "f/1.4"
        set(value) = prefs.edit().putString(KEY_PORTRAIT_APERTURE, value).apply()
}
