
package com.bhm.rtspdemo.rtsp.utils

import android.util.Base64
import java.nio.ByteBuffer

fun ByteArray.encodeToString(flags: Int = Base64.NO_WRAP): String {
  return Base64.encodeToString(this, flags)
}

fun ByteBuffer.getData(): ByteArray {
  val startCodeSize = this.getVideoStartCodeSize()
  val bytes = ByteArray(this.capacity() - startCodeSize)
  this.position(startCodeSize)
  this.get(bytes, 0, bytes.size)
  return bytes
}

fun ByteArray.setLong(n: Long, begin: Int, end: Int) {
  var value = n
  for (i in end - 1 downTo begin step 1) {
    this[i] = (value % 256).toByte()
    value = value shr 8
  }
}

fun ByteBuffer.getVideoStartCodeSize(): Int {
  var startCodeSize = 0
  if (this.get(0).toInt() == 0x00 && this.get(1).toInt() == 0x00
    && this.get(2).toInt() == 0x00 && this.get(3).toInt() == 0x01) {
    //match 00 00 00 01
    startCodeSize = 4
  } else if (this.get(0).toInt() == 0x00 && this.get(1).toInt() == 0x00
    && this.get(2).toInt() == 0x01) {
    //match 00 00 01
    startCodeSize = 3
  }
  return startCodeSize
}