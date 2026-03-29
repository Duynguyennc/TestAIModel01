package com.example.testaimodel01

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    lateinit var interpreter: Interpreter
    lateinit var labels: List<String>
    val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val imageView = findViewById<ImageView>(R.id.imgPreview)
                imageView.setImageURI(uri)


                loadModel(this.applicationContext)
                labels = loadLabels(this)

                // val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)


                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

                val input = convertBitmapToByteBuffer(resizedBitmap)
                val output = Array(1) { ByteArray(1001) }
                interpreter.run(input, output)


                val result = output[0].withIndex().maxByOrNull { it.value.toInt() and 0xFF }
                val label = labels[result!!.index]

                // Tỉ lệ này in ra để dễ test với debug hơn. Đến lúc demo cho thầy xem thì có thể xóa
                val probabilityValue = result.value.toInt() and 0xFF // Giá trị từ 0-255
                val percent = (probabilityValue / 255.0f) * 100      // Đổi ra phần trăm (%)
                val percentFormatted = String.format("%.2f", percent)

                var textResult = findViewById<TextView>(R.id.tvResult)
                textResult.setText("Kết quả: $label\nĐộ tin cậy: $percentFormatted%")
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val imageView = findViewById<ImageView>(R.id.imgPreview)
                imageView.setImageBitmap(bitmap)

                loadModel(this.applicationContext)
                labels = loadLabels(this)

                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

                val input = convertBitmapToByteBuffer(resizedBitmap)
                val output = Array(1) { ByteArray(1001) }
                interpreter.run(input, output)


                val result = output[0].withIndex().maxByOrNull { it.value.toInt() and 0xFF }
                val label = labels[result!!.index]

                // Tỉ lệ này in ra để dễ test với debug hơn. Đến lúc demo cho thầy xem thì có thể xóa
                val probabilityValue = result.value.toInt() and 0xFF // Giá trị từ 0-255
                val percent = (probabilityValue / 255.0f) * 100      // Đổi ra phần trăm (%)
                val percentFormatted = String.format("%.2f", percent)

                var textResult = findViewById<TextView>(R.id.tvResult)
                textResult.setText("Kết quả: $label\nĐộ tin cậy: $percentFormatted%")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btn_select = findViewById<Button>(R.id.btnSelectImage)
        btn_select.setOnClickListener {
            SelectPhoto()
        }

        val btn_takePhoto = findViewById<Button>(R.id.btnTakePhoto)
        btn_takePhoto.setOnClickListener {
            TakePhoto()
        }

    }

    fun loadModel(context: Context) {
        val fileDescriptor = context.assets.openFd("model.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        val modelBuffer = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )

        interpreter = Interpreter(modelBuffer)
    }

    fun loadLabels(context: Context): List<String> {
        return context.assets.open("labels.txt")
            .bufferedReader()
            .readLines()
    }

    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(224 * 224 * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(224 * 224)
        bitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224)

        for (pixel in pixels) {
            // Lấy giá trị màu thô (0-255) và ép kiểu sang Byte, dùng hàm .put() thay vì .putFloat()
            buffer.put((pixel shr 16 and 0xFF).toByte())
            buffer.put((pixel shr 8 and 0xFF).toByte())
            buffer.put((pixel and 0xFF).toByte())
        }

        return buffer
    }

    fun SelectPhoto() {
        pickImageLauncher.launch("image/*")
    }

    fun TakePhoto() {
        cameraLauncher.launch(null)
    }

    fun UpdateUI() {

    }
}
