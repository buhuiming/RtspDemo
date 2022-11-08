
package com.bhm.rtspdemo.rtsp.rtp.packets

import com.bhm.rtspdemo.rtsp.rtsp.RtpFrame

interface VideoPacketCallback {
  fun onVideoFrameCreated(rtpFrame: RtpFrame)
}