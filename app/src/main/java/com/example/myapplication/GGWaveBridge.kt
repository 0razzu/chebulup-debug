package com.example.myapplication

object GgWaveBridge {
    init {
        System.loadLibrary("ggwave")
    }

    external fun init()
    external fun encode(text: String): ShortArray
    external fun decode(pcm: ShortArray): ByteArray?
}
