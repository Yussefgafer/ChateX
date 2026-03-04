import sys

with open('app/src/test/java/com/kai/ghostmesh/data/GhostRepositoryTest.kt', 'r') as f:
    content = f.read()

# Add imports
if 'import kotlinx.coroutines.launch' not in content:
    content = content.replace('import kotlinx.coroutines.flow.first', 'import kotlinx.coroutines.flow.first\nimport kotlinx.coroutines.launch')
if 'import kotlinx.coroutines.test.UnconfinedTestDispatcher' not in content:
    content = content.replace('import kotlinx.coroutines.test.runTest', 'import kotlinx.coroutines.test.runTest\nimport kotlinx.coroutines.test.UnconfinedTestDispatcher\nimport kotlinx.coroutines.test.advanceUntilIdle')
if 'import org.junit.Assert.assertNotNull' not in content:
    content = content.replace('import org.junit.Assert.assertEquals', 'import org.junit.Assert.assertEquals\nimport org.junit.Assert.assertNotNull')

# Update test method
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
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
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
    }"""

if old_test in content:
    content = content.replace(old_test, new_test)
    with open('app/src/test/java/com/kai/ghostmesh/data/GhostRepositoryTest.kt', 'w') as f:
        f.write(content)
    print("Successfully updated GhostRepositoryTest.kt to v2")
else:
    print("Could not find the target test method in GhostRepositoryTest.kt")
    # Show content for debugging
    # print(content)
