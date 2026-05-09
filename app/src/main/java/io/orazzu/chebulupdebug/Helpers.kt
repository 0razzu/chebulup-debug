package io.orazzu.chebulupdebug

import kotlin.math.abs

fun ShortArray.trimSilence(threshold: Short = 100): ShortArray {
    var end = size - 1
    while (end > 0 && abs(this[end].toInt()) < threshold) {
        end--
    }

    return copyOfRange(0, end + 1)
}

const val CHECKSUM_SIZE = 2

fun crc16(data: ByteArray, begin: Int, end: Int): UShort {
    var crc = 0
    for (i in begin..<end) {
        val byte = data[i]
        crc = crc xor ((byte.toInt() and 0xFF) shl 8)
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

fun appendChecksum(chunk: ByteArray, begin: Int, end: Int): ByteArray {
    val chunkSize = end - begin
    val signedChunk = ByteArray(chunkSize + CHECKSUM_SIZE)
    System.arraycopy(chunk, begin, signedChunk, 0, chunkSize)

    val checksum = crc16(signedChunk, 0, chunkSize).toInt()
    signedChunk[chunkSize] = (checksum shr 8).toByte()
    signedChunk[chunkSize + 1] = checksum.toByte()

    return signedChunk
}
