package com.example.myapplication

import android.media.AudioRecord
import android.media.MediaRecorder

class FakeAudioRecord(
    sampleRate: Int,
    channelConfig: Int,
    format: Int,
    bufferSize: Int
) : AudioRecord(
    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
    sampleRate,
    channelConfig,
    format,
    bufferSize
) {

    private val temp = ShortArray(bufferSize / 2)

    override fun read(audioData: ShortArray, offsetInShorts: Int, sizeInShorts: Int): Int {
        return FakeMicBuffer.read(audioData, sizeInShorts)
    }
}
