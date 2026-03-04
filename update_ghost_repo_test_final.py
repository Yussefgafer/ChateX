import sys

with open('app/src/test/java/com/kai/ghostmesh/data/GhostRepositoryTest.kt', 'r') as f:
    content = f.read()

# Add imports if missing
imports = [
    'import kotlinx.coroutines.launch',
    'import kotlinx.coroutines.test.UnconfinedTestDispatcher',
    'import kotlinx.coroutines.test.advanceUntilIdle',
    'import org.junit.Assert.assertNotNull',
    'import org.robolectric.annotation.Config'
]

for imp in imports:
    if imp not in content:
        content = content.replace('package com.kai.ghostmesh.data', 'package com.kai.ghostmesh.data\n' + imp)

# Fix class annotations
if '@Config(manifest = Config.NONE)' not in content:
    content = content.replace('@RunWith(RobolectricTestRunner::class)', '@RunWith(RobolectricTestRunner::class)\n@Config(manifest = Config.NONE)')

# Replace the specific test that hangs
old_test = """    @Test
    fun getMessagesForGhostReturnsMappedMessages() = runTest {
        val ghostId = "test-ghost"
        val entities = listOf(
            MessageEntity(id = "1", ghostId = ghostId, senderName = "Ghost", content = "Hi", isMe = false, timestamp = 1000L, status = MessageStatus.DELIVERED, metadata = "{}")
        )
        // Use any() to match any ghostId to be more robust
        every { messageDao.getMessagesForGhost(any()) } returns flowOf(entities)

        // Taking first emission to ensure the Flow completes in the test environment
        val result = repository.getMessagesForGhost(ghostId).first()

        assertEquals(1, result.size)
        assertEquals("Hi", result[0].content)
        assertEquals("Ghost", result[0].sender)
    }"""

new_test = """    @Test
    fun getMessagesForGhostReturnsMappedMessages() = runTest {
        val ghostId = "test-ghost"
        val entities = listOf(
            MessageEntity(id = "1", ghostId = ghostId, senderName = "Ghost", content = "Hi", isMe = false, timestamp = 1000L, status = MessageStatus.DELIVERED, metadata = "{}")
        )
        // Ensure the mock returns a flow that emits the entities
        every { messageDao.getMessagesForGhost(any()) } returns flowOf(entities)

        var result: List<Message>? = null
        // Using backgroundScope.launch to collect the flow as suggested to prevent hangs
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getMessagesForGhost(ghostId).collect {
                result = it
            }
        }

        // Wait for coroutines to process the emission
        advanceUntilIdle()

        assertNotNull("Result should not be null", result)
        assertEquals(1, result?.size)
        assertEquals("Hi", result?.get(0)?.content)
        assertEquals("Ghost", result?.get(0)?.sender)
        job.cancel()
    }"""

if old_test in content:
    content = content.replace(old_test, new_test)
else:
    print("Could not find old test block")

with open('app/src/test/java/com/kai/ghostmesh/data/GhostRepositoryTest.kt', 'w') as f:
    f.write(content)
