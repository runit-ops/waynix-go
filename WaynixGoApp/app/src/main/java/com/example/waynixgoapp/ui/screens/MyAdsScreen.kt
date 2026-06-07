package com.example.waynixgoapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.waynixgoapp.data.UserPreferences
import com.example.waynixgoapp.data.network.ApiRideOffer
import com.example.waynixgoapp.ui.components.*
import com.example.waynixgoapp.ui.theme.*

// ─────────────────────────────────────────────
// MY ADS SCREEN
// ─────────────────────────────────────────────

@Composable
fun MyAdsScreen(
    onProfileClick: () -> Unit,
    viewModel: MyAdsViewModel = viewModel()
) {
    val strings = LocalWaynixStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val activeRides = uiState.rides.filter { it.status == "active" }
    val historyRides = uiState.rides.filter { it.status != "active" }

    LaunchedEffect(Unit) {
        viewModel.loadMyAds(prefs.driverId)
    }

    Column(Modifier.fillMaxSize()) {
        WaynixTopBar(
            trailingContent = { ProfileAvatarButton(onClick = onProfileClick) }
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = WaynixColors.White,
            contentColor = WaynixColors.Teal,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = WaynixColors.Teal
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(strings.activeAds, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(strings.history, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.paddingMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingMd)
        ) {
            val currentList = if (selectedTab == 0) activeRides else historyRides

            if (uiState.isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WaynixColors.Teal)
                }
            } else if (currentList.isEmpty()) {
                EmptyState(
                    icon = if (selectedTab == 0) Icons.Filled.DirectionsCar else Icons.Outlined.History,
                    message = if (selectedTab == 0) strings.noActiveAds else strings.emptyHistory
                )
            } else {
                currentList.forEach { ride ->
                    DriverRideCard(
                        ride = ride,
                        onUpdateBookingStatus = { bookingId, status ->
                            viewModel.updateBookingStatus(prefs.driverId, bookingId, status)
                        },
                        onFinishRide = {
                            viewModel.updateRideStatus(prefs.driverId, ride.id, "done")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DriverRideCard(
    ride: ApiRideOffer,
    onUpdateBookingStatus: (Int, String) -> Unit,
    onFinishRide: () -> Unit
) {
    val strings = LocalWaynixStrings.current
    Surface(
        shape = RoundedCornerShape(Dimens.radiusMed),
        color = WaynixColors.White,
        border = BorderStroke(1.dp, WaynixColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("${ride.fromCity} → ${ride.toDistrict}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(ride.departureTime?.take(16)?.replace("T", " ") ?: "", fontSize = 12.sp, color = WaynixColors.TextGray)
                }
                Badge(
                    containerColor = when(ride.status) {
                        "active" -> WaynixColors.Teal.copy(alpha=0.1f)
                        "done" -> WaynixColors.GreenDot.copy(alpha=0.1f)
                        else -> WaynixColors.Border
                    },
                    contentColor = when(ride.status) {
                        "active" -> WaynixColors.Teal
                        "done" -> WaynixColors.GreenDot
                        else -> WaynixColors.TextGray
                    }
                ) {
                    Text(
                        if (ride.status == "done") strings.rideFinished.uppercase() else ride.status.uppercase(),
                        modifier = Modifier.padding(4.dp),
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = WaynixColors.Border.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))

            if (ride.bookings.isEmpty()) {
                Text(strings.noActiveAds, fontSize = 12.sp, color = WaynixColors.TextGray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                ride.bookings.forEach { booking ->
                    BookingItem(booking = booking, onUpdateStatus = onUpdateBookingStatus)
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (ride.status == "active") {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onFinishRide,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WaynixColors.Teal),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.finishRide, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BookingItem(
    booking: com.example.waynixgoapp.data.network.ApiBooking,
    onUpdateStatus: (Int, String) -> Unit
) {
    val statusColor = when (booking.status) {
        "pending" -> WaynixColors.Yellow
        "confirmed" -> WaynixColors.Teal
        "checked_in" -> WaynixColors.GreenDot
        "no_show", "cancelled", "rejected" -> WaynixColors.Red
        else -> WaynixColors.TextGray
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = WaynixColors.BgLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, null, tint = WaynixColors.TextGray, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(booking.passengerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    statusLabel(booking.status),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(booking.passengerPhone, fontSize = 12.sp, color = WaynixColors.TextGray)
            if (!booking.note.isNullOrBlank()) {
                Text(booking.note, fontSize = 11.sp, color = WaynixColors.TextGray)
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (booking.status) {
                    "pending" -> {
                        ActionButton(label = "Принять", color = WaynixColors.Teal, icon = Icons.Filled.Check) {
                            onUpdateStatus(booking.id, "confirmed")
                        }
                        ActionButton(label = "Отклонить", color = WaynixColors.Red, icon = Icons.Filled.Block) {
                            onUpdateStatus(booking.id, "rejected")
                        }
                    }
                    "confirmed" -> {
                        ActionButton(label = "Сел", color = WaynixColors.GreenDot, icon = Icons.Filled.Check) {
                            onUpdateStatus(booking.id, "checked_in")
                        }
                        ActionButton(label = "Не пришел", color = WaynixColors.Red, icon = Icons.Filled.Block) {
                            onUpdateStatus(booking.id, "no_show")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(label: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun statusLabel(status: String) = when (status) {
    "pending" -> "ОЖИДАЕТ"
    "confirmed" -> "ПОДТВЕРЖДЕНО"
    "checked_in" -> "В МАШИНЕ"
    "no_show" -> "НЕ ПРИШЕЛ"
    "cancelled" -> "ОТМЕНЕНО"
    "rejected" -> "ОТКЛОНЕНО"
    else -> status.uppercase()
}

// ─────────────────────────────────────────────
// PREVIEW
// ─────────────────────────────────────────────
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MyAdsScreenPreview() {
    MyAdsScreen(onProfileClick = {})
}