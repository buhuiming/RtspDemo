
package com.bhm.rtspdemo.rtsp.utils

import timber.log.Timber
import java.io.IOException
import java.net.Socket
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException

/**
 * this class is used for secure transport, to use replace socket on RtspClient with this and
 * you will have a secure stream under ssl/tls.
 */
object CreateSSLSocket {
  /**
   * @param host variable from RtspClient
   * @param port variable from RtspClient
   */
  @JvmStatic
  fun createSSlSocket(host: String, port: Int): Socket? {
    return try {
      val socketFactory = TLSSocketFactory()
      socketFactory.createSocket(host, port)
    } catch (e: NoSuchAlgorithmException) {
      Timber.e(e, "Error")
      null
    } catch (e: KeyManagementException) {
      Timber.e(e, "Error")
      null
    } catch (e: IOException) {
      Timber.e(e, "Error")
      null
    }
  }
}