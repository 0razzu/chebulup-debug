package com.example.myapplication

object GgWaveBridge {
    init {
        System.loadLibrary("ggwave")
    }

    external fun init()
    external fun encodeText(text: String): ShortArray
    external fun encodeBytes(data: ByteArray): ShortArray
    external fun decode(pcm: ShortArray): ByteArray?
}
