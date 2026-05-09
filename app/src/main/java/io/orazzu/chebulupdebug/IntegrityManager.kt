package io.orazzu.chebulupdebug

import java.util.concurrent.atomic.AtomicInteger

const val CHECKSUM_SIZE = 2
const val SEQ_NO_SIZE = 4
const val INTEGRITY_BYTES = CHECKSUM_SIZE + SEQ_NO_SIZE

object IntegrityManager {
    private val nextSeqNo = AtomicInteger(0)

    fun crc16(
        data: ByteArray,
        begin: Int,
        end: Int,
    ): UShort {
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

    fun sign(
        chunk: ByteArray,
        begin: Int,
        end: Int,
    ): ByteArray {
        val chunkSize = end - begin
        val signedChunk = ByteArray(chunkSize + INTEGRITY_BYTES)
        System.arraycopy(chunk, begin, signedChunk, 0, chunkSize)

        val seqNo = nextSeqNo.getAndIncrement().toUInt()
        for (i in 0..<SEQ_NO_SIZE) {
            signedChunk[chunkSize + i] = ((seqNo shr ((SEQ_NO_SIZE - 1 - i) * 8)) and 0xFFu).toByte()
        }

        val checksum = crc16(signedChunk, 0, chunkSize).toInt()
        val checksumOffset = chunkSize + SEQ_NO_SIZE
        signedChunk[checksumOffset] = (checksum shr 8).toByte()
        signedChunk[checksumOffset + 1] = checksum.toByte()

        return signedChunk
    }
}
