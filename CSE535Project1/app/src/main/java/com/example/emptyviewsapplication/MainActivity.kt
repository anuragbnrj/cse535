package com.example.emptyviewsapplication

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.emptyviewsapplication.data.entity.SymptomsOfUser
import com.example.emptyviewsapplication.databinding.ActivityMainBinding
import com.example.emptyviewsapplication.viewmodel.SymptomsOfUserViewModel
import com.example.emptyviewsapplication.viewmodel.SymptomsOfUserViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var viewBinding: ActivityMainBinding

    // Respiratory Rate required variables
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var handler: Handler

    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    private var isMeasuring = false
    private var elapsedTime = 0

    // Lists to collect accelerometer readings
    private val accX = ArrayList<Float>()
    private val accY = ArrayList<Float>()
    private val accZ = ArrayList<Float>()

    private val collectionIntervalRespRate = 100 // milliseconds (10 readings per second)
    private val collectionDurationRespRate = 45000 // milliseconds (45 seconds)


    // Heart Rate required variables
    private lateinit var cameraExecutor: ExecutorService

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var timer: CountDownTimer? = null
    private val recordingDurationMillisHeartRate = 45 * 1000L  // 45 seconds

    private var heartRate: Int? = null
    private var respiratoryRate: Int? = null



    private val newSymptomsOfUserActivityRequestCode = 1
    private val symptomsOfUserViewModel: SymptomsOfUserViewModel by viewModels {
        SymptomsOfUserViewModelFactory((application as SymptomsOfUserApplication).repository)
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
    }


    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

//            Log.d("onSensorChanged", "Sensor values collected. X: $x\tY: $y\tZ: $z")
            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used in this example
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }


        viewBinding.buttonMeasureRespRate.setOnClickListener {
            if (!isMeasuring) {
                // Start collecting readings
                startCollectingReadings()
            } else {
                stopCollectingReadings()
            }
        }

        viewBinding.buttonMeasureHeartRate.setOnClickListener {
            timer = object : CountDownTimer(recordingDurationMillisHeartRate, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    // Do something on each tick (optional)
                    val secondsRemaining = millisUntilFinished / 1000
                    viewBinding.buttonMeasureHeartRate.text =
                        "Please Wait $secondsRemaining seconds"
                }

                @RequiresApi(Build.VERSION_CODES.P)
                override fun onFinish() {
                    // Enable the button after 45 seconds
//                viewBinding.videoCaptureButton.isEnabled = true
                    captureVideo()
                }
            }

            (timer as CountDownTimer).start()
            captureVideo()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        viewBinding.buttonNavigateToSymptomLoggingPage.setOnClickListener {
            val intent = Intent(this, SymptomsActivity::class.java)
            intent.putExtra("RESPIRATORY_RATE", respiratoryRate.toString())
            intent.putExtra("HEART_RATE", heartRate.toString())
            startActivityForResult(intent, newSymptomsOfUserActivityRequestCode)
//            resultLauncher.launch(intent)
        }

        viewBinding.buttonNavigateToMapsPage.setOnClickListener{
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }



        val recyclerView = viewBinding.recyclerview
        val adapter = SymptomsOfUserListAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intentData: Intent?) {
        super.onActivityResult(requestCode, resultCode, intentData)

        if (requestCode == newSymptomsOfUserActivityRequestCode && resultCode == Activity.RESULT_OK) {
            val symptomsOfUser = intentData?.getSerializableExtra(SymptomsActivity.EXTRA_REPLY) as? SymptomsOfUser
            if (symptomsOfUser != null) {
                symptomsOfUserViewModel.upsert(symptomsOfUser)
            }


        } else {
            Toast.makeText(
                applicationContext,
                R.string.empty_not_saved,
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }


            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)


            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture //imageCapture, imageAnalyzer
                )

                if (camera.cameraInfo.hasFlashUnit()) {
                    camera.cameraControl.enableTorch(true)
                }

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    // Implements VideoCapture use case, including start and stop capturing.
    @RequiresApi(Build.VERSION_CODES.P)
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        viewBinding.buttonMeasureHeartRate.isEnabled = false

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


        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) ==
                    PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
//                        viewBinding.videoCaptureButton.apply {
//                            text = getString(R.string.stop_capture)
//                            isEnabled = true
//                        }
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
//                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
//                                .show()
                            Log.d(TAG, msg)

                            val path = convertMediaUriToPath(recordEvent.outputResults.outputUri)
//                            Toast.makeText(baseContext, path, Toast.LENGTH_SHORT)
//                                .show()
                            Log.d(TAG, "The path is $path")

                            val res = getHeartRate(path)
