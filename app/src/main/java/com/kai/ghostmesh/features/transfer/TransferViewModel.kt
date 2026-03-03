package com.kai.ghostmesh.features.transfer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kai.ghostmesh.base.GhostApplication
import com.kai.ghostmesh.core.mesh.FileTransferManager
import com.kai.ghostmesh.core.mesh.MeshManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TransferViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as? GhostApplication)?.container
        ?: (application.applicationContext as? GhostApplication)?.container
    private val meshManager: MeshManager? = container?.meshManager

    private val _transfers = MutableStateFlow<List<FileTransferManager.FileTransfer>>(emptyList())
    val transfers = _transfers.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _transfers.value = meshManager?.fileTransferManager?.getActiveTransfers() ?: emptyList()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun cancelTransfer(fileId: String) {
        meshManager?.fileTransferManager?.cancelTransfer(fileId)
    }
}
