
package com.bhm.rtspdemo.encoder

import android.media.MediaCodec
import android.media.MediaCodec.CodecException
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import com.bhm.rtspdemo.tools.CodecUtil
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.math.max
import kotlin.math.min

abstract class BaseEncoder : EncoderCallback {

    private val tag = "BaseEncoder"
    private val bufferInfo = MediaCodec.BufferInfo()
    private var handlerThread: HandlerThread? = null
    var queue: BlockingQueue<Frame> = ArrayBlockingQueue(80)
    var codec: MediaCodec? = null

    @Volatile
    var isRunning = false
        protected set
    protected var isBufferMode = true
    var force = CodecUtil.Force.FIRST_COMPATIBLE_FOUND
    private var callback: MediaCodec.Callback? = null
    private var oldTimeStamp = 0L
    var shouldReset = true
    private var handler: Handler? = null

    fun restart() {
        start(false)
        initCodec()
    }

    fun start() {
        if (presentTimeUs == 0L) {
            presentTimeUs = System.nanoTime() / 1000
        }
        start(true)
        initCodec()
    }

    protected fun setCallback() {
        handlerThread = HandlerThread(tag)
        handlerThread!!.start()
        handler = Handler(handlerThread!!.looper)
        createAsyncCallback()
        codec!!.setCallback(callback, handler)
    }

    private fun initCodec() {
        codec!!.start()
        isRunning = true
    }

    abstract fun reset()
    abstract fun start(resetTs: Boolean)
    protected abstract fun stopImp()
    protected fun fixTimeStamp(info: MediaCodec.BufferInfo) {
        if (oldTimeStamp > info.presentationTimeUs) {
            info.presentationTimeUs = oldTimeStamp
        } else {
            oldTimeStamp = info.presentationTimeUs
        }
    }

    private fun reloadCodec() {
        //Sometimes encoder crash, we will try recover it. Reset encoder a time if crash
        if (shouldReset) {
            Timber.e("Encoder crashed, trying to recover it")
            reset()
        }
    }

    @JvmOverloads
    fun stop(resetTs: Boolean = true) {
        if (resetTs) {
            presentTimeUs = 0
        }
        isRunning = false
        stopImp()
        handlerThread?.let {
            it.looper?.let { looper ->
                looper.thread.interrupt()
                looper.quit()
            }
            it.quit()
            if (codec != null) {
                try {
                    codec!!.flush()
                } catch (ignored: IllegalStateException) {
                }
            }
            //wait for thread to die for 500ms.
            try {
                it.looper.thread.join(500)
            } catch (ignored: Exception) {
            }
        }
        queue.clear()
        queue = ArrayBlockingQueue(80)
        codec = try {
            codec!!.stop()
            codec!!.release()
            null
        } catch (e: IllegalStateException) {
            null
        } catch (e: NullPointerException) {
            null
        }
        oldTimeStamp = 0L
    }

    protected abstract fun chooseEncoder(mime: String?): MediaCodecInfo?

    @get:Throws(IllegalStateException::class)
    protected val dataFromEncoder: Unit
        get() {
            if (isBufferMode) {
                val inBufferIndex = codec!!.dequeueInputBuffer(0)
                if (inBufferIndex >= 0) {
                    inputAvailable(codec!!, inBufferIndex)
                }
            }
            while (isRunning) {
                val outBufferIndex = codec!!.dequeueOutputBuffer(bufferInfo, 0)
                if (outBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val mediaFormat = codec!!.outputFormat
                    formatChanged(codec!!, mediaFormat)
                } else if (outBufferIndex >= 0) {
                    outputAvailable(codec!!, outBufferIndex, bufferInfo)
                } else {
                    break
                }
            }
        }

    @get:Throws(InterruptedException::class)
    protected abstract val inputFrame: Frame?

    protected abstract fun calculatePts(frame: Frame?, presentTimeUs: Long): Long

    @Throws(IllegalStateException::class)
    private fun processInput(
        byteBuffer: ByteBuffer, mediaCodec: MediaCodec,
        inBufferIndex: Int
    ) {
        try {
            var frame = inputFrame
            while (frame == null) frame = inputFrame
            byteBuffer.clear()
            val size = max(0, min(frame.size, byteBuffer.remaining()) - frame.offset)
            byteBuffer.put(frame.buffer, frame.offset, size)
            val pts = calculatePts(frame, presentTimeUs)
            mediaCodec.queueInputBuffer(inBufferIndex, 0, size, pts, 0)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: NullPointerException) {
            Timber.i(e, "Encoding error")
        } catch (e: IndexOutOfBoundsException) {
            Timber.e(e, "Encoding error")
        }
    }

    protected abstract fun checkBuffer(
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    )

    protected abstract fun sendBuffer(
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    )

    @Throws(IllegalStateException::class)
    private fun processOutput(
        byteBuffer: ByteBuffer, mediaCodec: MediaCodec,
        outBufferIndex: Int, bufferInfo: MediaCodec.BufferInfo
    ) {
        checkBuffer(byteBuffer, bufferInfo) //校验buffer
        sendBuffer(byteBuffer, bufferInfo) //发送buffer
        mediaCodec.releaseOutputBuffer(outBufferIndex, false) //释放
    }

    @JvmName("setForce1")
    fun setForce(force: CodecUtil.Force) {
        this.force = force
    }

    @Throws(IllegalStateException::class)
    override fun inputAvailable(mediaCodec: MediaCodec, inBufferIndex: Int) {
        val byteBuffer: ByteBuffer? = mediaCodec.getInputBuffer(inBufferIndex)
        processInput(byteBuffer!!, mediaCodec, inBufferIndex)
    }

    @Throws(IllegalStateException::class)
    override fun outputAvailable(
        mediaCodec: MediaCodec, outBufferIndex: Int,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        val byteBuffer: ByteBuffer? = mediaCodec.getOutputBuffer(outBufferIndex)
        processOutput(byteBuffer!!, mediaCodec, outBufferIndex, bufferInfo)
    }

    private fun createAsyncCallback() {
        callback = object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(mediaCodec: MediaCodec, inBufferIndex: Int) {
                try {
                    inputAvailable(mediaCodec, inBufferIndex)
                } catch (e: IllegalStateException) {
                    Timber.e(e, "Encoding error")
                    reloadCodec()
                }
            }

            override fun onOutputBufferAvailable(
                mediaCodec: MediaCodec, outBufferIndex: Int,
                bufferInfo: MediaCodec.BufferInfo
            ) {
                try {
                    outputAvailable(mediaCodec, outBufferIndex, bufferInfo)
                } catch (e: IllegalStateException) {
                    Timber.e(e, "Encoding error")
                    reloadCodec()
                }
            }

            override fun onError(mediaCodec: MediaCodec, e: CodecException) {
                Timber.e(e, "Error")
            }

            override fun onOutputFormatChanged(
                mediaCodec: MediaCodec,
                mediaFormat: MediaFormat
            ) {
                formatChanged(mediaCodec, mediaFormat)
            }
        }
    }

    companion object {
        @JvmStatic
        protected var presentTimeUs: Long = 0
    }
}