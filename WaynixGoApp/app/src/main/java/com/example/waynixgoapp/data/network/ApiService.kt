package com.example.waynixgoapp.data.network

import com.example.waynixgoapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Retrofit API for the WaynixGO backend.
 *
 * BASE_URL is taken from BuildConfig.API_BASE_URL, which comes from
 * `WAYNIX_API_BASE_URL` in gradle.properties (overridable via local.properties
 * or `-PWAYNIX_API_BASE_URL=...`). For ngrok during dev, set it to the
 * https://<id>.ngrok-free.app/ URL with a trailing slash.
 *
 * The "ngrok-skip-browser-warning" header is required by ngrok free tier so
 * that the tunnel returns the actual JSON instead of the HTML interstitial.
 */
interface ApiService {

    @GET("api/health/")
    suspend fun healthCheck(): HealthResponse

    // ─── ОБЪЯВЛЕНИЯ (RIDES) ──────────────────────────────

    @GET("api/rides/")
    suspend fun getRides(
        @Query("to") to: String? = null,
        @Query("from") from: String? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("driver_id") driverId: Int? = null
    ): PaginatedResponse<ApiRideOffer>

    @GET("api/rides/{id}/")
    suspend fun getRideDetail(@Path("id") id: Int): ApiRideOffer

    @POST("api/rides/create/")
    suspend fun createRide(@Body request: CreateRideRequest): CreateRideResponse

    @PATCH("api/rides/{id}/status/")
    suspend fun changeRideStatus(
        @Path("id") id: Int,
        @Body request: StatusChangeRequest
    ): StatusChangeResponse

    // ─── ТАКСИСТЫ (DRIVERS) ──────────────────────────────

    @POST("api/drivers/register/")
    suspend fun registerDriver(@Body request: CreateDriverRequest): CreateDriverResponse

    @GET("api/drivers/{id}/")
    suspend fun getDriverDetail(@Path("id") id: Int): ApiDriver

    // ─── БРОНИРОВАНИЯ (BOOKINGS) ─────────────────────────

    @POST("api/bookings/create/")
    suspend fun createBooking(@Body request: CreateBookingRequest): CreateBookingResponse

    @GET("api/bookings/")
    suspend fun getBookings(@Query("phone") phone: String): List<ApiBooking>

    @PATCH("api/bookings/{id}/status/")
    suspend fun updateBookingStatus(
        @Path("id") id: Int,
        @Body request: BookingStatusRequest
    ): Map<String, Any>

    // ─── FACTORY ─────────────────────────────────────────

    companion object {
        @Volatile
        private var INSTANCE: ApiService? = null

        fun create(): ApiService {
            INSTANCE?.let { return it }
            return synchronized(this) {
                INSTANCE ?: build().also { INSTANCE = it }
            }
        }

        private fun build(): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        // Required so ngrok free tier doesn't return the
                        // HTML browser-warning page instead of our JSON.
                        .addHeader("ngrok-skip-browser-warning", "true")
                        .addHeader("User-Agent", "WaynixGoApp/1.0 Android")
                        .build()
                    chain.proceed(req)
                }
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
