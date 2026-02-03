package com.example.myapplication

object FakeMicBuffer {
    private val queue = ArrayDeque<ShortArray>()

    @Synchronized
    fun push(data: ShortArray) {
        queue.addLast(data)
    }

    @Synchronized
    fun read(out: ShortArray, size: Int): Int {
        if (queue.isEmpty()) {
            java.util.Arrays.fill(out, 0, size, 0)

            return size
        }

        val chunk = queue.removeFirst()
        val len = minOf(size, chunk.size)
        System.arraycopy(chunk, 0, out, 0, len)

        return len
    }
}
