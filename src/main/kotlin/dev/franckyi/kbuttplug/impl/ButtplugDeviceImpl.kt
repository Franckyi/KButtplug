package dev.franckyi.kbuttplug.impl

import com.google.protobuf.ByteString
import com.sun.jna.Pointer
import dev.franckyi.kbuttplug.api.*
import dev.franckyi.kbuttplug.proto.*
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.fFIMessage
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.linearCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.linearComponent
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.rawReadCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.rawSubscribeCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.rawUnsubscribeCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.rawWriteCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.rotateCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.rotateComponent
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.vibrateCmd
import dev.franckyi.kbuttplug.proto.DeviceMessageKt.vibrateComponent
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

internal class ButtplugDeviceImpl internal constructor(client: Pointer, msg: DeviceAddedMessage) : ButtplugDevice {
    override val index: Int
    override val name: String
    override val attributes: Map<DeviceAttributeType, DeviceAttributeData>
    private var pointer: Pointer?
    private val futureResponseMap: MutableMap<Pointer, CompletableFuture<FFIMessage>> = ConcurrentHashMap()

    private val onServerResponseCallback = ButtplugCallback.of(::onServerResponse)
    private val endpointSubscriptions = mutableMapOf<Endpoint, EndpointCallback>()

    init {
        pointer = ButtplugFFI.INSTANCE.buttplug_create_device(client, u32(msg.index.toLong()))
        index = msg.index
        name = msg.name
        attributes = msg.messageAttributesList.associateByTo(
            EnumMap(DeviceAttributeType::class.java),
            { DeviceAttributeType.inverse[it.messageType]!! },
            { DeviceAttributeData(it.featureCount, it.stepCountList, it.endpointsList.map { Endpoint.inverse[it]!! }) }
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
        return sendDeviceMessage {
            stopDeviceCmd = defaultStopDeviceCmd
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
    }

    override fun vibrate(speeds: Map<Int, Double>): CompletableFuture<Void> {
        check(hasAttribute(DeviceAttributeType.VIBRATE)) { "The device $name doesn't support VibrateCmd" }
        return sendDeviceMessage {
            vibrateCmd = vibrateCmd {
                this.speeds.addAll(
                    speeds.map {
                        vibrateComponent {
                            index = it.key
                            speed = it.value
                        }
                    }
                )
            }
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
    }

    override fun vibrate(speed: Double): CompletableFuture<Void> {
        return vibrate(mapFromConst(speed, DeviceAttributeType.VIBRATE))
    }

    override fun vibrate(speeds: Iterable<Double>): CompletableFuture<Void> {
        return vibrate(mapFromIterable(speeds))
    }

    override fun rotate(components: Map<Int, RotateData>): CompletableFuture<Void> {
        check(hasAttribute(DeviceAttributeType.ROTATE)) { "The device $name doesn't support RotateCmd" }
        return sendDeviceMessage {
            rotateCmd = rotateCmd {
                rotations.addAll(
                    components.map {
                        rotateComponent {
                            index = it.key
                            speed = it.value.speed
                            clockwise = it.value.clockwise
                        }
                    }
                )
            }
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
    }

    override fun rotate(speed: Double, clockwise: Boolean): CompletableFuture<Void> {
        return rotate(RotateData(speed, clockwise))
    }

    override fun rotate(component: RotateData): CompletableFuture<Void> {
        return rotate(mapFromConst(component, DeviceAttributeType.ROTATE))
    }

    override fun rotate(components: Iterable<RotateData>): CompletableFuture<Void> {
        return rotate(mapFromIterable(components))
    }

    override fun linear(components: Map<Int, LinearData>): CompletableFuture<Void> {
        check(hasAttribute(DeviceAttributeType.LINEAR)) { "The device $name doesn't support LinearCmd" }
        return sendDeviceMessage {
            linearCmd = linearCmd {
                movements.addAll(
                    components.map {
                        linearComponent {
                            index = it.key
                            duration = it.value.duration
                            position = it.value.position
                        }
                    }
                )
            }
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
    }

    override fun linear(duration: Int, position: Double): CompletableFuture<Void> {
        return linear(LinearData(duration, position))
    }

    override fun linear(component: LinearData): CompletableFuture<Void> {
        return linear(mapFromConst(component, DeviceAttributeType.LINEAR))
    }

    override fun linear(components: Iterable<LinearData>): CompletableFuture<Void> {
        return linear(mapFromIterable(components))
    }

    override fun fetchBatteryLevel(): CompletableFuture<Double> {
        check(hasAttribute(DeviceAttributeType.BATTERY_LEVEL)) { "The device $name doesn't support BatteryLevelCmd" }
        return sendDeviceMessage {
            this.batteryLevelCmd = defaultBatteryLevelCmd
        }
            .thenApply(::expectDeviceEvent)
            .thenApply(::expectBatteryLevelReading)
            .thenApply { it.reading }
    }

    override fun fetchRSSILevel(): CompletableFuture<Int> {
        check(hasAttribute(DeviceAttributeType.RSSI_LEVEL)) { "The device $name doesn't support RSSILevelCmd" }
        return sendDeviceMessage {
            this.rssiLevelCmd = defaultRSSILevelCmd
        }
            .thenApply(::expectDeviceEvent)
            .thenApply(::expectRSSILevelReading)
            .thenApply { it.reading }
    }

    override fun rawWrite(endpoint: Endpoint, data: ByteArray, writeWithResponse: Boolean): CompletableFuture<Void> {
        check(hasAttribute(DeviceAttributeType.RAW_WRITE)) { "The device $name doesn't support RawWriteCmd" }
        return sendDeviceMessage {
            rawWriteCmd = rawWriteCmd {
                this.endpoint = endpoint.endpoint
                this.data = ByteString.copyFrom(data)
                this.writeWithResponse = writeWithResponse
            }
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
    }

    override fun rawRead(endpoint: Endpoint, expectedLength: Int, timeout: Int): CompletableFuture<ByteArray> {
        check(hasAttribute(DeviceAttributeType.RAW_READ)) { "The device $name doesn't support RawReadCmd" }
        return sendDeviceMessage {
            this.rawReadCmd = rawReadCmd {
                this.endpoint = endpoint.endpoint
                this.expectedLength = expectedLength
                this.timeout = timeout
            }
        }
            .thenApply(::expectDeviceEvent)
            .thenApply { it.rawReading.data.toByteArray() }
    }

    override fun rawSubscribe(endpoint: Endpoint, callback: EndpointCallback): CompletableFuture<Void> {
        check(hasAttribute(DeviceAttributeType.RAW_SUBSCRIBE)) { "The device $name doesn't support RawSubscribeCmd" }
        return sendDeviceMessage {
            rawSubscribeCmd = rawSubscribeCmd {
                this.endpoint = endpoint.endpoint
            }
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun { endpointSubscriptions[endpoint] = callback }
    }

    override fun rawUnsubscribe(endpoint: Endpoint): CompletableFuture<Void> {
        check(hasAttribute(DeviceAttributeType.RAW_UNSUBSCRIBE)) { "The device $name doesn't support RawUnsubscribeCmd" }
        return sendDeviceMessage {
            rawUnsubscribeCmd = rawUnsubscribeCmd {
                this.endpoint = endpoint.endpoint
            }
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun { endpointSubscriptions.remove(endpoint) }
    }

    private fun <T> mapFromConst(value: T, ty: DeviceAttributeType): Map<Int, T> {
        val count = attributes[ty]?.featureCount ?: 1
        return (0 until count).associateWith { value }
    }

    private fun <T> mapFromIterable(vals: Iterable<T>): Map<Int, T> = vals.mapIndexed { i, v -> i to v }.toMap()

    private fun sendDeviceMessage(block: DeviceMessageBuilder): CompletableFuture<FFIMessage> =
        sendDeviceMessage(block, CompletableFuture(), futureResponseMap, onServerResponseCallback)


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
        val message = readFFIMessage(ptr, len)
        CompletableFuture.runAsync {
            if (ctx != null) {
                ctx.let { futureResponseMap.remove(it) }?.complete(message)
            } else if (message.hasDeviceEvent() && message.deviceEvent.hasRawReading()) {
                val rawReading = message.deviceEvent.rawReading
                endpointSubscriptions[Endpoint.inverse[rawReading.endpoint]]?.invoke(rawReading.data.toByteArray())
            }
        }
    }
}

private fun expectDeviceEvent(msg: FFIMessage) =
    checkNotNull(msg.deviceEventOrNull) { "Expected DEVICE_EVENT message, got ${msg.msgCase.name} instead" }

private fun expectBatteryLevelReading(msg: DeviceEvent) =
    checkNotNull(msg.batteryLevelReadingOrNull) { "Expected BATTERY_LEVEL_READING message, got ${msg.msgCase.name} instead" }

private fun expectRSSILevelReading(msg: DeviceEvent) =
    checkNotNull(msg.rssiLevelReadingOrNull) { "Expected RSSI_LEVEL_READING message, got ${msg.msgCase.name} instead" }

private typealias DeviceMessageBuilder = DeviceMessageKt.FFIMessageKt.Dsl.() -> Unit

private typealias DeviceEvent = ButtplugRsFfi.DeviceEvent

private val defaultStopDeviceCmd = ButtplugRsFfi.DeviceMessage.StopDeviceCmd.getDefaultInstance()

private val defaultBatteryLevelCmd = ButtplugRsFfi.DeviceMessage.BatteryLevelCmd.getDefaultInstance()

private val defaultRSSILevelCmd = ButtplugRsFfi.DeviceMessage.RSSILevelCmd.getDefaultInstance()