package com.bhm.rtspdemo.encoder

import android.graphics.ImageFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.util.Pair
import android.view.Surface
import com.bhm.rtspdemo.tools.CodecUtil
import com.bhm.rtspdemo.tools.YUVUtil
import timber.log.Timber
import java.nio.ByteBuffer

open class VideoEncoder : BaseEncoder() {

    private var spsPpsSetted = false
    private var forceKey = false

    //video data necessary to send after requestKeyframe.
    private var oldSps: ByteBuffer? = null
    private var oldPps: ByteBuffer? = null
    private var oldVps: ByteBuffer? = null

    //surface to buffer encoder
    private var inputSurface: Surface? = null
    private var width = 800
    private var height = 600
    private var fps = 30
    private var bitRate = 1200 * 1024 //in kbps
    private var rotation = 90
    private var iFrameInterval = 2

    //for disable video
    private val fpsLimiter = FpsLimiter()
    private var type: String = CodecUtil.H264_MIME
    private var formatVideoEncoder: FormatVideoEncoder? = FormatVideoEncoder.YUV420Dynamical
    private var avcProfile = -1
    private var avcProfileLevel = -1

    private var onSpsPpsVpsRtp: ((sps: ByteBuffer?, pps: ByteBuffer?, vps: ByteBuffer?) -> Unit)? = null

    private var onVideoData: ((h264Buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) -> Unit)? = null

    fun onListener(
        onSpsPpsVpsRtp: (sps: ByteBuffer?, pps: ByteBuffer?, vps: ByteBuffer?) -> Unit,
        onVideoData: (h264Buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) -> Unit,
    ) {
        this.onSpsPpsVpsRtp = onSpsPpsVpsRtp
        this.onVideoData = onVideoData
    }

