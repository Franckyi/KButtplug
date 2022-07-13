package dev.franckyi.kbuttplug.impl

import com.sun.jna.Pointer
import dev.franckyi.kbuttplug.api.*
import dev.franckyi.kbuttplug.proto.ButtplugRsFfi
import dev.franckyi.kbuttplug.proto.DeviceMessageKt
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.fFIMessage
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.linearCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.linearComponent
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.rotateCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.rotateComponent
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.vibrateCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.vibrateComponent
import dev.franckyi.kbuttplug.proto.deviceMessage
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

internal class ButtplugDeviceImpl internal constructor(client: Pointer, msg: DeviceAddedMessage) : ButtplugDevice {
    override val index: Int
    override val name: String
    override val attributes: Map<DeviceAttributeType, DeviceAttributeData>
    private var pointer: Pointer?
    private val futureServerMessageMap: MutableMap<Pointer, CompletableFuture<ServerMessage>> =
        ConcurrentHashMap()
    private val futureDeviceEventMap: MutableMap<Pointer, CompletableFuture<DeviceEventMessage>> =
        ConcurrentHashMap()

    private val onServerResponseCallback = ButtplugCallback.of(::onServerResponse)
    private val onDeviceEventCallback = ButtplugCallback.of(::onDeviceEvent)

    init {
        pointer = ButtplugFFI.INSTANCE.buttplug_create_device(client, u32(msg.index.toLong()))
        index = msg.index
        name = msg.name
        attributes = msg.messageAttributesList.associateByTo(
            EnumMap(DeviceAttributeType::class.java),
            { DeviceAttributeType.inverse[it.messageType]!! },
            { DeviceAttributeData(it.featureCount, it.stepCountList) }
        )
    }

    override fun close() {
        synchronized(this) {
            pointer?.let {
                ButtplugFFI.INSTANCE.buttplug_free_device(it)
                pointer = null
            }
        }
    }

    override fun stop(): CompletableFuture<Void> {
        return sendDeviceMessageForServer {
            this.stopDeviceCmd = defaultStopDeviceCmd
        }.thenAccept(::expectServerOk)
    }

    override fun vibrate(speeds: Map<Int, Double>): CompletableFuture<Void> {
        check(hasAttribute(DeviceAttributeType.VibrateCmd)) { "The device doesn't support VibrateCmd" }
        return sendDeviceMessageForServer {
            this.vibrateCmd = vibrateCmd {
                this.speeds.addAll(
                    speeds.map {
                        vibrateComponent {
                            this.index = it.key
                            this.speed = it.value
                        }
                    }
                )
            }
        }.thenAccept(::expectServerOk)
    }

    override fun vibrate(speed: Double): CompletableFuture<Void> {
        return vibrate(mapFromConst(speed, DeviceAttributeType.VibrateCmd))
    }

    override fun vibrate(speeds: Iterable<Double>): CompletableFuture<Void> {
        return vibrate(mapFromIterable(speeds))
    }

    override fun rotate(components: Map<Int, RotateData>): CompletableFuture<Void> {
        check(hasAttribute(DeviceAttributeType.RotateCmd)) { "The device doesn't support RotateCmd" }
        return sendDeviceMessageForServer {
            this.rotateCmd = rotateCmd {
                this.rotations.addAll(
                    components.map {
                        rotateComponent {
                            this.index = it.key
                            this.speed = it.value.speed
                            this.clockwise = it.value.clockwise
                        }
                    }
                )
            }
        }.thenAccept(::expectServerOk)
    }

    override fun rotate(speed: Double, clockwise: Boolean): CompletableFuture<Void> {
        return rotate(RotateData(speed, clockwise))
    }

    override fun rotate(component: RotateData): CompletableFuture<Void> {
        return rotate(mapFromConst(component, DeviceAttributeType.RotateCmd))
    }

    override fun rotate(components: Iterable<RotateData>): CompletableFuture<Void> {
        return rotate(mapFromIterable(components))
    }

    override fun linear(components: Map<Int, LinearData>): CompletableFuture<Void> {
        check(hasAttribute(DeviceAttributeType.LinearCmd)) { "The device doesn't support LinearCmd" }
        return sendDeviceMessageForServer {
            this.linearCmd = linearCmd {
                this.movements.addAll(
                    components.map {
                        linearComponent {
                            this.index = it.key
                            this.duration = it.value.duration
                            this.position = it.value.position
                        }
                    }
                )
            }
        }.thenAccept(::expectServerOk)
    }

