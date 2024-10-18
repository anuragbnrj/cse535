package com.example.emptyviewsapplication.data.dto

class DataClasses {

    data class Location(
        val latitude: Double,
        val longitude: Double
    )

    data class DisplayName(
        val text: String,
        val languageCode: String
    )

    data class Place(
        val formattedAddress: String,
        val location: Location,
        val rating: Double,
        val displayName: DisplayName,
        var element: Element,
        var fuzzyRating: Double
    )

    data class PlacesResponse(
        val places: List<Place>
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

    data class DistanceMatrixResponse(
        val rows: List<Row>
    )
}