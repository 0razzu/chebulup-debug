package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private lateinit var voipManager: VoipManagerV1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.RECORD_AUDIO),
//                0
//            )
//        }

        voipManager = VoipManagerV1()
        GgWaveBridge.init()

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                VoipScreen(voipManager)
            }
        }
    }
}

@Composable
fun VoipScreen(voipManager: VoipManager) {
    val tag = "VoipScreen"

    var username by remember { mutableStateOf("1003") }
    var password by remember { mutableStateOf("1234") }
    var domain by remember { mutableStateOf("192.168.31.245") }
    var peerUsername by remember { mutableStateOf("550") }
    var message by remember { mutableStateOf("Test") }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
            )

            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Asterisk IP") },
            )

            Button(
                onClick = {
                    voipManager.login(username, password, domain)
                },
            ) {
                Text("LOGIN")
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = peerUsername,
                onValueChange = { peerUsername = it },
                label = { Text("Call number") },
            )

            Row {
                Button(
                    onClick = {
                        voipManager.call(peerUsername, domain)
                    },
                ) {
                    Text("CALL")
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        voipManager.hangup()
                    },
                ) {
                    Text("HANGUP")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
            )

            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val pcmChunks = buildList {
                            add(GgWaveBridge.encode(message.length.toString()))
                            for (i in 0..<message.length step 140) {
                                val end = min(i + 140, message.length) - 1
                                add(GgWaveBridge.encode(message.slice(IntRange(i, end))))
                            }
                        }

                        val wavFiles = pcmChunks.map { pcm ->
                            async { voipManager.write(pcm.trimSilence()) }
                        }

                        wavFiles.awaitAll().forEach { wavFile ->
                            voipManager.play(wavFile)
                        }
                    }
                },
            ) {
                Text("ENCODE")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoipPreview() {
    MyApplicationTheme {
        VoipScreen(voipManager = FakeVoipManager())
    }
}

class FakeVoipManager : VoipManager {
    override fun login(username: String, password: String, domain: String) {}
    override fun call(username: String, domain: String) {}
    override fun hangup() {}

    override fun write(pcm: ShortArray): File {
        throw NotImplementedError()
    }

    override fun play(wavFile: File) {}
}