    /**
     * Prepare encoder with custom parameters
     */
    fun prepareVideoEncoder(
        width: Int = this.width,
        height: Int = this.height,
        fps: Int = this.fps,
        bitRate: Int = this.bitRate,
        rotation: Int = this.rotation,
        iFrameInterval: Int = this.iFrameInterval,
        formatVideoEncoder: FormatVideoEncoder? = this.formatVideoEncoder,
        avcProfile: Int = this.avcProfile,
        avcProfileLevel: Int = this.avcProfileLevel
    ): Boolean {
        this.width = width
        this.height = height
        this.fps = fps
        this.bitRate = bitRate
        this.rotation = rotation
        this.iFrameInterval = iFrameInterval
        this.formatVideoEncoder = formatVideoEncoder
        this.avcProfile = avcProfile
        this.avcProfileLevel = avcProfileLevel
        isBufferMode = true
        val encoder: MediaCodecInfo? = chooseEncoder(type)
        return try {
            if (encoder != null) {
                Timber.i("Encoder selected " + encoder.name)
                codec = MediaCodec.createByCodecName(encoder.name)
                if (this.formatVideoEncoder === FormatVideoEncoder.YUV420Dynamical) {
                    this.formatVideoEncoder = chooseColorDynamically(encoder)
                    if (this.formatVideoEncoder == null) {
                        Timber.e("YUV420 dynamical choose failed")
                        return false
                    }
                }
            } else {
                Timber.e("Valid encoder not found")
                return false
            }
            val videoFormat: MediaFormat
            //if you dont use mediacodec rotation you need swap width and height in rotation 90 or 270
            // for correct encoding resolution
            val resolution: String
            if (rotation == 90 || rotation == 270) {
                resolution = height.toString() + "x" + width
                videoFormat = MediaFormat.createVideoFormat(type, height, width)
            } else {
                resolution = width.toString() + "x" + height
                videoFormat = MediaFormat.createVideoFormat(type, width, height)
            }
            Timber.i("Prepare video info: " + this.formatVideoEncoder!!.name + ", " + resolution)
            videoFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                this.formatVideoEncoder!!.formatCodec
            )
            videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            //Set CBR mode if supported by encoder.
            if (CodecUtil.isCBRModeSupported(
                    encoder,
                    type
                )
            ) {
                Timber.i("set bitrate mode CBR")
                videoFormat.setInteger(
                    MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
                )
            } else {
                Timber.i("bitrate mode CBR not supported using default mode")
            }
            // Rotation by encoder.
            // Removed because this is ignored by most encoders, producing different results on different devices
            //  videoFormat.setInteger(MediaFormat.KEY_ROTATION, rotation);
            if (this.avcProfile > 0) {
                // MediaFormat.KEY_PROFILE, API > 21
                videoFormat.setInteger("profile", this.avcProfile)
            }
            if (this.avcProfileLevel > 0) {
                // MediaFormat.KEY_LEVEL, API > 23
                videoFormat.setInteger("level", this.avcProfileLevel)
            }
            setCallback()
            codec!!.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            isRunning = false
            if (formatVideoEncoder === FormatVideoEncoder.SURFACE) {
                isBufferMode = false
                inputSurface = codec!!.createInputSurface()
            }
            Timber.i("prepared")
            true
        } catch (e: Exception) {
            Timber.e(e, "Create VideoEncoder failed.")
            stop()
            false
        }
    }

    override fun start(resetTs: Boolean) {
        forceKey = false
        shouldReset = resetTs
        spsPpsSetted = false
        if (resetTs) {
            fpsLimiter.setFPS(fps)
        }
        if (formatVideoEncoder !== FormatVideoEncoder.SURFACE) {
            YUVUtil.preAllocateBuffers(width * height * 3 / 2)
        }
        Timber.i("started")
    }

    override fun stopImp() {
        spsPpsSetted = false
        if (inputSurface != null) inputSurface!!.release()
        inputSurface = null
        oldSps = null
        oldPps = null
        oldVps = null
        Timber.i("stopped")
    }

    override fun reset() {
        stop(false)
        prepareVideoEncoder(
            width, height, fps, bitRate, rotation, iFrameInterval, formatVideoEncoder,
            avcProfile, avcProfileLevel
        )
        restart()
    }

    private fun chooseColorDynamically(mediaCodecInfo: MediaCodecInfo): FormatVideoEncoder? {
        for (color in mediaCodecInfo.getCapabilitiesForType(type).colorFormats) {
            if (color == FormatVideoEncoder.YUV420PLANAR.formatCodec) {
                return FormatVideoEncoder.YUV420PLANAR
            } else if (color == FormatVideoEncoder.YUV420SEMIPLANAR.formatCodec) {
                return FormatVideoEncoder.YUV420SEMIPLANAR
            }
        }
        return null
    }

