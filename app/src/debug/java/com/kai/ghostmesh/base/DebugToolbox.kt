package com.kai.ghostmesh.base

import android.app.Application
import com.kai.ghostmesh.BuildConfig
import com.kai.ghostmesh.core.util.LogBuffer

object DebugToolbox {
    private var server: DebugLogServer? = null

    fun init(application: Application) {
        if (BuildConfig.DEBUG) {
            LogBuffer.log("Debug Toolbox Initialized")
            server = DebugLogServer(8080)
            server?.start()
        }
    }

    fun stop() {
        server?.stop()
        server = null
    }
}
