package com.example.waynixgoapp.ui.screens

import com.example.waynixgoapp.data.UserPreferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waynixgoapp.data.network.ApiService
import com.example.waynixgoapp.data.network.CreateRideRequest
import com.example.waynixgoapp.ui.components.*
import com.example.waynixgoapp.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// POST SCREEN
@Composable
fun PostScreen(
    onClose: () -> Unit,
    onPublish: (from: String, to: String, persons: Int, additional: String) -> Unit = { _, _, _, _ -> }
) {
    val strings = LocalWaynixStrings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val apiService = remember { ApiService.create() }
    val scope = rememberCoroutineScope()

    var from by remember { mutableStateOf("(Все)") }
    var to by remember { mutableStateOf("(Все)") }
    var dayTab by remember { mutableIntStateOf(0) }
    var persons by remember { mutableIntStateOf(1) }
    var additional by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var timeTab by remember { mutableIntStateOf(0) }
    var published by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showFromSheet by remember { mutableStateOf(false) }
    var showToSheet by remember { mutableStateOf(false) }

    val currentTime = remember { LocalDateTime.now() }
    var selectedHour by remember { mutableIntStateOf((currentTime.hour + 1) % 24) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    
    var selectedHourTo by remember { mutableIntStateOf((currentTime.hour + 3) % 24) }
    var selectedMinuteTo by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        WaynixTopBar(
            trailingContent = {
                Spacer(Modifier.size(48.dp))
            },
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.paddingMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingSm)
        ) {
            WaynixDropdown(
                label = strings.fromCity,
                value = from,
                icon = Icons.Filled.LocationOn,
                onClick = { showFromSheet = true }
            )
            WaynixDropdown(
                label = strings.toDistrict,
                value = to,
                icon = Icons.Filled.Navigation,
                onClick = { showToSheet = true }
            )


            Text(
                strings.seatsCount,
                fontSize = 11.sp,
                color = WaynixColors.TextGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            PersonCounter(
                count = persons,
                onDecrement = { if (persons > 1) persons-- },
                onIncrement = { if (persons < 20) persons++ }
            )

            Text(
                strings.setPrice,
                fontSize = 11.sp,
                color = WaynixColors.TextGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            OutlinedTextField(
                value = price,
                onValueChange = { newVal ->
                    if (newVal.all { it.isDigit() } && newVal.length <= 8) price = newVal
                },
                label = { Text(strings.setPrice) },
                placeholder = { Text("50000") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.radiusMed),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WaynixColors.Teal,
                    unfocusedBorderColor = WaynixColors.Border
                ),
                singleLine = true
            )

            OutlinedTextField(
                value = additional,
                onValueChange = { additional = it },
                label = { Text(strings.commentLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.radiusMed),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WaynixColors.Teal,
                    unfocusedBorderColor = WaynixColors.Border
                ),
                minLines = 2
            )

            Text(
                strings.timeLabel,
                fontSize = 11.sp,
                color = WaynixColors.TextGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WaynixColors.Border)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf(strings.now, strings.later, strings.period).forEachIndexed { idx, label ->
                    SegmentedTab(
                        label = label,
                        selected = timeTab == idx,
                        onClick = { timeTab = idx },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedVisibility(visible = timeTab == 1) { // Позже
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        strings.selectTime,
                        fontSize = 11.sp,
                        color = WaynixColors.TextGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    TimeSelector(
                        hour = selectedHour,
                        minute = selectedMinute,
                        onUpdate = { h, m -> selectedHour = h; selectedMinute = m }
                    )
                }
            }

            AnimatedVisibility(visible = timeTab == 2) { // Период
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        strings.period,
                        fontSize = 11.sp,
                        color = WaynixColors.TextGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            TimeSelector(
                                hour = selectedHour,
                                minute = selectedMinute,
                                onUpdate = { h, m -> selectedHour = h; selectedMinute = m }
                              )
                        }
                        Text(" — ", Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                        Box(Modifier.weight(1f)) {
                            TimeSelector(
                                hour = selectedHourTo,
                                minute = selectedMinuteTo,
                                onUpdate = { h, m -> selectedHourTo = h; selectedMinuteTo = m }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (from == "(Все)") {
                        errorMessage = strings.selectFromCity
                        return@Button
                    }
                    if (to == "(Все)") {
                        errorMessage = strings.selectToDistrict
                        return@Button
                    }
                    if (prefs.phone.isBlank()) {
                        errorMessage = strings.setPhoneInProfile
                        return@Button
                    }
                    val priceInt = price.toIntOrNull()
                    if (priceInt == null || priceInt <= 0) {
                        errorMessage = strings.setCorrectPrice
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        try {
                            val driverResponse = apiService.registerDriver(
                                com.example.waynixgoapp.data.network.CreateDriverRequest(
                                    name = "${prefs.name} ${prefs.lastName}",
                                    phone = prefs.phone,
                                    cardNumber = null
                                )
                            )
                            val driverId = driverResponse.driver.id
                            prefs.driverId = driverId

                            val nowDt = LocalDateTime.now()
                            var departureTime = when (dayTab) {
                                0 -> nowDt
                                1 -> nowDt.plusDays(1)
                                else -> nowDt.plusDays(2)
                            }
                            
                            var finalAdditional = additional
                            
                            when (timeTab) {
                                0 -> {
                                    departureTime = departureTime.withMinute((nowDt.minute + 5) % 60)
                                    if (departureTime.minute < nowDt.minute) departureTime = departureTime.plusHours(1)
                                }
                                1 -> {
                                    departureTime = departureTime.withHour(selectedHour).withMinute(selectedMinute)
                                }
                                2 -> {
                                    departureTime = departureTime.withHour(selectedHour).withMinute(selectedMinute)
                                    val timeTo = "%02d:%02d".format(selectedHourTo, selectedMinuteTo)
                                    val periodNote = "⏰ В ожидании до $timeTo"
                                    finalAdditional = if (finalAdditional.isEmpty()) periodNote else "$periodNote\n$finalAdditional"
                                }
                            }
                            
                            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

                            apiService.createRide(
                                CreateRideRequest(
                                    driverId = driverId,
                                    fromCity = from,
                                    toDistrict = to,
                                    routeDescription = finalAdditional,
                                    departureTime = departureTime.format(formatter),
                                    pricePerSeat = priceInt,
                                    seatsAvailable = persons
                                )
                            )

                            published = true
                            onPublish(from, to, persons, additional)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorMessage = strings.connectionFailed
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(Dimens.radiusMed),
                colors = ButtonDefaults.buttonColors(containerColor = WaynixColors.Teal),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        strings.publish,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusMed),
                    color = WaynixColors.Red.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, WaynixColors.Red)
                ) {
                    Column(Modifier.padding(Dimens.paddingSm)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = WaynixColors.Red,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                errorMessage ?: "",
                                color = WaynixColors.Red,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = published) {
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusMed),
                    color = WaynixColors.GreenDot.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, WaynixColors.GreenDot)
                ) {
                    Row(
                        Modifier.padding(Dimens.paddingSm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = WaynixColors.GreenDot,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            strings.yourRidePublished,
                            color = WaynixColors.GreenDot,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showFromSheet) {
        LocationPickerSheet(
            title = strings.fromCity,
            selected = from,
            onSelect = { from = it },
            onDismiss = { showFromSheet = false }
        )
    }
    if (showToSheet) {
        LocationPickerSheet(
            title = strings.toDistrict,
            selected = to,
            onSelect = { to = it },
            onDismiss = { showToSheet = false }
        )
    }
}

@Composable
fun TimeSelector(
    hour: Int,
    minute: Int,
    onUpdate: (Int, Int) -> Unit
) {
    val strings = LocalWaynixStrings.current
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        onClick = { showSheet = true },
        shape = RoundedCornerShape(Dimens.radiusMed),
        color = WaynixColors.White,
        border = BorderStroke(1.dp, WaynixColors.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Filled.Schedule,
                contentDescription = null,
                tint = WaynixColors.Teal,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "%02d:%02d".format(hour, minute),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WaynixColors.TextMain
            )
        }
    }

    if (showSheet) {
        AlertDialog(
            onDismissRequest = { showSheet = false },
            confirmButton = {
                TextButton(onClick = { showSheet = false }) { Text(strings.done, color = WaynixColors.Teal) }
            },
            title = { Text(strings.selectTime) },
            text = {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberPicker(
                        value = hour,
                        range = 0..23,
                        onValueChange = { onUpdate(it, minute) }
                    )
                    Text(" : ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    NumberPicker(
                        value = minute,
                        range = 0..59,
                        step = 5,
                        onValueChange = { onUpdate(hour, it) }
                    )
                }
            }
        )
    }
}

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { 
            val next = if (value + step > range.last) range.first else value + step
            onValueChange(next)
        }) {
            Icon(androidx.compose.material.icons.Icons.Filled.KeyboardArrowUp, null)
        }
        Text(
            "%02d".format(value),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = WaynixColors.Teal
        )
        IconButton(onClick = { 
            val prev = if (value - step < range.first) range.last else value - step
            onValueChange(prev)
        }) {
            Icon(androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown, null)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun PostScreenPreview() {
    PostScreen(onClose = {})
}
