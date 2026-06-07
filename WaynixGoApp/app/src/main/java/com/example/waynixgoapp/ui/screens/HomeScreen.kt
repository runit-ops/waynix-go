package com.example.waynixgoapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.waynixgoapp.ui.components.*
import com.example.waynixgoapp.ui.theme.*

// HOME SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
    onFromSelected: (String) -> Unit = {},
    onToSelected: (String) -> Unit = {}
) {
    val strings = LocalWaynixStrings.current
    
    // Обновляем список при каждом заходе на экран
    LaunchedEffect(Unit) {
        viewModel.refreshRides()
    }

    val rides by viewModel.rides.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var fromText by remember { mutableStateOf("(Все)") }
    var toText by remember { mutableStateOf("(Все)") }
    var dayTab by remember { mutableIntStateOf(0) }
    var showFromSheet by remember { mutableStateOf(false) }
    var showToSheet by remember { mutableStateOf(false) }

    // Профильтрация
    val filteredRides = remember(fromText, toText, rides) {
        rides.filter { ride ->
            val matchesFrom = fromText == "(Все)" || ride.from == fromText
            val matchesTo = toText == "(Все)" || ride.to == toText
            matchesFrom && matchesTo
        }
    }

    Column(Modifier.fillMaxSize()) {
        WaynixTopBar(trailingContent = { ProfileAvatarButton(onClick = onProfileClick) })

        val pullToRefreshState = rememberPullToRefreshState()
        
        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isLoading,
            onRefresh = {
                viewModel.refreshRides()
            },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Dimens.paddingMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.paddingSm)
            ) {
                item {
                    WaynixDropdown(
                        label = strings.fromCity,
                        value = fromText,
                        icon = Icons.Filled.LocationOn,
                        onClick = { showFromSheet = true }
                    )
                }

                item {
                    SwapButton(onSwap = {
                        val tmp = fromText
                        fromText = toText
                        toText = tmp
                    })
                }

                item {
                    WaynixDropdown(
                        label = strings.toDistrict,
                        value = toText,
                        icon = Icons.Filled.Navigation,
                        onClick = { showToSheet = true }
                    )
                }

                item {
                    DayTabRow(selected = dayTab, onSelect = { dayTab = it })
                }

                item {
                    Text(
                        "${strings.adsDrivers} (${filteredRides.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = WaynixColors.Yellow,
                        letterSpacing = 0.5.sp
                    )
                }

                if (filteredRides.isEmpty()) {
                    item {
                        EmptyState(icon = Icons.Filled.Search, message = strings.noDrivers)
                    }
                } else {
                    items(filteredRides, key = { it.id ?: it.hashCode() }) { ride ->
                        RideCard(ride = ride)
                    }
                }
            }
        }
    }

    if (showFromSheet) {
        LocationPickerSheet(
            title = strings.fromCity,
            selected = fromText,
            onSelect = {
                fromText = it
                onFromSelected(it)
            },
            onDismiss = { showFromSheet = false }
        )
    }
    if (showToSheet) {
        LocationPickerSheet(
            title = strings.toDistrict,
            selected = toText,
            onSelect = {
                toText = it
                onToSelected(it)
            },
            onDismiss = { showToSheet = false }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(onProfileClick = {})
}
