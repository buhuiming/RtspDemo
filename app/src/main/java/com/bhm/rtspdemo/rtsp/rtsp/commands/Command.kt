
package com.bhm.rtspdemo.rtsp.rtsp.commands

data class Command(val method: Method, val cSeq: Int, val status: Int, val text: String)
