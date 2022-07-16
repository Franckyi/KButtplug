package dev.franckyi.kbuttplug.api

import dev.franckyi.kbuttplug.proto.ButtplugRsFfi
import java.util.concurrent.CompletableFuture

/**
 * Interface representing a device connected to a Buttplug server that can be accessed from a [ButtplugClient].
 *
 * This interface exposes attributes about the device, such as the different [DeviceAttributeType]
 * that are supported by the device and their corresponding [DeviceAttributeData].
 *
 * Each device have their own [index] that is unique for the server on which the device is connected to.
 *
 * It also exposes methods to control the device such as [vibrate]
 * and to fetch sensor values such as [fetchBatteryLevel].
 */
interface ButtplugDevice : AutoCloseable {
    /**
     * The unique identifier of the device for the server.
     */
    val index: Int

    /**
     * The name of the device.
     */
    val name: String

    /**
     * The attributes of the device.
     * Each [DeviceAttributeType] supported by the device is mapped to a [DeviceAttributeData] object
     * that contains data about said attribute.
     *
     * @see DeviceAttributeData
     */
    val attributes: Map<DeviceAttributeType, DeviceAttributeData>

    /**
     * Cleans up the device and frees the memory.
     */
    override fun close()

    /**
     * Method to check whether the device supports a certain [DeviceAttributeType].
     *
     * @return true if the device supports this [DeviceAttributeType]
     */
    fun hasAttribute(type: DeviceAttributeType): Boolean = attributes.containsKey(type)

    /**
     * Request to have the server stop the device from whatever actions it may be taking.
     * This message should be supported by all devices, and the server should know how to stop any device it supports.
     *
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     */
    fun stop(): CompletableFuture<Void>

    /**
     * Sends a vibrate command to the device with the given speed
     * and with the ability to control each motor individually.
     *
     * @param speeds the vibration speed of each motor, with the key being the motor index
     * and the value being the vibration speed of the motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     *
     * @see DeviceAttributeType.VIBRATE
     */
    fun vibrate(speeds: Map<Int, Double>): CompletableFuture<Void>

    /**
     * Sends a vibrate command to the device with the given speed of each motor.
     *
     * @param speed the vibration speed of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.VIBRATE
     */
    fun vibrate(speed: Double): CompletableFuture<Void>

    /**
     * Sends a vibrate command to the device with the given speed
     * and with the ability to control each motor individually.
     *
     * @param speeds the vibration speed of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.VIBRATE
     */
    fun vibrate(speeds: Iterable<Double>): CompletableFuture<Void>

    /**
     * Sends a rotate command to the device with the given data
     * and with the ability to control each motor individually.
     *
     * @param components the rotation data of each motor, with the key being the motor index
     * and the value being the rotation data of the motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.ROTATE
     * @see RotateData
     */
    fun rotate(components: Map<Int, RotateData>): CompletableFuture<Void>

    /**
     * Sends a rotate command to the device with the given speed and direction.
     *
     * @param speed the rotation speed of each motor with a range of [[0.0-1.0]]
     * @param clockwise the direction of rotation of each motor (clockwise may be subjective)
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.ROTATE
     */
    fun rotate(speed: Double, clockwise: Boolean): CompletableFuture<Void>

    /**
     * Sends a rotate command to the device with the given rotation data of each motor.
     *
     * @param component the rotation data of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.ROTATE
     * @see RotateData
     */
    fun rotate(component: RotateData): CompletableFuture<Void>

    /**
     * Sends a rotate command to the device with the given data
     * and with the ability to control each motor individually.
     *
     * @param components the rotation data of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.ROTATE
     * @see RotateData
     */
    fun rotate(components: Iterable<RotateData>): CompletableFuture<Void>

    /**
     * Sends a linear command to the device with the given data
     * and with the ability to control each motor individually.
     *
     * @param components the linear data of each motor, with the key being the motor index
     * and the value being the linear data of the motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.LINEAR
     * @see LinearData
     */
    fun linear(components: Map<Int, LinearData>): CompletableFuture<Void>

    /**
     * Sends a linear command to the device with the given duration and speed of each motor.
     *
     * @param duration the movement time of each motor in milliseconds
     * @param position the target position of each motor with a range of [[0.0-1.0]]
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.LINEAR
     */
    fun linear(duration: Int, position: Double): CompletableFuture<Void>

    /**
     * Sends a linear command to the device with the given linear data of each motor.
     *
     * @param component the linear data of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.LINEAR
     * @see LinearData
     */
    fun linear(component: LinearData): CompletableFuture<Void>

    /**
     * Sends a linear command to the device with the given data
     * and with the ability to control each motor individually.
     *
     * @param components the linear data of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.LINEAR
     * @see LinearData
     */
    fun linear(components: Iterable<LinearData>): CompletableFuture<Void>

