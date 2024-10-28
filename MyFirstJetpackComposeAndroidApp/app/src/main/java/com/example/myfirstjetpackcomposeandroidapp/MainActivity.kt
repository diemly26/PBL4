package com.example.myfirstjetpackcomposeandroidapp

import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myfirstjetpackcomposeandroidapp.ui.theme.MyFirstJetpackComposeAndroidAppTheme
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

var ESP8266_URL = "http://10.10.27.246"
private const val REQUEST_MIC_PERMISSION = 200
private const val REQUEST_STORAGE_PERMISSION = 300

class MainActivity : ComponentActivity() {
//    private val ESP8266_URL = "http://10.10.27.246"
    private val TAG: String = "HTTP_Response"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var outputFile: File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MyFirstJetpackComposeAndroidAppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(red = 4, green = 41, blue = 64),
                                    Color(red = 0, green = 92, blue = 83)
                                )
                            )
                        )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, top = 50.dp, end = 16.dp, bottom = 50.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            var lightIsOn by remember { mutableStateOf(false) }
                            var textOfLightButton by remember { mutableStateOf("Bật đèn ") }
                            var lightOnPainter = painterResource(id = R.drawable.light_is_on)
                            var lightOffPainter = painterResource(id = R.drawable.light_is_off)
                            var lightPainter by remember { mutableStateOf(lightOffPainter) }

                            Button(
                                onClick = {
                                    lightIsOn = !lightIsOn
                                    textOfLightButton = if (lightIsOn) "Tắt đèn " else "Bật đèn "
                                    if (lightIsOn) {
                                        lightPainter = lightOnPainter
                                        sendRequest("/light/on")
                                    } else {
                                        lightPainter = lightOffPainter
                                        sendRequest("/light/off")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (lightIsOn) Color(red = 242, green = 235, blue = 133) else Color(red = 180, green = 190, blue = 201)
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "${textOfLightButton}",
                                    color = Color.Black,
                                    fontSize = 30.sp
                                )
                                Image(
                                    painter = lightPainter,
                                    contentDescription = "Sample Image",
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(30.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            var fanIsOn by remember { mutableStateOf(false) }
                            var textOfFanButton by remember { mutableStateOf("Bật quạt ") }
                            var fanOnPainter = painterResource(id = R.drawable.fan_is_on)
                            var fanOffPainter = painterResource(id = R.drawable.fan_is_off)
                            var fanPainter by remember { mutableStateOf(fanOffPainter) }

                            Button(
                                onClick = {
                                    fanIsOn = !fanIsOn
                                    textOfFanButton = if (fanIsOn) "Tắt quạt " else "Bật quạt "
                                    if (fanIsOn) {
                                        fanPainter = fanOnPainter
                                        sendRequest("/fan/on")
                                    } else {
                                        fanPainter = fanOffPainter
                                        sendRequest("/fan/off")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (fanIsOn) Color(red = 150, green = 200, blue = 255) else Color(red = 180, green = 190, blue = 201)
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "${textOfFanButton}",
                                    color = Color.Black,
                                    fontSize = 30.sp
                                )
                                Image(
                                    painter = fanPainter,
                                    contentDescription = "Sample Image",
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(30.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            var doorIsOpen by remember { mutableStateOf(false) }
                            var textOfDoorButton by remember { mutableStateOf("Mở cửa") }
                            var doorOnPainter = painterResource(id = R.drawable.door_is_open)
                            var doorOffPainter = painterResource(id = R.drawable.door_is_close)
                            var doorPainter by remember { mutableStateOf(doorOffPainter) }

                            Button(
                                onClick = {
                                    doorIsOpen = !doorIsOpen
                                    textOfDoorButton = if (doorIsOpen) "Đóng cửa" else "Mở cửa"
                                    if (doorIsOpen) {
                                        doorPainter = doorOnPainter
                                        sendRequest("/door/open")
                                    } else {
                                        doorPainter = doorOffPainter
                                        sendRequest("/door/close")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (doorIsOpen) Color(red = 140, green = 82, blue = 62) else Color(red = 180, green = 190, blue = 201)
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "${textOfDoorButton}",
                                    color = Color.Black,
                                    fontSize = 30.sp
                                )
                                Image(
                                    painter = doorPainter,
                                    contentDescription = "Sample Image",
                                    modifier = Modifier
                                        .width(30.dp)
                                        .height(30.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Thêm biến để quản lý trạng thái ẩn/hiện của TextField
                            var isTextFieldVisible by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = { isTextFieldVisible = !isTextFieldVisible },
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = "Nhà Thông Minh",
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        fontSize = 25.sp
                                    )
                                }

                                var micIsOn by remember { mutableStateOf(false) }
                                var textOfMicButton by remember { mutableStateOf("Mic") }
                                var micOnPainter = painterResource(id = R.drawable.record)
                                var micOffPainter = painterResource(id = R.drawable.mic)
                                var micPainter by remember { mutableStateOf(micOffPainter) }

                                Button(
                                    onClick = {
                                        if (!checkPermissions()) {
                                            requestPermissions()
                                        } else {

                                            micIsOn = !micIsOn
                                            textOfMicButton = if (micIsOn) "Nói gì đi" else "Mic"
                                            if (micIsOn) {
                                                micPainter = micOnPainter
                                                startRecording()
                                            } else {
                                                micPainter = micOffPainter
                                                stopRecording()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .width(90.dp)
                                        .fillMaxHeight()
                                        .padding(end = 0.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (micIsOn) Color(red = 191, green = 4, blue = 38) else Color(red = 180, green = 190, blue = 201)
                                    ),
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Image(
                                        painter = micPainter,
                                        contentDescription = "Sample Image",
                                        modifier = Modifier
                                            .width(30.dp)
                                            .height(30.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            var currentUrl by remember { mutableStateOf(ESP8266_URL) }
                            // Hiển thị TextField khi isTextFieldVisible là true
                            if (isTextFieldVisible) {
                                BasicTextField(
                                    value = currentUrl,
                                    onValueChange = { newValue ->
                                        currentUrl = newValue
                                        ESP8266_URL = newValue // Cập nhật giá trị ESP8266_URL theo giá trị người dùng nhập
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendRequest(endpoint: String) {
        Thread {
            try {
                val url = URL(ESP8266_URL + endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                Log.d(TAG, "Response Code: $responseCode")

                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Response: $response")
                } else {
                    Log.e(TAG, "Error Response Code: $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Exception: ${e.message}")
            }
        }.start()
    }

    private fun checkPermissions(): Boolean {
        val micPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
        val storagePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return micPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_PERMISSIONS_CODE)
        }
    }

    // Thêm mã request code để nhận diện yêu cầu
    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 100
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Tất cả quyền đã được cấp", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Không đủ quyền truy cập", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecording() {
        outputFile = File(getExternalFilesDir(null), "recorded_audio.mp4")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        Toast.makeText(this, "Bắt đầu ghi âm", Toast.LENGTH_SHORT).show()
        Log.d(TAG,"Bắt đầu ghi âm")
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null

        Toast.makeText(this, "Đã dừng ghi âm", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Đã dừng ghi âm và lưu tại ${outputFile.absolutePath}")

        // Phát file âm thanh sau khi dừng ghi
        mediaPlayer = MediaPlayer().apply {
            setDataSource(outputFile.absolutePath)
            prepare()  // Chuẩn bị phát âm thanh
            setOnCompletionListener {
                release()
                mediaPlayer = null
            }
            start()    // Phát âm thanh
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}