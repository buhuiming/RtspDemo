
package com.bhm.rtspdemo.rtsp.rtcp

import com.bhm.rtspdemo.rtsp.rtsp.Protocol
import com.bhm.rtspdemo.rtsp.rtsp.RtpFrame
import com.bhm.rtspdemo.rtsp.utils.RtpConstants
import com.bhm.rtspdemo.rtsp.utils.setLong
import java.io.IOException
import java.io.OutputStream

abstract class BaseSenderReport internal constructor() {

  private val interval: Long = 3000
  private val videoBuffer = ByteArray(RtpConstants.MTU)
  private val audioBuffer = ByteArray(RtpConstants.MTU)
  private var videoTime: Long = 0
  private var audioTime: Long = 0
  private var videoPacketCount = 0L
  private var videoOctetCount = 0L
  private var audioPacketCount = 0L
  private var audioOctetCount = 0L
  val PACKET_LENGTH = 28

  companion object {
    @JvmStatic
    fun getInstance(protocol: Protocol, videoSourcePort: Int, audioSourcePort: Int): BaseSenderReport {
      return if (protocol === Protocol.TCP) {
        SenderReportTcp()
      } else {
        SenderReportUdp(videoSourcePort, audioSourcePort)
      }
    }
  }

  init {
    /*							     Version(2)  Padding(0)					 					*/
    /*									 ^		  ^			PT = 0	    						*/
    /*									 |		  |				^								*/
    /*									 | --------			 	|								*/
    /*									 | |---------------------								*/
    /*									 | ||													*/
    /*									 | ||													*/
    videoBuffer[0] = 0x80.toByte()
    audioBuffer[0] = 0x80.toByte()

    /* Packet Type PT */
    videoBuffer[1] = 200.toByte()
    audioBuffer[1] = 200.toByte()

    /* Byte 2,3          ->  Length		                     */
    videoBuffer.setLong(PACKET_LENGTH / 4 - 1L, 2, 4)
    audioBuffer.setLong(PACKET_LENGTH / 4 - 1L, 2, 4)
    /* Byte 4,5,6,7      ->  SSRC                            */
    /* Byte 8,9,10,11    ->  NTP timestamp hb				 */
    /* Byte 12,13,14,15  ->  NTP timestamp lb				 */
    /* Byte 16,17,18,19  ->  RTP timestamp		             */
    /* Byte 20,21,22,23  ->  packet count				 	 */
    /* Byte 24,25,26,27  ->  octet count			         */
  }

  fun setSSRC(ssrcVideo: Long, ssrcAudio: Long) {
    videoBuffer.setLong(ssrcVideo, 4, 8)
    audioBuffer.setLong(ssrcAudio, 4, 8)
  }

  @Throws(IOException::class)
  abstract fun setDataStream(outputStream: OutputStream, host: String)

  @Throws(IOException::class)
  fun update(rtpFrame: RtpFrame, isEnableLogs: Boolean): Boolean {
    return if (rtpFrame.channelIdentifier == RtpConstants.trackVideo) {
      updateVideo(rtpFrame, isEnableLogs)
    } else {
      updateAudio(rtpFrame, isEnableLogs)
    }
  }

  @Throws(IOException::class)
  abstract fun sendReport(buffer: ByteArray, rtpFrame: RtpFrame, type: String, packetCount: Long, octetCount: Long, isEnableLogs: Boolean)

  @Throws(IOException::class)
  private fun updateVideo(rtpFrame: RtpFrame, isEnableLogs: Boolean): Boolean {
    videoPacketCount++
    videoOctetCount += rtpFrame.length
    videoBuffer.setLong(videoPacketCount, 20, 24)
    videoBuffer.setLong(videoOctetCount, 24, 28)
    if (System.currentTimeMillis() - videoTime >= interval) {
      videoTime = System.currentTimeMillis()
      setData(videoBuffer, System.nanoTime(), rtpFrame.timeStamp)
      sendReport(videoBuffer, rtpFrame, "Video", videoPacketCount, videoOctetCount, isEnableLogs)
      return true
    }
    return false
  }

  @Throws(IOException::class)
  private fun updateAudio(rtpFrame: RtpFrame, isEnableLogs: Boolean): Boolean {
    audioPacketCount++
    audioOctetCount += rtpFrame.length
    audioBuffer.setLong(audioPacketCount, 20, 24)
    audioBuffer.setLong(audioOctetCount, 24, 28)
    if (System.currentTimeMillis() - audioTime >= interval) {
      audioTime = System.currentTimeMillis()
      setData(audioBuffer, System.nanoTime(), rtpFrame.timeStamp)
      sendReport(audioBuffer, rtpFrame, "Audio", audioPacketCount, audioOctetCount, isEnableLogs)
      return true
    }
    return false
  }

  fun reset() {
    videoOctetCount = 0
    videoPacketCount = videoOctetCount
    audioOctetCount = 0
    audioPacketCount = audioOctetCount
    audioTime = 0
    videoTime = audioTime
    videoBuffer.setLong(videoPacketCount, 20, 24)
    videoBuffer.setLong(videoOctetCount, 24, 28)
    audioBuffer.setLong(audioPacketCount, 20, 24)
    audioBuffer.setLong(audioOctetCount, 24, 28)
  }

  abstract fun close()

  private fun setData(buffer: ByteArray, ntpts: Long, rtpts: Long) {
    val hb = ntpts / 1000000000
    val lb = (ntpts - hb * 1000000000) * 4294967296L / 1000000000
    buffer.setLong(hb, 8, 12)
    buffer.setLong(lb, 12, 16)
    buffer.setLong(rtpts, 16, 20)
  }
}