    override fun linear(duration: Int, position: Double): CompletableFuture<Void> {
        return linear(LinearData(duration, position))
    }

    override fun linear(component: LinearData): CompletableFuture<Void> {
        return linear(mapFromConst(component, DeviceAttributeType.LinearCmd))
    }

    override fun linear(components: Iterable<LinearData>): CompletableFuture<Void> {
        return linear(mapFromIterable(components))
    }

    override fun fetchBatteryLevel(): CompletableFuture<Double> {
        check(hasAttribute(DeviceAttributeType.BatteryLevelCmd)) { "The device doesn't support BatteryLevelCmd" }
        return sendDeviceMessageForEvent {
            this.batteryLevelCmd = defaultBatteryLevelCmd
        }.thenApply { it.batteryLevelReading.reading }
    }

    override fun fetchRSSILevel(): CompletableFuture<Int> {
        check(hasAttribute(DeviceAttributeType.RSSILevelCmd)) { "The device doesn't support RSSILevelCmd" }
        return sendDeviceMessageForEvent {
            this.rssiLevelCmd = defaultRSSILevelCmd
        }.thenApply { it.rssiLevelReading.reading }
    }

    private fun <T> mapFromConst(value: T, ty: DeviceAttributeType): Map<Int, T> {
        val count = attributes[ty]?.featureCount ?: 1
        return (0 until count).associateWith { value }
    }

    private fun <T> mapFromIterable(vals: Iterable<T>): Map<Int, T> = vals.mapIndexed { i, v -> i to v }.toMap()

    private fun sendDeviceMessageForServer(block: DeviceMessageBuilder): CompletableFuture<ServerMessage> =
        sendDeviceMessage(block, CompletableFuture(), futureServerMessageMap, onServerResponseCallback)

    private fun sendDeviceMessageForEvent(block: DeviceMessageBuilder): CompletableFuture<DeviceEventMessage> =
        sendDeviceMessage(block, CompletableFuture(), futureDeviceEventMap, onDeviceEventCallback)

    private fun <T> sendDeviceMessage(
        block: DeviceMessageBuilder,
        future: CompletableFuture<T>,
        futureMap: MutableMap<Pointer, CompletableFuture<T>>,
        callback: ButtplugCallback
    ): CompletableFuture<T> {
        val devicePointer = checkNotNull(pointer) { "Attempt to send message when device has already been closed!" }
        val msg = deviceMessage {
            this.index = this@ButtplugDeviceImpl.index
            this.message = fFIMessage(block)
        }
        val ptr = createPointer(future)
        futureMap[ptr] = future
        val buf = msg.toByteArray()
        ButtplugFFI.INSTANCE.buttplug_device_protobuf_message(
            devicePointer, buf,
            i32(buf.size.toLong()), callback, ptr
        )
        return future
    }

    private fun onServerResponse(ctx: Pointer?, ptr: Pointer, len: u32) {
        val message = readServerMessage(ptr, len)
        CompletableFuture.runAsync {
            val future = futureServerMessageMap.remove(ctx)
            if (message.hasError()) {
                future?.completeExceptionally(createButtplugExceptionFromError(message.error))
            }
            future?.complete(message)
        }
    }

    private fun onDeviceEvent(ctx: Pointer?, ptr: Pointer, len: u32) {
        val message = readFFIMessage(ptr, len)
        CompletableFuture.runAsync {
            val future = futureDeviceEventMap.remove(ctx)
            if (message.hasServerMessage() && message.serverMessage.hasError()) {
                future?.completeExceptionally(createButtplugExceptionFromError(message.serverMessage.error))
            }
            future?.complete(message.deviceEvent)
        }
    }
}

private typealias DeviceMessageBuilder = DeviceMessageKt.FFIMessageKt.Dsl.() -> Unit

private typealias DeviceEventMessage = ButtplugRsFfi.DeviceEvent

private val defaultStopDeviceCmd = ButtplugRsFfi.DeviceMessage.StopDeviceCmd.getDefaultInstance()

private val defaultBatteryLevelCmd = ButtplugRsFfi.DeviceMessage.BatteryLevelCmd.getDefaultInstance()

private val defaultRSSILevelCmd = ButtplugRsFfi.DeviceMessage.RSSILevelCmd.getDefaultInstance()