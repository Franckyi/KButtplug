package dev.franckyi.kbuttplug.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Create a log handler that forwards the log messages through SLF4J.
 *
 * @param level the logging level
 * @return the log handler
 */
fun ButtplugLogHandler.Companion.createSlf4jLogger(level: Level = Level.Trace): ButtplugLogHandler =
    ButtplugLogHandler(level, true) {
        val log = json.decodeFromString(LogMessage.serializer(), it)
        val msg = "[${log.target}] ${log.fields.message}"
        when (log.level) {
            "INFO" -> logger.info(msg)
            "WARN" -> logger.warn(msg)
            "ERROR" -> logger.error(msg)
            "DEBUG" -> logger.debug(msg)
            "TRACE" -> logger.trace(msg)
        }
    }

private val logger = LoggerFactory.getLogger("buttplug_rs_ffi")

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private class LogMessage(val level: String, val fields: Fields, val target: String) {
    @Serializable
    class Fields(val message: String)
}