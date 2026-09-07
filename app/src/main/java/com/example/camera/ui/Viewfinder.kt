package com.example.camera.ui

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camera.model.CameraMode
import com.example.camera.model.GridType

@Composable
fun Viewfinder(
    aspectRatio: Float,
    gridType: GridType,
    focusRingPoint: Offset?,
    isAeLocked: Boolean,
    isAfLocked: Boolean,
    isFrontCamera: Boolean = false,
    cameraMode: CameraMode = CameraMode.PHOTO,
    onSurfaceTextureAvailable: (SurfaceTexture?) -> Unit,
    onTapToFocus: (Offset, Float, Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var textureViewInstance by remember { mutableStateOf<TextureView?>(null) }
    var currentScale by remember { mutableFloatStateOf(1.0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("viewfinder_container"),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        // Aspect ratio is Height / Width in Portrait mode (e.g. 4/3 = 1.333, 16/9 = 1.777, 20/9 = 2.222)
        // Ensure preview is letterboxed/pillarboxed inside container without any stretch/distortion
        val targetWidth: androidx.compose.ui.unit.Dp
        val targetHeight: androidx.compose.ui.unit.Dp

        val containerRatio = containerHeight.value / containerWidth.value

        if (containerRatio > aspectRatio) {
            // Container is taller than camera preview -> fit width, letterbox top/bottom
            targetWidth = containerWidth
            targetHeight = containerWidth * aspectRatio
        } else {
            // Container is wider than camera preview -> fit height, pillarbox left/right
            targetHeight = containerHeight
            targetWidth = containerHeight / aspectRatio
        }

        Box(
            modifier = Modifier
                .size(width = targetWidth, height = targetHeight)
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        currentScale = (currentScale * zoom).coerceIn(1.0f, 8.0f)
                        onZoomChange(currentScale)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val normX = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val normY = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        onTapToFocus(offset, normX, normY)
                    }
                }
        ) {
            // TextureView Preview
            AndroidView(
                factory = { context ->
                    TextureView(context).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                onSurfaceTextureAvailable(st)
                            }
                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                onSurfaceTextureAvailable(null)
                                return true
                            }
                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                        textureViewInstance = this
                    }
                },
                update = { textureView ->
                    // The live viewfinder must remain exactly as the camera normally previews it.
                    // Do not apply any horizontal flip, matrix transformation, or scale transformation.
                    textureView.scaleX = 1f
                    textureView.scaleY = 1f
                },
                modifier = Modifier.fillMaxSize()
            )

            // Grid Overlay
            if (gridType != GridType.NONE) {
                CameraGridOverlay(gridType = gridType, modifier = Modifier.fillMaxSize())
            }

            // Tap to focus animated ring
            AnimatedVisibility(
                visible = focusRingPoint != null,
                enter = fadeIn() + scaleIn(initialScale = 1.3f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                focusRingPoint?.let { point ->
                    FocusRingIndicator(
                        point = point,
                        isAeLocked = isAeLocked,
                        isAfLocked = isAfLocked
                    )
                }
            }
        }
    }
}

@Composable
fun FocusRingIndicator(
    point: Offset,
    isAeLocked: Boolean,
    isAfLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "focusPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset(
                    x = (point.x - 36).dp,
                    y = (point.y - 36).dp
                )
                .size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ringColor = if (isAeLocked || isAfLocked) Color(0xFFFFD54F) else Color(0xFFEEEEEE)
                drawCircle(
                    color = ringColor.copy(alpha = alpha),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Small crosshair in center
                drawLine(
                    color = ringColor.copy(alpha = 0.7f),
                    start = Offset(size.width / 2f - 6.dp.toPx(), size.height / 2f),
                    end = Offset(size.width / 2f + 6.dp.toPx(), size.height / 2f),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = ringColor.copy(alpha = 0.7f),
                    start = Offset(size.width / 2f, size.height / 2f - 6.dp.toPx()),
                    end = Offset(size.width / 2f, size.height / 2f + 6.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            if (isAeLocked || isAfLocked) {
                Row(
                    modifier = Modifier
                        .offset(y = 42.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isAeLocked && isAfLocked) "AE/AF LOCK" else if (isAeLocked) "AE LOCK" else "AF LOCK",
                        color = Color(0xFFFFD54F),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CameraGridOverlay(
    gridType: GridType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val gridColor = Color.White.copy(alpha = 0.35f)
        val strokeWidth = 1.dp.toPx()

        when (gridType) {
            GridType.THIRDS -> {
                // Vertical lines
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
                drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth)
                // Horizontal lines
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
                drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth)
            }
            GridType.GOLDEN -> {
                val phi = 0.618f
                val left = w * (1f - phi)
                val right = w * phi
                val top = h * (1f - phi)
                val bottom = h * phi

                drawLine(gridColor, Offset(left, 0f), Offset(left, h), strokeWidth)
                drawLine(gridColor, Offset(right, 0f), Offset(right, h), strokeWidth)
                drawLine(gridColor, Offset(0f, top), Offset(w, top), strokeWidth)
                drawLine(gridColor, Offset(0f, bottom), Offset(w, bottom), strokeWidth)
            }
            GridType.SQUARE -> {
                val squareDim = minOf(w, h)
                val startX = (w - squareDim) / 2f
                val startY = (h - squareDim) / 2f
                drawRect(
                    color = gridColor,
                    topLeft = Offset(startX, startY),
                    size = Size(squareDim, squareDim),
                    style = Stroke(strokeWidth)
                )
            }
            GridType.LEVEL -> {
                // Center horizon line with dashed styling
                drawLine(
                    color = Color(0xFF64FFDA).copy(alpha = 0.75f),
                    start = Offset(w * 0.2f, h / 2f),
                    end = Offset(w * 0.8f, h / 2f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )
                // Center level dot
                drawCircle(
                    color = Color(0xFF64FFDA),
                    radius = 3.dp.toPx(),
                    center = Offset(w / 2f, h / 2f)
                )
            }
            GridType.NONE -> {}
        }
    }
}
