package io.orazzu.chebulupdebug

import java.nio.ByteBuffer

enum class PayloadType(
    private val serialized: Byte,
) {
    DATA(0b00),
    TEXT(0b01),
    ;

    fun toByte(): Byte = serialized

    companion object {
        fun fromByte(b: Byte): PayloadType = entries.first { it.serialized == b }
    }
}

abstract class Payload(
    val v: Byte,
) {
    abstract fun toByteArray(): ByteArray
}

abstract class PayloadHeader(
    val v: Byte,
) {
    abstract fun toByteArray(): ByteArray
}

class PayloadV1(
    val type: PayloadType,
    val name: String?,
    val data: ByteArray,
) : Payload(1) {
    override fun toByteArray(): ByteArray =
        when (type) {
            PayloadType.DATA -> toByteArrayData()
            PayloadType.TEXT -> toByteArrayText()
        }

    private fun toByteArrayCommon(buf: ByteBuffer) {
        buf.put(v)
        buf.put(type.toByte())
        buf.putLong(data.size.toLong())
    }

    private fun toByteArrayData(): ByteArray {
        val nameLen = (name?.length ?: 0).toUShort()
        val buf = ByteBuffer.allocate(1 + 1 + 8 + 2 + nameLen.toInt() + data.size)

        toByteArrayCommon(buf)
        buf.putShort(nameLen.toShort())
        if (name != null) {
            buf.put(name.toByteArray())
        }
        buf.put(data)

        return buf.array()
    }

    private fun toByteArrayText(): ByteArray {
        val buf = ByteBuffer.allocate(1 + 1 + 8 + data.size)

        toByteArrayCommon(buf)
        buf.put(data)

        return buf.array()
    }
}

class PayloadHeaderV1(
    val type: PayloadType,
    val size: ULong,
    val name: String?,
) : PayloadHeader(1) {
    override fun toByteArray(): ByteArray =
        when (type) {
            PayloadType.DATA -> toByteArrayData()
            PayloadType.TEXT -> toByteArrayText()
        }

    private fun toByteArrayCommon(buf: ByteBuffer) {
        buf.put(v)
        buf.put(type.toByte())
        buf.putLong(size.toLong())
    }

    private fun toByteArrayData(): ByteArray {
        val nameLen = (name?.length ?: 0).toUShort()
        val buf = ByteBuffer.allocate(1 + 1 + 8 + 2 + nameLen.toInt())

        toByteArrayCommon(buf)
        buf.putShort(nameLen.toShort())
        if (name != null) {
            buf.put(name.toByteArray())
        }

        return buf.array()
    }

    private fun toByteArrayText(): ByteArray {
        val buf = ByteBuffer.allocate(1 + 1 + 8)

        toByteArrayCommon(buf)

        return buf.array()
    }
}
