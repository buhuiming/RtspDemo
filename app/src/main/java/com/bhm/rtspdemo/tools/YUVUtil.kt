@file:Suppress("unused")

package com.bhm.rtspdemo.tools

import android.graphics.Bitmap
import android.media.MediaCodecInfo
import com.bhm.rtspdemo.encoder.FormatVideoEncoder
import com.bhm.rtspdemo.encoder.Frame

/**
 * https://wiki.videolan.org/YUV/#I420
 *
 * Example YUV images 4x4 px.
 *
 * NV21 example:
 *
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * U1   V1   U2   V2
 * U3   V3   U4   V4
 *
 *
 * YV12 example:
 *
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * U1   U2   U3   U4
 * V1   V2   V3   V4
 *
 *
 * YUV420 planar example (I420):
 *
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * V1   V2   V3   V4
 * U1   U2   U3   U4
 *
 *
 * YUV420 semi planar example (NV12):
 *
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * V1   U1   V2   U2
 * V3   U3   V4   U4
 */
object YUVUtil {

    fun preAllocateBuffers(length: Int) {
        NV21Utils.preAllocateBuffers(length)
        YV12Utils.preAllocateBuffers(length)
    }

    fun NV21toYUV420byColor(
        input: ByteArray, width: Int, height: Int,
        formatVideoEncoder: FormatVideoEncoder?
    ): ByteArray? {
        return when (formatVideoEncoder) {
            FormatVideoEncoder.YUV420PLANAR -> NV21Utils.toI420(input, width, height)
            FormatVideoEncoder.YUV420SEMIPLANAR -> NV21Utils.toNV12(input, width, height)
            else -> null
        }
    }

    fun rotateNV21(data: ByteArray, width: Int, height: Int, rotation: Int): ByteArray? {
        return when (rotation) {
            0 -> data
            90 -> NV21Utils.rotate90(data, width, height)
            180 -> NV21Utils.rotate180(data, width, height)
            270 -> NV21Utils.rotate270(data, width, height)
            else -> null
        }
    }

    fun YV12toYUV420byColor(
        input: ByteArray, width: Int, height: Int,
        formatVideoEncoder: FormatVideoEncoder?
    ): ByteArray? {
        return when (formatVideoEncoder) {
            FormatVideoEncoder.YUV420PLANAR -> YV12Utils.toI420(input, width, height)
            FormatVideoEncoder.YUV420SEMIPLANAR -> YV12Utils.toNV12(input, width, height)
            else -> null
        }
    }

    fun rotateYV12(data: ByteArray, width: Int, height: Int, rotation: Int): ByteArray? {
        return when (rotation) {
            0 -> data
            90 -> YV12Utils.rotate90(data, width, height)
            180 -> YV12Utils.rotate180(data, width, height)
            270 -> YV12Utils.rotate270(data, width, height)
            else -> null
        }
    }

    fun frameToBitmap(frame: Frame, width: Int, height: Int, orientation: Int): Bitmap {
        val w = if (orientation == 90 || orientation == 270) height else width
        val h = if (orientation == 90 || orientation == 270) width else height
        val argb = NV21Utils.toARGB(rotateNV21(frame.buffer, width, height, orientation)!!, w, h)
        return Bitmap.createBitmap(argb, w, h, Bitmap.Config.ARGB_8888)
    }

    fun ARGBtoYUV420SemiPlanar(input: IntArray, width: Int, height: Int): ByteArray {
        /*
     * COLOR_FormatYUV420SemiPlanar is NV12
     */
        val frameSize = width * height
        val yuv420sp = ByteArray(width * height * 3 / 2)
        var yIndex = 0
        var uvIndex = frameSize
        var a: Int
        var R: Int
        var G: Int
        var B: Int
        var Y: Int
        var U: Int
        var V: Int
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                a = input[index] and -0x1000000 shr 24 // a is not used obviously
                R = input[index] and 0xff0000 shr 16
                G = input[index] and 0xff00 shr 8
                B = input[index] and 0xff shr 0

                // well known RGB to YUV algorithm
                Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                    yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                }
                index++
            }
        }
        return yuv420sp
    }

    fun CropYuv(
        src_format: Int, src_yuv: ByteArray?, src_width: Int, src_height: Int,
        dst_width: Int, dst_height: Int
    ): ByteArray? {
        var dst_yuv: ByteArray?
        if (src_yuv == null) return null
        // simple implementation: copy the corner
        if (src_width == dst_width && src_height == dst_height) {
            dst_yuv = src_yuv
        } else {
            dst_yuv = ByteArray((dst_width * dst_height * 1.5).toInt())
            when (src_format) {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> {

                    // copy Y
                    var src_yoffset = 0
                    var dst_yoffset = 0
                    run {
                        var i = 0
                        while (i < dst_height) {
                            System.arraycopy(src_yuv, src_yoffset, dst_yuv, dst_yoffset, dst_width)
                            src_yoffset += src_width
                            dst_yoffset += dst_width
                            i++
                        }
                    }

                    // copy u
                    var src_uoffset = 0
                    var dst_uoffset = 0
                    src_yoffset = src_width * src_height
                    dst_yoffset = dst_width * dst_height
                    run {
                        var i = 0
                        while (i < dst_height / 2) {
                            System.arraycopy(
                                src_yuv,
                                src_yoffset + src_uoffset,
                                dst_yuv,
                                dst_yoffset + dst_uoffset,
                                dst_width / 2
                            )
                            src_uoffset += src_width / 2
                            dst_uoffset += dst_width / 2
                            i++
                        }
                    }

                    // copy v
                    var src_voffset = 0
                    var dst_voffset = 0
                    src_uoffset = src_width * src_height + src_width * src_height / 4
                    dst_uoffset = dst_width * dst_height + dst_width * dst_height / 4
                    var i = 0
                    while (i < dst_height / 2) {
                        System.arraycopy(
                            src_yuv, src_uoffset + src_voffset, dst_yuv, dst_uoffset + dst_voffset,
                            dst_width / 2
                        )
                        src_voffset += src_width / 2
                        dst_voffset += dst_width / 2
                        i++
                    }
                }
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar, MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar, MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar -> {

                    // copy Y
                    var src_yoffset = 0
                    var dst_yoffset = 0
                    run {
                        var i = 0
                        while (i < dst_height) {
                            System.arraycopy(src_yuv, src_yoffset, dst_yuv, dst_yoffset, dst_width)
                            src_yoffset += src_width
                            dst_yoffset += dst_width
                            i++
                        }
                    }

                    // copy u and v
                    var src_uoffset = 0
                    var dst_uoffset = 0
                    src_yoffset = src_width * src_height
                    dst_yoffset = dst_width * dst_height
                    var i = 0
                    while (i < dst_height / 2) {
                        System.arraycopy(
                            src_yuv, src_yoffset + src_uoffset, dst_yuv, dst_yoffset + dst_uoffset,
                            dst_width
                        )
                        src_uoffset += src_width
                        dst_uoffset += dst_width
                        i++
                    }
                }
                else -> {
                    dst_yuv = null
                }
            }
        }
        return dst_yuv
    }
}