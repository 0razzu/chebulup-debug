package com.example.myapplication


const val PAYLOAD_VERSION: Int = 1


enum class PayloadType(private val serialized: String) {
    DATA("d"),
    TEXT("t");

    override fun toString(): String = serialized
}
