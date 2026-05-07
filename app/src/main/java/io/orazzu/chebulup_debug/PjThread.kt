package io.orazzu.chebulup_debug

import android.os.Handler
import android.os.HandlerThread
import org.pjsip.pjsua2.Endpoint

class PjThread {
    private val thread = HandlerThread("PJSIP-THREAD").apply { start() }
    private val handler = Handler(thread.looper)

    fun run(block: () -> Unit) {
        handler.post(block)
    }
}


fun registerThreadIfNeeded() {
    try {
        Endpoint.instance().libRegisterThread("PJSIP")
    } catch (_: Exception) {
        // already registered
    }
}
