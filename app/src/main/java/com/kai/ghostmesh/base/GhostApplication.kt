package com.kai.ghostmesh.base

import android.app.Application

class GhostApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
