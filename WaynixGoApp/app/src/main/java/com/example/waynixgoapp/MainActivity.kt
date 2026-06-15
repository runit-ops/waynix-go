package com.example.waynixgoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaynixAppWithAuth()
        }
    }
}

@Composable
fun WaynixAppWithAuth() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { com.example.waynixgoapp.data.UserPreferences(context) }
    
    var isAuthenticated by remember { 
        mutableStateOf(
            prefs.phone.isNotEmpty() && prefs.name.isNotEmpty() && prefs.lastName.isNotEmpty()
        ) 
    }
    
    var language by remember { mutableStateOf(prefs.language) }
    var serverStatus by remember { mutableStateOf<ServerStatus>(ServerStatus.Checking) }
    var serverErrorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val apiService = remember { com.example.waynixgoapp.data.network.ApiService.create() }

    // Check server on start
    LaunchedEffect(Unit) {
        checkServer(apiService) { status, msg ->
            serverStatus = status
            serverErrorMessage = msg
        }
    }

    com.example.waynixgoapp.ui.theme.ProvideWaynixStrings(languageCode = language) {
        when (serverStatus) {
            ServerStatus.Checking -> {
                LoadingScreen()
            }
            ServerStatus.Error -> {
                ConnectionErrorScreen(
                    errorMessage = serverErrorMessage,
                    onRetry = {
                        serverStatus = ServerStatus.Checking
                        serverErrorMessage = null
                        scope.launch {
                            checkServer(apiService) { status, msg ->
                                serverStatus = status
                                serverErrorMessage = msg
                            }
                        }
                    }
                )
            }
            ServerStatus.Ok -> {
                if (isAuthenticated) {
                    WaynixGoApp(
                        onLogout = {
                            prefs.clear()
                            isAuthenticated = false
                        },
                        onLanguageChange = { newLang ->
                            language = newLang
                        }
                    )
                } else {
                    AuthRoot(onAuthComplete = { phone, firstName, lastName, googleEmail ->
                        prefs.phone = phone
                        prefs.name = firstName
                        prefs.lastName = lastName
                        prefs.googleEmail = googleEmail
                        isAuthenticated = true
                    })
                }
            }
        }
    }
}

enum class ServerStatus { Checking, Ok, Error }

suspend fun checkServer(
    apiService: com.example.waynixgoapp.data.network.ApiService,
    onResult: (ServerStatus, String?) -> Unit
) {
    try {
        apiService.healthCheck()
        onResult(ServerStatus.Ok, null)
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(ServerStatus.Error, e.message ?: "Unknown error")
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.waynixgoapp.ui.theme.WaynixColors.Background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = com.example.waynixgoapp.ui.theme.WaynixColors.Teal
        )
    }
}

@Composable
fun ConnectionErrorScreen(errorMessage: String? = null, onRetry: () -> Unit) {
    val strings = com.example.waynixgoapp.ui.theme.LocalWaynixStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.waynixgoapp.ui.theme.WaynixColors.Background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = com.example.waynixgoapp.ui.theme.WaynixColors.Red
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = strings.connectionFailed,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = com.example.waynixgoapp.ui.theme.WaynixColors.TextMain
            )
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = com.example.waynixgoapp.ui.theme.WaynixColors.TextMain.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.waynixgoapp.ui.theme.WaynixColors.Teal
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = strings.retry,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
