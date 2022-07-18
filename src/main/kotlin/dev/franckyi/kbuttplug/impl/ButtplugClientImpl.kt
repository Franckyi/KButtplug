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
import mu.KotlinLogging
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

internal class ButtplugClientImpl(override val name: String) : ButtplugClient {
    private var pointer: Pointer?
    private val futureResponseMap: MutableMap<Pointer, CompletableFuture<FFIMessage>> = ConcurrentHashMap()
    private val onServerEventCallback = ButtplugCallback.of(::onServerEvent)
    private val onServerResponseCallback = ButtplugCallback.of(::onServerResponse)

    private var shouldBeConnected: Boolean = false
    override var connected: Boolean = false
        private set

    private var shouldBeScanning: Boolean = false
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
        logger.debug { "Initialized client $name" }
    }

    override fun close() {
        synchronized(this) {
            pointer?.let {
                ButtplugFFI.INSTANCE.buttplug_free_client(it)
                pointer = null
                logger.debug { "Closed client $name" }
            }
        }
    }

    @Deprecated("Causes memory leaks in some cases", ReplaceWith("connectWebsocket()"))
    override fun connectLocal(
        serverName: String,
        deviceConfigJSON: String,
        userDeviceConfigJSON: String,
        deviceCommunicationTypes: EnumSet<DeviceCommunicationType>,
        allowRawMessages: Boolean,
        maxPingTime: Int
    ): CompletableFuture<Void> {
        if (connected && shouldBeConnected) return CompletableFuture.completedFuture(null)
        shouldBeConnected = true
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
            .thenRun {
                connected = true
                logger.info { "Client $name connected to local server $serverName" }
            }
    }

    override fun connectWebsocket(address: String): CompletableFuture<Void> {
        if (connected && shouldBeConnected) return CompletableFuture.completedFuture(null)
        shouldBeConnected = true
        return sendClientMessage {
            this.connectWebsocket = connectWebsocket {
                this.address = address
            }
        }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun {
                connected = true
                logger.info { "Client $name connected to server at address $address" }
            }
    }

    override fun startScanning(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        if (scanning && shouldBeScanning) return CompletableFuture.completedFuture(null)
        shouldBeScanning = true
        return sendClientMessage { this.startScanning = defaultStartScanning }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun {
                scanning = true
                logger.debug { "Started scanning for devices" }
            }
    }

    override fun stopScanning(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        if (!scanning && !shouldBeScanning) return CompletableFuture.completedFuture(null)
        shouldBeScanning = false
        return sendClientMessage { this.stopScanning = defaultStopScanning }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun {
                scanning = false
                logger.debug { "Stopped scanning for devices" }
            }
    }

    override fun stopAllDevices(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        return sendClientMessage { this.stopAllDevices = defaultStopAllDevices }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun { logger.debug { "Stopped all devices" } }
    }

    override fun ping(): CompletableFuture<Void> {
        check(connected) { "Client is not connected to a server" }
        return sendClientMessage { this.ping = defaultPing }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun { logger.debug { "Pinged server" } }
    }

    override fun disconnect(): CompletableFuture<Void> {
        if (!connected && !shouldBeConnected) return CompletableFuture.completedFuture(null)
        shouldBeConnected = false
        return sendClientMessage { this.disconnect = defaultDisconnect }
            .thenApply(::expectServerMessage)
            .thenAccept(::expectOk)
            .thenRun {
                connected = false
                scanning = false
                _devices.clear()
                logger.info { "Client $name disconnected from server" }
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
        logger.trace { "Client message sent to server: $msg" }
        return future.orTimeout(10, TimeUnit.SECONDS).exceptionally {
            logger.error { "Did not get a response from server in time" }
            futureResponseMap.remove(ptr)
            throw it
        }
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
                    val exception = createButtplugExceptionFromError(serverMessage.error)
                    logger.error(exception) { "Server error occured" }
                    onError?.invoke(exception)
                } else if (serverMessage.hasScanningFinished()) {
                    shouldBeScanning = false
                    scanning = false
                    logger.debug { "Scanning finished" }
                    onScanningFinished?.invoke()
                } else if (serverMessage.hasDeviceAdded()) {
                    val deviceAddedMessage = serverMessage.deviceAdded
                    check(!devices.containsKey(deviceAddedMessage.index)) { "Duplicate device index ${deviceAddedMessage.index} received" }
                    val device = createDevice(deviceAddedMessage)
                    _devices[deviceAddedMessage.index] = device
                    logger.debug { "Device added: $device" }
                    onDeviceAdded?.invoke(device)
                } else if (serverMessage.hasDeviceRemoved()) {
                    val deviceRemovedMessage = serverMessage.deviceRemoved
                    val device = _devices.remove(deviceRemovedMessage.index)
                    device?.use {
                        logger.debug { "Device removed: $it" }
                        onDeviceRemoved?.invoke(it)
                    }
                } else if (serverMessage.hasDisconnect()) {
                    shouldBeConnected = false
                    connected = false
                    _devices.clear()
                    logger.debug { "Server disconnected" }
                    onDisconnect?.invoke()
                } else {
                    logger.warn { "Ignoring received message: $serverMessage" }
                }
            } else {
                logger.warn { "Ignoring received message: $ffiMessage" }
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