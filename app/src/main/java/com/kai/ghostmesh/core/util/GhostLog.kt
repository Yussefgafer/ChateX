package com.kai.ghostmesh.core.util

import android.util.Log

object GhostLog {
    private var isAndroid: Boolean? = null

    private fun checkAndroid(): Boolean {
        if (isAndroid == null) {
            isAndroid = try {
                Class.forName("android.util.Log")
                true
            } catch (e: Exception) {
                false
            }
        }
        return isAndroid!!
    }

    fun d(tag: String, msg: String) {
        if (checkAndroid()) {
            try {
                Log.d(tag, msg)
            } catch (e: Exception) {
                println("D/$tag: $msg")
            }
        } else {
            println("D/$tag: $msg")
        }
        LogBuffer.log("D/$tag: $msg")
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (checkAndroid()) {
            try {
                Log.e(tag, msg, tr)
            } catch (e: Exception) {
                println("E/$tag: $msg ${tr?.message ?: ""}")
            }
        } else {
            println("E/$tag: $msg ${tr?.message ?: ""}")
        }
        LogBuffer.log("E/$tag: $msg ${tr?.message ?: ""}")
    }

    fun i(tag: String, msg: String) {
        if (checkAndroid()) {
            try {
                Log.i(tag, msg)
            } catch (e: Exception) {
                println("I/$tag: $msg")
            }
        } else {
            println("I/$tag: $msg")
        }
        LogBuffer.log("I/$tag: $msg")
    }
}
