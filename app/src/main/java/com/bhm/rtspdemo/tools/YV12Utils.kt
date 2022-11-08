package com.bhm.rtspdemo.tools

object YV12Utils {
    private lateinit var preAllocatedBufferRotate: ByteArray
    private lateinit var preAllocatedBufferColor: ByteArray

    fun preAllocateBuffers(length: Int) {
        preAllocatedBufferRotate = ByteArray(length)
        preAllocatedBufferColor = ByteArray(length)
    }

    // the color transform, @see http://stackoverflow.com/questions/15739684/mediacodec-and-camera-color-space-incorrect
    fun toNV12(input: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val qFrameSize = frameSize / 4
        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize) // Y
        for (i in 0 until qFrameSize) {
            preAllocatedBufferColor[frameSize + i * 2] = input[frameSize + i + qFrameSize] // Cb (U)
            preAllocatedBufferColor[frameSize + i * 2 + 1] = input[frameSize + i] // Cr (V)
        }
        return preAllocatedBufferColor
    }

    fun toI420(input: ByteArray?, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val qFrameSize = frameSize / 4
        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize) // Y
        System.arraycopy(
            input, frameSize + qFrameSize, preAllocatedBufferColor, frameSize,
            qFrameSize
        ) // Cb (U)
        System.arraycopy(
            input, frameSize, preAllocatedBufferColor, frameSize + qFrameSize,
            qFrameSize
        ) // Cr (V)
        return preAllocatedBufferColor
    }

    fun toNV21(input: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val qFrameSize = frameSize / 4
        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize) // Y
        for (i in 0 until qFrameSize) {
            preAllocatedBufferColor[frameSize + i * 2 + 1] =
                input[frameSize + i + qFrameSize] // Cb (U)
            preAllocatedBufferColor[frameSize + i * 2] = input[frameSize + i] // Cr (V)
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
        val size = imageWidth * imageHeight
        val colorSize = size / 4
        val colorHeight = colorSize / imageWidth
        // Rotate the U and V color components
        for (x in 0 until imageWidth / 2) {
            for (y in colorHeight - 1 downTo 0) {
                //V
                preAllocatedBufferRotate[i + colorSize] =
                    data[colorSize + size + imageWidth * y + x + imageWidth / 2]
                preAllocatedBufferRotate[i + colorSize + 1] =
                    data[colorSize + size + imageWidth * y + x]
                //U
                preAllocatedBufferRotate[i++] = data[size + imageWidth * y + x + imageWidth / 2]
                preAllocatedBufferRotate[i++] = data[size + imageWidth * y + x]
            }
        }
        return preAllocatedBufferRotate
    }

    fun rotate180(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray {
        var count = 0
        val size = imageWidth * imageHeight
        for (i in size - 1 downTo 0) {
            preAllocatedBufferRotate[count++] = data[i]
        }
        val midColorSize = size / 4
        //U
        for (i in size + midColorSize - 1 downTo size) {
            preAllocatedBufferRotate[count++] = data[i]
        }
        //V
        for (i in data.size - 1 downTo imageWidth * imageHeight + midColorSize) {
            preAllocatedBufferRotate[count++] = data[i]
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
        val size = imageWidth * imageHeight
        val colorSize = size / 4
        val colorHeight = colorSize / imageWidth
        for (x in 0 until imageWidth / 2) {
            for (y in 0 until colorHeight) {
                //V
                preAllocatedBufferRotate[i + colorSize] =
                    data[colorSize + size + imageWidth * y - x + imageWidth / 2 - 1]
                preAllocatedBufferRotate[i + colorSize + 1] =
                    data[colorSize + size + imageWidth * y - x + imageWidth - 1]
                //U
                preAllocatedBufferRotate[i++] = data[size + imageWidth * y - x + imageWidth / 2 - 1]
                preAllocatedBufferRotate[i++] = data[size + imageWidth * y - x + imageWidth - 1]
            }
        }
        return preAllocatedBufferRotate
    }
}