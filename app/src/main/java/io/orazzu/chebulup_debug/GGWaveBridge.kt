package io.orazzu.chebulup_debug

object GgWaveBridge {
    init {
        System.loadLibrary("ggwave")
    }

    external fun init()
    external fun encode(data: ByteArray): ShortArray
    external fun decode(pcm: ShortArray): ByteArray?
}
