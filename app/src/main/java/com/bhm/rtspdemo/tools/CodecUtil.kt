@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.bhm.rtspdemo.tools

import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.EncoderCapabilities
import android.media.MediaCodecList
import android.os.Build
import java.util.*

object CodecUtil {

    const val H264_MIME = "video/avc"
    const val H265_MIME = "video/hevc"
    const val AAC_MIME = "audio/mp4a-latm"
    const val VORBIS_MIME = "audio/ogg"
    const val OPUS_MIME = "audio/opus"

    fun getAllCodecs(filterBroken: Boolean): List<MediaCodecInfo> {
        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val mediaCodecInfos = mediaCodecList.codecInfos
        val mediaCodecInfoList: List<MediaCodecInfo> = ArrayList(listOf(*mediaCodecInfos))
        return if (filterBroken) filterBrokenCodecs(mediaCodecInfoList) else mediaCodecInfoList
    }

    fun getAllHardwareEncoders(mime: String?, cbrPriority: Boolean): List<MediaCodecInfo> {
        val mediaCodecInfoList = getAllEncoders(mime)
        val mediaCodecInfoHardware: MutableList<MediaCodecInfo> = ArrayList()
        val mediaCodecInfoHardwareCBR: MutableList<MediaCodecInfo> = ArrayList()
        for (mediaCodecInfo in mediaCodecInfoList) {
            if (isHardwareAccelerated(mediaCodecInfo)) {
                mediaCodecInfoHardware.add(mediaCodecInfo)
                if (cbrPriority && isCBRModeSupported(mediaCodecInfo, mime)) {
                    mediaCodecInfoHardwareCBR.add(mediaCodecInfo)
                }
            }
        }
        mediaCodecInfoHardware.removeAll(mediaCodecInfoHardwareCBR)
        mediaCodecInfoHardware.addAll(0, mediaCodecInfoHardwareCBR)
        return mediaCodecInfoHardware
    }

    fun getAllHardwareDecoders(mime: String?): List<MediaCodecInfo> {
        val mediaCodecInfoList = getAllDecoders(mime)
        val mediaCodecInfoHardware: MutableList<MediaCodecInfo> = ArrayList()
        for (mediaCodecInfo in mediaCodecInfoList) {
            if (isHardwareAccelerated(mediaCodecInfo)) {
                mediaCodecInfoHardware.add(mediaCodecInfo)
            }
        }
        return mediaCodecInfoHardware
    }

    fun getAllSoftwareEncoders(mime: String?, cbrPriority: Boolean): List<MediaCodecInfo> {
        val mediaCodecInfoList = getAllEncoders(mime)
        val mediaCodecInfoSoftware: MutableList<MediaCodecInfo> = ArrayList()
        val mediaCodecInfoSoftwareCBR: MutableList<MediaCodecInfo> = ArrayList()
        for (mediaCodecInfo in mediaCodecInfoList) {
            if (isSoftwareOnly(mediaCodecInfo)) {
                mediaCodecInfoSoftware.add(mediaCodecInfo)
                if (cbrPriority && isCBRModeSupported(mediaCodecInfo, mime)) {
                    mediaCodecInfoSoftwareCBR.add(mediaCodecInfo)
                }
            }
        }
        mediaCodecInfoSoftware.removeAll(mediaCodecInfoSoftwareCBR)
        mediaCodecInfoSoftware.addAll(0, mediaCodecInfoSoftwareCBR)
        return mediaCodecInfoSoftware
    }

    fun getAllSoftwareDecoders(mime: String?): List<MediaCodecInfo> {
        val mediaCodecInfoList = getAllDecoders(mime)
        val mediaCodecInfoSoftware: MutableList<MediaCodecInfo> = ArrayList()
        for (mediaCodecInfo in mediaCodecInfoList) {
            if (isSoftwareOnly(mediaCodecInfo)) {
                mediaCodecInfoSoftware.add(mediaCodecInfo)
            }
        }
        return mediaCodecInfoSoftware
    }

    /**
     * choose encoder by mime.
     */
    fun getAllEncoders(mime: String?): List<MediaCodecInfo> {
        val mediaCodecInfoList: MutableList<MediaCodecInfo> = ArrayList()
        val mediaCodecInfos = getAllCodecs(true)
        for (mci in mediaCodecInfos) {
            if (!mci.isEncoder) {
                continue
            }
            val types = mci.supportedTypes
            for (type in types) {
                if (type.equals(mime, ignoreCase = true)) {
                    mediaCodecInfoList.add(mci)
                }
            }
        }
        return mediaCodecInfoList
    }

    fun getAllEncoders(
        mime: String?,
        hardwarePriority: Boolean,
        cbrPriority: Boolean
    ): List<MediaCodecInfo> {
        val mediaCodecInfoList: MutableList<MediaCodecInfo> = ArrayList()
        if (hardwarePriority) {
            mediaCodecInfoList.addAll(getAllHardwareEncoders(mime, cbrPriority))
            mediaCodecInfoList.addAll(getAllSoftwareEncoders(mime, cbrPriority))
        } else {
            mediaCodecInfoList.addAll(getAllEncoders(mime))
        }
        return mediaCodecInfoList
    }

