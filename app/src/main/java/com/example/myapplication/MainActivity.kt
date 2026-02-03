package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.MyApplicationTheme

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

        voipManager = VoipManagerV1(this)

        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                voipManager.iterate()
                handler.postDelayed(this, 20)
            }
        })

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
    var username by remember { mutableStateOf("1001") }
    var password by remember { mutableStateOf("1234") }
    var domain by remember { mutableStateOf("192.168.31.245") }
    var extension by remember { mutableStateOf("1002") }
    var message by remember { mutableStateOf("Test") }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") }
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") }
            )

            OutlinedTextField(
                value = domain,
                onValueChange = { domain = it },
                label = { Text("Asterisk IP") }
            )

            Button(onClick = {
                voipManager.login(username, password, domain)
            }) {
                Text("LOGIN")
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = extension,
                onValueChange = { extension = it },
                label = { Text("Call number") }
            )

            Button(onClick = {
                voipManager.call(extension)
            }) {
                Text("CALL")
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") }
            )

            Button(onClick = {
                val pcm = GgWaveBridge.encode(message)
//                AudioPlayer.playPcm(pcm)
                FakeMicBuffer.push(pcm)
            }) {
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
    override fun call(extension: String) {}
}
