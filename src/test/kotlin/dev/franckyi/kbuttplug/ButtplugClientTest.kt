package dev.franckyi.kbuttplug

import dev.franckyi.kbuttplug.api.ButtplugClient
import dev.franckyi.kbuttplug.api.ButtplugLogHandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class ButtplugClientTest {
    @Test
    fun testClientLifecycle() {
        val logHandler = ButtplugLogHandler.createSlf4jLogger(LoggerFactory.getLogger("TestLogger"))
        val client = ButtplugClient.create("Test")
        logHandler.use {
            client.use {
                it.connectLocal().get()
                assertTrue(it.connected)
                it.startScanning().get()
                assertTrue(it.scanning)
                Thread.sleep(1000)
                it.stopScanning().get()
                assertFalse(it.scanning)
                it.disconnect().get()
                assertFalse(it.connected)
                assertThrows(IllegalStateException::class.java) { it.startScanning().get() }
            }
        }
    }
}