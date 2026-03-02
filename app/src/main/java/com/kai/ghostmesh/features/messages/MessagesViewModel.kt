package com.kai.ghostmesh.features.messages

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.model.RecentChat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MessagesViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as? GhostApplication)?.container 
        ?: (application.applicationContext as GhostApplication).container
    private val repository = container.repository
    private val meshManager = container.meshManager

    val recentChats: StateFlow<List<RecentChat>> = repository.recentChats
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun refreshConnections() {
        meshManager.stop()
        val prefs = getApplication<Application>().getSharedPreferences(com.kai.ghostmesh.core.model.Constants.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        meshManager.startMesh(prefs.getString("nick", "Ghost")!!, prefs.getBoolean("stealth", false))
    }
}
