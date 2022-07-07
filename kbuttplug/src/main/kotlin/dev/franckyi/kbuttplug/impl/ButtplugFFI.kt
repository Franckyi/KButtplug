package dev.franckyi.kbuttplug.impl

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

@Suppress("FunctionName")
internal interface ButtplugFFI : Library {
    companion object {
        val INSTANCE: ButtplugFFI by lazy {
            Native.load("buttplug_rs_ffi", ButtplugFFI::class.java)
        }
    }

    fun buttplug_create_protobuf_client(clientName: String, callback: ButtplugCallback, ctx: u32): Pointer
    fun buttplug_free_client(client: Pointer)
    fun buttplug_client_protobuf_message(
        client: Pointer,
        buf: ByteArray,
        bufLen: i32,
        callback: ButtplugCallback,
        ctx: Pointer
    )

    fun buttplug_create_device(client: Pointer, index: u32): Pointer
    fun buttplug_device_protobuf_message(
        device: Pointer,
        buf: ByteArray,
        bufLen: i32,
        callback: ButtplugCallback,
        ctx: Pointer
    )

    fun buttplug_free_device(device: Pointer)
    fun buttplug_create_log_handle(
        callback: ButtplugLogCallback,
        ctx: Pointer?,
        maxLevel: String,
        useJson: Boolean
    ): Pointer

    fun buttplug_free_log_handle(logHandle: Pointer)
    fun buttplug_activate_env_logger()
}

internal interface ButtplugCallback : Callback {
    fun callback(ctx: Pointer?, ptr: Pointer, len: u32)

    companion object {
        fun of(callback: (Pointer?, Pointer, u32) -> Unit) = object : ButtplugCallback {
            override fun callback(ctx: Pointer?, ptr: Pointer, len: u32) = callback(ctx, ptr, len)
        }
    }
}

internal interface ButtplugLogCallback : Callback {
    fun callback(ctx: Pointer?, str: String)

    companion object {
        fun of(callback: (Pointer?, String) -> Unit) = object : ButtplugLogCallback {
            override fun callback(ctx: Pointer?, str: String) = callback(ctx, str)
        }
    }
}