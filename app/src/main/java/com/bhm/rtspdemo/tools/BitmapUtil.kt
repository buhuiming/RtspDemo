@file:Suppress("SameParameterValue", "UNUSED_VALUE")

package com.bhm.rtspdemo.tools

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bhm.rtspdemo.Constants
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

/**
 * @author Buhuiming
 * @description:
 * @date :2022/11/7 14:14
 */
object BitmapUtil {

    fun readBitmapFromAssets(context: Context, fileName: String): ByteArray {
        var image: Bitmap? = null
        val am: AssetManager = context.resources.assets
        try {
            var inputStream: InputStream? = am.open(fileName)
            image = BitmapFactory.decodeStream(inputStream)
            inputStream!!.close()
            inputStream = null
        } catch (e: IOException) {
            e.printStackTrace()
            Timber.e("Can't read assets file: $fileName")
        }

        //width height不能超过bitmap的宽高
        return getNV21FromBitmap(Constants.WIDTH, Constants.HEIGHT, image)
    }

    private fun getNV21FromBitmap(inputWidth: Int, inputHeight: Int, scaled: Bitmap?): ByteArray {
        val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
        scaled?.let {
            val argb = IntArray(inputWidth * inputHeight)
            it.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
            encodeYUV420SP(yuv, argb, inputWidth, inputHeight)
            it.recycle()
        }
        return yuv
    }

    /***3.根据RGB数组采样分别获取Y，U，V数组，并存储为NV21格式的数组 */
    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
//        var a: Int
        var r: Int
        var g: Int
        var b: Int
        var y: Int
        var u: Int
        var v: Int
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
//                a = argb[index] and -0x1000000 shr 24 // a is not used obviously
                r = argb[index] and 0xff0000 shr 16
                g = argb[index] and 0xff00 shr 8
                b = argb[index] and 0xff shr 0

                // well known RGB to YUV algorithm
                y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128

                // NV21 has a plane of y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 y pixels there are 1 v and 1 u.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                    yuv420sp[uvIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                }
                index++
            }
        }
    }
}