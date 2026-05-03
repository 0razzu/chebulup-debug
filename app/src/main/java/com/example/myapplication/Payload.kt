package com.example.myapplication

import java.nio.ByteBuffer


enum class PayloadType(private val serialized: Byte) {
    DATA(0b00),
    TEXT(0b01);

    fun toByte(): Byte = serialized

    companion object {
        fun fromByte(b: Byte): PayloadType = entries.first { it.serialized == b }
    }
}


abstract class Payload(val v: Byte) {
    abstract fun toByteArray(): ByteArray
}


class PayloadV1(
    val type: PayloadType,
    val data: ByteArray,
) : Payload(1) {
    override fun toByteArray(): ByteArray {
        val buf = ByteBuffer.allocate(1 + 1 + 4 + data.size)

        buf.put(v)
        buf.put(type.toByte())
        buf.putInt(data.size)
        buf.put(data)

        return buf.array()
    }
}
