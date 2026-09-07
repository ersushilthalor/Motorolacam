package com.example.camera.engine

import android.util.Log
import java.io.File
import java.io.RandomAccessFile

/**
 * Utility to apply horizontal mirror transformation to MP4 video tracks in-place.
 *
 * For front-facing camera recordings in Video and Cinema modes:
 * The live viewfinder preview is shown naturally without artificial flips.
 * To make the recorded/saved video match the viewfinder orientation exactly,
 * this utility updates the ISO/IEC 14496-12 3x3 transformation matrix in the video track's
 * `tkhd` (Track Header) box.
 *
 * This performs an instantaneous metadata transform on the container level,
 * perfectly mirroring the video display output to match the selfie preview
 * without re-encoding or degrading video quality.
 */
object Mp4VideoMirrorProcessor {
    private const val TAG = "Mp4MirrorProcessor"

    /**
     * Inspects the MP4 file, finds all video tracks in the `moov` container,
     * and updates their `tkhd` transformation matrix to apply a horizontal reflection (flip).
     *
     * @param file The MP4 file to transform in-place.
     * @return true if at least one video track header was successfully transformed.
     */
    fun applyHorizontalFlip(file: File): Boolean {
        if (!file.exists() || file.length() < 32) {
            Log.w(TAG, "File does not exist or is too small: ${file.absolutePath}")
            return false
        }

        return try {
            RandomAccessFile(file, "rw").use { raf ->
                processMp4File(raf)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply horizontal flip to MP4: ${file.absolutePath}", e)
            false
        }
    }

    private fun processMp4File(raf: RandomAccessFile): Boolean {
        val fileLength = raf.length()
        var offset = 0L
        var modified = false

        while (offset + 8 <= fileLength) {
            raf.seek(offset)
            val size32 = raf.readInt().toLong() and 0xFFFFFFFFL
            val typeBytes = ByteArray(4)
            raf.readFully(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            val (boxSize, headerSize) = when (size32) {
                1L -> {
                    if (offset + 16 > fileLength) break
                    val size64 = raf.readLong()
                    Pair(size64, 16L)
                }
                0L -> {
                    Pair(fileLength - offset, 8L)
                }
                else -> {
                    Pair(size32, 8L)
                }
            }

            if (boxSize < headerSize) break

            if (type == "moov") {
                if (processMoovBox(raf, offset + headerSize, offset + boxSize)) {
                    modified = true
                }
            }

            offset += boxSize
        }
        return modified
    }

    private fun processMoovBox(raf: RandomAccessFile, startOffset: Long, endOffset: Long): Boolean {
        var offset = startOffset
        var modified = false

        while (offset + 8 <= endOffset) {
            raf.seek(offset)
            val size32 = raf.readInt().toLong() and 0xFFFFFFFFL
            val typeBytes = ByteArray(4)
            raf.readFully(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            val (boxSize, headerSize) = when (size32) {
                1L -> {
                    if (offset + 16 > endOffset) break
                    val size64 = raf.readLong()
                    Pair(size64, 16L)
                }
                0L -> {
                    Pair(endOffset - offset, 8L)
                }
                else -> {
                    Pair(size32, 8L)
                }
            }

            if (boxSize < headerSize) break

            if (type == "trak") {
                if (processTrakBox(raf, offset + headerSize, offset + boxSize)) {
                    modified = true
                }
            }

            offset += boxSize
        }
        return modified
    }

    private fun processTrakBox(raf: RandomAccessFile, startOffset: Long, endOffset: Long): Boolean {
        var offset = startOffset
        var modified = false

        while (offset + 8 <= endOffset) {
            raf.seek(offset)
            val size32 = raf.readInt().toLong() and 0xFFFFFFFFL
            val typeBytes = ByteArray(4)
            raf.readFully(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            val (boxSize, headerSize) = when (size32) {
                1L -> {
                    if (offset + 16 > endOffset) break
                    val size64 = raf.readLong()
                    Pair(size64, 16L)
                }
                0L -> {
                    Pair(endOffset - offset, 8L)
                }
                else -> {
                    Pair(size32, 8L)
                }
            }

            if (boxSize < headerSize) break

            if (type == "tkhd") {
                if (processTkhdBox(raf, offset + headerSize, offset + boxSize)) {
                    modified = true
                }
            }

            offset += boxSize
        }
        return modified
    }

    private fun processTkhdBox(raf: RandomAccessFile, bodyStartOffset: Long, boxEndOffset: Long): Boolean {
        if (bodyStartOffset + 4 > boxEndOffset) return false

        raf.seek(bodyStartOffset)
        val version = raf.readByte().toInt()
        // Skip 3 bytes of flags
        raf.skipBytes(3)

        // Matrix offset calculation according to ISO/IEC 14496-12:
        // Version 0: 1 (ver) + 3 (flags) + 4 (creation) + 4 (modification) + 4 (track_id) +
        //            4 (reserved) + 4 (duration) + 8 (reserved) + 2 (layer) + 2 (alt_group) +
        //            2 (volume) + 2 (reserved) = 40 bytes from body start
        // Version 1: 1 (ver) + 3 (flags) + 8 (creation) + 8 (modification) + 4 (track_id) +
        //            4 (reserved) + 8 (duration) + 8 (reserved) + 2 (layer) + 2 (alt_group) +
        //            2 (volume) + 2 (reserved) = 52 bytes from body start
        val matrixOffset = bodyStartOffset + if (version == 0) 40L else 52L
        val dimOffset = matrixOffset + 36L

        if (dimOffset + 8 > boxEndOffset) return false

        // Read width and height (16.16 fixed point)
        raf.seek(dimOffset)
        val rawWidth = raf.readInt()
        val rawHeight = raf.readInt()

        val trackWidth = (rawWidth.toLong() and 0xFFFFFFFFL) shr 16
        val trackHeight = (rawHeight.toLong() and 0xFFFFFFFFL) shr 16

        // Audio or non-visual tracks have 0 width and height - do not transform them
        if (trackWidth <= 0 || trackHeight <= 0) {
            return false
        }

        // Read current 3x3 matrix: [a, b, u, c, d, v, x, y, w]
        raf.seek(matrixOffset)
        val a = raf.readInt()
        val b = raf.readInt()
        val u = raf.readInt()
        val c = raf.readInt()
        val d = raf.readInt()
        val v = raf.readInt()
        val x = raf.readInt()
        val y = raf.readInt()
        val w = raf.readInt()

        // Calculate horizontal reflection matrix:
        // Given display mapping [X, Y, 1] = [px, py, 1] * M
        // Horizontal reflection across display width W_disp maps X -> (W_disp - X), Y -> Y.
        // For any rotation angle (0, 90, 180, 270):
        // W_disp in 16.16 fixed point is maxX = abs(a * trackWidth + c * trackHeight)
        // New values: a' = -a, c' = -c, x' = maxX - x
        // b, d, y, u, v, w remain identical.
        val maxX = kotlin.math.abs(a.toLong() * trackWidth + c.toLong() * trackHeight).toInt()

        val newA = -a
        val newB = b
        val newU = u
        val newC = -c
        val newD = d
        val newV = v
        val newX = maxX - x
        val newY = y
        val newW = w

        // Write modified matrix back to file
        raf.seek(matrixOffset)
        raf.writeInt(newA)
        raf.writeInt(newB)
        raf.writeInt(newU)
        raf.writeInt(newC)
        raf.writeInt(newD)
        raf.writeInt(newV)
        raf.writeInt(newX)
        raf.writeInt(newY)
        raf.writeInt(newW)

        Log.d(TAG, "Applied horizontal reflection to video track (${trackWidth}x${trackHeight}): " +
                "a=$a->$newA, c=$c->$newC, x=$x->$newX")
        return true
    }
}
