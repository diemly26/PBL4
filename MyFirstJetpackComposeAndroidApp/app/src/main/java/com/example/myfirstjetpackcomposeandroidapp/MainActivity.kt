package com.example.myfirstjetpackcomposeandroidapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.myfirstjetpackcomposeandroidapp.ui.theme.MyFirstJetpackComposeAndroidAppTheme
import com.example.myfirstjetpackcomposeandroidapp.ui.theme.SensorViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask


data class ResponseData(
    val bestMatch: String,
    val recognizedText: String,
    val score: Int
)

var ESP8266_URL = "http://10.10.28.63"
private const val REQUEST_MIC_PERMISSION = 200
private const val REQUEST_STORAGE_PERMISSION = 300
val GAS_THRESHOLD = 375

class MainActivity : ComponentActivity() {

    private val hhandler = Handler(Looper.getMainLooper())
    private val updateInterval = 5000L // 5 seconds

//    private val ESP8266_URL = "http://10.10.27.246"
    val database = FirebaseDatabase.getInstance().reference.child("keys")
    private val TAG: String = "HTTP_Response"
    private var mediaRecorder: MediaRecorder? = null
    private var heymisaMediaRecord: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var outputFile: File
    private lateinit var outputAudioFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    if (!checkPermissions()) {
        requestPermissions()
    }
        startTimer()
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
                    var lightIsOn by remember { mutableStateOf(false) }
                    var textOfLightButton by remember { mutableStateOf("Bật đèn ") }
                    var lightOnPainter = painterResource(id = R.drawable.light_is_on)
                    var lightOffPainter = painterResource(id = R.drawable.light_is_off)
                    var lightPainter by remember { mutableStateOf(lightOffPainter) }

                    var fanIsOn by remember { mutableStateOf(false) }
                    var textOfFanButton by remember { mutableStateOf("Bật quạt ") }
                    var fanOnPainter = painterResource(id = R.drawable.fan_is_on)
                    var fanOffPainter = painterResource(id = R.drawable.fan_is_off)
                    var fanPainter by remember { mutableStateOf(fanOffPainter) }

                    var doorIsOpen by remember { mutableStateOf(false) }
                    var textOfDoorButton by remember { mutableStateOf("Mở cửa") }
                    var doorOnPainter = painterResource(id = R.drawable.door_is_open)
                    var doorOffPainter = painterResource(id = R.drawable.door_is_close)
                    var doorPainter by remember { mutableStateOf(doorOffPainter) }

                    var temperature by remember { mutableStateOf("--") }
                    var humidity by remember { mutableStateOf("--") }
                    var gasLevel by remember { mutableStateOf(0) }
                    var showDialog by remember { mutableStateOf(false) }

                    database.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            // Cập nhật trạng thái từ Firebase
                            lightIsOn = snapshot.child("1").getValue(Int::class.java) == 1
                            textOfLightButton = if (lightIsOn) "Tắt đèn " else "Bật đèn "
                            lightPainter = if (lightIsOn) lightOnPainter else lightOffPainter

                            fanIsOn = snapshot.child("2").getValue(Int::class.java) == 1
                            textOfFanButton = if (fanIsOn) "Tắt quạt " else "Bật quạt "
                            fanPainter = if (fanIsOn) fanOnPainter else fanOffPainter

                            doorIsOpen = snapshot.child("3").getValue(Int::class.java) == 1
                            textOfDoorButton = if (doorIsOpen) "Đóng cửa" else "Mở cửa"
                            doorPainter = if (doorIsOpen) doorOnPainter else doorOffPainter
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Xử lý lỗi nếu cần
                            Log.e("FirebaseError", "Không thể lấy dữ liệu từ Firebase: ${error.message}")
                        }
                    })

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

