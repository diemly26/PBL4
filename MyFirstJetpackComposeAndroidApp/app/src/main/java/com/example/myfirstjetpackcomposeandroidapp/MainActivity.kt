package com.example.myfirstjetpackcomposeandroidapp

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.content.pm.PackageManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.myfirstjetpackcomposeandroidapp.ui.theme.MyFirstJetpackComposeAndroidAppTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


data class ResponseData(
    val bestMatch: String,
    val recognizedText: String,
    val score: Int
)

var ESP8266_URL = "http://10.10.26.4"
private const val REQUEST_MIC_PERMISSION = 200
private const val REQUEST_STORAGE_PERMISSION = 300
private lateinit var porcupineManager: PorcupineManager

var wakeWordCallback =
    PorcupineManagerCallback { keywordIndex ->
        if (keywordIndex == 0) {
            // Xử lý khi từ khóa "porcupine" được phát hiện
            println("Porcupine keyword detected!")
        } else if (keywordIndex == 1) {
            // Xử lý khi từ khóa "bumblebee" được phát hiện
            println("Bumblebee keyword detected!")
        }
    }

class MainActivity : ComponentActivity() {

//    private val ESP8266_URL = "http://10.10.27.246"
    val database = FirebaseDatabase.getInstance().reference.child("keys")
    private val TAG: String = "HTTP_Response"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var outputFile: File
    private lateinit var outputAudioFile: File



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey("YOUR_ACCESS_KEY") // Thay YOUR_ACCESS_KEY bằng Access Key của bạn
                .setKeywords(
                    arrayOf(
                        Porcupine.BuiltInKeyword.PORCUPINE,
                        Porcupine.BuiltInKeyword.BUMBLEBEE
                    )
                )
                .build(applicationContext, wakeWordCallback)

