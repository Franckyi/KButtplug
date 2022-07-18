package dev.franckyi.kbuttplug.api

import dev.franckyi.kbuttplug.api.ButtplugLogHandler.Companion.activateBuiltinLogger
import dev.franckyi.kbuttplug.api.ButtplugLogHandler.Companion.createAdvancedLogger
import dev.franckyi.kbuttplug.api.ButtplugLogHandler.Companion.createLogger
import dev.franckyi.kbuttplug.api.ButtplugLogHandler.Companion.createSlf4jLogger
import dev.franckyi.kbuttplug.impl.ButtplugLogHandlerImpl
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents a log callback that the FFI can call to output log messages.
 *
 * You can either use the builtin log handler which logs to console using [activateBuiltinLogger],
 * or you can instanciate your own logger with [createLogger], [createAdvancedLogger] or [createSlf4jLogger].
 *
 * Only one instance of [ButtplugLogHandler] should be constructed at a given time.
 * If you want to change the log handler, you first need to [close] the previous log handler.
 */
interface ButtplugLogHandler : AutoCloseable {
    companion object {
        /**
         * Activates the logger built-in to buttplug, which logs to console.
         *
         * This should not be used in conjunction with a [ButtplugLogHandler] instance.
         * **This cannot be deactivated once it has been activated.**
         */
        fun activateBuiltinLogger() = ButtplugLogHandlerImpl.activateBuiltinLogger()

        /**
         * Creates a new [ButtplugLogHandler] that calls the [callback] everytime a log message is received.
         *
         * @param level the maximum supported logging level
         * @param useJson if true, the log message will be formatted as JSON
         * @param callback the callback to call when a log message is received
         * @return the log handler
         */
        fun createLogger(
            level: Level = Level.TRACE,
            useJson: Boolean = false,
            callback: (String) -> Unit
        ): ButtplugLogHandler = ButtplugLogHandlerImpl.createLogger(level, useJson, callback)

        /**
         * Creates a new [ButtplugLogHandler] that calls the [callback] everytime a log message is received.
         *
         * The difference with [createLogger] is that it will parse JSON log strings into a [ButtplugLogMessage] object
         * that you can use in the callback.
         *
         * @param level the maximum supported logging level
         * @param callback the callback to call when a log message is received
         * @return the log handler
         */
        fun createAdvancedLogger(
            level: Level = Level.TRACE,
            callback: (ButtplugLogMessage) -> Unit
        ): ButtplugLogHandler = ButtplugLogHandlerImpl.createAdvancedLogger(level, callback)

        /**
         * Creates a new [ButtplugLogHandler] that forwards the log messages to a SLF4J [Logger].
         *
         * @param logger the logger to use
         * @param level the maximum supported logging level
         * @param format the format of the log message
         * @return the log handler
         * @see ButtplugLogMessage
         */
        fun createSlf4jLogger(
            logger: Logger = LoggerFactory.getLogger("buttplug_rs_ffi"),
            level: Level = Level.TRACE,
            format: (ButtplugLogMessage) -> String = { "[${it.target}] ${it.fields.message}" }
        ): ButtplugLogHandler = ButtplugLogHandlerImpl.createSlf4jLogger(logger, level, format)
    }
}

/**
 * Data class of a JSON log message received by the connector and parsed.
 *
 * @property timestamp The timestamp of the log message.
 * @property level The level of the log message.
 * @property fields The contents of the log message.
 * @see ButtplugLogHandler.createAdvancedLogger
 */
@Serializable
data class ButtplugLogMessage(
    val timestamp: String,
    val level: Level,
    val fields: Fields,
    val target: String,
    val span: Span? = null,
    val spans: List<Span>? = null
) {
    /**
     * The contents of a log message.
     *
     * @property message The message of the log message.
     */
    @Serializable
    data class Fields(val message: String)

    @Serializable
    data class Span(val name: String, val id: Int? = null)
}

/**
 * Logging level of a [ButtplugLogHandler].
 */
enum class Level(val value: String) {
    /**
     * @see Logger.error
     */
    ERROR("Error"),

    /**
     * @see Logger.warn
     */
    WARN("Warn"),

    /**
     * @see Logger.info
     */
    INFO("Info"),

    /**
     * @see Logger.debug
     */
    DEBUG("Debug"),

    /**
     * @see Logger.trace
     */
    TRACE("Trace");
}
