package com.maxai.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackCaptureConfiguration
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Night = Color(0xFF080812)
private val Violet = Color(0xFF9B5CFF)
private val Cyan = Color(0xFF54E7F2)

class MainActivity : ComponentActivity() {
    private val askMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) status.value = "Microphone permission is needed for voice chat"
    }
    private val status = mutableStateOf("Add your Gemini API key to begin")
    private var live: GeminiLiveClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 8, 18)
        window.navigationBarColor = android.graphics.Color.rgb(8, 8, 18)
        setContent { MaxScreen(status.value, ::startLive, ::stopLive, ::saveKey, ::loadKey) }
    }

    private fun saveKey(key: String) {
        try {
            val master = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val prefs = EncryptedSharedPreferences.create(this, "max_secure", master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
            prefs.edit().putString("gemini_key", key.trim()).apply()
            status.value = "API key saved securely on this device"
        } catch (e: Exception) { status.value = "Could not securely save key: ${e.message}" }
    }

    private fun loadKey(): String {
        return try {
            val master = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val prefs = EncryptedSharedPreferences.create(this, "max_secure", master,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
            prefs.getString("gemini_key", "") ?: ""
        } catch (_: Exception) { "" }
    }

    private fun startLive() {
        val key = loadKey()
        if (key.isBlank()) { status.value = "Enter and save your Gemini API key first"; return }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            askMic.launch(Manifest.permission.RECORD_AUDIO); return
        }
        stopLive()
        val client = GeminiLiveClient(this, key, { status.value = it })
        live = client
        client.connect()
    }

    private fun stopLive() { live?.stop(); live = null; status.value = "Voice session stopped" }

    override fun onDestroy() { stopLive(); super.onDestroy() }
}

