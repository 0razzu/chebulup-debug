package io.orazzu.chebulup_debug

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
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
import java.io.InputStream
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
    fun sendChunked(stream: InputStream, size: Long, name: String?, chunkSize: Int = 560)
}

class VoipManagerV1 : VoipManager {
    private val logTag = "VoipManagerV1"

    private val pjThread = PjThread()
    private lateinit var acc: Account
    private lateinit var ep: Endpoint
    private var call: Call? = null
    private var player: AudioMediaPlayer? = null
    private val sendRawSem: Semaphore = Semaphore(16)

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
            Log.d(logTag, "Calling")
            call = Call(acc)
            try {
                call!!.makeCall("sip:$username@$domain", CallOpParam(true))
            } catch (e: Exception) {
                call!!.delete()
                call = null
                Log.e(logTag, "Failed to call", e)
            }
        }
    }

    private fun hangupInternal() {
        Log.d(logTag, "Hanging up")
        try {
            call?.hangup(CallOpParam())
        } catch (e: Exception) {
            Log.i(logTag, "Failed to hangup", e)
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
        send(PayloadV1(PayloadType.TEXT, null, text.toByteArray()))
    }

    override fun send(data: ByteArray) {
        send(PayloadV1(PayloadType.DATA, null, data))
    }

    override fun sendChunked(stream: InputStream, size: Long, name: String?, chunkSize: Int) {
        Log.d(logTag, "sendChunked(size=$size, chunkSize=$chunkSize)")
        CoroutineScope(Dispatchers.IO).launch {
            val header: PayloadHeader = PayloadHeaderV1(PayloadType.DATA, size.toULong(), name)
            sendRaw(header.toByteArray())

            val chunk = ByteArray(chunkSize)
            var read: Int
            stream.use {
                while (it.read(chunk).also { read = it } != -1) {
                    sendRaw(chunk.copyOfRange(0, read))
                }
            }
        }
    }

    private suspend fun sendRaw(data: ByteArray) {
        Log.d(logTag, "sendRaw(), data size: ${data.size}")

        sendRawSem.acquire()
        try {
            val pcmChunks = buildList {
                for (i in 0..<data.size step 140) {
                    val end = min(i + 140, data.size)
                    add(GgWaveBridge.encode(data.copyOfRange(i, end)))
                }
            }

            val wavFiles = pcmChunks.map { pcm ->
                write(pcm.trimSilence())
            }

            wavFiles.forEach { play(it) }
        } finally {
            sendRawSem.release()
        }
    }

    private fun send(payload: Payload) {
        CoroutineScope(Dispatchers.IO).launch {
            val pcmChunks = buildList {
                val payloadBytes = payload.toByteArray()
                for (i in 0..<payloadBytes.size step 140) {
                    val end = min(i + 140, payloadBytes.size)
                    add(GgWaveBridge.encode(payloadBytes.copyOfRange(i, end)))
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
        Log.d(logTag, "write()")

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
                    Log.e(logTag, "No active call")
                    return@run
                }

                val latch = CountDownLatch(1)

                player?.delete()
                player = object : AudioMediaPlayer() {
                    override fun onEof2() {
                        Log.d(logTag, "Finishing transmission of ${wavFile.name}")
                        latch.countDown()
                    }
                }
                player!!.createPlayer(wavFile.absolutePath, PJMEDIA_FILE_NO_LOOP.toLong())
                Log.d(logTag, "Starting transmission of ${wavFile.name}")
                player!!.startTransmit(call!!.getAudioMedia(-1))

                latch.await()
            } catch (e: Exception) {
                Log.e(logTag, "play() failed", e)
            } finally {
                wavFile.delete()
            }
        }
    }
}
