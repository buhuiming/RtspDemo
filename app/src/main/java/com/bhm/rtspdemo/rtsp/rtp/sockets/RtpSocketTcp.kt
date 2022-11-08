
package com.bhm.rtspdemo.rtsp.rtp.sockets

import com.bhm.rtspdemo.rtsp.rtsp.RtpFrame
import com.bhm.rtspdemo.rtsp.utils.RtpConstants
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream

open class RtpSocketTcp : BaseRtpSocket() {

  private var outputStream: OutputStream? = null
  private val tcpHeader: ByteArray = byteArrayOf('$'.code.toByte(), 0, 0, 0)

  @Throws(IOException::class)
  override fun setDataStream(outputStream: OutputStream, host: String) {
    this.outputStream = outputStream
  }

  @Throws(IOException::class)
  override fun sendFrame(rtpFrame: RtpFrame, isEnableLogs: Boolean) {
    sendFrameTCP(rtpFrame, isEnableLogs)
  }

  override fun close() {}

  @Throws(IOException::class)
  private fun sendFrameTCP(rtpFrame: RtpFrame, isEnableLogs: Boolean) {
    synchronized(RtpConstants.lock) {
      val len = rtpFrame.length
      tcpHeader[1] = (2 * rtpFrame.channelIdentifier).toByte()
      tcpHeader[2] = (len shr 8).toByte()
      tcpHeader[3] = (len and 0xFF).toByte()
      outputStream?.write(tcpHeader)
      outputStream?.write(rtpFrame.buffer, 0, len)
      outputStream?.flush()
      if (isEnableLogs) {
        Timber.i("wrote packet: " + (if (rtpFrame.isVideoFrame()) "Video" else "Audio") + ", size: " + len + tcpHeader.size)
      }
    }
  }
}