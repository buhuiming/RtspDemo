
package com.bhm.rtspdemo.rtsp.rtsp

import com.bhm.rtspdemo.rtsp.utils.RtpConstants

data class RtpFrame(val buffer: ByteArray, val timeStamp: Long, val length: Int,
                    val rtpPort: Int, val rtcpPort: Int, val channelIdentifier: Int) {

  fun isVideoFrame(): Boolean = channelIdentifier == RtpConstants.trackVideo

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RtpFrame

    if (!buffer.contentEquals(other.buffer)) return false
    if (timeStamp != other.timeStamp) return false
    if (length != other.length) return false
    if (rtpPort != other.rtpPort) return false
    if (rtcpPort != other.rtcpPort) return false
    if (channelIdentifier != other.channelIdentifier) return false

    return true
  }

  override fun hashCode(): Int {
    var result = buffer.contentHashCode()
    result = 31 * result + timeStamp.hashCode()
    result = 31 * result + length
    result = 31 * result + rtpPort
    result = 31 * result + rtcpPort
    result = 31 * result + channelIdentifier
    return result
  }
}