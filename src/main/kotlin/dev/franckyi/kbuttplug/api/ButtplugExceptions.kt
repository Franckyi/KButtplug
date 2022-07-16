package dev.franckyi.kbuttplug.api

/**
 * Root class for all exceptions thrown by Buttplug.
 *
 * @see ButtplugConnectorException
 * @see ButtplugDeviceException
 * @see ButtplugHandshakeException
 * @see ButtplugMessageException
 * @see ButtplugPingException
 * @see ButtplugUnknownException
 */
sealed class ButtplugException(msg: String?) : Exception(msg)

/**
 * Thrown when an error occurs while connecting to the server.
 */
class ButtplugConnectorException(msg: String?) : ButtplugException(msg)

/**
 * Thrown when a command sent to a device returned an error.
 */
class ButtplugDeviceException(msg: String?) : ButtplugException(msg)

/**
 * Thrown when the handshake with the server fails.
 */
class ButtplugHandshakeException(msg: String?) : ButtplugException(msg)

/**
 * Thrown when a message parsing or permission error occurred.
 */
class ButtplugMessageException(msg: String?) : ButtplugException(msg)

/**
 * Thrown when a ping was not sent to the server in the expected time.
 */
class ButtplugPingException(msg: String?) : ButtplugException(msg)

/**
 * Thrown when an unknown server error occured.
 */
class ButtplugUnknownException(msg: String?) : ButtplugException(msg)