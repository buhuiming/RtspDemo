package com.bhm.rtspdemo.encoder

class FpsLimiter {
    private var startTS = System.currentTimeMillis()
    private var ratioF = (1000 / 30).toLong()
    private var ratio = (1000 / 30).toLong()
    fun setFPS(fps: Int) {
        startTS = System.currentTimeMillis()
        ratioF = (1000 / fps).toLong()
        ratio = (1000 / fps).toLong()
    }

    fun limitFPS(): Boolean {
        val lastFrameTimestamp = System.currentTimeMillis() - startTS
        if (ratio < lastFrameTimestamp) {
            ratio += ratioF
            return false
        }
        return true
    }
}