@Composable
private fun MaxScreen(status: String, onStart: () -> Unit, onStop: () -> Unit, onSave: (String) -> Unit, onLoad: () -> String) {
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(false) }
    val infinite = rememberInfiniteTransition(label = "orb")
    val pulse by infinite.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    LaunchedEffect(Unit) { apiKey = onLoad() }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111024), Night, Color(0xFF070B16))))) {
        Box(Modifier.align(Alignment.TopStart).offset(x = (-70).dp, y = 90.dp).size(210.dp).blur(90.dp).background(Violet.copy(alpha = .24f), CircleShape))
        Box(Modifier.align(Alignment.BottomEnd).offset(x = 70.dp, y = (-90).dp).size(230.dp).blur(100.dp).background(Cyan.copy(alpha = .15f), CircleShape))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MAX", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 5.sp)
                    Text("YOUR AI COMPANION", color = Cyan, fontSize = 9.sp, letterSpacing = 3.sp)
                }
                Text("GEMINI LIVE", color = Color.White.copy(alpha = .75f), fontSize = 10.sp, modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = .07f)).border(1.dp, Color.White.copy(alpha = .13f), CircleShape).padding(horizontal = 13.dp, vertical = 8.dp))
            }
            Spacer(Modifier.weight(.7f))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
                Box(Modifier.size((230 * pulse).dp).blur(32.dp).background(Brush.radialGradient(listOf(Violet.copy(alpha = .40f), Cyan.copy(alpha = .10f), Color.Transparent)), CircleShape))
                Box(Modifier.size(204.dp).border(1.dp, Brush.linearGradient(listOf(Cyan.copy(alpha = .8f), Violet.copy(alpha = .9f), Color.White.copy(alpha = .12f))), CircleShape).padding(12.dp).border(1.dp, Color.White.copy(alpha = .12f), CircleShape).background(Color(0xAA17152C), CircleShape), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (active) "LISTENING" else "MAX AI", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            repeat(7) { i ->
                                val h by infinite.animateFloat(8f, (17 + (i % 3) * 9).toFloat(), infiniteRepeatable(tween(420 + i * 70, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "wave$i")
                                Box(Modifier.width(4.dp).height(if (active) h.dp else 5.dp).clip(CircleShape).background(if (i % 2 == 0) Cyan else Violet))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(if (active) "I'm listening. Speak naturally." else "Ready when you are", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(status, color = Color.White.copy(alpha = .55f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { if (active) { onStop(); active = false } else { onStart(); active = true } }, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFF38234E) else Color(0xFF7448D8)), modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text(if (active) "END VOICE SESSION" else "START TALKING", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(24.dp))
            Surface(color = Color.White.copy(alpha = .055f), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .12f)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("GEMINI API KEY", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Paste your API key", color = Color.White.copy(alpha = .35f)) }, visualTransformation = if (showKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Cyan, unfocusedBorderColor = Color.White.copy(alpha = .2f), cursorColor = Cyan))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show key", color = Color.White.copy(alpha = .55f), fontSize = 11.sp, modifier = Modifier.weight(1f).clickable { showKey = !showKey })
                        TextButton(onClick = { onSave(apiKey) }) { Text("SAVE KEY", color = Cyan, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Live voice requires internet and available Gemini API quota.", color = Color.White.copy(alpha = .38f), fontSize = 10.sp)
            Spacer(Modifier.weight(.35f))
        }
    }
}

private class GeminiLiveClient(private val context: Context, private val apiKey: String, private val onStatus: (String) -> Unit) {
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private var socket: WebSocket? = null
    @Volatile private var running = false
    private var recorder: AudioRecord? = null
    private var player: AudioTrack? = null
    private var captureThread: Thread? = null

    fun connect() {
        onStatus("Connecting to Gemini Live…")
        val request = Request.Builder().url("wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey").build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val setup = JSONObject().put("setup", JSONObject()
                    .put("model", "models/gemini-3.8-live")
                    .put("generationConfig", JSONObject()
                        .put("responseModalities", org.json.JSONArray().put("AUDIO"))
                        .put("speechConfig", JSONObject().put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", "Aoede")))))
                    .put("systemInstruction", JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", "You are MAX, a friendly and capable AI assistant. Speak naturally in the user's language, including Hindi, English and Hinglish. Use a warm, clear feminine-sounding voice. Keep replies conversational and concise.")))))
                webSocket.send(setup.toString())
                onStatus("Connected • Starting microphone…")
                startAudio()
            }
            override fun onMessage(webSocket: WebSocket, text: String) { handleMessage(text) }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { onStatus("Connection error: ${t.message ?: "unknown error"}"); stopAudio() }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { onStatus("Session closed: $reason"); stopAudio() }
        })
    }

    private fun startAudio() {
        try {
            val minIn = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val minOut = AudioTrack.getMinBufferSize(24000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minIn, 4096))
            player = AudioTrack(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(), AudioFormat.Builder().setSampleRate(24000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(), maxOf(minOut, 8192), AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
            recorder?.startRecording(); player?.play(); running = true
            captureThread = Thread {
                val buffer = ByteArray(2048)
                while (running) {
                    val count = recorder?.read(buffer, 0, buffer.size) ?: -1
                    if (count > 0) {
                        val payload = Base64.encodeToString(buffer.copyOf(count), Base64.NO_WRAP)
                        val msg = JSONObject().put("realtimeInput", JSONObject().put("audio", JSONObject().put("mimeType", "audio/pcm;rate=16000").put("data", payload)))
                        socket?.send(msg.toString())
                    }
                }
            }.apply { name = "MAX-AudioCapture"; start() }
            onStatus("Listening • Speak naturally")
        } catch (e: Exception) { onStatus("Audio error: ${e.message}"); stopAudio() }
    }

    private fun handleMessage(raw: String) {
        try {
            val root = JSONObject(raw)
            if (root.has("error")) { onStatus("Gemini error: ${root.getJSONObject("error").optString("message")}"); return }
            val server = root.optJSONObject("serverContent") ?: return
            val turn = server.optJSONObject("modelTurn") ?: return
            val parts = turn.optJSONArray("parts") ?: return
            for (i in 0 until parts.length()) {
                val inline = parts.optJSONObject(i)?.optJSONObject("inlineData") ?: continue
                val encoded = inline.optString("data")
                if (encoded.isNotBlank()) {
                    val pcm = Base64.decode(encoded, Base64.DEFAULT)
                    player?.write(pcm, 0, pcm.size)
                    onStatus("MAX is speaking…")
                }
            }
            if (server.optBoolean("turnComplete")) onStatus("Listening • Speak naturally")
        } catch (_: Exception) { }
    }

    private fun stopAudio() {
        running = false
        try { recorder?.stop() } catch (_: Exception) { }
        try { recorder?.release() } catch (_: Exception) { }
        try { player?.pause(); player?.flush(); player?.release() } catch (_: Exception) { }
        recorder = null; player = null
    }

    fun stop() { stopAudio(); socket?.close(1000, "User ended session"); socket = null; client.dispatcher.executorService.shutdown() }
}