    /**
     * Requests the battery level of the device from the server.
     *
     * @return a [CompletableFuture] that completes when the server returns a `BatteryLevelReading` message
     * returning the battery level of the device with a range of [[0.0-1.0]],
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.BATTERY_LEVEL
     */
    fun fetchBatteryLevel(): CompletableFuture<Double>

    /**
     * Requests the RSSI level of the device from the server.
     *
     * @return a [CompletableFuture] that completes when the server returns a `RSSILevelReading` message
     * returning the RSSI level of the device expressed as dB gain (usually in the range of [[-100-0]]),
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.RSSI_LEVEL
     */
    fun fetchRSSILevel(): CompletableFuture<Int>

    /**
     * Writes a byte array to a device endpoint.
     *
     * @param endpoint the endpoint to write data to
     * @param data the raw data to write to the endpoint
     * @param writeWithResponse `true` if BLE WriteWithResponse required, `false` otherwise
     */
    fun rawWrite(endpoint: Endpoint, data: ByteArray, writeWithResponse: Boolean = false): CompletableFuture<Void>

    /**
     * Reads a byte array from a device endpoint.
     *
     * @param endpoint the endpoint to read data from
     * @param expectedLength amount of data to read, `0` to read all currently available
     * @param timeout TODO check what it actually stands for
     */
    fun rawRead(endpoint: Endpoint, expectedLength: Int = 0, timeout: Int = 0): CompletableFuture<ByteArray>

    /**
     * Subscribes to an endpoint, sending all data that comes in from an endpoint that is not explicitly read.
     * Usually useful for Bluetooth notify endpoints, or other streaming data endpoints.
     *
     * @param endpoint the endpoint to subscribe to
     * @param callback the callback to call when data from this endpoint is received
     */
    fun rawSubscribe(endpoint: Endpoint, callback: EndpointCallback): CompletableFuture<Void>

    /**
     * Unsubscribes from an endpoint to which it had previously subscribed.
     *
     * @param endpoint the endpoint to unsubscribe from
     */
    fun rawUnsubscribe(endpoint: Endpoint): CompletableFuture<Void>
}

/**
 * Enum class of the different device attribute types available.
 */
