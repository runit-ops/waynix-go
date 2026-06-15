package com.example.waynixgoapp.data.network

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════
//  API RESPONSE MODELS — точно соответствуют JSON бэкенда
// ═══════════════════════════════════════════════

/**
 * Пагинированный ответ DRF (PageNumberPagination).
 */
data class PaginatedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)

/**
 * Водитель — вложен в RideOffer.
 */
data class ApiDriver(
    val id: Int,
    val name: String,
    val phone: String,
    @SerializedName("card_number") val cardNumber: String?,
    @SerializedName("car_model") val carModel: String?,
    @SerializedName("car_color") val carColor: String?,
    @SerializedName("car_plate") val carPlate: String?,
    val rating: String?,  // DecimalField приходит как строка
    @SerializedName("total_rides") val totalRides: Int?
)

/**
 * Объявление о поездке — главная сущность.
 * Бэкенд отдаёт и snake_case и camelCase, мы берём camelCase.
 */
data class ApiRideOffer(
    val id: Int,
    val driver: ApiDriver,
    @SerializedName("from_city") val fromCity: String,
    @SerializedName("to_district") val toDistrict: String,
    @SerializedName("route_description") val routeDescription: String?,
    @SerializedName("departure_time") val departureTime: String?,
    @SerializedName("price_per_seat") val pricePerSeat: Int,
    @SerializedName("total_seats") val totalSeats: Int,
    @SerializedName("seats_available") val seatsAvailable: Int,
    val status: String,
    val bookings: List<ApiBooking> = emptyList(),
    @SerializedName("is_available") val isAvailable: Boolean?,
    @SerializedName("created_at") val createdAt: String?
)

// ═══════════════════════════════════════════════
//  REQUEST MODELS — для POST/PATCH запросов
// ═══════════════════════════════════════════════

data class CreateDriverRequest(
    val name: String,
    val phone: String,
    @SerializedName("card_number") val cardNumber: String? = null,
    @SerializedName("car_model") val carModel: String? = null,
    @SerializedName("car_color") val carColor: String? = null,
    @SerializedName("car_plate") val carPlate: String? = null
)

data class CreateRideRequest(
    @SerializedName("driver_id") val driverId: Int,
    @SerializedName("from_city") val fromCity: String,
    @SerializedName("to_district") val toDistrict: String,
    @SerializedName("route_description") val routeDescription: String = "",
    @SerializedName("departure_time") val departureTime: String,
    @SerializedName("price_per_seat") val pricePerSeat: Int,
    @SerializedName("total_seats") val totalSeats: Int = 4,
    val seatsAvailable: Int
)

data class CreateBookingRequest(
    @SerializedName("offer_id") val offerId: Int,
    @SerializedName("passenger_name") val passengerName: String,
    @SerializedName("passenger_phone") val passengerPhone: String,
    @SerializedName("seats_requested") val seatsRequested: Int = 1,
    val note: String = ""
)

data class StatusChangeRequest(
    @SerializedName("driver_id") val driverId: Int,
    val status: String
)

data class BookingStatusRequest(
    @SerializedName("driver_id") val driverId: Int,
    val status: String
)

// ═══════════════════════════════════════════════
//  RESPONSE WRAPPERS
// ═══════════════════════════════════════════════

data class CreateDriverResponse(
    val message: String,
    val driver: ApiDriver
)

data class CreateRideResponse(
    val message: String,
    val offer: ApiRideOffer
)

data class BookingContact(
    val phone: String
)

data class ApiBooking(
    val id: Int,
    @SerializedName("offer_summary") val offerSummary: Map<String, Any>?,
    @SerializedName("passenger_name") val passengerName: String,
    @SerializedName("passenger_phone") val passengerPhone: String,
    @SerializedName("seats_requested") val seatsRequested: Int,
    val status: String,
    val note: String?,
    @SerializedName("created_at") val createdAt: String?
)

data class CreateBookingResponse(
    val message: String,
    val booking: ApiBooking,
    val contact: BookingContact
)

data class StatusChangeResponse(
    val message: String,
    val offer: ApiRideOffer
)

// ═══════════════════════════════════════════════
//  МАППЕР — ApiRideOffer → локальная модель Ride
// ═══════════════════════════════════════════════

fun ApiRideOffer.toRide(): com.example.waynixgoapp.data.Ride {
    return com.example.waynixgoapp.data.Ride(
        id = id.toString(),
        driverName = driver.name,
        from = fromCity,
        to = toDistrict,
        pricePerSeat = pricePerSeat,
        availableSeats = seatsAvailable,
        totalSeats = totalSeats,
        status = status,
        comment = routeDescription ?: "",
        phoneNumber = driver.phone,
        rating = driver.rating?.toDoubleOrNull() ?: 5.0,
        carModel = driver.carModel ?: "",
        departureTime = departureTime ?: ""
    )
}

data class HealthResponse(
    val status: String,
    val message: String
)

// ═══════════════════════════════════════════════
//  TELEGRAM AUTH MODELS
// ═══════════════════════════════════════════════

data class TelegramAuthInitRequest(
    val phone: String
)

data class TelegramAuthInitResponse(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("bot_url") val botUrl: String
)

data class TelegramAuthVerifyRequest(
    @SerializedName("session_id") val sessionId: String,
    val code: String
)

data class TelegramAuthVerifyResponse(
    val success: Boolean,
    val phone: String
)
