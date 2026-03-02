package com.kai.ghostmesh.base

import android.content.Context
import com.kai.ghostmesh.core.data.local.AppDatabase
import com.kai.ghostmesh.core.data.repository.GhostRepository
import com.kai.ghostmesh.core.mesh.MeshManager
import com.kai.ghostmesh.core.model.Constants
import com.kai.ghostmesh.core.security.SecurityManager
import java.util.*

class AppContainer(private val context: Context) {

    private val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    val myNodeId: String by lazy {
        val storedId = prefs.getString(Constants.KEY_NODE_ID, null)
        if (storedId != null) storedId else {
            val newId = SecurityManager.getNostrPublicKey()
            prefs.edit().putString(Constants.KEY_NODE_ID, newId).apply()
            newId
        }
    }

    val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    val repository: GhostRepository by lazy { GhostRepository(database.messageDao(), database.profileDao()) }

    val meshManager: MeshManager by lazy { MeshManager(context, myNodeId) }
}
