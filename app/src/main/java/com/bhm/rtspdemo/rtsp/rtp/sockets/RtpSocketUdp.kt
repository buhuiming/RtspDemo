
package com.bhm.rtspdemo.rtsp.rtp.sockets

import com.bhm.rtspdemo.rtsp.rtsp.RtpFrame
import com.bhm.rtspdemo.rtsp.utils.RtpConstants
import timber.log.Timber
import java.io.IOException
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

open class RtpSocketUdp(videoSourcePort: Int, audioSourcePort: Int) : BaseRtpSocket() {

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
  override fun sendFrame(rtpFrame: RtpFrame, isEnableLogs: Boolean) {
    sendFrameUDP(rtpFrame, isEnableLogs)
  }

  override fun close() {
    multicastSocketVideo?.close()
    multicastSocketAudio?.close()
  }

  @Throws(IOException::class)
  private fun sendFrameUDP(rtpFrame: RtpFrame, isEnableLogs: Boolean) {
    synchronized(RtpConstants.lock) {
      datagramPacket.data = rtpFrame.buffer
      datagramPacket.port = rtpFrame.rtpPort
      datagramPacket.length = rtpFrame.length
      if (rtpFrame.isVideoFrame()) {
        multicastSocketVideo?.send(datagramPacket)
      } else {
        multicastSocketAudio?.send(datagramPacket)
      }
      if (isEnableLogs) {
        Timber.i("wrote packet: " + (if (rtpFrame.isVideoFrame()) "Video" else "Audio") + ", size: " + rtpFrame.length + ", port: " + rtpFrame.rtpPort)
      }
    }
  }
}