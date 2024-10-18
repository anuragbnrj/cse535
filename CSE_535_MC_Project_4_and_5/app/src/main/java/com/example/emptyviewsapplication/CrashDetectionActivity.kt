package com.example.emptyviewsapplication

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class CrashDetectionActivity : AppCompatActivity(), LocationListener, SensorEventListener {
    private var locationManager: LocationManager? = null
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var prevSpeed = 0.0
    private var acceleration = 0.0
    private val accThreshold = 50.0
    private val speedThreshold = 45.0
    private val probThreshold = 0.70
    private var prevUpdateTs = 0L
    val fuzzySystem = FuzzyInferenceSystem()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_detection)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        pollSpeed()

    }

    override fun onLocationChanged(location: Location) {

        val speed = 49 * 1.0

        // convert speed to km/hr from m/s
        //val speed = location.speed * 3.6
        if (speed >= speedThreshold){
            // stop updates for the time being
            locationManager?.removeUpdates(this)

            if (acceleration > accThreshold){
                if(isCrash(acceleration, speed)) {
                    showAlert()
                }
            }
            pollSpeed()
        }
        prevSpeed = speed

    }

    private fun isCrash(acc: Double, speed: Double): Boolean {
        val prob = fuzzySystem.calculateCrashProbability(speed, acc)
        Toast.makeText(this, " crash prob: (Speed: $speed , Acc: $acc) : $prob", Toast.LENGTH_SHORT).show()

        return prob > probThreshold
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
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

            val curAcceleration = kotlin.math.sqrt(x * x + y * y + z * z).toDouble()
            if (curAcceleration > kotlin.math.max(accThreshold, acceleration) || (System.currentTimeMillis() - prevUpdateTs > 5000)) {
                prevUpdateTs = System.currentTimeMillis()
                acceleration = curAcceleration
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this example
    }

    private fun pollSpeed(){
        prevSpeed = 0.0
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            // Request location updates every 5 s
            locationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, 0f, this
            )
        }
    }

    private fun showAlert(){

        var isAlertCanceled = false
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Crash Detected!!!")
        alertDialogBuilder.setMessage("Calling 911 in 30 seconds")


        alertDialogBuilder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            isAlertCanceled = true
            dialog.dismiss()
        }

        // Create and show the alert dialog
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

        Handler().postDelayed({
            if (!isAlertCanceled) {
                // Perform the action to make the emergency call here
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:6025155922"))
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(intent)
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf<String>(Manifest.permission.CALL_PHONE),
                        1
                    )
                    startActivity(intent)
                }
            }
        }, 30000) // 30 s
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
