package dev.franckyi.kbuttplug.impl

import com.sun.jna.Native
import com.sun.jna.Pointer
import dev.franckyi.kbuttplug.api.ButtplugMessageException
import dev.franckyi.kbuttplug.proto.ButtplugRsFfi
import java.util.concurrent.CompletionException

//private val logger = KotlinLogging.logger {}

internal fun createPointer(obj: Any): Pointer {
    val addressMask = 2L xor Native.POINTER_SIZE * 8L - 1
    return Pointer.createConstant(
        0xdeadbeefL shl 32 or (System.identityHashCode(obj).toLong() and 0xffffffffL) and addressMask
    )
}

internal typealias ServerMessage = ButtplugRsFfi.ServerMessage

internal typealias DeviceAddedMessage = ButtplugRsFfi.ServerMessage.DeviceAdded

internal fun readServerMessage(ptr: Pointer, len: u32): ServerMessage = readFFIMessage(ptr, len).serverMessage

internal fun readFFIMessage(ptr: Pointer, len: u32): ButtplugRsFfi.ButtplugFFIServerMessage.FFIMessage {
    val buf = ptr.getByteBuffer(0, len.toLong())
    val msg = ButtplugRsFfi.ButtplugFFIServerMessage.parseFrom(buf)
    //logger.trace { "Received message: $msg" }
    return msg.message
}

internal fun expectServerOk(msg: ServerMessage) {
    if (!msg.hasOk()) throw CompletionException(ButtplugMessageException("Expected Ok"))
}