    /**
     * choose decoder by mime.
     */
    fun getAllDecoders(mime: String?): List<MediaCodecInfo> {
        val mediaCodecInfoList: MutableList<MediaCodecInfo> = ArrayList()
        val mediaCodecInfos = getAllCodecs(true)
        for (mci in mediaCodecInfos) {
            if (mci.isEncoder) {
                continue
            }
            val types = mci.supportedTypes
            for (type in types) {
                if (type.equals(mime, ignoreCase = true)) {
                    mediaCodecInfoList.add(mci)
                }
            }
        }
        return mediaCodecInfoList
    }

    fun getAllDecoders(mime: String?, hardwarePriority: Boolean): List<MediaCodecInfo> {
        val mediaCodecInfoList: MutableList<MediaCodecInfo> = ArrayList()
        if (hardwarePriority) {
            mediaCodecInfoList.addAll(getAllHardwareDecoders(mime))
            mediaCodecInfoList.addAll(getAllSoftwareDecoders(mime))
        } else {
            mediaCodecInfoList.addAll(getAllDecoders(mime))
        }
        return mediaCodecInfoList
    }

    /* Adapted from google/ExoPlayer
   * https://github.com/google/ExoPlayer/commit/48555550d7fcf6953f2382466818c74092b26355
   */
    private fun isHardwareAccelerated(codecInfo: MediaCodecInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= 29) {
            codecInfo.isHardwareAccelerated
        } else !isSoftwareOnly(codecInfo)
        // codecInfo.isHardwareAccelerated() != codecInfo.isSoftwareOnly() is not necessarily true.
        // However, we assume this to be true as an approximation.
    }

    /* Adapted from google/ExoPlayer
   * https://github.com/google/ExoPlayer/commit/48555550d7fcf6953f2382466818c74092b26355
   */
    private fun isSoftwareOnly(mediaCodecInfo: MediaCodecInfo): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            //mediaCodecInfo.isSoftwareOnly() is not working on emulators.
            //Use !mediaCodecInfo.isHardwareAccelerated() to make sure that all codecs are classified as software or hardware
            return !mediaCodecInfo.isHardwareAccelerated
        }
        val name = mediaCodecInfo.name.lowercase(Locale.getDefault())
        return if (name.startsWith("arc.")) { // App Runtime for Chrome (ARC) codecs
            false
        } else name.startsWith("omx.google.")
                || name.startsWith("omx.ffmpeg.")
                || name.startsWith("omx.sec.") && name.contains(".sw.")
                || name == "omx.qcom.video.decoder.hevcswvdec" || name.startsWith("c2.android.")
                || name.startsWith("c2.google.")
                || !name.startsWith("omx.") && !name.startsWith("c2.")
    }

    @JvmStatic
    fun isCBRModeSupported(mediaCodecInfo: MediaCodecInfo, mime: String?): Boolean {
        val codecCapabilities = mediaCodecInfo.getCapabilitiesForType(mime)
        val encoderCapabilities = codecCapabilities.encoderCapabilities
        return encoderCapabilities.isBitrateModeSupported(
            EncoderCapabilities.BITRATE_MODE_CBR
        )
    }

    /**
     * Filter broken codecs by name and device model.
     *
     * Note:
     * There is no way to know broken encoders so we will check by name and device.
     * Please add your encoder to this method if you detect one.
     *
     * @param codecs All device codecs
     * @return a list without broken codecs
     */
    private fun filterBrokenCodecs(codecs: List<MediaCodecInfo>): List<MediaCodecInfo> {
        val listFilter: MutableList<MediaCodecInfo> = ArrayList()
        val listLowPriority: MutableList<MediaCodecInfo> = ArrayList()
        val listUltraLowPriority: MutableList<MediaCodecInfo> = ArrayList()
        for (mediaCodecInfo in codecs) {
            if (isValid(mediaCodecInfo.name)) {
                when (checkCodecPriority(mediaCodecInfo.name)) {
                    CodecPriority.ULTRA_LOW -> listUltraLowPriority.add(mediaCodecInfo)
                    CodecPriority.LOW -> listLowPriority.add(mediaCodecInfo)
                    CodecPriority.NORMAL -> listFilter.add(mediaCodecInfo)
                }
            }
        }
        listFilter.addAll(listLowPriority)
        listFilter.addAll(listUltraLowPriority)
        return listFilter
    }

    /**
     * For now, none broken codec reported.
     */
    private fun isValid(name: String): Boolean {
        //This encoder is invalid and produce errors (Only found in AVD API 16)
        return !name.equals("aacencoder", ignoreCase = true)
    }

    /**
     * Few devices have codecs that is not working properly in few cases like using AWS MediaLive or YouTube
     * but it is still usable in most of cases.
     * @return priority level.
     */
    private fun checkCodecPriority(name: String): CodecPriority {
        //maybe only broke on samsung with Android 12+ using YouTube and AWS MediaLive
        // but set as ultra low priority in all cases.
        return if (name.equals(
                "c2.sec.aac.encoder",
                ignoreCase = true
            )
        ) CodecPriority.ULTRA_LOW else if (name.equals(
                "omx.google.aac.encoder",
                ignoreCase = true
            )
        ) CodecPriority.LOW else CodecPriority.NORMAL
    }

    enum class Force {
        FIRST_COMPATIBLE_FOUND, SOFTWARE, HARDWARE
    }

    private enum class CodecPriority {
        NORMAL, LOW, ULTRA_LOW
    }
}