package com.example.emptyviewsapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.emptyviewsapplication.data.dto.DataClasses
import com.example.emptyviewsapplication.data.dto.DataClasses.DistanceMatrixResponse
import com.example.emptyviewsapplication.data.dto.DataClasses.PlacesResponse
import com.example.emptyviewsapplication.databinding.ActivityGasStationBinding
import com.fuzzylite.Engine
import com.fuzzylite.activation.General
import com.fuzzylite.defuzzifier.Centroid
import com.fuzzylite.norm.s.Maximum
import com.fuzzylite.norm.t.Minimum
import com.fuzzylite.rule.Rule
import com.fuzzylite.rule.RuleBlock
import com.fuzzylite.term.Gaussian
import com.fuzzylite.term.Trapezoid
import com.fuzzylite.variable.InputVariable
import com.fuzzylite.variable.OutputVariable
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Locale



class GasStationActivity : AppCompatActivity(), OnMapReadyCallback, TextToSpeech.OnInitListener {

    private lateinit var googleMap: GoogleMap
//    private lateinit var speedLimitTextView: TextView
//    private lateinit var currentSpeedTextView: TextView
//    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var tts: TextToSpeech
    private lateinit var locationCallback: LocationCallback


    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var viewBinding: ActivityGasStationBinding

    private var locationRequest: LocationRequest? = null
    private var lastLocation: android.location.Location? = null
    private var speedLimitThreshold = 1 // Set your speed limit threshold here in mph

    private val apiKey = "AIzaSyCSQTtgoqowfdjzDBY5_gfeUzn8ZsL8p_U"

    private val gson = Gson()

    private var userLat: Double = 0.0
    private var userLon: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityGasStationBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Initialize views
//        speedLimitTextView = findViewById(R.id.speedLimit)
//        currentSpeedTextView = findViewById(R.id.currentSpeed)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

//        viewBinding.buttonGetCurrentLocation.setOnClickListener {
//            getCurrentLocation()
//        }

        viewBinding.buttonSuggestGasStation.setOnClickListener {
            getSuggestedGasStation()
        }

        // Initialize Places API
        Places.initialize(applicationContext, "AIzaSyA6N208PM2c_pBzPDaQdlOiko2SZ12FASk")
        placesClient = Places.createClient(this)

        // Initialize TextToSpeech
        tts = TextToSpeech(this, this)

