@file:Suppress("unused")

package com.bhm.rtspdemo.tools

object NV21Utils {

    private lateinit var preAllocatedBufferRotate: ByteArray
    private lateinit var preAllocatedBufferColor: ByteArray

    fun preAllocateBuffers(length: Int) {
        preAllocatedBufferRotate = ByteArray(length)
        preAllocatedBufferColor = ByteArray(length)
    }

    fun toARGB(yuv: ByteArray, width: Int, height: Int): IntArray {
        val argb = IntArray(width * height)
        val frameSize = width * height
        val ii = 0
        val ij = 0
        val di = +1
        val dj = +1
        var a = 0
        var i = 0
        var ci = ii
        while (i < height) {
            var j = 0
            var cj = ij
            while (j < width) {
                var y = 0xff and yuv[ci * width + cj].toInt()
                val v = 0xff and yuv[frameSize + (ci shr 1) * width + (cj and 1.inv()) + 0]
                    .toInt()
                val u = 0xff and yuv[frameSize + (ci shr 1) * width + (cj and 1.inv()) + 1]
                    .toInt()
                y = if (y < 16) 16 else y
                var r = (1.164f * (y - 16) + 1.596f * (v - 128)).toInt()
                var g = (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128)).toInt()
                var b = (1.164f * (y - 16) + 2.018f * (u - 128)).toInt()
                r = if (r < 0) 0 else if (r > 255) 255 else r
                g = if (g < 0) 0 else if (g > 255) 255 else g
                b = if (b < 0) 0 else if (b > 255) 255 else b
                argb[a++] = -0x1000000 or (r shl 16) or (g shl 8) or b
                ++j
                cj += dj
            }
            ++i
            ci += di
        }
        return argb
    }

    fun toYV12(input: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val qFrameSize = frameSize / 4
        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize) // Y
        for (i in 0 until qFrameSize) {
            preAllocatedBufferColor[frameSize + i + qFrameSize] =
                input[frameSize + i * 2 + 1] // Cb (U)
            preAllocatedBufferColor[frameSize + i] = input[frameSize + i * 2] // Cr (V)
        }
        return preAllocatedBufferColor
    }

    // the color transform, @see http://stackoverflow.com/questions/15739684/mediacodec-and-camera-color-space-incorrect
    fun toNV12(input: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val qFrameSize = frameSize / 4
        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize) // Y
        for (i in 0 until qFrameSize) {
            preAllocatedBufferColor[frameSize + i * 2] = input[frameSize + i * 2 + 1] // Cb (U)
            preAllocatedBufferColor[frameSize + i * 2 + 1] = input[frameSize + i * 2] // Cr (V)
        }
        return preAllocatedBufferColor
    }

    fun toI420(input: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val qFrameSize = frameSize / 4
        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize) // Y
        for (i in 0 until qFrameSize) {
            preAllocatedBufferColor[frameSize + i] = input[frameSize + i * 2 + 1] // Cb (U)
            preAllocatedBufferColor[frameSize + i + qFrameSize] = input[frameSize + i * 2] // Cr (V)
        }
        return preAllocatedBufferColor
    }

    fun rotate90(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        // Rotate the Y luma
        var i = 0
        for (x in 0 until imageWidth) {
            for (y in imageHeight - 1 downTo 0) {
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x]
            }
        }
        // Rotate the U and V color components
        val size = imageWidth * imageHeight
        i = size * 3 / 2 - 1
        var x = imageWidth - 1
        while (x > 0) {
            for (y in 0 until imageHeight / 2) {
                preAllocatedBufferRotate[i--] = data[size + y * imageWidth + x]
                preAllocatedBufferRotate[i--] = data[size + y * imageWidth + (x - 1)]
            }
            x -= 2
        }
        return preAllocatedBufferRotate
    }

    fun rotate180(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        var count = 0
        for (i in imageWidth * imageHeight - 1 downTo 0) {
            preAllocatedBufferRotate[count] = data[i]
            count++
        }
        var i = imageWidth * imageHeight * 3 / 2 - 1
        while (i >= imageWidth * imageHeight) {
            preAllocatedBufferRotate[count++] = data[i - 1]
            preAllocatedBufferRotate[count++] = data[i]
            i -= 2
        }
        return preAllocatedBufferRotate
    }

    fun rotate270(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        // Rotate the Y luma
        var i = 0
        for (x in imageWidth - 1 downTo 0) {
            for (y in 0 until imageHeight) {
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x]
            }
        }

        // Rotate the U and V color components
        i = imageWidth * imageHeight
        val uvHeight = imageHeight / 2
        var x = imageWidth - 1
        while (x >= 0) {
            for (y in imageHeight until uvHeight + imageHeight) {
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x - 1]
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x]
            }
            x -= 2
        }
        return preAllocatedBufferRotate
    }

    fun rotatePixels(input: ByteArray, width: Int, height: Int, rotation: Int): ByteArray {
        val output = ByteArray(input.size)
        val swap = rotation == 90 || rotation == 270
        val yflip = rotation == 90 || rotation == 180
        val xflip = rotation == 270 || rotation == 180
        for (x in 0 until width) {
            for (y in 0 until height) {
                var xo = x
                var yo = y
                var w = width
                var h = height
                var xi = xo
                var yi = yo
                if (swap) {
                    xi = w * yo / h
                    yi = h * xo / w
                }
                if (yflip) {
                    yi = h - yi - 1
                }
                if (xflip) {
                    xi = w - xi - 1
                }
                output[w * yo + xo] = input[w * yi + xi]
                val fs = w * h
                val qs = fs shr 2
                xi = xi shr 1
                yi = yi shr 1
                xo = xo shr 1
                yo = yo shr 1
                w = w shr 1
                h = h shr 1
                // adjust for interleave here
                val ui = fs + (w * yi + xi) * 2
                val uo = fs + (w * yo + xo) * 2
                // and here
                val vi = ui + 1
                val vo = uo + 1
                output[uo] = input[ui]
                output[vo] = input[vi]
            }
        }
        return output
    }

    fun mirror(input: ByteArray, width: Int, height: Int): ByteArray {
        val output = ByteArray(input.size)
        for (x in 0 until width) {
            for (y in 0 until height) {
                var xo = x
                var yo = y
                var w = width
                var h = height
                var xi = xo
                var yi = yo
                yi = h - yi - 1
                output[w * yo + xo] = input[w * yi + xi]
                val fs = w * h
                val qs = fs shr 2
                xi = xi shr 1
                yi = yi shr 1
                xo = xo shr 1
                yo = yo shr 1
                w = w shr 1
                h = h shr 1
                // adjust for interleave here
                val ui = fs + (w * yi + xi) * 2
                val uo = fs + (w * yo + xo) * 2
                // and here
                val vi = ui + 1
                val vo = uo + 1
                output[uo] = input[ui]
                output[vo] = input[vi]
            }
        }
        return output
    }
}