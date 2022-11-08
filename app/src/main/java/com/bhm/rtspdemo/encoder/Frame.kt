@file:Suppress("unused")

package com.bhm.rtspdemo.encoder

import android.graphics.ImageFormat

class Frame {
    var buffer: ByteArray
    var offset: Int
    var size: Int
    var orientation = 0
    var isFlip = false
    var format = ImageFormat.NV21 //nv21 or yv12 supported

    /**
     * Used with video frame
     */
    constructor(buffer: ByteArray, orientation: Int, flip: Boolean, format: Int) {
        this.buffer = buffer
        this.orientation = orientation
        isFlip = flip
        this.format = format
        offset = 0
        size = buffer.size
    }

    /**
     * Used with audio frame
     */
    constructor(buffer: ByteArray, offset: Int, size: Int) {
        this.buffer = buffer
        this.offset = offset
        this.size = size
    }
}