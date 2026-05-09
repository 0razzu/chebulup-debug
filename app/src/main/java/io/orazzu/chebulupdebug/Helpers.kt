package io.orazzu.chebulupdebug

import kotlin.math.abs

fun ShortArray.trimSilence(threshold: Short = 100): ShortArray {
    var end = size - 1
    while (end > 0 && abs(this[end].toInt()) < threshold) {
        end--
    }

    return copyOfRange(0, end + 1)
}
