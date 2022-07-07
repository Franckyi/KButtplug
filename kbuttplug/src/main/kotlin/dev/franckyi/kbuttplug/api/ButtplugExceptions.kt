package dev.franckyi.kbuttplug.api

import dev.franckyi.kbuttplug.proto.ButtplugRsFfi

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
sealed class ButtplugException(msg: String?) : Exception(msg) {
    companion object {
        /**
         * Parses a Buttplug `Error` message sent by the server to the client
         * into a [ButtplugException] of the corresponding error type.
         *
         * @param err the `Error` message sent by the server to the client
         * @return the [ButtplugException] of the corresponding error type
         */
        fun fromError(err: ButtplugRsFfi.ServerMessage.Error): ButtplugException {
            val msg = err.message
            return when (err.errorType) {
                ButtplugRsFfi.ServerMessage.ButtplugErrorType.ButtplugConnectorError -> ButtplugConnectorException(msg)
                ButtplugRsFfi.ServerMessage.ButtplugErrorType.ButtplugPingError -> ButtplugPingException(msg)
                ButtplugRsFfi.ServerMessage.ButtplugErrorType.ButtplugMessageError -> ButtplugMessageException(msg)
                ButtplugRsFfi.ServerMessage.ButtplugErrorType.ButtplugHandshakeError -> ButtplugHandshakeException(msg)
                ButtplugRsFfi.ServerMessage.ButtplugErrorType.ButtplugDeviceError -> ButtplugDeviceException(msg)
                ButtplugRsFfi.ServerMessage.ButtplugErrorType.ButtplugUnknownError -> ButtplugUnknownException(msg)
                else -> ButtplugUnknownException("Unknown error type: ${err.errorType.number} | Message: $msg")
            }
        }
    }
}

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