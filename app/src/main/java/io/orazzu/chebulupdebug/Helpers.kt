package io.orazzu.chebulupdebug

import kotlin.math.abs

fun ShortArray.trimSilence(threshold: Short = 100): ShortArray {
    var end = size - 1
    while (end > 0 && abs(this[end].toInt()) < threshold) {
        end--
    }

    return copyOfRange(0, end + 1)
}

fun crc16(data: ByteArray): UShort {
    var crc = 0
    for (byte in data) {
        crc = crc xor (byte.toInt() and 0xFF shl 8)
        repeat(8) {
            crc =
                if (crc and 0x8000 != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
        }
    }
    return (crc and 0xFFFF).toUShort()
}
