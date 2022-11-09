package com.bhm.rtspdemo

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.widget.Toast
import com.bhm.rtspdemo.databinding.ActivityMainBinding
import com.bhm.rtspdemo.encoder.FormatVideoEncoder
import com.bhm.rtspdemo.encoder.Frame
import com.bhm.rtspdemo.encoder.VideoEncoder
import com.bhm.rtspdemo.rtsp.rtsp.RtspClient
import com.bhm.rtspdemo.rtsp.utils.ConnectCheckerRtsp
import com.bhm.rtspdemo.tools.BitmapUtil
import com.bhm.support.sdk.common.BaseVBActivity
import com.bhm.support.sdk.common.BaseViewModel
import com.bhm.support.sdk.core.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("SetTextI18n")
class MainActivity : BaseVBActivity<BaseViewModel, ActivityMainBinding>(), ConnectCheckerRtsp {

    private var videoEncoder: VideoEncoder = VideoEncoder()

    private var rtspClient: RtspClient? = null

    @Volatile
    private var streaming = false

    @Volatile
    private var index = 0

    override fun createViewModel() = BaseViewModel(application)

    override fun initData() {
        super.initData()
        AppTheme.setStatusBarColor(this, R.color.purple_500)
        rtspClient = RtspClient(this)
        rtspClient?.setProtocol(Constants.PROTOCOL)
    }

    override fun initEvent() {
        super.initEvent()
        viewBinding.btnStart.setOnClickListener {
            streaming()
        }
        videoEncoder.onListener(
            { sps, pps, vps ->
                rtspClient?.setVideoInfo(sps, pps, vps)
            },
            { h264Buffer, bufferInfo ->
                //硬编码后，组包发送
                rtspClient?.sendVideo(h264Buffer, bufferInfo)
            },
        )
    }

    private fun streaming() {
        if (viewBinding.etUrl.text.isEmpty()) return

        if (streaming) {
            //停止
            rtspClient?.disconnect()
            videoEncoder.stop()
            streaming = false
            viewBinding.etUrl.isEnabled = true
            viewBinding.btnStart.text = "Start"
            return
        }
        if (videoEncoder.prepareVideoEncoder(
                Constants.WIDTH,
                Constants.HEIGHT,
                Constants.FPS,
                Constants.BITRATE,
                Constants.ORIENTATION,
                2,
                FormatVideoEncoder.YUV420Dynamical,
                -1,
                -1,
            )
        ) {
            viewBinding.etUrl.isEnabled = false
            viewBinding.btnStart.text = "Stop"
            streaming = true
            videoEncoder.start()
            rtspClient?.setOnlyVideo(true)
            rtspClient?.setCheckServerAlive(true)
            rtspClient?.connect(viewBinding.etUrl.text.toString(), true)
            CoroutineScope(Dispatchers.IO).launch {
                while (index < 332 && streaming) {
                    //读取文件
                    val bitmapBytes = BitmapUtil.readBitmapFromAssets(applicationContext, "data/${index}.png")
                    //硬编码
                    videoEncoder.inputYUVData(Frame(bitmapBytes, Constants.ORIENTATION, false, ImageFormat.NV21))
                    index++
                    if (index == 332) {
                        index = 0
                    }
                }
            }
            return
        }

        //If you see this all time when you start stream,
        //it is because your encoder device dont support the configuration
        //in video encoder maybe color format.
        //If you have more encoder go to VideoEncoder or AudioEncoder class,
        //change encoder and try
        Toast.makeText(
            this, "Error preparing stream, This device cant do it",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onConnectionStartedRtsp(rtspUrl: String) {

    }

    override fun onConnectionSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(applicationContext, "Connection success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailedRtsp(reason: String) {
        runOnUiThread {
            Toast.makeText(applicationContext, "Connection failed. $reason", Toast.LENGTH_SHORT).show()
            rtspClient?.disconnect()
            videoEncoder.stop()
            streaming = false
            viewBinding.etUrl.isEnabled = true
            viewBinding.btnStart.text = "Start"
        }
    }

    override fun onNewBitrateRtsp(bitrate: Long) {

    }

    override fun onDisconnectRtsp() {
        runOnUiThread {
            Toast.makeText(applicationContext, "Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthErrorRtsp() {
        runOnUiThread {
            Toast.makeText(applicationContext, "Auth error", Toast.LENGTH_SHORT).show()
            rtspClient?.disconnect()
            videoEncoder.stop()
            streaming = false
            viewBinding.etUrl.isEnabled = true
            viewBinding.btnStart.text = "Start"
        }
    }

    override fun onAuthSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(applicationContext, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rtspClient?.disconnect()
        videoEncoder.stop()
    }
}