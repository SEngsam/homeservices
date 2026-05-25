package com.example.homeservices.model

import com.google.firebase.database.PropertyName
import java.io.Serializable

data class LocationModel(
    @get:PropertyName("lat")
    @set:PropertyName("lat")
    var latitude: Double = 0.0,

    @get:PropertyName("lng")
    @set:PropertyName("lng")
    var longitude: Double = 0.0
) : Serializable {
    // Adding secondary names for broader compatibility with different Firebase nodes
    @get:PropertyName("latitude")
    @set:PropertyName("latitude")
    var altLatitude: Double 
        get() = latitude
        set(value) { latitude = value }

    @get:PropertyName("longitude")
    @set:PropertyName("longitude")
    var altLongitude: Double
        get() = longitude
        set(value) { longitude = value }
}