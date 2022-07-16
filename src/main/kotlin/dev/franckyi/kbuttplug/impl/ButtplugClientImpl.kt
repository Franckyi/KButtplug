package dev.franckyi.kbuttplug.impl

import com.sun.jna.Pointer
import dev.franckyi.kbuttplug.api.ButtplugClient
import dev.franckyi.kbuttplug.api.ButtplugDevice
import dev.franckyi.kbuttplug.api.ButtplugException
import dev.franckyi.kbuttplug.api.DeviceCommunicationType
import dev.franckyi.kbuttplug.proto.ButtplugRsFfi
import dev.franckyi.kbuttplug.proto.ClientMessageKt
import dev.franckyi.kbuttplug.proto.ClientMessageKt.connectLocal
import dev.franckyi.kbuttplug.proto.ClientMessageKt.connectWebsocket
import dev.franckyi.kbuttplug.proto.ClientMessageKt.fFIMessage
import dev.franckyi.kbuttplug.proto.clientMessage
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

internal class ButtplugClientImpl(override val name: String) : ButtplugClient {
    private var pointer: Pointer?
    private val futureResponseMap: MutableMap<Pointer, CompletableFuture<FFIMessage>> = ConcurrentHashMap()
    private val onServerEventCallback = ButtplugCallback.of(::onServerEvent)
    private val onServerResponseCallback = ButtplugCallback.of(::onServerResponse)

    override var connected: Boolean = false
        private set

    override var scanning: Boolean = false
        private set

    private var _devices: MutableMap<Int, ButtplugDeviceImpl> = mutableMapOf()
    override val devices: Map<Int, ButtplugDeviceImpl> get() = ImmutableMap(_devices)

    override var onError: ((ButtplugException) -> Unit)? = null
    override var onScanningFinished: (() -> Unit)? = null
    override var onDeviceAdded: ((ButtplugDevice) -> Unit)? = null
    override var onDeviceRemoved: ((ButtplugDevice) -> Unit)? = null
    override var onDisconnect: (() -> Unit)? = null

    init {
        pointer = ButtplugFFI.INSTANCE.buttplug_create_protobuf_client(
            name,
            onServerEventCallback,
            u32(Pointer.nativeValue(createPointer(this)))
        )
    }

    override fun close() {
        synchronized(this) {
            pointer?.let {
                ButtplugFFI.INSTANCE.buttplug_free_client(it)
                pointer = null
            }
        }
    }

    override fun connectLocal(
        serverName: String,
        deviceConfigJSON: String,
        userDeviceConfigJSON: String,
        deviceCommunicationTypes: EnumSet<DeviceCommunicationType>,
        allowRawMessages: Boolean,
        maxPingTime: Int
    ): CompletableFuture<Void> {
        check(!connected) { "Client is already connected to a server" }
        return sendClientMessage {
            this.connectLocal = connectLocal {
                this.serverName = serverName
                this.deviceConfigurationJson = deviceConfigJSON
                this.userDeviceConfigurationJson = userDeviceConfigJSON
                this.commManagerTypes = deviceCommunicationTypes.sumOf { it.type.number }
                this.allowRawMessages = allowRawMessages
                this.maxPingTime = maxPingTime
            }
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun { connected = true }
    }

    override fun connectWebsocket(address: String): CompletableFuture<Void> {
        check(!connected) { "Client is already connected to a server" }
        return sendClientMessage {
            this.connectWebsocket = connectWebsocket {
                this.address = address
            }
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun { connected = true }
    }

    override fun startScanning(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        if (scanning) return CompletableFuture.completedFuture(null)
        return sendClientMessage { this.startScanning = defaultStartScanning }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun { scanning = true }
    }

    override fun stopScanning(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        if (!scanning) return CompletableFuture.completedFuture(null)
        return sendClientMessage { this.stopScanning = defaultStopScanning }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun { scanning = false }
    }

    override fun stopAllDevices(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        return sendClientMessage {
            this.stopAllDevices = defaultStopAllDevices
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
    }

    override fun ping(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        return sendClientMessage { this.ping = defaultPing }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
    }

    override fun disconnect(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        return sendClientMessage { this.disconnect = defaultDisconnect }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun {
                connected = false
                _devices.clear()
            }
    }

    private fun sendClientMessage(block: ClientMessageBuilder): CompletableFuture<FFIMessage> {
        val clientPointer = checkNotNull(pointer) { "Attempt to send message when client has already been closed" }
        val msg = clientMessage {
            this.message = fFIMessage(block)
        }
        val future = CompletableFuture<FFIMessage>()
        val ptr = createPointer(future)
        futureResponseMap[ptr] = future
        val buf = msg.toByteArray()
        ButtplugFFI.INSTANCE.buttplug_client_protobuf_message(
            clientPointer,
            buf,
            i32(buf.size.toLong()),
            onServerResponseCallback,
            ptr
        )
        return future
    }

    private fun onServerResponse(ctx: Pointer?, ptr: Pointer, len: u32) {
        val message = readFFIMessage(ptr, len)
        CompletableFuture.runAsync { ctx?.let { futureResponseMap.remove(it) }?.complete(message) }
    }

    @Suppress("unused_parameter")
    private fun onServerEvent(ctx: Pointer?, ptr: Pointer, len: u32) {
        val ffiMessage = readFFIMessage(ptr, len)
        CompletableFuture.runAsync {
            if (ffiMessage.hasServerMessage()) {
                val serverMessage = ffiMessage.serverMessage
                if (serverMessage.hasError()) {
                    onError?.invoke(createButtplugExceptionFromError(serverMessage.error))
                } else if (serverMessage.hasScanningFinished()) {
                    scanning = false
                    onScanningFinished?.invoke()
                } else if (serverMessage.hasDeviceAdded()) {
                    val deviceAddedMessage = serverMessage.deviceAdded
                    check(!devices.containsKey(deviceAddedMessage.index)) { "Duplicate device index ${deviceAddedMessage.index} received" }
                    val device = createDevice(deviceAddedMessage)
                    _devices[deviceAddedMessage.index] = device
                    onDeviceAdded?.invoke(device)
                } else if (serverMessage.hasDeviceRemoved()) {
                    val deviceRemovedMessage = serverMessage.deviceRemoved
                    val device = _devices.remove(deviceRemovedMessage.index)
                    device?.use { onDeviceRemoved?.invoke(it) }
                } else if (serverMessage.hasDisconnect()) {
                    connected = false
                    _devices.clear()
                    onDisconnect?.invoke()
                }
            }
        }
    }

    private fun createDevice(msg: DeviceAddedMessage): ButtplugDeviceImpl {
        val ptr = checkNotNull(pointer) { "Attempt to create device when client has already been closed" }
        return ButtplugDeviceImpl(ptr, msg)
    }
}

private typealias ClientMessageBuilder = ClientMessageKt.FFIMessageKt.Dsl.() -> Unit

private val defaultStartScanning = ButtplugRsFfi.ClientMessage.StartScanning.getDefaultInstance()

private val defaultStopScanning = ButtplugRsFfi.ClientMessage.StopScanning.getDefaultInstance()

private val defaultStopAllDevices = ButtplugRsFfi.ClientMessage.StopAllDevices.getDefaultInstance()

private val defaultPing = ButtplugRsFfi.ClientMessage.Ping.getDefaultInstance()

private val defaultDisconnect = ButtplugRsFfi.ClientMessage.Disconnect.getDefaultInstance()

private class ImmutableMap<K, V>(private val map: Map<K, V>) : Map<K, V> by map