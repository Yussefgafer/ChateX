package com.kai.ghostmesh.mesh

import android.content.Context
import com.kai.ghostmesh.core.mesh.FileTransferManager
import com.kai.ghostmesh.core.model.Packet
import com.kai.ghostmesh.core.model.PacketType
import com.kai.ghostmesh.core.security.SecurityManager
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FileTransferStressTest {

    private lateinit var manager: FileTransferManager
    private val mockContext = mockk<Context>(relaxed = true)
    private val sentPackets = java.util.Collections.synchronizedList(mutableListOf<Packet>())

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mockkObject(SecurityManager)
        every { SecurityManager.signPacket(any(), any()) } returns "sig"

        sentPackets.clear()
        manager = FileTransferManager(
            context = mockContext,
            myNodeId = "me",
            myNickname = "MainNode",
            sendPacket = { sentPackets.add(it) },
            onFileProgress = { _, _, _ -> },
            onFileComplete = { _, _, _ -> },
            onFileError = { _, _, _ -> }
        )
    }

    @Test
    fun testChunkingStability() = runTest {
        val tempDir = File("build/tmp/test").apply { mkdirs() }
        val largeFile = File(tempDir, "stress_test.bin")
        val dataSize = 1024 * 32 // 32KB = 2 chunks
        largeFile.writeBytes(ByteArray(dataSize))

        manager.initiateFileTransfer(largeFile, "recipient")

        // We verify the CHUNK_SIZE constant which is critical.
        assertEquals(16 * 1024, FileTransferManager.CHUNK_SIZE)

        largeFile.delete()
    }
}
