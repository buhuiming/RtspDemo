
package com.bhm.rtspdemo.rtsp.rtcp

import com.bhm.rtspdemo.rtsp.rtsp.RtpFrame
import com.bhm.rtspdemo.rtsp.utils.RtpConstants
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

open class SenderReportUdp(videoSourcePort: Int, audioSourcePort: Int) : BaseSenderReport() {

  private var multicastSocketVideo: MulticastSocket? = null
  private var multicastSocketAudio: MulticastSocket? = null
  private val datagramPacket = DatagramPacket(byteArrayOf(0), 1)

  init {
    multicastSocketVideo = MulticastSocket(videoSourcePort)
    multicastSocketVideo?.timeToLive = 64
    multicastSocketAudio = MulticastSocket(audioSourcePort)
    multicastSocketAudio?.timeToLive = 64
  }

  @Throws(IOException::class)
  override fun setDataStream(outputStream: OutputStream, host: String) {
    datagramPacket.address = InetAddress.getByName(host)
  }

  @Throws(IOException::class)
  override fun sendReport(buffer: ByteArray, rtpFrame: RtpFrame, type: String, packetCount: Long, octetCount: Long, isEnableLogs: Boolean) {
    sendReportUDP(buffer, rtpFrame.rtcpPort, type, packetCount, octetCount, isEnableLogs)
  }

  override fun close() {
    multicastSocketVideo?.close()
    multicastSocketAudio?.close()
  }

  @Throws(IOException::class)
  private fun sendReportUDP(buffer: ByteArray, port: Int, type: String, packet: Long, octet: Long, isEnableLogs: Boolean) {
    synchronized(RtpConstants.lock) {
      datagramPacket.data = buffer
      datagramPacket.port = port
      datagramPacket.length = PACKET_LENGTH
      if (type == "Video") {
        multicastSocketVideo?.send(datagramPacket)
      } else {
        multicastSocketAudio?.send(datagramPacket)
      }
      if (isEnableLogs) {
        Timber.i("wrote report: $type, port: $port, packets: $packet, octet: $octet")
      }
    }
  }
}