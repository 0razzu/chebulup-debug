package com.example.myapplication

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
import java.util.concurrent.CountDownLatch
import kotlin.math.min
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


interface VoipManager {
    fun login(username: String, password: String, domain: String)
    fun call(username: String, domain: String)
    fun hangup()
    fun send(text: String)
    fun send(data: ByteArray)
}

class VoipManagerV1 : VoipManager {
    private val pjThread = PjThread()
    private lateinit var acc: Account
    private lateinit var ep: Endpoint
    private var call: Call? = null
    private var player: AudioMediaPlayer? = null

    companion object {
        private const val TAG = "VoipManagerV1"
    }

    init {
        System.loadLibrary("pjsua2")

        pjThread.run {
            ep = Endpoint()
            ep.libCreate()
            ep.libInit(EpConfig())
            val sipTpConfig = TransportConfig()
            sipTpConfig.port = 5060
            ep.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig)

            ep.libStart()
        }
    }

    override fun login(username: String, password: String, domain: String) {
        pjThread.run {
            registerThreadIfNeeded()

            val accCfg = AccountConfig()
            accCfg.idUri = "sip:$username@$domain"
            accCfg.regConfig.registrarUri = "sip:$domain"
            val cred = AuthCredInfo("digest", "*", username, 0, password)
            accCfg.sipConfig.authCreds.add(cred)

            acc = Account()
            acc.create(accCfg)
        }
    }

    override fun call(username: String, domain: String) {
        pjThread.run {
            registerThreadIfNeeded()

            hangupInternal()
            Log.d(TAG, "Calling")
            call = Call(acc)
            try {
                call!!.makeCall("sip:$username@$domain", CallOpParam(true))
            } catch (e: Exception) {
                call!!.delete()
                call = null
                Log.e(TAG, "Failed to call", e)
            }
        }
    }

    private fun hangupInternal() {
        Log.d(TAG, "Hanging up")
        try {
            call?.hangup(CallOpParam())
        } catch (e: Exception) {
            Log.i(TAG, "Failed to hangup", e)
        }
        call?.delete()
        call = null
    }

    override fun hangup() {
        pjThread.run {
            registerThreadIfNeeded()
            hangupInternal()
        }
    }

    override fun send(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val pcmChunks = buildList {
                val header = "$PAYLOAD_VERSION${PayloadType.TEXT}${text.length}"
                add(GgWaveBridge.encodeText(header))

                for (i in 0..<text.length step 140) {
                    val end = min(i + 140, text.length) - 1
                    add(GgWaveBridge.encodeText(text.substring(i, end)))
                }
            }

            val wavFiles = pcmChunks.map { pcm ->
                async { write(pcm.trimSilence()) }
            }

            wavFiles.forEach { fut ->
                val wavFile = fut.await()
                play(wavFile)
            }
        }
    }

    override fun send(data: ByteArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val pcmChunks = buildList {
                val header = "$PAYLOAD_VERSION${PayloadType.DATA}${data.size}"
                add(GgWaveBridge.encodeText(header))

                for (i in 0..<data.size step 140) {
                    val end = min(i + 140, data.size)
                    add(GgWaveBridge.encodeBytes(data.copyOfRange(i, end)))
                }
            }

            val wavFiles = pcmChunks.map { pcm ->
                async { write(pcm.trimSilence()) }
            }

            wavFiles.forEach { fut ->
                val wavFile = fut.await()
                play(wavFile)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun write(pcm: ShortArray): File {
        val sampleRate = 48000
        val byteRate = sampleRate * 2
        val dataSize = pcm.size * 2
        val totalSize = 36 + dataSize

        val file = File.createTempFile("ggwave_${Uuid.random()}", ".wav")
        FileOutputStream(file).use { out ->
            fun writeIntLE(v: Int) {
                out.write(
                    byteArrayOf(
                        (v and 0xff).toByte(),
                        ((v shr 8) and 0xff).toByte(),
                        ((v shr 16) and 0xff).toByte(),
                        ((v shr 24) and 0xff).toByte(),
                    ),
                )
            }

            fun writeShortLE(v: Int) {
                out.write(
                    byteArrayOf(
                        (v and 0xff).toByte(),
                        ((v shr 8) and 0xff).toByte(),
                    ),
                )
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

//        val debugFile = File(
//            ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
//            "ggwave_${System.currentTimeMillis()}.wav",
//        )
//        file.copyTo(debugFile, overwrite = true)
//        Log.d(TAG, "Saved debug WAV to ${debugFile.absolutePath}")

        return file
    }

    private fun play(wavFile: File) {
        pjThread.run {
            registerThreadIfNeeded()

            try {
                if (call == null || !call!!.isActive) {
                    Log.e(TAG, "No active call")
                    return@run
                }

                val latch = CountDownLatch(1)

                player?.delete()
                player = object : AudioMediaPlayer() {
                    override fun onEof2() {
                        Log.d(TAG, "Finishing transmission of ${wavFile.name}")
                        latch.countDown()
                    }
                }
                player!!.createPlayer(wavFile.absolutePath, PJMEDIA_FILE_NO_LOOP.toLong())
                Log.d(TAG, "Starting transmission of ${wavFile.name}")
                player!!.startTransmit(call!!.getAudioMedia(-1))

                latch.await()
            } catch (e: Exception) {
                Log.e(TAG, "play() failed", e)
            } finally {
                wavFile.delete()
            }
        }
    }
}
