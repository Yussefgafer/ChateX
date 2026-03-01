package com.kai.ghostmesh.base

import android.content.Context
import com.kai.ghostmesh.core.data.local.AppDatabase
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.mesh.MeshManager
import com.kai.ghostmesh.core.model.Constants
import java.util.*

class AppContainer(private val context: Context) {

    private val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    val myNodeId: String = prefs.getString(Constants.KEY_NODE_ID, null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString(Constants.KEY_NODE_ID, it).apply()
    }

    val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    val repository: GhostRepository by lazy { GhostRepository(database.messageDao(), database.profileDao()) }

    val meshManager: MeshManager by lazy { MeshManager(context, myNodeId) }
}
