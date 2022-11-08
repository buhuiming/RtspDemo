@file:Suppress("DEPRECATION")

package com.bhm.rtspdemo.encoder

import android.media.MediaCodecInfo

enum class FormatVideoEncoder {
    YUV420FLEXIBLE,
    YUV420PLANAR,
    YUV420SEMIPLANAR,
    YUV420PACKEDPLANAR,
    YUV420PACKEDSEMIPLANAR,
    YUV422FLEXIBLE,
    YUV422PLANAR,
    YUV422SEMIPLANAR,
    YUV422PACKEDPLANAR,
    YUV422PACKEDSEMIPLANAR,
    YUV444FLEXIBLE,
    YUV444INTERLEAVED,
    SURFACE,  //take first valid color for encoder (YUV420PLANAR, YUV420SEMIPLANAR or YUV420PACKEDPLANAR)
    YUV420Dynamical;

    val formatCodec: Int
        get() = when (this) {
            YUV420FLEXIBLE -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            YUV420PLANAR -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            YUV420SEMIPLANAR -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            YUV420PACKEDPLANAR -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar
            YUV420PACKEDSEMIPLANAR -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar
            YUV422FLEXIBLE -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Flexible
            YUV422PLANAR -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422Planar
            YUV422SEMIPLANAR -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422SemiPlanar
            YUV422PACKEDPLANAR -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedPlanar
            YUV422PACKEDSEMIPLANAR -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV422PackedSemiPlanar
            YUV444FLEXIBLE -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Flexible
            YUV444INTERLEAVED -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV444Interleaved
            SURFACE -> MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            else -> -1
        }
}