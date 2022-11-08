
package com.bhm.rtspdemo.rtsp.rtp.sockets

import com.bhm.rtspdemo.rtsp.rtsp.Protocol
import com.bhm.rtspdemo.rtsp.rtsp.RtpFrame
import java.io.IOException
import java.io.OutputStream

abstract class BaseRtpSocket {

  companion object {
    @JvmStatic
    fun getInstance(protocol: Protocol, videoSourcePort: Int, audioSourcePort: Int): BaseRtpSocket {
      return if (protocol === Protocol.TCP) {
        RtpSocketTcp()
      } else {
        RtpSocketUdp(videoSourcePort, audioSourcePort)
      }
    }
  }

  @Throws(IOException::class)
  abstract fun setDataStream(outputStream: OutputStream, host: String)

  @Throws(IOException::class)
  abstract fun sendFrame(rtpFrame: RtpFrame, isEnableLogs: Boolean)

  abstract fun close()
}