package com.kai.ghostmesh.base

import android.app.Application
import com.kai.ghostmesh.base.DebugToolbox

class GhostApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        com.kai.ghostmesh.core.security.SecurityManager.init(this)
        container = AppContainer(this)
        DebugToolbox.init(this)
    }
}
