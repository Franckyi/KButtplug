package dev.franckyi.kbuttplug.impl

import com.sun.jna.Native
import com.sun.jna.Pointer
import dev.franckyi.kbuttplug.api.*
import dev.franckyi.kbuttplug.proto.ButtplugRsFfi
import dev.franckyi.kbuttplug.proto.serverMessageOrNull
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal fun createPointer(obj: Any): Pointer {
    val addressMask = 2L xor Native.POINTER_SIZE * 8L - 1
    return Pointer.createConstant(
        0xdeadbeefL shl 32 or (System.identityHashCode(obj).toLong() and 0xffffffffL) and addressMask
    )
}

internal typealias FFIMessage = ButtplugRsFfi.ButtplugFFIServerMessage.FFIMessage

internal typealias DeviceAddedMessage = ButtplugRsFfi.ServerMessage.DeviceAdded

internal fun readFFIMessage(ptr: Pointer, len: u32): FFIMessage {
    val buf = ptr.getByteBuffer(0, len.toLong())
    val msg = ButtplugRsFfi.ButtplugFFIServerMessage.parseFrom(buf)
    logger.trace { "Received message from server: $msg" }
    return msg.message
}

internal fun expectServerMessage(msg: FFIMessage) =
    checkNotNull(msg.serverMessageOrNull) { "Expected SERVER_MESSAGE message, got ${msg.msgCase.name} instead" }

internal fun expectOk(msg: ButtplugRsFfi.ServerMessage) {
    if (!msg.hasOk()) {
        if (msg.hasError()) {
            throw createButtplugExceptionFromError(msg.error)
        } else {
            throw IllegalStateException("Expected OK message, got ${msg.msgCase.name} instead")
        }
    }
}

internal fun createButtplugExceptionFromError(err: ButtplugRsFfi.ServerMessage.Error): ButtplugException {
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
