package dev.franckyi.kbuttplug.impl

import com.sun.jna.Pointer
import dev.franckyi.kbuttplug.api.*
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
    private val futureServerMessageMap: MutableMap<Pointer, CompletableFuture<ServerMessage>> =
        ConcurrentHashMap()
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
            ButtplugCallback.of(::onServerEvent),
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
        if (connected) return CompletableFuture.completedFuture(null)
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
            .thenAccept(::expectServerOk)
            .thenRun { connected = true }
    }

    override fun connectWebsocket(address: String): CompletableFuture<Void> {
        if (connected) return CompletableFuture.completedFuture(null)
        return sendClientMessage {
            this.connectWebsocket = connectWebsocket {
                this.address = address
            }
        }
            .thenAccept(::expectServerOk)
            .thenRun { connected = true }
    }

    override fun startScanning(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        if (scanning) return CompletableFuture.completedFuture(null)
        return sendClientMessage { this.startScanning = defaultStartScanning }
            .thenAccept(::expectServerOk)
            .thenRun { scanning = true }
    }

    override fun stopScanning(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        if (!scanning) return CompletableFuture.completedFuture(null)
        scanning = false
        return sendClientMessage { this.stopScanning = defaultStopScanning }
            .thenAccept(::expectServerOk)
    }

    override fun stopAllDevices(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        return sendClientMessage {
            this.stopAllDevices = defaultStopAllDevices
        }
            .thenAccept(::expectServerOk)
    }

    override fun ping(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        return sendClientMessage { this.ping = defaultPing }
            .thenAccept(::expectServerOk)
    }

    override fun disconnect(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        connected = false
        _devices.clear()
        return sendClientMessage { this.disconnect = defaultDisconnect }
            .thenAccept(::expectServerOk)
    }

    private fun sendClientMessage(block: ClientMessageBuilder): CompletableFuture<ServerMessage> {
        val clientPointer = checkNotNull(pointer) { "Attempt to send message when client has already been closed!" }
        val msg = clientMessage {
            this.message = fFIMessage(block)
        }
        val future = CompletableFuture<ServerMessage>()
        val ptr = createPointer(future)
        futureServerMessageMap[ptr] = future
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

    private fun onServerEvent(ctx: Pointer?, ptr: Pointer, len: u32) {
        val message = readServerMessage(ptr, len)
        CompletableFuture.runAsync {
            if (message.hasError()) {
                onError?.invoke(ButtplugException.fromError(message.error))
            } else if (message.hasScanningFinished()) {
                scanning = false
                onScanningFinished?.invoke()
            } else if (message.hasDeviceAdded()) {
                val msg = message.deviceAdded
                if (devices.containsKey(msg.index)) {
                    throw ButtplugDeviceException("Duplicate device id received")
                }
                val device = createDevice(msg)
                _devices[msg.index] = device
                onDeviceAdded?.invoke(device)
            } else if (message.hasDeviceRemoved()) {
                val msg = message.deviceRemoved
                val device = _devices.remove(msg.index)
                device?.let {
                    onDeviceRemoved?.invoke(it)
                    it.close()
                }
            } else if (message.hasDisconnect()) {
                connected = false
                _devices.clear()
                onDisconnect?.invoke()
            }
        }
    }

    private fun onServerResponse(ctx: Pointer?, ptr: Pointer, len: u32) {
        val message = readServerMessage(ptr, len)
        CompletableFuture.runAsync {
            val future = ctx?.let { futureServerMessageMap.remove(it) }
            if (message.hasError()) {
                future?.completeExceptionally(ButtplugException.fromError(message.error))
            }
            future?.complete(message)
        }
    }

    private fun createDevice(msg: DeviceAddedMessage): ButtplugDeviceImpl {
        val ptr = checkNotNull(pointer) { "Attempt to create device when client has already been closed!" }
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