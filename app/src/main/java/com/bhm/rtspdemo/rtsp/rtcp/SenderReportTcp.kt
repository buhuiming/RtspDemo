
package com.bhm.rtspdemo.rtsp.rtcp

import com.bhm.rtspdemo.rtsp.rtsp.RtpFrame
import com.bhm.rtspdemo.rtsp.utils.RtpConstants
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream

open class SenderReportTcp : BaseSenderReport() {

  private var outputStream: OutputStream? = null
  private val tcpHeader: ByteArray = byteArrayOf('$'.code.toByte(), 0, 0, PACKET_LENGTH.toByte())

  @Throws(IOException::class)
  override fun setDataStream(outputStream: OutputStream, host: String) {
    this.outputStream = outputStream
  }

  @Throws(IOException::class)
  override fun sendReport(buffer: ByteArray, rtpFrame: RtpFrame, type: String, packetCount: Long, octetCount: Long, isEnableLogs: Boolean) {
    sendReportTCP(buffer, rtpFrame.channelIdentifier, type, packetCount, octetCount, isEnableLogs)
  }

  override fun close() {}

  @Throws(IOException::class)
  private fun sendReportTCP(buffer: ByteArray, channelIdentifier: Int, type: String, packet: Long, octet: Long, isEnableLogs: Boolean) {
    synchronized(RtpConstants.lock) {
      tcpHeader[1] = (2 * channelIdentifier + 1).toByte()
      outputStream?.write(tcpHeader)
      outputStream?.write(buffer, 0, PACKET_LENGTH)
      outputStream?.flush()
      if (isEnableLogs) {
        Timber.i("wrote report: $type, packets: $packet, octet: $octet")
      }
    }
  }
}