        // Check and request location permissions
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permissions already granted
            initializeMap()
            startLocationUpdates()
        }

    }

    private fun getSuggestedGasStation() {
//        val latitudeString = viewBinding.editTextLatitude.text.toString()
//        val longitudeString = viewBinding.editTextLongitude.text.toString()

        try {
            val url = "https://places.googleapis.com/v1/places:searchNearby"

            val includedTypes = listOf("\"gas_station\"") //listOf("restaurant")
            val maxResultCount = 3 //10
            val latitude = userLat//.toDouble() //37.7937
            val longitude = userLon//.toDouble() //-122.3965
            val radius = 50000.0


            val json = """
                        {
                          "includedTypes": $includedTypes,
                          "maxResultCount": $maxResultCount,
                          "locationRestriction": {
                            "circle": {
                              "center": {
                                "latitude": $latitude,
                                "longitude": $longitude
                              },
                              "radius": $radius
                            }
                          },
                          "rankPreference": "DISTANCE"
                        }
                    """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toRequestBody(mediaType)
            val fieldMask =
                "places.displayName,places.formattedAddress,places.rating,places.location" //"*" //"places.displayName"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Key", apiKey)
                .addHeader("X-Goog-FieldMask", fieldMask)
                .build()

            val client = OkHttpClient()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle error
                    Log.e("ERROR", e.toString())
                    Toast.makeText(
                        applicationContext,
                        "Could not process request",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val placesAPIResponse: PlacesResponse =
                            gson.fromJson(responseBody, PlacesResponse::class.java)

//                        viewBinding.textViewSuggestedGasStation.text =
//                            placesAPIResponse.places[0].displayName.toString()

                        val currLocation: DataClasses.Location =
                            DataClasses.Location(latitude, longitude)

                        getDistancesAndSuggestGasStation(currLocation, placesAPIResponse)


                    } else {
                        // Handle non-successful response
                        Log.e("ERROR", "Request Unsuccessful")

                        Toast.makeText(
                            applicationContext,
                            "Request Unsuccessful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })

        } catch (e: NumberFormatException) {
            // Handle the case where the input is not a valid double
            println("Please check the your inputs")
        }

    }

    private fun getDistancesAndSuggestGasStation(
        currLocation: DataClasses.Location,
        placesAPIResponse: PlacesResponse
    ) {
        val origins = "${currLocation.latitude}%2C${currLocation.longitude}"

        val places: List<DataClasses.Place> = placesAPIResponse.places
        var station1: DataClasses.Place = places[0]
        var station2: DataClasses.Place = places[1]
        var station3: DataClasses.Place = places[2]

        var destinations =
            "${station1.location.latitude}%2C${station1.location.longitude}%7C${station2.location.latitude}%2C${station2.location.longitude}%7C${station3.location.latitude}%2C${station3.location.longitude}"

        val client = OkHttpClient()
        val url =
            "https://maps.googleapis.com/maps/api/distancematrix/json?origins=${origins}&destinations=${destinations}&departure_time=now&key=${apiKey}"
        val request = Request.Builder().url(url).build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle error
                Log.e("ERROR", e.toString())

            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    val gson = Gson()
                    val distanceMatrix =
                        gson.fromJson(responseBody, DistanceMatrixResponse::class.java)

                    station1.element = distanceMatrix.rows[0].elements[0]
                    station2.element = distanceMatrix.rows[0].elements[0]
                    station3.element = distanceMatrix.rows[0].elements[0]

                    suggestBestGasStation(station1, station2, station3)
                } else {
                    // Handle non-successful response
                    Log.e("ERROR", "Request Unsuccessful")
                }
            }
        })

    }

    private fun suggestBestGasStation(station1: DataClasses.Place, station2: DataClasses.Place, station3: DataClasses.Place) {
        Log.d("Inside suggestBestGasStation", station1.element.toString())

        val rating1: Double = getFuzzyRating(station1)
        station1.fuzzyRating = rating1

        val rating2: Double = getFuzzyRating(station2)
        station2.fuzzyRating = rating2

        val rating3: Double = getFuzzyRating(station3)
        station3.fuzzyRating = rating3


        var highestRatedGasStation: DataClasses.Place = station1

        if (highestRatedGasStation.fuzzyRating < station2.fuzzyRating) {
            highestRatedGasStation = station2
        }

        if (highestRatedGasStation.fuzzyRating < station3.fuzzyRating) {
            highestRatedGasStation = station3
        }

        viewBinding.textViewSuggestedGasStation.text =
            "The suggested gas station is : ${highestRatedGasStation.displayName.text} at ${highestRatedGasStation.formattedAddress}"

        runOnUiThread {
            showNearestGasStation(highestRatedGasStation.location.latitude, highestRatedGasStation.location.longitude)
        }
    }

    private fun getFuzzyRating(station: DataClasses.Place): Double {
        val engine = Engine()
        engine.name = "GasStationRating"
        engine.description =
            "Fuzzy Engine to rate Gas stations based on distance, traffic and rating"

        val distance = InputVariable()
        distance.name = "distance"
        distance.description = "distance of the gas station from the users location"
        distance.isEnabled = true
        distance.setRange(0.000, 50000.000)
        distance.isLockValueInRange = false
        distance.addTerm(Trapezoid("near", 0.000, 0.000, 800.000, 1000.000))
        distance.addTerm(Trapezoid("moderate", 800.000, 3000.000, 7800.000, 10000.000))
        distance.addTerm(Trapezoid("far", 7800.000, 10000.000, 50000.000, 50000.000))
        engine.addInputVariable(distance)

        val traffic = InputVariable()
        traffic.name = "traffic"
        traffic.description =
            "traffic condition on route from the users location to the gas station"
        traffic.isEnabled = true
        traffic.setRange(0.000, 300.000)
        traffic.isLockValueInRange = false
        traffic.addTerm(Gaussian("light", -150.000, 100.000))
        traffic.addTerm(Gaussian("heavy", 300.000, 100.000))
        engine.addInputVariable(traffic)

        val userRating = InputVariable()
        userRating.name = "userRating"
        userRating.description = "rating of the gas station based on past user reviews"
        userRating.isEnabled = true
        userRating.setRange(0.000, 5.000)
        userRating.isLockValueInRange = false
        userRating.addTerm(Trapezoid("poor", 0.000, 0.000, 1.000, 2.000))
        userRating.addTerm(Trapezoid("good", 1.000, 2.000, 3.000, 4.000))
        userRating.addTerm(Trapezoid("excellent", 3.000, 4.000, 5.000, 5.000))
        engine.addInputVariable(userRating)


        val fuzzyRating = OutputVariable()
        fuzzyRating.name = "fuzzyRating"
        fuzzyRating.description = ""
        fuzzyRating.isEnabled = true
        fuzzyRating.setRange(0.000, 100.000)
        fuzzyRating.isLockValueInRange = false
        fuzzyRating.aggregation = Maximum()
        fuzzyRating.defuzzifier = Centroid(200)
        fuzzyRating.defaultValue = Double.NaN
        fuzzyRating.isLockPreviousValue = false
        fuzzyRating.addTerm(Trapezoid("low", 0.000, 0.000, 25.000, 50.000))
        fuzzyRating.addTerm(Trapezoid("average", 25.000, 50.000, 75.000, 100.000))
        fuzzyRating.addTerm(Trapezoid("high", 50.000, 75.000, 100.000, 100.000))
        engine.addOutputVariable(fuzzyRating)

        val ruleBlock = RuleBlock()
        ruleBlock.name = ""
        ruleBlock.description = ""
        ruleBlock.isEnabled = true
        ruleBlock.conjunction = Minimum()
        ruleBlock.disjunction = Maximum()
        ruleBlock.implication = Minimum()
        ruleBlock.activation = General()

        // Add rules to the rule block
        ruleBlock.addRule(
            Rule.parse(
                "if distance is near and traffic is light and userRating is poor then fuzzyRating is average",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is near and traffic is light and userRating is good then fuzzyRating is high",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is near and traffic is light and userRating is excellent then fuzzyRating is high",
                engine
            )
        )

        ruleBlock.addRule(
            Rule.parse(
                "if distance is near and traffic is heavy and userRating is poor then fuzzyRating is average",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is near and traffic is heavy and userRating is good then fuzzyRating is average",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is near and traffic is heavy and userRating is excellent then fuzzyRating is high",
                engine
            )
        )

        // =========================================================================================
        ruleBlock.addRule(
            Rule.parse(
                "if distance is moderate and traffic is light and userRating is poor then fuzzyRating is low",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is moderate and traffic is light and userRating is good then fuzzyRating is average",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is moderate and traffic is light and userRating is excellent then fuzzyRating is average",
                engine
            )
        )

        ruleBlock.addRule(
            Rule.parse(
                "if distance is moderate and traffic is heavy and userRating is poor then fuzzyRating is low",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is moderate and traffic is heavy and userRating is good then fuzzyRating is low",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is moderate and traffic is heavy and userRating is excellent then fuzzyRating is average",
                engine
            )
        )

        // =========================================================================================
        ruleBlock.addRule(
            Rule.parse(
                "if distance is far and traffic is light and userRating is poor then fuzzyRating is low",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is far and traffic is light and userRating is good then fuzzyRating is low",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is far and traffic is light and userRating is excellent then fuzzyRating is average",
                engine
            )
        )

        ruleBlock.addRule(
            Rule.parse(
                "if distance is far and traffic is heavy and userRating is poor then fuzzyRating is low",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is far and traffic is heavy and userRating is good then fuzzyRating is low",
                engine
            )
        )
        ruleBlock.addRule(
            Rule.parse(
                "if distance is far and traffic is heavy and userRating is excellent then fuzzyRating is average",
                engine
            )
        )

        // Add rule block to rule engine
        engine.addRuleBlock(ruleBlock)

        // Set input values
        engine.setInputValue("distance", station.element.distance.value.toDouble())
        engine.setInputValue("traffic", (station.element.duration_in_traffic.value - station.element.duration.value).toDouble())
        engine.setInputValue("userRating", station.rating)

        // Evaluate the fuzzy system
        engine.process()

        // Get the output value
        val outputValue = engine.getOutputValue("fuzzyRating")
        println("Output: $outputValue")

        // return the output value
        return outputValue
    }


    private fun getCurrentLocation() {
        if (checkPermission()) {
            if (isLocationEnabled()) {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location = task.result

                    if (location == null) {
                        Toast.makeText(this, "Null Location", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Location: " + location.latitude + ",  " + location.longitude,
                            Toast.LENGTH_LONG
                        ).show()

//                        viewBinding.editTextLatitude.setText(location.latitude.toString())
//                        viewBinding.editTextLongitude.setText(location.longitude.toString())

                    }
                }
            } else {
                // settings open
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val intent = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                startActivity(Intent(intent))

            }
        } else {
            // request permission
            requestPermission()
        }

    }


    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Granted", Toast.LENGTH_SHORT).show()

                // Location permission granted
                initializeMap()
                startLocationUpdates()
            } else {
                Toast.makeText(applicationContext, "Denied", Toast.LENGTH_SHORT).show()

            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val DEFAULT_ZOOM = 15f
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) ==
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun initializeMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Get the user's last known location and update the map
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: android.location.Location? ->
                location?.let {
                    updateMapLocation(it.latitude, it.longitude)
                }
            }
    }

    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000).apply {
//            setMinUpdateDistanceMeters(minimalDistance)
//            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
//            setWaitForAccurateLocation(true)
        }.build()

