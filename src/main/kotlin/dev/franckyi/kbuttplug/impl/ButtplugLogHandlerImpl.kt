package dev.franckyi.kbuttplug.impl

import com.sun.jna.Pointer
import dev.franckyi.kbuttplug.api.ButtplugLogHandler
import dev.franckyi.kbuttplug.api.ButtplugLogMessage
import dev.franckyi.kbuttplug.api.Level
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicBoolean

class ButtplugLogHandlerImpl(level: Level, useJson: Boolean, callback: (String) -> Unit) : ButtplugLogHandler {
    private var logHandle: Pointer?
    private var callback: ButtplugLogCallback?

    init {
        check(logHandlerActive.compareAndSet(false, true)) { "There is already an active log handler!" }
        this.callback = ButtplugLogCallback.of { _, str -> callback(str) }
        logHandle = ButtplugFFI.INSTANCE.buttplug_create_log_handle(this.callback!!, null, level.value, useJson)
    }

    override fun close() {
        synchronized(this) {
            logHandle?.let {
                ButtplugFFI.INSTANCE.buttplug_free_log_handle(it)
                logHandle = null
                callback = null
                logHandlerActive.set(false)
            }
        }
    }

    companion object {
        private val logHandlerActive = AtomicBoolean(false)
        private val json = Json { ignoreUnknownKeys = true }

        fun activateBuiltinLogger() {
            check(logHandlerActive.compareAndSet(false, true)) { "There is already an active log handler!" }
            ButtplugFFI.INSTANCE.buttplug_activate_env_logger()
        }

        fun createLogger(level: Level, useJson: Boolean, callback: (String) -> Unit) =
            ButtplugLogHandlerImpl(level, useJson, callback)

        fun createAdvancedLogger(level: Level, callback: (ButtplugLogMessage) -> Unit) = createLogger(level, true) {
            callback(json.decodeFromString(ButtplugLogMessage.serializer(), it))
        }

        fun createSlf4jLogger(logger: Logger, level: Level, formatter: (ButtplugLogMessage) -> String) =
            createAdvancedLogger(level) {
                val msg = formatter(it)
                when (it.level) {
                    Level.INFO -> logger.info(msg)
                    Level.WARN -> logger.warn(msg)
                    Level.ERROR -> logger.error(msg)
                    Level.DEBUG -> logger.debug(msg)
                    Level.TRACE -> logger.trace(msg)
                }
            }
    }
}