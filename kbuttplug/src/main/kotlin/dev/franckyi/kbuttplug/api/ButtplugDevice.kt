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
     * @see DeviceAttributeType.VibrateCmd
     */
    fun vibrate(speeds: Map<Int, Double>): CompletableFuture<Void>

    /**
     * Sends a vibrate command to the device with the given speed of each motor.
     *
     * @param speed the vibration speed of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.VibrateCmd
     */
    fun vibrate(speed: Double): CompletableFuture<Void>

    /**
     * Sends a vibrate command to the device with the given speed
     * and with the ability to control each motor individually.
     *
     * @param speeds the vibration speed of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.VibrateCmd
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
     * @see DeviceAttributeType.RotateCmd
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
     * @see DeviceAttributeType.RotateCmd
     */
    fun rotate(speed: Double, clockwise: Boolean): CompletableFuture<Void>

    /**
     * Sends a rotate command to the device with the given rotation data of each motor.
     *
     * @param component the rotation data of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.RotateCmd
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
     * @see DeviceAttributeType.RotateCmd
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
     * @see DeviceAttributeType.LinearCmd
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
     * @see DeviceAttributeType.LinearCmd
     */
    fun linear(duration: Int, position: Double): CompletableFuture<Void>

    /**
     * Sends a linear command to the device with the given linear data of each motor.
     *
     * @param component the linear data of each motor
     * @return a [CompletableFuture] that completes when the server returns an `Ok` message,
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.LinearCmd
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
     * @see DeviceAttributeType.LinearCmd
     * @see LinearData
     */
    fun linear(components: Iterable<LinearData>): CompletableFuture<Void>

    /**
     * Requests the battery level of the device from the server.
     *
     * @return a [CompletableFuture] that completes when the server returns a `BatteryLevelReading` message
     * returning the battery level of the device with a range of [[0.0-1.0]],
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.BatteryLevelCmd
     */
    fun fetchBatteryLevel(): CompletableFuture<Double>

    /**
     * Requests the RSSI level of the device from the server.
     *
     * @return a [CompletableFuture] that completes when the server returns a `RSSILevelReading` message
     * returning the RSSI level of the device expressed as dB gain (usually in the range of [[-100-0]]),
     * or fails if the server returns an `Error` message
     * @see DeviceAttributeType.RSSILevelCmd
     */
    fun fetchRSSILevel(): CompletableFuture<Int>
}

/**
 * Enum class of the different device attribute types available.
 */
enum class DeviceAttributeType(internal val type: ButtplugRsFfi.ServerMessage.MessageAttributeType) {
    /**
     * @see ButtplugDevice.vibrate
     */
    VibrateCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    /**
     * @see ButtplugDevice.rotate
     */
    RotateCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    /**
     * @see ButtplugDevice.linear
     */
    LinearCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    /**
     * @see ButtplugDevice.stop
     */
    StopDeviceCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    @Deprecated("No implementation yet")
    RawReadCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    @Deprecated("No implementation yet")
    RawWriteCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    @Deprecated("No implementation yet")
    RawSubscribeCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    @Deprecated("No implementation yet")
    RawUnsubscribeCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    /**
     * @see ButtplugDevice.fetchBatteryLevel
     */
    BatteryLevelCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd),

    /**
     * @see ButtplugDevice.fetchRSSILevel
     */
    RSSILevelCmd(ButtplugRsFfi.ServerMessage.MessageAttributeType.VibrateCmd);

    companion object {
        internal val inverse = values().associateBy({ it.type }, { it })
    }
}

/**
 * Data of a device attribute.
 *
 * @param featureCount number of features available to be controlled on the device for this attribute type.
 * The meaning of "feature" is specific to the attribute type.
 * For instance, the [featureCount] attribute of a [DeviceAttributeType.VibrateCmd] attribute type will refer to
 * the number of vibration motors that can be controlled on a device.
 * @param stepCount for each feature of this attribute, lists the number of discrete steps the feature can use.
 * Returning to the VibrateCmd example from the above [featureCount] specification, if a device had 2 motors,
 * and each motor has 20 steps of vibration speeds from 0%-100% (this is exactly what the Lovense Edge is),
 * the [stepCount] attribute would be [[20, 20]].
 * Having the array allows to specify different amounts of steps for multiple vibrators on the device.
 * @see DeviceAttributeType
 */
data class DeviceAttributeData(val featureCount: Int, val stepCount: List<Int>)

/**
 * Data class of a rotate command.
 *
 * @param speed the rotation speed with a range of [[0.0-1.0]]
 * @param clockwise the direction of rotation (clockwise may be subjective)
 *
 * @see ButtplugDevice.rotate
 */
data class RotateData(var speed: Double, var clockwise: Boolean)

/**
 * Data class of a linear command.
 *
 * @param duration the movement time in milliseconds
 * @param position the target position with a range of [[0.0-1.0]]
 *
 * @see ButtplugDevice.linear
 */
data class LinearData(var duration: Int, var position: Double)