package com.example.emptyviewsapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.emptyviewsapplication.data.entity.SymptomsOfUser
import com.example.emptyviewsapplication.databinding.ActivitySymptomsBinding

class SymptomsActivity : AppCompatActivity() {

    private val symptoms = arrayOf(
        "Nausea",
        "Headache",
        "Diarrhea",
        "Sore Throat",
        "Fever",
        "Muscle Ache",
        "Loss of Smell or taste",
        "Cough",
        "Shortness of Breath",
        "Feeling Tired"
    )


    private lateinit var viewBinding: ActivitySymptomsBinding
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var adapterItems: ArrayAdapter<String>

    private var selectedSymptom: String = ""
    private var symptomRatingMap: HashMap<String, Int> = HashMap()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySymptomsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        autoCompleteTextView = viewBinding.autoCompleteTextView
        adapterItems = ArrayAdapter<String>(this, R.layout.list_item, symptoms)
        autoCompleteTextView.setAdapter(adapterItems)
        autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, p1, position, p3 ->
                val symptom = adapterView?.getItemAtPosition(position).toString()
                Toast.makeText(
                    this@SymptomsActivity,
                    "Selected Symptom is $symptom",
                    Toast.LENGTH_SHORT
                ).show()
                selectedSymptom = symptom
            }

        val buttonConfirmRatingOfSymptom = viewBinding.buttonConfirmRatingOfSymptom
        buttonConfirmRatingOfSymptom.setOnClickListener {

            val selectedRating = viewBinding.ratingBarOfSymptom.rating.toInt()

            if (selectedSymptom != "") {
                val selectedSymptomSnakeCase = selectedSymptom.split(" ").joinToString(separator = "_").lowercase()
                Log.d("AnuragLogs", selectedSymptomSnakeCase)

                symptomRatingMap[selectedSymptomSnakeCase] = selectedRating
                Toast.makeText(
                    this@SymptomsActivity,
                    "Your rating for $selectedSymptom is $selectedRating",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@SymptomsActivity,
                    "Please select a symptom first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val buttonSaveAllSymptomsRatings = viewBinding.buttonSaveAllSymptomsRatings
        buttonSaveAllSymptomsRatings.setOnClickListener {

            val symptomsOfUser = SymptomsOfUser()
            symptomsOfUser.nausea = if (symptomRatingMap.containsKey("nausea")) (
                    symptomRatingMap["nausea"]
                    )!! else 0

            symptomsOfUser.headache = if (symptomRatingMap.containsKey("headache")) (
                    symptomRatingMap["headache"]
                    )!! else 0

            symptomsOfUser.diarrhea = if (symptomRatingMap.containsKey("diarrhea")) (
                    symptomRatingMap["diarrhea"]
                    )!! else 0

            symptomsOfUser.sore_throat = if (symptomRatingMap.containsKey("sore_throat")) (
                    symptomRatingMap["sore_throat"]
                    )!! else 0

            symptomsOfUser.fever = if (symptomRatingMap.containsKey("fever")) (
                    symptomRatingMap["fever"]
                    )!! else 0

            symptomsOfUser.muscle_ache = if (symptomRatingMap.containsKey("muscle_ache")) (
                    symptomRatingMap["muscle_ache"]
                    )!! else 0

            symptomsOfUser.loss_of_smell_or_taste = if (symptomRatingMap.containsKey("loss_of_smell_or_taste")) (
                    symptomRatingMap["loss_of_smell_or_taste"]
                    )!! else 0

            symptomsOfUser.cough = if (symptomRatingMap.containsKey("cough")) (
                    symptomRatingMap["cough"]
                    )!! else 0

            symptomsOfUser.shortness_of_breath = if (symptomRatingMap.containsKey("shortness_of_breath")) (
                    symptomRatingMap["shortness_of_breath"]
                    )!! else 0

            symptomsOfUser.feeling_tired = if (symptomRatingMap.containsKey("feeling_tired")) (
                    symptomRatingMap["feeling_tired"]
                    )!! else 0

            val intentFromMain = intent
            val respiratoryRate = intentFromMain.getStringExtra("RESPIRATORY_RATE")
            val heartRate = intentFromMain.getStringExtra("HEART_RATE")


//            if (!respiratoryRate.isNullOrBlank()) {
//                symptomsOfUser.respiratory_rate = respiratoryRate.toInt()
//            }
//
//            if (!heartRate.isNullOrBlank()) {
//                symptomsOfUser.heart_rate = heartRate.toInt()
//            }

            val replyIntent = Intent()
            replyIntent.putExtra(EXTRA_REPLY, symptomsOfUser)
            setResult(Activity.RESULT_OK, replyIntent)

            finish()
        }
    }

    companion object {
        const val EXTRA_REPLY = "com.example.android.symptomsOfUserListSql.REPLY"
    }
}
