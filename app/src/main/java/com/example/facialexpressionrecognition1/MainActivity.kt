package com.example.facialexpressionrecognition1

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var faceDetector: FaceDetector
    private lateinit var emotionTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewFinder = findViewById(R.id.viewFinder)
        emotionTextView = findViewById(R.id.emotionTextView)

        faceDetector = FaceDetector.Builder(applicationContext)
            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
            .build()

        if (!faceDetector.isOperational) {
            Log.e(TAG, "Face detector dependencies are not yet available.")
            return
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
                        processImageProxy(imageProxy)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(markerClass = [ExperimentalGetImage::class])
    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return
        val bitmap = mediaImage.toBitmap()

        if (bitmap != null) {
            val frame = Frame.Builder().setBitmap(bitmap).build()
            val faces = faceDetector.detect(frame)

            for (i in 0 until faces.size()) {
                val face = faces.valueAt(i)
                val emotion = detectEmotion(face)

                // Actualizar el TextView con la emoción detectada
                runOnUiThread {
                    emotionTextView.text = emotion
                }

                Log.d(TAG, "Face detected: $emotion")
            }
        } else {
            Log.e(TAG, "Bitmap conversion failed")
        }
        imageProxy.close()
    }

    private fun detectEmotion(face: Face): String {
        val smileProbability = face.isSmilingProbability
        val leftEyeOpenProbability = face.isLeftEyeOpenProbability
        val rightEyeOpenProbability = face.isRightEyeOpenProbability

        Log.d(TAG, "Smile: $smileProbability, Left Eye: $leftEyeOpenProbability, Right Eye: $rightEyeOpenProbability")

        return when {
            smileProbability > 0.5 -> "Feliz"
            smileProbability < 0.5 -> "Triste"
            leftEyeOpenProbability < 0.5 && rightEyeOpenProbability < 0.5 -> "Sleepy"
            else -> "Neutral"
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    // Función de extensión para convertir Image a Bitmap
    private fun Image.toBitmap(): Bitmap? {
        val nv21: ByteArray
        val yuvImage: YuvImage
        val outputStream = ByteArrayOutputStream()

        return try {
            val yPlane = planes[0].buffer
            val uPlane = planes[1].buffer
            val vPlane = planes[2].buffer

            val ySize = yPlane.remaining()
            val uSize = uPlane.remaining()
            val vSize = vPlane.remaining()

            nv21 = ByteArray(ySize + uSize + vSize)

            yPlane.get(nv21, 0, ySize)
            vPlane.get(nv21, ySize, vSize)
            uPlane.get(nv21, ySize + vSize, uSize)

            val imageWidth = width
            val imageHeight = height

            yuvImage = YuvImage(nv21, ImageFormat.NV21, imageWidth, imageHeight, null)
            yuvImage.compressToJpeg(Rect(0, 0, imageWidth, imageHeight), 100, outputStream)
            val bytes = outputStream.toByteArray()

            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