//    fun setVideoBitrateOnFly(bitrate: Int) {
//        if (isRunning) {
//            bitRate = bitrate
//            val bundle = Bundle()
//            bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrate)
//            try {
//                codec!!.setParameters(bundle)
//            } catch (e: IllegalStateException) {
//                Timber.e(e, "encoder need be running")
//            }
//        }
//    }

    private fun requestKeyframe() {
        if (isRunning) {
            if (spsPpsSetted) {
                val bundle = Bundle()
                bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                try {
                    codec!!.setParameters(bundle)
                    onSpsPpsVpsRtp?.let {
                        it(oldSps, oldPps, oldVps)
                    }
                } catch (e: IllegalStateException) {
                    Timber.e(e, "encoder need be running")
                }
            } else {
                //You need wait until encoder generate first frame.
                forceKey = true
            }
        }
    }

    fun inputYUVData(frame: Frame?) {
        if (isRunning && !queue.offer(frame)) {
            Timber.i("frame discarded")
        }
    }

    private fun sendSPSandPPS(mediaFormat: MediaFormat) {
        //H265
        if (type == CodecUtil.H265_MIME) {
            val byteBufferList = extractVpsSpsPpsFromH265(mediaFormat.getByteBuffer("csd-0")!!)
            oldSps = byteBufferList[1]
            oldPps = byteBufferList[2]
            oldVps = byteBufferList[0]
            onSpsPpsVpsRtp?.let {
                it(oldSps, oldPps, oldVps)
            }
            //H264
        } else {
            oldSps = mediaFormat.getByteBuffer("csd-0")
            oldPps = mediaFormat.getByteBuffer("csd-1")
            oldVps = null
            onSpsPpsVpsRtp?.let {
                it(oldSps, oldPps, oldVps)
            }
        }
    }

    /**
     * choose the video encoder by mime.
     */
    override fun chooseEncoder(mime: String?): MediaCodecInfo? {
        val mediaCodecInfoList: List<MediaCodecInfo> = if (force === CodecUtil.Force.HARDWARE) {
            CodecUtil.getAllHardwareEncoders(mime, true)
        } else if (force === CodecUtil.Force.SOFTWARE) {
            CodecUtil.getAllSoftwareEncoders(mime, true)
        } else {
            //Priority: hardware CBR > hardware > software CBR > software
            CodecUtil.getAllEncoders(mime, hardwarePriority = true, cbrPriority = true)
        }
        Timber.i(mediaCodecInfoList.size.toString() + " encoders found")
        for (mci in mediaCodecInfoList) {
            Timber.i("Encoder " + mci.name)
            val codecCapabilities: MediaCodecInfo.CodecCapabilities = mci.getCapabilitiesForType(mime)
            for (color in codecCapabilities.colorFormats) {
                Timber.i("Color supported: $color")
                if (formatVideoEncoder === FormatVideoEncoder.SURFACE) {
                    if (color == FormatVideoEncoder.SURFACE.formatCodec) return mci
                } else {
                    //check if encoder support any yuv420 color
                    if (color == FormatVideoEncoder.YUV420PLANAR.formatCodec
                        || color == FormatVideoEncoder.YUV420SEMIPLANAR.formatCodec
                    ) {
                        return mci
                    }
                }
            }
        }
        return null
    }

    /**
     * decode sps and pps if the encoder never call to MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
     */
    private fun decodeSpsPpsFromBuffer(
        outputBuffer: ByteBuffer,
        length: Int
    ): Pair<ByteBuffer, ByteBuffer>? {
        val csd = ByteArray(length)
        outputBuffer[csd, 0, length]
        outputBuffer.rewind()
        var i = 0
        var spsIndex = -1
        var ppsIndex = -1
        while (i < length - 4) {
            if (csd[i].toInt() == 0 && csd[i + 1].toInt() == 0 && csd[i + 2].toInt() == 0 && csd[i + 3].toInt() == 1) {
                if (spsIndex == -1) {
                    spsIndex = i
                } else {
                    ppsIndex = i
                    break
                }
            }
            i++
        }
        if (spsIndex != -1 && ppsIndex != -1) {
            val sps = ByteArray(ppsIndex)
            System.arraycopy(csd, spsIndex, sps, 0, ppsIndex)
            val pps = ByteArray(length - ppsIndex)
            System.arraycopy(csd, ppsIndex, pps, 0, length - ppsIndex)
            return Pair(ByteBuffer.wrap(sps), ByteBuffer.wrap(pps))
        }
        return null
    }

    /**
     * You need find 0 0 0 1 byte sequence that is the initiation of vps, sps and pps
     * buffers.
     *
     * @param csd0byteBuffer get in mediacodec case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
     * @return list with vps, sps and pps
     */
    private fun extractVpsSpsPpsFromH265(csd0byteBuffer: ByteBuffer): List<ByteBuffer> {
        val byteBufferList: MutableList<ByteBuffer> = ArrayList()
        var vpsPosition = -1
        var spsPosition = -1
        var ppsPosition = -1
        var contBufferInitiation = 0
        val length = csd0byteBuffer.remaining()
        val csdArray = ByteArray(length)
        csd0byteBuffer[csdArray, 0, length]
        csd0byteBuffer.rewind()
        for (i in csdArray.indices) {
            if (contBufferInitiation == 3 && csdArray[i].toInt() == 1) {
                if (vpsPosition == -1) {
                    vpsPosition = i - 3
                } else if (spsPosition == -1) {
                    spsPosition = i - 3
                } else {
                    ppsPosition = i - 3
                }
            }
            if (csdArray[i].toInt() == 0) {
                contBufferInitiation++
            } else {
                contBufferInitiation = 0
            }
        }
        val vps = ByteArray(spsPosition)
        val sps = ByteArray(ppsPosition - spsPosition)
        val pps = ByteArray(csdArray.size - ppsPosition)
        for (i in csdArray.indices) {
            if (i < spsPosition) {
                vps[i] = csdArray[i]
            } else if (i < ppsPosition) {
                sps[i - spsPosition] = csdArray[i]
            } else {
                pps[i - ppsPosition] = csdArray[i]
            }
        }
        byteBufferList.add(ByteBuffer.wrap(vps))
        byteBufferList.add(ByteBuffer.wrap(sps))
        byteBufferList.add(ByteBuffer.wrap(pps))
        return byteBufferList
    }

    @get:Throws(InterruptedException::class)
    override val inputFrame: Frame? = null
        get() {
            val frame = queue.take() ?: return null
            if (fpsLimiter.limitFPS()) return field
            var buffer = frame.buffer
            val isYV12 = frame.format == ImageFormat.YV12
            var orientation = if (frame.isFlip) frame.orientation + 180 else frame.orientation
            if (orientation >= 360) orientation -= 360
            buffer = if (isYV12) YUVUtil.rotateYV12(
                buffer,
                width,
                height,
                orientation
            )!! else YUVUtil.rotateNV21(buffer, width, height, orientation)!!
            buffer = if (isYV12) YUVUtil.YV12toYUV420byColor(
                buffer,
                width,
                height,
                formatVideoEncoder
            )!! else YUVUtil.NV21toYUV420byColor(buffer, width, height, formatVideoEncoder)!!
            frame.buffer = buffer
            return frame
        }

    override fun calculatePts(frame: Frame?, presentTimeUs: Long): Long {
        return System.nanoTime() / 1000 - presentTimeUs
    }

    override fun formatChanged(mediaCodec: MediaCodec, mediaFormat: MediaFormat) {
        sendSPSandPPS(mediaFormat)
        spsPpsSetted = true
    }

    override fun checkBuffer(
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        if (forceKey) {
            forceKey = false
            requestKeyframe()
        }
        fixTimeStamp(bufferInfo)
        if (!spsPpsSetted && type == CodecUtil.H264_MIME) {
            Timber.i("formatChanged not called, doing manual sps/pps extraction...")
            val buffers = decodeSpsPpsFromBuffer(byteBuffer.duplicate(), bufferInfo.size)
            if (buffers != null) {
                Timber.i("manual sps/pps extraction success")
                oldSps = buffers.first
                oldPps = buffers.second
                oldVps = null
                onSpsPpsVpsRtp?.let {
                    it(oldSps, oldPps, oldVps)
                }
                spsPpsSetted = true
            } else {
                Timber.e("manual sps/pps extraction failed")
            }
        } else if (!spsPpsSetted && type == CodecUtil.H265_MIME) {
            Timber.i("formatChanged not called, doing manual vps/sps/pps extraction...")
            val byteBufferList = extractVpsSpsPpsFromH265(byteBuffer)
            if (byteBufferList.size == 3) {
                Timber.i("manual vps/sps/pps extraction success")
                oldSps = byteBufferList[1]
                oldPps = byteBufferList[2]
                oldVps = byteBufferList[0]
                onSpsPpsVpsRtp?.let {
                    it(oldSps, oldPps, oldVps)
                }
                spsPpsSetted = true
            } else {
                Timber.e("manual vps/sps/pps extraction failed")
            }
        }
        if (formatVideoEncoder === FormatVideoEncoder.SURFACE) {
            bufferInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs
        }
    }

    override fun sendBuffer(
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        onVideoData?.let {
            it(byteBuffer, bufferInfo)
        }
    }
}