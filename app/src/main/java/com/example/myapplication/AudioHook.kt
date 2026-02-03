package com.example.myapplication

import android.media.AudioRecord
import android.util.Log

object AudioHook {
    fun install() {
        try {
            val cls = Class.forName("android.media.AudioRecord")
            Log.d("AUDIO_HOOK", "Fields: ${cls.fields.map { it.name }}")
            val field = cls.getDeclaredField("sAudioRecordImpl")
            field.isAccessible = true
            field.set(null, FakeAudioRecordFactory())
        } catch (e: Exception) {
            Log.e("AUDIO_HOOK", "Hook failed", e)
        }
    }
}

class FakeAudioRecordFactory {
    fun create(
        sampleRate: Int,
        channelConfig: Int,
        format: Int,
        bufferSize: Int
    ): AudioRecord {
        return FakeAudioRecord(sampleRate, channelConfig, format, bufferSize)
    }
}