//                            LaunchedEffect(Unit) {
//                                while (true) {
//                                    try {
//                                        // Gọi API
//                                        val url = URL("http://10.10.28.63/status")
//                                        val connection = url.openConnection() as HttpURLConnection
//                                        connection.requestMethod = "GET"
//
//                                        val responseCode = connection.responseCode
//                                        if (responseCode == HttpURLConnection.HTTP_OK) {
//                                            val response = connection.inputStream.bufferedReader().use { it.readText() }
//                                            val jsonObject = JSONObject(response)
//
//                                            // Cập nhật state
//                                            temperature = "${jsonObject.getDouble("temperature")}"
//                                            humidity = "${jsonObject.getDouble("humidity")}"
//                                            gasLevel = jsonObject.getInt("gas")
//                                            Log.d(TAG,"${temperature}")
//
//                                            // Kiểm tra gas vượt ngưỡng
//                                            if (gasLevel.toInt() > GAS_THRESHOLD) {
//                                                showDialog = true
//                                            }
//                                        }
//                                    } catch (e: Exception) {
//                                        e.printStackTrace()
//                                        temperature = "Error"
//                                        humidity = "Error"
//                                        gasLevel = 0
//                                    }
//                                    delay(5000) // Lặp lại mỗi 5 giây
//                                }
//                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .padding(start = 10.dp, end = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {},
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .padding(end = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(red = 180, green = 190, blue = 201),
                                        disabledContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = false
                                ) {
                                    Text(
                                        text = "Temperature:\n ${temperature} °C",
                                        color = Color.Black,
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(0.25f)
                                        .fillMaxHeight()
                                        .padding(0.dp)
                                        .background(
                                            color = Color.Transparent, // Nền màu trắng
                                            shape = RoundedCornerShape(8.dp) // Bo góc nếu cần
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = "GAS",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(bottom = 10.dp)
                                    )

                                    Button(
                                        onClick = { /* Hành động của bạn */ },
                                        modifier = Modifier
                                            .height(18.dp)
                                            .width(18.dp), // Chiều cao và chiều dài là 5x5 dp
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (gasLevel < GAS_THRESHOLD) Color.Green else Color.Red// Màu nền của nút
                                        ),
                                        shape = RoundedCornerShape(9.dp) // Độ bo tròn của nút
                                    ) {
                                    }
                                }


                                Button(
                                    onClick = {},
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(red = 180, green = 190, blue = 201),
                                        disabledContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = false
                                ) {
                                    Text(
                                        text = "Humidity:\n ${humidity} %",
                                        color = Color.Black,
                                        textAlign = TextAlign.Center,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    lightIsOn = !lightIsOn
                                    textOfLightButton = if (lightIsOn) "Tắt đèn " else "Bật đèn "
                                    database.child("1").setValue(if (lightIsOn) 1 else 0)
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



                            Button(
                                onClick = {
                                    fanIsOn = !fanIsOn
                                    textOfFanButton = if (fanIsOn) "Tắt quạt " else "Bật quạt "
                                    database.child("2").setValue(if (fanIsOn) 1 else 0)
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


                            Button(
                                onClick = {
                                    doorIsOpen = !doorIsOpen
                                    textOfDoorButton = if (doorIsOpen) "Đóng cửa" else "Mở cửa"
                                    database.child("3").setValue(if (doorIsOpen) 1 else 0)
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
                                    .height(90.dp)
                                    .padding(bottom = 10.dp),
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
                                                stopTimer()
                                                startRecording()
                                                CoroutineScope(Dispatchers.Main).launch{ //ham delay 5 giay
                                                    delay(3000)
                                                    micIsOn = false
                                                    textOfMicButton = "Mic"
                                                    micPainter = micOffPainter
                                                    stopRecording()
                                                    startTimer()
                                                }
                                            } else {
                                                micPainter = micOffPainter
                                                stopRecording()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .width(80.dp)
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

    fun uploadFile(filePath: String, callback: (ResponseData?) -> Unit) {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "Tệp không tồn tại hoặc trống: $filePath")
            return
        } else {
            Log.d(TAG,"tep ton tai va duoc gui di")
        }
        Thread {
            val url = URL("http://10.10.28.37:5000/upload")
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val file = File(filePath)
            var responseData: ResponseData? = null

            // Phát file âm thanh sau khi dừng ghi
//            mediaPlayer = MediaPlayer().apply {
//                setDataSource(filePath)
//                prepare()
//                setOnCompletionListener {
//                    release()
//                    mediaPlayer = null
//                }
//                start()
//            }

            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.doOutput = true

                val outputStream = connection.outputStream
                outputStream.bufferedWriter().use { writer ->
                    writer.write("--$boundary\r\n")
                    writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n")
                    writer.write("Content-Type: audio/mpeg\r\n\r\n")
                    writer.flush()

                    file.inputStream().use { fileInputStream ->
                        fileInputStream.copyTo(outputStream)
                    }

                    writer.write("\r\n--$boundary--\r\n")
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("HTTP_Response", "responeText: ${responseText}")
                    val jsonResponse = JSONObject(responseText)
                    Log.d("HTTP_Response", "jsonObject: ${jsonResponse}")
                    responseData = ResponseData(
                        bestMatch = jsonResponse.getString("best_match"),
                        recognizedText = jsonResponse.getString("recognized_text"),
                        score = jsonResponse.getInt("score")
                    )
                } else {
                    Log.e("TAG", "Error Response Code: $responseCode")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("TAG", "Exception: ${e.message}")
            }

            // Chuyển về luồng chính để trả kết quả qua callback
            Handler(Looper.getMainLooper()).post {
                callback(responseData)
            }

        }.start()
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
//        val storagePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        // Kiểm tra quyền chạy nền nếu chạy trên Android 10 trở lên
        val foregroundServicePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE)
        } else {
            PackageManager.PERMISSION_GRANTED // Không cần kiểm tra quyền này trên Android 9 trở xuống
        }

        return micPermission == PackageManager.PERMISSION_GRANTED &&
//                storagePermission == PackageManager.PERMISSION_GRANTED &&
                foregroundServicePermission == PackageManager.PERMISSION_GRANTED
    }


    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // Yêu cầu quyền chạy nền nếu chạy trên Android 10 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.FOREGROUND_SERVICE)
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
            val deniedPermissions = mutableListOf<String>()
            val grantedPermissions = mutableListOf<String>()

            // Kiểm tra từng quyền
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    grantedPermissions.add(permissions[i])
                } else {
                    deniedPermissions.add(permissions[i])
                }
            }

            // Xử lý kết quả
            if (deniedPermissions.isEmpty()) {
                Toast.makeText(this, "Tất cả quyền đã được cấp", Toast.LENGTH_SHORT).show()
                // Bắt đầu lắng nghe hoặc logic cần thiết
            } else {
                Toast.makeText(
                    this,
                    "Các quyền bị từ chối: ${deniedPermissions.joinToString()}",
                    Toast.LENGTH_LONG
                ).show()

                // Gợi ý người dùng mở cài đặt nếu cần quyền quan trọng
                if (deniedPermissions.contains(android.Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(
                        this,
                        "Ứng dụng cần quyền microphone để hoạt động",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    fun convertM4aToMp3(m4aFilePath: String, mp3FilePath: String, onComplete: (Boolean, String?) -> Unit) {
        val command = "-i $m4aFilePath -ar 44100 -ac 2 -b:a 192k -codec:a libmp3lame -qscale:a 2 $mp3FilePath"

        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                onComplete(true, mp3FilePath)
            } else {
                onComplete(false, session.failStackTrace?.toString())
            }
        }
    }


    private fun startRecording() {
        Log.d(TAG,"bắt đầu nghe lệnh")
        outputFile = File(getExternalFilesDir(null), "recorded_audio.mp4")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
//        Toast.makeText(this, "Bắt đầu ghi âm", Toast.LENGTH_SHORT).show()
        Log.d(TAG,"Bắt đầu ghi âm record")
    }

    private fun stopRecording() {
        Log.d(TAG,"in stop recording")
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null

        Toast.makeText(this, "Đã dừng ghi âm", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Đã dừng ghi âm và lưu tại ${outputFile.absolutePath}")

        uploadFile(outputFile.absolutePath.toString()) { response ->
            response?.let {
                Log.d(TAG,"Best Match: ${it.bestMatch}")
                handleBestMatch(it.bestMatch)
                Log.d(TAG,"Recognized Text: ${it.recognizedText}")
            } ?: Log.e(TAG,"Failed to get response")
        }

    }

    fun handleBestMatch(recordText: String) {
        Log.d(TAG, "in handle best match")
        if (recordText == "bật đèn") {
            sendRequest("/light/on")
            database.child("1").setValue(1)
            Log.d(TAG,"bat den")
            return
        }
        if (recordText == "tắt đèn") {
            sendRequest("/light/off")
            database.child("1").setValue(0)
            Log.d(TAG,"tat den")
            return
        }
        if (recordText == "bật quạt") {
            sendRequest("/fan/on")
            database.child("2").setValue(1)
            Log.d(TAG,"bat quat")
            return
        }
        if (recordText == "tắt quạt") {
            sendRequest("/fan/off")
            database.child("2").setValue(0)
            Log.d(TAG,"tat quat")
            return
        }
        if (recordText == "mở cửa") {
            sendRequest("/door/open")
            database.child("3").setValue(1)
            Log.d(TAG,"mo cua")
            return
        }
        if (recordText == "đóng cửa") {
            sendRequest("/door/close")
            database.child("3").setValue(0)
            Log.d(TAG,"dong cua")
            return
        }
    }

    private fun startListenHeyMisa() {
        outputFile = File(getExternalFilesDir(null), "recorded_audio.mp4")
        heymisaMediaRecord = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
//        Toast.makeText(this, "Bắt đầu nghe hey misa", Toast.LENGTH_SHORT).show()
        Log.d(TAG,"Bắt đầu ghi âm hey misa")
    }

    private fun checkHeyMisa(){
        try {
            heymisaMediaRecord?.apply {
                stop()
                release()
            }
            heymisaMediaRecord = null

            Log.d(TAG, "Đã dừng ghi âm và lưu tại ${outputFile.absolutePath}")

            uploadFile(outputFile.absolutePath.toString()) { response ->
                response?.let {
                    Log.d(TAG,"Best Match: ${it.bestMatch}")
                    handleHeyMisa(it.bestMatch)
                } ?: Log.e(TAG,"Failed to get response")
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "lỗi khi dừng ghi âm: ${e.message}")
        }
    }

    private fun handleHeyMisa(text: String) {
        Log.d(TAG,"in handle heymisa")
        if (text == "wake_word_detected") {
            stopTimer()
            playAudio(R.raw.toi_nghe_day)
            startRecording()
            CoroutineScope(Dispatchers.Main).launch{ //ham delay 5 giay
                delay(5000)
                Log.d(TAG,"nghe xong xu ly day")
                stopRecording()
            }
            CoroutineScope(Dispatchers.Main).launch{ //ham delay 5 giay
                delay(10000)
                startTimer()
            }
        }
    }

    private var isRecording = false // Biến cờ để kiểm soát trạng thái ghi âm
//    private lateinit var timer: Timer
//
//    private var isProcessing = false
//
//    private fun startTimer() {
//        timer = Timer()
//        timer.scheduleAtFixedRate(object : TimerTask() {
//            override fun run() {
//                if (isProcessing) return
//                isProcessing = true
//
//                runOnUiThread {
//                    try {
//                        if (isRecording) {
//                            Log.d(TAG, "check hey misa")
//                            checkHeyMisa()
//                        } else {
//                            startListenHeyMisa()
//                            Log.d(TAG, "bat dau nghe")
//                        }
//                        isRecording = !isRecording
//                    } finally {
//                        isProcessing = false
//                    }
//                }f
//            }
//        }, 0, 3000) // Chạy mỗi 3 giây
//    }
//
//
//    private fun stopTimer() {
//        timer.cancel()
//        isRecording = false
//    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    private fun startTimer() {
        runnable = Runnable {
            // Thực hiện tác vụ
            if (isRecording) {
                checkHeyMisa()
            } else {
                startListenHeyMisa()
            }
            isRecording = !isRecording

            // Lặp lại sau 3 giây
            handler.postDelayed(runnable, 5000)
        }
        handler.post(runnable)
    }

    private fun stopTimer() {
        Log.d(TAG, "stop timer")
        handler.removeCallbacks(runnable)
    }

    private fun playAudio(resourceId: Int) {
        // Giải phóng MediaPlayer nếu đang sử dụng
        mediaPlayer?.release()
        mediaPlayer = null

        try {
            // Khởi tạo MediaPlayer với file từ res/raw
            mediaPlayer = MediaPlayer.create(this, resourceId)
            mediaPlayer?.apply {
                setOnCompletionListener {
                    release() // Giải phóng MediaPlayer sau khi phát xong
                    mediaPlayer = null
                    Log.d("Audio", "Âm thanh phát xong")
                }
                start() // Bắt đầu phát
            }
        } catch (e: Exception) {
            Log.e("Audio", "Lỗi khi phát âm thanh: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        heymisaMediaRecord?.release()
        heymisaMediaRecord = null
        hhandler.removeCallbacksAndMessages(null)
        handler.removeCallbacksAndMessages(null)
    }
}


//@Composable
//fun SensorDataScreen() {
//    // Gọi API liên tục để lấy dữ liệu mỗi 5 giây
//    LaunchedEffect(Unit) {
//        while (true) {
//            isLoading = true
//            try {
//                // Gọi API để lấy dữ liệu từ ESP8266
//                val data = RetrofitInstance.api.getSensorData()
//                temperature = data.temperature.toString()
//                humidity = data.humidity.toString()
//                gasLevel = data.gas.toString()
//
//                // Nếu mức gas vượt quá ngưỡng, hiển thị cảnh báo
//                if (gasLevel.toInt() > GAS_THRESHOLD) {
//                    showDialog = true
//                }
//            } catch (e: Exception) {
//                temperature = "Error"
//                humidity = "Error"
//                gasLevel = "Error"
//            } finally {
//                isLoading = false
//            }
//
//            delay(5000)  // Đợi 5 giây trước khi gọi lại API
//        }
//    }
//
//    if (showDialog) {
//        AlertDialog(
//            onDismissRequest = { showDialog = false },
//            title = { Text("Gas Leak Alert") },
//            text = { Text("Warning: High Gas Level detected!") },
//            confirmButton = {
//                TextButton(onClick = { showDialog = false }) {
//                    Text("OK")
//                }
//            }
//        )
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text(text = "Temperature: $temperature °C", style = MaterialTheme.typography.bodySmall)
//        Text(text = "Humidity: $humidity %", style = MaterialTheme.typography.bodySmall)
//        Text(text = "Gas Level: $gasLevel", style = MaterialTheme.typography.bodySmall)
//    }
//}