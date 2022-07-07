package dev.franckyi.kbuttplug.api

import com.sun.jna.Pointer
import dev.franckyi.kbuttplug.impl.ButtplugFFI
import dev.franckyi.kbuttplug.impl.ButtplugLogCallback
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Represents a log callback that the FFI can call to output log messages.
 * Only one should be constructed at a given time.
 * If you want to change the log handler, you first need to [close] the previous log handler.
 *
 * @param callback the callback to call when a log message is received
 * @param level the logging level
 * @param useJson if true, the log message will be formatted as JSON
 */
class ButtplugLogHandler(level: Level, useJson: Boolean, callback: (String) -> Unit) : AutoCloseable {
    private var logHandle: Pointer?
    private var callback: ButtplugLogCallback?

    init {
        check(logHandlerActive.compareAndSet(false, true)) { "There is already an active log handler!" }
        this.callback = ButtplugLogCallback.of { _, str -> callback(str) }
        logHandle = ButtplugFFI.INSTANCE.buttplug_create_log_handle(this.callback!!, null, level.value, useJson)
    }

    /**
     * Cleans up the log handler and frees the memory.
     * To set up a new log handler, instantiate a new [ButtplugLogHandler].
     */
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

        /**
         * Activates the logger built-in to buttplug, which logs to console.
         *
         * This should not be used in conjunction with an [ButtplugLogHandler] instance.
         * This cannot be deactivated once it has been activated.
         */
        fun activateBuiltinLogger() {
            check(logHandlerActive.compareAndSet(false, true)) { "There is already an active log handler!" }
            ButtplugFFI.INSTANCE.buttplug_activate_env_logger()
        }
    }
}

/**
 * Logging level of a [ButtplugLogHandler]
 */
enum class Level(val value: String) {
    Error("Error"), Warn("Warn"), Info("Info"), Debug("Debug"), Trace("Trace");
}