            porcupineManager.start() // Bắt đầu lắng nghe từ khóa
            Log.d("PorcupineManager", "PorcupineManager started successfully.")
        } catch (e: PorcupineException) {
            e.printStackTrace()
            Log.e("PorcupineManager", "Failed to initialize PorcupineManager: ${e.message}")
        }

        if (!checkMicrophonePermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                REQUEST_MIC_PERMISSION
            )
        }

        var wakeWordCallback =
            PorcupineManagerCallback { keywordIndex ->
                when (keywordIndex) {
                    0 -> {
                        Log.d("WakeWord", "Porcupine detected!")
                        sendRequest("/light/on")
                        database.child("1").setValue(1)
                    }
                    1 -> {
                        Log.d("WakeWord", "Bumblebee detected!")
                        sendRequest("/fan/on")
                        database.child("2").setValue(1)
                    }
                    else -> Log.d("WakeWord", "Unknown keyword detected!")
                }
            }


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

    fun uploadFile(filePath: String, callback: (ResponseData?) -> Unit) {
        Thread {
            val url = URL("http://10.10.26.88:5000/upload")
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
                    Log.d("TAG", "responeText: ${responseText}")
                    val jsonResponse = JSONObject(responseText)
                    Log.d("TAG", "jsonObject: ${jsonResponse}")
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
    // Thêm mã request code để nhận diện yêu cầu
    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 100
    }

    private fun checkPermissions(): Boolean {
        val micPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
        val storagePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        val readImagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        val readVideoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        val readAudioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        Log.d("PermissionCheck", "Mic permission: ${micPermission == PackageManager.PERMISSION_GRANTED}")
        Log.d("PermissionCheck", "Storage permission: ${storagePermission == PackageManager.PERMISSION_GRANTED}")
        Log.d("PermissionCheck", "Read Images permission: ${readImagesPermission == PackageManager.PERMISSION_GRANTED}")
        Log.d("PermissionCheck", "Read Video permission: ${readVideoPermission == PackageManager.PERMISSION_GRANTED}")
        Log.d("PermissionCheck", "Read Audio permission: ${readAudioPermission == PackageManager.PERMISSION_GRANTED}")

        return micPermission == PackageManager.PERMISSION_GRANTED &&
                storagePermission == PackageManager.PERMISSION_GRANTED &&
                readImagesPermission == PackageManager.PERMISSION_GRANTED &&
                readVideoPermission == PackageManager.PERMISSION_GRANTED &&
                readAudioPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(android.Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_PERMISSIONS_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            permissions.forEachIndexed { index, permission ->
                val granted = grantResults[index] == PackageManager.PERMISSION_GRANTED
                Log.d("PermissionResult", "Permission: $permission, Granted: $granted")
            }

            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Tất cả quyền đã được cấp
                Toast.makeText(this, "Tất cả quyền đã được cấp", Toast.LENGTH_SHORT).show()
                Log.d("PermissionResult", "All permissions granted")

                // Khởi tạo PorcupineManager nếu quyền RECORD_AUDIO được cấp
                try {
                    porcupineManager = PorcupineManager.Builder()
                        .setAccessKey("5S5l2+By8Ezeb8LTXbPr4W6ncpbhkUI4faR+Af/k70PDzoW+WV8iPA==") // Thay YOUR_ACCESS_KEY bằng khóa của bạn
                        .setKeywords(
                            arrayOf(
                                Porcupine.BuiltInKeyword.PORCUPINE,
                                Porcupine.BuiltInKeyword.BUMBLEBEE
                            )
                        )
                        .build(applicationContext, wakeWordCallback)

                    porcupineManager.start() // Bắt đầu lắng nghe từ khóa
                    Log.d("PorcupineManager", "PorcupineManager started successfully.")
                } catch (e: PorcupineException) {
                    e.printStackTrace()
                    Log.e("PorcupineManager", "Failed to initialize PorcupineManager: ${e.message}")
                }

            } else {
                // Một số quyền bị từ chối
                if (permissions.contains(android.Manifest.permission.RECORD_AUDIO) &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)
                ) {
                    Toast.makeText(this, "Không đủ quyền truy cập", Toast.LENGTH_SHORT).show()
                    Log.d("PermissionResult", "Permissions not fully granted")
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

        // Đặt đường dẫn file MP3
        outputAudioFile = File(getExternalFilesDir(null), "recorded_audio.mp3")

        // Chuyển đổi từ M4A sang MP3
        convertM4aToMp3(outputFile.absolutePath, outputAudioFile.absolutePath) { success, result ->
            if (success) {
                Log.d(TAG, "Chuyển đổi thành công! File MP3: $result")
            } else {
                Log.e(TAG, "Lỗi chuyển đổi: $result")
            }
        }
// Phát file âm thanh sau khi dừng ghi
//        mediaPlayer = MediaPlayer().apply {
//            setDataSource(outputFile.absolutePath)
//            prepare()
//            setOnCompletionListener {
//                release()
//                mediaPlayer = null
//            }
//            start()
//        }
// Phát file âm thanh sau khi dừng ghi
//        mediaPlayer = MediaPlayer().apply {
//            setDataSource(outputAudioFile.absolutePath)
//            prepare()
//            setOnCompletionListener {
//                release()
//                mediaPlayer = null
//            }
//            start()
//        }
        uploadFile(outputFile.absolutePath.toString()) { response ->
            response?.let {
                Log.d(TAG,"Best Match: ${it.bestMatch}")
                handleBestMatch(it.bestMatch)
                Log.d(TAG,"Recognized Text: ${it.recognizedText}")
                Log.d(TAG,"Score: ${it.score}")
            } ?: Log.d(TAG,"Failed to get response")
        }
    }
    fun handleBestMatch(recordText: String) {
        if (recordText == "bật đèn") {
            sendRequest("/light/on")
            database.child("1").setValue(1)
            Log.d("TAG","bat den")
            return
        }
        if (recordText == "tắt đèn") {
            sendRequest("/light/off")
            database.child("1").setValue(0)
            Log.d("TAG","tat den")
            return
        }
        if (recordText == "bật quạt") {
            sendRequest("/fan/on")
            database.child("2").setValue(1)
            Log.d("TAG","bat quat")
            return
        }
        if (recordText == "tắt quạt") {
            sendRequest("/fan/off")
            database.child("2").setValue(0)
            Log.d("TAG","tat quat")
            return
        }
        if (recordText == "mở cửa") {
            sendRequest("/door/open")
            database.child("3").setValue(1)
            Log.d("TAG","mo cua")
            return
        }
        if (recordText == "đóng cửa") {
            sendRequest("/door/close")
            database.child("3").setValue(0)
            Log.d("TAG","dong cua")
            return
        }
    }
    private fun convertToMp3(inputPath: String) {
    }
    private fun checkMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }



    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        try {
            if (::porcupineManager.isInitialized) {
                porcupineManager.stop()
                porcupineManager.delete()
                Log.d("PorcupineManager", "PorcupineManager stopped and deleted successfully.")
            }
        } catch (e: PorcupineException) {
            e.printStackTrace()
            Log.e("PorcupineManager", "Error stopping or deleting PorcupineManager: ${e.message}")
        }
    }

}