//        locationRequest = LocationRequest.create().apply {
//            interval = 3000 // Update every 3 second
//            fastestInterval = 3000
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->

                    // Store the latitude and longitude of use in global variable
                    userLat = location.latitude
                    userLon = location.longitude

                    updateMapLocation(location.latitude, location.longitude)
                    calculateAndDisplaySpeed(location)
                    lastLocation = location
//                    showNearestGasStation(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest!!,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun calculateAndDisplaySpeed(currentLocation: android.location.Location) {
        lastLocation?.let { lastLocation ->
            val distance = lastLocation.distanceTo(currentLocation)
            val timeDifference = (currentLocation.time - lastLocation.time) / 1000 // in seconds
            val speed = if (timeDifference > 0) distance / timeDifference else 0f

            // Display the speed in km/h (you can adjust the unit as needed)
            var speedInmph = speed * 3.6 * 0.62

            // Demo Mode: Uncomment below to test in demo mode
            // speedInmph = 140.0
            viewBinding.currentSpeed.text = String.format("Current Speed: %.2f mph", speedInmph)

            speedLimitThreshold = fetchSpeedLimit(currentLocation)
            // Check if speed exceeds the limit
            if (speedInmph > speedLimitThreshold) {
                speak("Go Slow")
            }
        }
    }

    private fun fetchSpeedLimit(location: android.location.Location): Int {
        var lat = location.latitude
        var lon = location.longitude
        var speedLimit = 50
        // Example URL for fetching speed limit from OpenStreetMap Overpass API
        val overpassApiUrl = "https://overpass-api.de/api/interpreter?data=" +
                "[out:json];" +
                "way(around:10,$lat,$lon)[maxspeed];" +
                "out;"

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(overpassApiUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let {
                    val jsonString = it.string()
                    speedLimit = parseSpeedLimitFromJson(jsonString)

                    // Update the UI with the speed limit
                    runOnUiThread {
                        viewBinding.speedLimit.text = "Speed Limit: $speedLimit mph"
                    }
                }
            }
        })
        return speedLimit
    }

    private fun parseSpeedLimitFromJson(jsonString: String): Int {
        try {
            val jsonObject = JSONObject(jsonString)
            val elementsArray = jsonObject.getJSONArray("elements")

            if (elementsArray.length() > 0) {
                val element = elementsArray.getJSONObject(0)
                if (element.has("tags")) {
                    val tags = element.getJSONObject("tags")
                    if (tags.has("maxspeed")) {
                        val maxSpeed = tags.getString("maxspeed")
                        return extractSpeedLimitFromString(maxSpeed)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 30 // Speed limit not found or error occurred
    }

    private fun extractSpeedLimitFromString(maxSpeed: String): Int {
        // Extract speed limit from the string, you may need additional parsing based on format
        try {
            val speedLimit = maxSpeed.split(" ")[0].toInt()
            return speedLimit
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 30 // Error extracting speed limit
    }

    private fun speak(message: String) {
        // Check if TextToSpeech is initialized
        if (::tts.isInitialized && tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    private fun updateMapLocation(latitude: Double, longitude: Double) {
        // Enable the user's location on the map
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true

            // Move the camera to the user's current location
            val userLocation = LatLng(latitude, longitude)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM))

            // Add a marker at the user's current location
            // googleMap.addMarker(MarkerOptions().position(userLocation).title("You are here"))
        }
    }

    private fun showNearestGasStation(latitude: Double, longitude: Double) {
        // val placeFields: List<Field> = listOf(Field.NAME, Field.LAT_LNG)
//        val fields = listOf(Field.NAME, Field.LAT_LNG, Field.TYPES)
//        val request = FindCurrentPlaceRequest.builder(fields).build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            //
            return
        }

        val gasStationLocation = LatLng(latitude, longitude)
        val markerOptions = MarkerOptions().position(gasStationLocation).title("Gas Station")
        googleMap.addMarker(markerOptions)

        // Move camera to the gas station location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(gasStationLocation, 15f))



//        val gasStationMarker = LatLng(gasLat, gasLon)
//                            // LatLng(place.latLng!!.latitude, place.latLng!!.longitude)

        // Add a marker for the gas station
//        googleMap.addMarker(
//            MarkerOptions().position(gasStationMarker).title("Gas Station")
//        )

//        placesClient.findCurrentPlace(request)
//            .addOnSuccessListener { response ->
//                for (placeLikelihood in response.placeLikelihoods) {
//                    val place = placeLikelihood.place
//                    val placeTypes = place.types
//
//                    Log.d("HELLO", "place $place")
//                    // Check if the place is a gas station
//                    if (placeTypes != null && placeTypes.contains(com.google.android.libraries.places.api.model.Place.Type.GAS_STATION)) {
//                        val gasStationMarker = LatLng(userLat, userLon)
//                            // LatLng(place.latLng!!.latitude, place.latLng!!.longitude)
//
//                        // Add a marker for the gas station
//                        googleMap.addMarker(
//                            MarkerOptions().position(gasStationMarker).title(place.name)
//                        )
//                    }
//
//                }
//            }
//            .addOnFailureListener { exception ->
//                // Handle error
//                exception.printStackTrace()
//            }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set the language for TextToSpeech
            val result = tts.setLanguage(Locale.getDefault())

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Handle language not supported or missing data
            }
        } else {
            // Handle TextToSpeech initialization failure
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable the user's location on the map
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true

            // Move the camera to the user's current location
            val userLocation = LatLng(37.7749, -122.4194) // Default to San Francisco coordinates
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM))

            // Add a marker at the user's current location
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop location updates when the activity is destroyed
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

}
