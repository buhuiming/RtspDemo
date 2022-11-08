package com.bhm.rtspdemo.encoder

import kotlin.Throws
import android.media.MediaCodec
import android.media.MediaFormat
import java.lang.IllegalStateException

interface EncoderCallback {

    @Throws(IllegalStateException::class)
    fun inputAvailable(mediaCodec: MediaCodec, inBufferIndex: Int)

    @Throws(IllegalStateException::class)
    fun outputAvailable(
        mediaCodec: MediaCodec, outBufferIndex: Int,
        bufferInfo: MediaCodec.BufferInfo
    )

    fun formatChanged(mediaCodec: MediaCodec, mediaFormat: MediaFormat)
}