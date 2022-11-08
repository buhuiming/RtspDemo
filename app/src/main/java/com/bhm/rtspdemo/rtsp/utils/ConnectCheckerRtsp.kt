
package com.bhm.rtspdemo.rtsp.utils

interface ConnectCheckerRtsp {
  fun onConnectionStartedRtsp(rtspUrl: String)
  fun onConnectionSuccessRtsp()
  fun onConnectionFailedRtsp(reason: String)
  fun onNewBitrateRtsp(bitrate: Long)
  fun onDisconnectRtsp()
  fun onAuthErrorRtsp()
  fun onAuthSuccessRtsp()
}