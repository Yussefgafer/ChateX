package com.kai.ghostmesh.core.util

import android.util.Log

object GhostLog {
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        LogBuffer.log("D/$tag: $msg")
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        Log.e(tag, msg, tr)
        LogBuffer.log("E/$tag: $msg ${tr?.message ?: ""}")
    }

    fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        LogBuffer.log("I/$tag: $msg")
    }
}
