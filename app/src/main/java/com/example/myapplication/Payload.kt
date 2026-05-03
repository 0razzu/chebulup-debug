package com.example.myapplication


enum class PayloadType(private val serialized: String) {
    DATA("d"), TEXT("t");

    override fun toString(): String = serialized
}


abstract class Payload(val v: Int) {
    abstract fun toByteArray(): ByteArray
}


class PayloadV1(
    val type: PayloadType,
    val data: ByteArray,
) : Payload(1) {
    override fun toByteArray() = "$v$type${data.size}".toByteArray() + data
}
