package com.kai.ghostmesh.base

import com.kai.ghostmesh.core.util.LogBuffer
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class DebugLogServer(port: Int) : NanoHTTPD(port) {
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        return if (uri == "/logs") {
            val logs = LogBuffer.getLogs().joinToString("\n")
            newFixedLengthResponse(Response.Status.OK, "text/plain", logs)
        } else {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }

    override fun start() {
        try {
            super.start(SOCKET_READ_TIMEOUT, false)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
