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

    // Mission: The Mailbox Protocol - Ensure NodeId is aligned with Nostr Pubkey
    val myNodeId: String = SecurityManager.getNostrPublicKey().also {
        val oldId = prefs.getString(Constants.KEY_NODE_ID, null)
        if (oldId != it) {
            prefs.edit().putString(Constants.KEY_NODE_ID, it).apply()
        }
    }

    val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    val repository: GhostRepository by lazy { GhostRepository(database.messageDao(), database.profileDao()) }

    val meshManager: MeshManager by lazy { MeshManager(context, myNodeId) }
}