//                            Toast.makeText(baseContext, res, Toast.LENGTH_SHORT)
//                                .show()

                            heartRate = res.toInt()

                            viewBinding.textViewHeartRate.text =
                                getString(R.string.heart_rate_display_text) + " " +
                                        res + " " +
                                        getString(R.string.bpm)

                        } else {
                            recording?.close()
                            recording = null
                            Log.e(
                                TAG, "Video capture ends with error: " +
                                        "${recordEvent.error}"
                            )
                        }

                        viewBinding.buttonMeasureHeartRate.apply {
                            text = getString(R.string.start_capture)
                            isEnabled = true
                        }
                    }
                }
            }

    }

    private fun startCollectingReadings() {
        viewBinding.textViewRespRate.text =
            "Please Wait for ${collectionDurationRespRate / 1000} seconds! Collecting your reading!"
        viewBinding.buttonMeasureRespRate.text = "Stop"

        isMeasuring = true
        accX.clear()
        accY.clear()
        accZ.clear()

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        handler = Handler(Looper.getMainLooper())


        handler.postDelayed(collectReadingsRunnable, collectionIntervalRespRate.toLong())

        handler.postDelayed({
            stopCollectingReadings()
        }, collectionDurationRespRate.toLong())
    }

    private fun stopCollectingReadings() {
        isMeasuring = false
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(collectReadingsRunnable)

        Log.d("stopCollectingReadings", "Time Elapsed: $elapsedTime")

        // Calculate the respiratory rate from the collected accelerometer data (basic approximation)
        var res = getRespRate()
        respiratoryRate = res

        viewBinding.textViewRespRate.text = "Respiratory Rate: $res"
        viewBinding.buttonMeasureRespRate.text = "Measure Respiratory Rate"
    }

    private val collectReadingsRunnable = object : Runnable {
        override fun run() {
            accX.add(lastX)
            accY.add(lastY)
            accZ.add(lastZ)

            elapsedTime += 100

            // Continue measuring
            if (isMeasuring) {
                handler.postDelayed(this, 100)
            }
        }
    }


    private fun getRespRate(): Int {
        var previousValue = 10f
        var currentValue: Float
        var k = 0

//        Log.d("getRespRate", "X size: ${accX.size} Y size: ${accY.size} Z size: ${accZ.size}")

        for (i in 11..<accX.size) {
            currentValue =
                kotlin.math.sqrt(
                    ((accX[i] * accX[i]) + (accY[i] * accY[i]) + (accZ[i] * accZ[i])).toDouble()
                ).toFloat()
            if (abs(x = previousValue - currentValue) > 0.15) {
                k++
            }
            previousValue = currentValue
        }
        val ret = (k / 45.00)

        // Return the approximate respiratory rate
        return (ret * 30).toInt()
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
            // Handle Permission granted/rejected
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

    // Provided Functions
    private fun convertMediaUriToPath(uri: Uri?): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri!!, proj, null, null, null)
        val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        val path = cursor.getString(columnIndex)
        cursor.close()
        return path
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getHeartRate(vararg params: String?): String {
        var m_bitmap: Bitmap? = null
        val retriever = MediaMetadataRetriever()
        val frameList = ArrayList<Bitmap>()
        try {
            retriever.setDataSource(params[0])
            val duration =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
            val aduration = duration!!.toInt()
            var i = 10
            while (i < aduration) {
                val bitmap = retriever.getFrameAtIndex(i)
                frameList.add(bitmap!!)
                i += 50
            }
        } catch (ex: Exception) {
            Log.d("doInBackground", "Exception is: ${ex.message}")
        } finally {
            retriever.release()
            var redBucket: Long = 0
            var pixelCount: Long = 0
            val a = mutableListOf<Long>()
            for (i in frameList) {
                redBucket = 0
                for (y in 550 until 650) {
                    for (x in 550 until 650) {
                        val c: Int = i.getPixel(x, y)
                        pixelCount++
                        redBucket += Color.red(c) + Color.blue(c) + Color.green(c)
                    }
                }
                a.add(redBucket)
            }

            val b = mutableListOf<Long>()
            for (i in 0 until a.lastIndex - 5) {
                val temp =
                    (a.elementAt(i) + a.elementAt(i + 1) + a.elementAt(i + 2) + a.elementAt(
                        i + 3
                    ) + a.elementAt(
                        i + 4
                    )) / 4
                b.add(temp)
            }

            var x = b.elementAt(0)
            var count = 0
            for (i in 1 until b.lastIndex) {
                val p = b.elementAt(i.toInt())
                if (abs(p - x) > 3500) {
                    count += 1
                }
                x = b.elementAt(i.toInt())
            }
            val rate = ((count.toFloat() / 45) * 60).toInt()
            return (rate * 5).toString()

        }
    }

}