enum class DeviceAttributeType(internal val type: ButtplugRsFfi.ServerMessage.MessageAttributeType) {
    /**
     * @see ButtplugDevice.vibrate
     */
    VIBRATE(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    /**
     * @see ButtplugDevice.rotate
     */
    ROTATE(ButtplugRsFfi.ServerMessage.MessageAttributeType.RotateCmd),

    /**
     * @see ButtplugDevice.linear
     */
    LINEAR(ButtplugRsFfi.ServerMessage.MessageAttributeType.LinearCmd),

    /**
     * @see ButtplugDevice.stop
     */
    STOP(ButtplugRsFfi.ServerMessage.MessageAttributeType.StopDeviceCmd),

    /**
     * @see ButtplugDevice.rawRead
     */
    RAW_READ(ButtplugRsFfi.ServerMessage.MessageAttributeType.RawReadCmd),

    /**
     * @see ButtplugDevice.rawWrite
     */
    RAW_WRITE(ButtplugRsFfi.ServerMessage.MessageAttributeType.RawWriteCmd),

    /**
     * @see ButtplugDevice.rawSubscribe
     */
    RAW_SUBSCRIBE(ButtplugRsFfi.ServerMessage.MessageAttributeType.RawSubscribeCmd),

    /**
     * @see ButtplugDevice.rawUnsubscribe
     */
    RAW_UNSUBSCRIBE(ButtplugRsFfi.ServerMessage.MessageAttributeType.RawUnsubscribeCmd),

    /**
     * @see ButtplugDevice.fetchBatteryLevel
     */
    BATTERY_LEVEL(ButtplugRsFfi.ServerMessage.MessageAttributeType.BatteryLevelCmd),

    /**
     * @see ButtplugDevice.fetchRSSILevel
     */
    RSSI_LEVEL(ButtplugRsFfi.ServerMessage.MessageAttributeType.RSSILevelCmd);

    companion object {
        internal val inverse = values().associateBy({ it.type }, { it })
    }
}

/**
 * Data of a device attribute.
 *
 * @property featureCount number of features available to be controlled on the device for this attribute type.
 * The meaning of "feature" is specific to the attribute type.
 * For instance, the [featureCount] attribute of a [DeviceAttributeType.VIBRATE] attribute type will refer to
 * the number of vibration motors that can be controlled on a device.
 * @property stepCount for each feature of this attribute, lists the number of discrete steps the feature can use.
 * Returning to the VibrateCmd example from the above [featureCount] specification, if a device had 2 motors,
 * and each motor has 20 steps of vibration speeds from 0%-100% (this is exactly what the Lovense Edge is),
 * the [stepCount] attribute would be [[20, 20]].
 * Having the array allows to specify different amounts of steps for multiple vibrators on the device.
 * @property endpoints
 * @see DeviceAttributeType
 */
data class DeviceAttributeData(val featureCount: Int, val stepCount: List<Int>, val endpoints: List<Endpoint>)

/**
 * Data class of a rotate command.
 *
 * @property speed the rotation speed with a range of [[0.0-1.0]]
 * @property clockwise the direction of rotation (clockwise may be subjective)
 *
 * @see ButtplugDevice.rotate
 */
data class RotateData(var speed: Double, var clockwise: Boolean)

/**
 * Data class of a linear command.
 *
 * @property duration the movement time in milliseconds
 * @property position the target position with a range of [[0.0-1.0]]
 *
 * @see ButtplugDevice.linear
 */
data class LinearData(var duration: Int, var position: Double)

/**
 * Enum class of the different endpoints available on a device.
 */
enum class Endpoint(internal val endpoint: ButtplugRsFfi.Endpoint) {
    COMMAND(ButtplugRsFfi.Endpoint.Command),
    FIRMWARE(ButtplugRsFfi.Endpoint.Firmware),
    RX(ButtplugRsFfi.Endpoint.Rx),
    RX_ACCEL(ButtplugRsFfi.Endpoint.RxAccel),
    RX_BLE_BATTERY(ButtplugRsFfi.Endpoint.RxBLEBattery),
    RX_PRESSURE(ButtplugRsFfi.Endpoint.RxPressure),
    RX_TOUCH(ButtplugRsFfi.Endpoint.RxTouch),
    TX(ButtplugRsFfi.Endpoint.Tx),
    TX_MODE(ButtplugRsFfi.Endpoint.TxMode),
    TX_SHOCK(ButtplugRsFfi.Endpoint.TxShock),
    TX_VIBRATE(ButtplugRsFfi.Endpoint.TxVibrate),
    TX_VENDORCONTROL(ButtplugRsFfi.Endpoint.TxVendorControl),
    WHITELIST(ButtplugRsFfi.Endpoint.Whitelist),
    GENERIC_0(ButtplugRsFfi.Endpoint.Generic0),
    GENERIC_1(ButtplugRsFfi.Endpoint.Generic1),
    GENERIC_2(ButtplugRsFfi.Endpoint.Generic2),
    GENERIC_3(ButtplugRsFfi.Endpoint.Generic3),
    GENERIC_4(ButtplugRsFfi.Endpoint.Generic4),
    GENERIC_5(ButtplugRsFfi.Endpoint.Generic5),
    GENERIC_6(ButtplugRsFfi.Endpoint.Generic6),
    GENERIC_7(ButtplugRsFfi.Endpoint.Generic7),
    GENERIC_8(ButtplugRsFfi.Endpoint.Generic8),
    GENERIC_9(ButtplugRsFfi.Endpoint.Generic9),
    GENERIC_10(ButtplugRsFfi.Endpoint.Generic10),
    GENERIC_11(ButtplugRsFfi.Endpoint.Generic11),
    GENERIC_12(ButtplugRsFfi.Endpoint.Generic12),
    GENERIC_13(ButtplugRsFfi.Endpoint.Generic13),
    GENERIC_14(ButtplugRsFfi.Endpoint.Generic14),
    GENERIC_15(ButtplugRsFfi.Endpoint.Generic15),
    GENERIC_16(ButtplugRsFfi.Endpoint.Generic16),
    GENERIC_17(ButtplugRsFfi.Endpoint.Generic17),
    GENERIC_18(ButtplugRsFfi.Endpoint.Generic18),
    GENERIC_19(ButtplugRsFfi.Endpoint.Generic19),
    GENERIC_20(ButtplugRsFfi.Endpoint.Generic20),
    GENERIC_21(ButtplugRsFfi.Endpoint.Generic21),
    GENERIC_22(ButtplugRsFfi.Endpoint.Generic22),
    GENERIC_23(ButtplugRsFfi.Endpoint.Generic23),
    GENERIC_24(ButtplugRsFfi.Endpoint.Generic24),
    GENERIC_25(ButtplugRsFfi.Endpoint.Generic25),
    GENERIC_26(ButtplugRsFfi.Endpoint.Generic26),
    GENERIC_27(ButtplugRsFfi.Endpoint.Generic27),
    GENERIC_28(ButtplugRsFfi.Endpoint.Generic28),
    GENERIC_29(ButtplugRsFfi.Endpoint.Generic29),
    GENERIC_30(ButtplugRsFfi.Endpoint.Generic30),
    GENERIC_31(ButtplugRsFfi.Endpoint.Generic31),
    RX_BLE_MODEL(ButtplugRsFfi.Endpoint.RxBLEModel);

    companion object {
        internal val inverse = values().associateBy({ it.endpoint }, { it })
    }
}

typealias EndpointCallback = (ByteArray) -> Unit