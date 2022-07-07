package dev.franckyi.kbuttplug.api

import dev.franckyi.kbuttplug.api.ButtplugClient.Companion.create
import dev.franckyi.kbuttplug.impl.ButtplugClientImpl
import dev.franckyi.kbuttplug.proto.ButtplugRsFfi
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Interface representing a Buttplug client that can connect to a Buttplug server,
 * either embedded with [connectLocal] or through websockets with [connectWebsocket].
 *
 * After connecting to a server, the client can send commands to the server,
 * for example to scan for devices using [startScanning].
 *
 * The client should also register callbacks before connecting to a server. For example, the [onError] callback is
 * called whenever the server sends an error to the client.
 *
 * A ButtplugClient implements [AutoCloseable] so it can be used within a try-with-resources block.
 * If not used that way, don't remember to call [close] to avoid memory leaks.
 *
 * @see create
 */
interface ButtplugClient : AutoCloseable {
    companion object {
        /**
         * Creates a new [ButtplugClient].
         *
         * @param name the name of the client
         * @return the new [ButtplugClient]
         */
        fun create(name: String): ButtplugClient = ButtplugClientImpl(name)
    }

    /**
     * The name of the client.
     */
    val name: String

    /**
     * True if the client is currently connected to a server.
     *
     * @see connectLocal
     * @see connectWebsocket
     * @see disconnect
     */
    val connected: Boolean

    /**
     * True if the server to which the client is connected is currently scanning for devices.
     *
     * @see startScanning
     * @see stopScanning
     */
    val scanning: Boolean

    /**
     * The [ButtplugDevice] map scanned by the server and accessible by the client.
     * The key is the unique identifier of the device for this server and the value is the [ButtplugDevice] object.
     *
     * The map is updated when the server sends a `DeviceAdded` or `DeviceRemoved` message to the client.
     * The [onDeviceAdded] or [onDeviceRemoved] callback is called after the map is updated.
     *
     * @see onDeviceAdded
     * @see onDeviceRemoved
     */
    val devices: Map<Int, ButtplugDevice>

    /**
     * This callback is called when an error message is sent by the server to the client.
     */
    var onError: ((ButtplugException) -> Unit)?

    /**
     * This callback is called when the server notifies the client that the scanning process has finished.
     *
     * @see startScanning
     * @see stopScanning
     */
    var onScanningFinished: (() -> Unit)?

    /**
     * This callback is called when a [ButtplugDevice] is scanned by the server and is available for the client.
     * It is called right after the device has been added to the [devices] map.
     *
     * @see devices
     * @see onDeviceRemoved
     */
    var onDeviceAdded: ((ButtplugDevice) -> Unit)?

    /**
     * This callback is called when a [ButtplugDevice] is no longer scanned by the server.
     * and is no longer available for the client.
     * It is called right after the device has been removed from the [devices] map.
     *
     * @see devices
     * @see onDeviceAdded
     */
    var onDeviceRemoved: ((ButtplugDevice) -> Unit)?

    /**
     * This callback is called when the server notifies that the client is disconnected.
     *
     * @see disconnect
     */
    var onDisconnect: (() -> Unit)?

    /**
     * Cleans up the client and frees the memory.
     * To reconnect to a Buttplug server, use a new instance of [ButtplugClient].
     */
    override fun close()

    /**
     * Connects to an embedded Buttplug server running within the application.
     *
     * @param serverName the name of the Buttplug server
     * @param deviceConfigJSON the device configuration as a JSON string, will use embedded device configuration by default
     * @param userDeviceConfigJSON the user device configuration as a JSON string
     * @param deviceCommunicationTypes the list of [DeviceCommunicationType] that the server can handle, all by default
     * @param allowRawMessages whether to allow the server to handle raw messages sent by the client
     * @param maxPingTime the maximum ping time allowed before a client times out in milliseconds, none by default
     * @return a [CompletableFuture] that completes after the connection to the server is established,
     * or fails if a connection error occurs
     * @see disconnect
     */
    fun connectLocal(
        serverName: String = "Buttplug Java Embedded Server",
        deviceConfigJSON: String = "",
        userDeviceConfigJSON: String = "",
        deviceCommunicationTypes: EnumSet<DeviceCommunicationType> = EnumSet.noneOf(DeviceCommunicationType::class.java),
        allowRawMessages: Boolean = false,
        maxPingTime: Int = 0
    ): CompletableFuture<Void>

    /**
     * Connects to a remote Buttplug server using Websockets.
     *
     * @param address The URL of the websocket
     * @return a [CompletableFuture] that completes after the connection to the server is established,
     * or fails if a connection error occurs
     * @see disconnect
     */
    fun connectWebsocket(address: String = "ws://127.0.0.1:12345"): CompletableFuture<Void>

    /**
     * Tells the server to start scanning for devices on all busses that it knows about.
     * Useful for protocols like Bluetooth, which require an explicit discovery phase.
     *
     * Whenever a new device is scanned by the server, the device is added to the [devices] map and the [onDeviceAdded] callback is called.
     *
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @throws IllegalStateException if the client is not connected to a server
     * @see stopScanning
     */
    fun startScanning(): CompletableFuture<Void>

    /**
     * Tells the server to stop scanning for devices.
     * Useful for protocols like Bluetooth, which may not timeout otherwise.
     *
     * Note that after this method is called, the server may still send a `ScanningFinished` message
     * that will trigger the [onScanningFinished] callback.
     *
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @throws IllegalStateException if the client is not connected to a server
     * @see startScanning
     */
    fun stopScanning(): CompletableFuture<Void>

    /**
     * Tells the server to stop all devices.
     * Can be used for emergency situations, on client shutdown for cleanup, etc…
     *
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @throws IllegalStateException if the client is not connected to a server
     * @see devices
     */
    fun stopAllDevices(): CompletableFuture<Void>

    /**
     * Sends a ping message to the server.
     *
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @throws IllegalStateException if the client is not connected to a server
     */
    fun ping(): CompletableFuture<Void>

    /**
     * Disconnects the client from the server.
     *
     * Note that after this method is called, the server may still send a `Disconnected` message
     * that will trigger the [onDisconnect] callback.
     *
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @throws IllegalStateException if the client is not connected to a server
     * @see connectWebsocket
     * @see onDisconnect
     */
    fun disconnect(): CompletableFuture<Void>
}

/**
 * A type of device communication that is used by the server to communicate with devices.
 *
 * @see ButtplugClient.connectLocal
 */
enum class DeviceCommunicationType(internal val type: ButtplugRsFfi.ClientMessage.DeviceCommunicationManagerTypes) {
    Btleplug(ButtplugRsFfi.ClientMessage.DeviceCommunicationManagerTypes.Btleplug),
    XInput(ButtplugRsFfi.ClientMessage.DeviceCommunicationManagerTypes.XInput),
    SerialPort(ButtplugRsFfi.ClientMessage.DeviceCommunicationManagerTypes.SerialPort),
    LovenseHIDDongle(ButtplugRsFfi.ClientMessage.DeviceCommunicationManagerTypes.LovenseHIDDongle),
    LovenseSerialDongle(ButtplugRsFfi.ClientMessage.DeviceCommunicationManagerTypes.LovenseSerialDongle)
}
