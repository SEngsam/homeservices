package com.example.homeservices.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ServiceRequest(
    var id: String = "",
    var customerID: String = "",
    var providerID: String = "",
    var serviceID: String = "",
    var status: String = "pending",
    var hoursRequested: Int = 1,
    var timestamp: Long = 0
)