package com.example.emptyviewsapplication

import android.Manifest
import android.content.ContentValues
import com.chaquo.python.Python
import android.content.pm.PackageManager
import com.chaquo.python.android.AndroidPlatform
import androidx.appcompat.app.AlertDialog
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.emptyviewsapplication.databinding.ActivityDrowsinessBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DrowsinessActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityDrowsinessBinding

    private lateinit var cameraExecutor: ExecutorService
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var timer: CountDownTimer? = null
    private var isRecording = false
    private var mediaPlayer: MediaPlayer? = null
    private var alarmStopHandler: Handler? = null

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }

    override fun onPause() {
        super.onPause()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityDrowsinessBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        val backButton: Button = findViewById(R.id.backBtn)

        backButton.setOnClickListener {
            onBackPressed()
        }

        viewBinding.detectBtn.setOnClickListener {
            if (!isRecording) {
                // Start recording
                startRecording()
            } else {
                // Stop recording
                stopRecording()
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startRecording() {
        viewBinding.detectBtn.setText(R.string.stop_detection)
        isRecording = true

        Toast.makeText(
            this@DrowsinessActivity,
            "Video recording started",
            Toast.LENGTH_SHORT
        ).show()

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture?.output
            ?.prepareRecording(this, mediaStoreOutputOptions)
            ?.apply {
                if (PermissionChecker.checkSelfPermission(
                        this@DrowsinessActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) ==
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            ?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {

                        if (detectDrowsiness()) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@DrowsinessActivity,
                                    "Alarm!!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            playAlarm()
                        }

                        // Recording started
                    }

                    is VideoRecordEvent.Finalize -> {
                        // Recording finished
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            Log.d(TAG, msg)

                            val path = convertMediaUriToPath(recordEvent.outputResults.outputUri)
                            Log.d(TAG, "The path is $path")



                        } else {
                            recording?.close()
                            recording = null
                            Log.e(
                                TAG, "Video capture ends with error: " +
                                        "${recordEvent.error}"
                            )
                        }

                        // Update button text after recording
                        runOnUiThread {
                            viewBinding.detectBtn.setText(R.string.detect_drowsiness)
                        }

                        Toast.makeText(
                            this@DrowsinessActivity,
                            "Video recording stopped",
                            Toast.LENGTH_SHORT
                        ).show()

                        isRecording = false
                    }
                }
            }
    }

    private fun detectDrowsiness(): Boolean {
        try {
            Python.start(AndroidPlatform(this))

            val python = Python.getInstance()
            val pythonModule = python.getModule("drowsiness")

            val consecutiveDrowsyCount = 2  // Number of consecutive drowsy detections required

            var drowsyCount = 0
            var notDrowsyCount = 0

            while (drowsyCount < consecutiveDrowsyCount) {
                val result = pythonModule.callAttr("detect_drowsiness")

                if (result.toString() == "Drowsy") {
                    runOnUiThread {
                        Toast.makeText(
                            this@DrowsinessActivity,
                            " Current state: " + result.toString() + "\nConsecutive Drowsy count: " + (drowsyCount+1),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    drowsyCount++
                    notDrowsyCount = 0
                } else {
                    notDrowsyCount++
                    drowsyCount = 0
                }

                Thread.sleep(1000)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun playAlarm() {
        // Play the alarm_sound sound
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound)
        mediaPlayer?.start()

        // Show an AlertDialog with a message and a stop button
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Attention")
            .setMessage("Drowsiness Detected")
            .setPositiveButton("Stop Alarm") { _, _ ->
                // User clicked the Stop Alarm button
                stopAlarm()
            }
            .setCancelable(false) // Prevent dismissing the dialog by clicking outside of it
            .show()

        // Schedule a delayed task to dismiss the dialog after 10 seconds
        alarmStopHandler = Handler(Looper.getMainLooper())
        alarmStopHandler?.postDelayed({
            alertDialog.dismiss()
            stopAlarm()
        }, 10000)
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        alarmStopHandler = null
    }

    private fun stopRecording() {
        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )

                if (camera.cameraInfo.hasFlashUnit()) {
                    camera.cameraControl.enableTorch(true)
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startCamera()
            }
        }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private fun convertMediaUriToPath(uri: Uri?): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri!!, proj, null, null, null)
        val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val path = cursor.getString(columnIndex)
        cursor.close()
        return path
    }
}