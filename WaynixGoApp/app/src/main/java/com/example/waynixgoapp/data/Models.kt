package com.example.waynixgoapp.data

import androidx.compose.ui.graphics.vector.ImageVector

data class Ride(
    val id: String? = null,
    val driverName: String,
    val from: String,
    val to: String,
    val pricePerSeat: Int,
    val availableSeats: Int,
    val totalSeats: Int = 4,
    val status: String = "active",
    val comment: String = "",
    val phoneNumber: String = "",
    val rating: Double = 5.0,
    val carModel: String = "",
    val departureTime: String = ""
)

data class Message(
    val sender: String, // "driver" or "passenger"
    val text: String,
    val time: String
)

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
