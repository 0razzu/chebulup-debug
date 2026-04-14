package com.example.myapplication

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

object AudioPlayer {
    fun playPcm(pcm: ShortArray, sampleRate: Int = 48000) {
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            pcm.size * 2,
            AudioTrack.MODE_STATIC
        )

        track.write(pcm, 0, pcm.size)
        track.play()
    }
}
