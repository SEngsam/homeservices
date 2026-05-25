package com.example.homeservices.model

import java.io.Serializable

data class ServiceProvider(
    val id: String = "",
    var name: String = "",
    val description: String = "",
    var categoryID: String = "",

    val image: String = "",
    val rate: Double = 0.0,
    var pricePerHour: Int = 0,
    val address: String = "",
    val location: LocationModel = LocationModel()
) : Serializable