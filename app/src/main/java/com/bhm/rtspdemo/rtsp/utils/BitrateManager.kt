
package com.bhm.rtspdemo.rtsp.utils

/**
 * Calculate video and audio bitrate per second
 */
open class BitrateManager(private val connectCheckerRtsp: ConnectCheckerRtsp) {

  private var bitrate: Long = 0
  private var timeStamp = System.currentTimeMillis()

  @Synchronized
  fun calculateBitrate(size: Long) {
    bitrate += size
    val timeDiff = System.currentTimeMillis() - timeStamp
    if (timeDiff >= 1000) {
      connectCheckerRtsp.onNewBitrateRtsp((bitrate / (timeDiff / 1000f)).toLong())
      timeStamp = System.currentTimeMillis()
      bitrate = 0
    }
  }
}