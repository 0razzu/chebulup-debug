package com.example.myapplication

import android.content.Context
import android.util.Log
import org.pjsip.pjsua2.Account
import org.pjsip.pjsua2.AccountConfig
import org.pjsip.pjsua2.AudioMediaPlayer
import org.pjsip.pjsua2.AuthCredInfo
import org.pjsip.pjsua2.Call
import org.pjsip.pjsua2.CallOpParam
import org.pjsip.pjsua2.Endpoint
import org.pjsip.pjsua2.EpConfig
import org.pjsip.pjsua2.TransportConfig
import org.pjsip.pjsua2.pjmedia_file_player_option.PJMEDIA_FILE_NO_LOOP
import org.pjsip.pjsua2.pjsip_transport_type_e
import java.io.File
import java.io.FileOutputStream
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


interface VoipManager {
    fun login(username: String, password: String, domain: String)
    fun call(username: String, domain: String): Boolean
    fun hangup()
    fun send(pcm: ShortArray)
}

class VoipManagerV1(context: Context) : VoipManager {
    private lateinit var acc: Account
    private var ep: Endpoint
    private lateinit var call: Call
    private var player: AudioMediaPlayer? = null

    companion object {
        private const val TAG = "VoipManagerV1"
    }

    init {
        System.loadLibrary("pjsua2")

        ep = Endpoint()
        ep.libCreate()
        ep.libInit(EpConfig())
        val sipTpConfig = TransportConfig()
        sipTpConfig.port = 5060
        ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig)

        ep.libStart()
    }

    override fun login(username: String, password: String, domain: String) {
        val accCfg = AccountConfig()
        accCfg.idUri = "sip:$username@$domain"
        accCfg.regConfig.registrarUri = "sip:$domain"
        val cred = AuthCredInfo("digest", "*", username, 0, password)
        accCfg.sipConfig.authCreds.add(cred)

        acc = Account()
        acc.create(accCfg)
    }

    override fun call(username: String, domain: String): Boolean {
        call = Call(acc)
        try {
            call.makeCall("sip:$username@$domain", CallOpParam(true))
            return true
        } catch (e: Exception) {
            call.delete()
            Log.e(TAG, "Failed to call", e)
        }
        return false
    }

    override fun hangup() {
        call.hangup(CallOpParam())
        call.delete()
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun send(pcm: ShortArray) {
        try {
            if (!call.isActive) {
                Log.e(TAG, "No active call")
                return
            }

            val wavFile = File.createTempFile("ggwave_${Uuid.random()}", ".wav")
            writeWav(wavFile, pcm)

            player?.delete()
            player = AudioMediaPlayer()
            player!!.createPlayer(wavFile.absolutePath, PJMEDIA_FILE_NO_LOOP.toLong())

            player!!.startTransmit(call.getAudioMedia(-1))
            Log.d(TAG, "Injected audio into call")
        } catch (e: Exception) {
            Log.e(TAG, "send() failed", e)
        }
    }

    private fun writeWav(file: File, pcm: ShortArray) {
        val sampleRate = 48000
        val byteRate = sampleRate * 2
        val dataSize = pcm.size * 2
        val totalSize = 36 + dataSize

        FileOutputStream(file).use { out ->
            fun writeIntLE(v: Int) {
                out.write(byteArrayOf(
                    (v and 0xff).toByte(),
                    ((v shr 8) and 0xff).toByte(),
                    ((v shr 16) and 0xff).toByte(),
                    ((v shr 24) and 0xff).toByte()
                ))
            }

            fun writeShortLE(v: Int) {
                out.write(byteArrayOf(
                    (v and 0xff).toByte(),
                    ((v shr 8) and 0xff).toByte()
                ))
            }

            out.write("RIFF".toByteArray())
            writeIntLE(totalSize)
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray())
            writeIntLE(16)
            writeShortLE(1)
            writeShortLE(1)
            writeIntLE(sampleRate)
            writeIntLE(byteRate)
            writeShortLE(2)
            writeShortLE(16)
            out.write("data".toByteArray())
            writeIntLE(dataSize)

            for (s in pcm) writeShortLE(s.toInt())
        }
    }
}