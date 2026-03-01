package com.kai.ghostmesh.base

import android.app.Application
import com.kai.ghostmesh.base.DebugToolbox

class GhostApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        DebugToolbox.init(this)
    }
}
