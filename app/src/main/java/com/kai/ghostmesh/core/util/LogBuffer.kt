package com.kai.ghostmesh.core.util

import java.util.*

object LogBuffer {
    private const val MAX_LOGS = 500
    private val logs = LinkedList<String>()

    @Synchronized
    fun log(message: String) {
        if (logs.size >= MAX_LOGS) {
            logs.removeFirst()
        }
        logs.addLast("${Date()}: $message")
    }

    @Synchronized
    fun getLogs(): List<String> {
        return logs.toList()
    }
}
