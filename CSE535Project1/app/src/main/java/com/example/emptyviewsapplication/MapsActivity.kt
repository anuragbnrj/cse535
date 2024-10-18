package com.example.emptyviewsapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.emptyviewsapplication.databinding.ActivityMapsBinding
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MapsActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMapsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.buttonCallMapsApi.setOnClickListener {
            var srcLat = viewBinding.srcLat.text.toString()
            if (srcLat == "") {
                srcLat = "37.7749"
            }
            var srcLon = viewBinding.srcLon.text.toString()
            if (srcLon == "") {
                srcLon = "-122.4194"
            }
            var src = "$srcLat%2C$srcLon"

            var desLat = viewBinding.desLat.text.toString()
            if (desLat == "") {
                desLat = "34.0522"
            }
            var desLon = viewBinding.desLon.text.toString()
            if (desLon == "") {
                desLon = "-118.2437"
            }
            var des = "$desLat%2C$desLon"

            var depTime = "now"
            var apiKey = "AIzaSyCSQTtgoqowfdjzDBY5_gfeUzn8ZsL8p_U"

            val client = OkHttpClient()
            val url =
                "https://maps.googleapis.com/maps/api/distancematrix/json?origins=$src&destinations=$des&departure_time=$depTime&key=$apiKey"
            val request = Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle error
                    Log.e("ERROR", e.toString())
                    viewBinding.textViewShowAPIResults.text = "Could not process request"
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()

                        val gson = Gson()
                        val distanceMatrix =
                            gson.fromJson(responseBody, DistanceMatrixResponse::class.java)

                        val distance = distanceMatrix.rows[0].elements[0].distance.value
                        val duration = distanceMatrix.rows[0].elements[0].duration.value
                        val durationInTraffic =
                            distanceMatrix.rows[0].elements[0].duration_in_traffic.value

                        val speed = (distance / duration) * 3.6
                        val speedInTraffic = (distance / durationInTraffic) * 3.6

                        Log.d("MapsActivity", "Distance is $distance Duration is $duration")

                        viewBinding.textViewShowAPIResults.text =
                            "Average speed is $speed kmph and Average speed in current traffic is $speedInTraffic kmph"

//                    Toast.makeText(
//                        this@MapsActivity,
//                        "Distance is $distance Duration is $duration",
//                        Toast.LENGTH_LONG
//                    ).show()

                    } else {
                        // Handle non-successful response
                        Log.e("ERROR", "Request Unsuccessful")

                        viewBinding.textViewShowAPIResults.text = "Could not process request"
                    }
                }
            })


        }


    }


    data class DistanceMatrixResponse(
        val rows: List<Row>
    )

    data class Row(
        val elements: List<Element>
    )

    data class Element(
        val distance: Distance,
        val duration: Duration,
        val duration_in_traffic: Duration
    )

    data class Distance(
        val text: String,
        val value: Int
    )

    data class Duration(
        val text: String,
        val value: Int
    )
}