package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.InputStream

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
    val logTag = "VoipScreen"
    val ctx = LocalContext.current

    var username by rememberSaveable { mutableStateOf("1003") }
    var password by rememberSaveable { mutableStateOf("1234") }
    var domain by rememberSaveable { mutableStateOf("192.168.31.245") }
    var peerUsername by rememberSaveable { mutableStateOf("550") }
    var message by rememberSaveable { mutableStateOf("Test") }
    var selectedFileUri by rememberSaveable() { mutableStateOf<Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        selectedFileUri = uri
    }

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
                modifier = Modifier.fillMaxWidth(),
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth(),
                value = peerUsername,
                onValueChange = { peerUsername = it },
                label = { Text("Call number") },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        voipManager.call(peerUsername, domain)
                    },
                ) {
                    Text("CALL")
                }

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
                modifier = Modifier.fillMaxWidth(),
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
            )

            Button(
                onClick = {
                    voipManager.send(message)
                },
            ) {
                Text("ENCODE")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        filePickerLauncher.launch("*/*")
                    },
                ) {
                    Text("SELECT A FILE")
                }

                Button(
                    onClick = {
                        selectedFileUri?.let { fileUri ->
                            val size = FileUtils.getSize(fileUri, ctx)
                            Log.d(logTag, "size=$size")
                            if (size == null) {
                                Log.e(logTag, "Failed to read file size: $fileUri")
                                return@let
                            }

                            Log.d(logTag, "creating stream")
                            val stream = FileUtils.getStream(fileUri, ctx)
                            if (stream == null) {
                                Log.e(logTag, "Failed to open file stream: $fileUri")
                                return@let
                            }
                            Log.d(logTag, "created stream")

                            voipManager.sendChunked(
                                stream,
                                size,
                            )
                        }
                    },
                ) {
                    Text("ENCODE")
                }
            }

            Text(text = (
                if (selectedFileUri != null)
                    "Selected: $selectedFileUri" else
                    "No file selected"
            ))
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
    override fun send(text: String) {}
    override fun send(data: ByteArray) {}
    override fun sendChunked(stream: InputStream, size: Long, chunkSize: Int) {}
}
