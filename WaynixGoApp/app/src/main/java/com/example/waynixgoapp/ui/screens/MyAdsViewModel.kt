package com.example.waynixgoapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.waynixgoapp.data.Ride
import com.example.waynixgoapp.data.network.ApiBooking
import com.example.waynixgoapp.data.network.ApiRideOffer
import com.example.waynixgoapp.data.network.ApiService
import com.example.waynixgoapp.data.network.BookingStatusRequest
import com.example.waynixgoapp.data.network.toRide
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MyAdsUiState(
    val rides: List<ApiRideOffer> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class MyAdsViewModel : ViewModel() {
    private val apiService = ApiService.create()
    
    private val _uiState = MutableStateFlow(MyAdsUiState())
    val uiState: StateFlow<MyAdsUiState> = _uiState

    fun loadMyAds(driverId: Int) {
        if (driverId == -1) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                // Загружаем все поездки этого водителя (даже полные или завершенные)
                val response = apiService.getRides(driverId = driverId)
                _uiState.value = _uiState.value.copy(rides = response.results)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(errorMessage = "Ошибка загрузки: ${e.localizedMessage}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateBookingStatus(driverId: Int, bookingId: Int, newStatus: String) {
        viewModelScope.launch {
            try {
                apiService.updateBookingStatus(
                    id = bookingId,
                    request = BookingStatusRequest(driverId = driverId, status = newStatus)
                )
                loadMyAds(driverId)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(errorMessage = "Не удалось обновить: ${e.localizedMessage}")
            }
        }
    }

    fun updateRideStatus(driverId: Int, rideId: Int, newStatus: String) {
        viewModelScope.launch {
            try {
                apiService.changeRideStatus(
                    id = rideId,
                    request = com.example.waynixgoapp.data.network.StatusChangeRequest(driverId = driverId, status = newStatus)
                )
                loadMyAds(driverId)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(errorMessage = "Не удалось обновить статус поездки: ${e.localizedMessage}")
            }
        }
    }
}
