package com.example.emptyviewsapplication.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class SymptomsOfUser (
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    var nausea: Int = 0,
    var headache: Int = 0,
    var diarrhea: Int = 0,
    var sore_throat: Int = 0,
    var fever: Int = 0,
    var muscle_ache: Int = 0,
    var loss_of_smell_or_taste: Int = 0,
    var cough: Int = 0,
    var shortness_of_breath: Int = 0,
    var feeling_tired: Int = 0,
    var respiratory_rate: Int = 0,
    var heart_rate: Int = 0

) : Serializable