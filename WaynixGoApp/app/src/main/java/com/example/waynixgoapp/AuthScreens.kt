package com.example.waynixgoapp

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waynixgoapp.data.network.ApiService
import com.example.waynixgoapp.data.network.TelegramAuthInitRequest
import com.example.waynixgoapp.data.network.TelegramAuthVerifyRequest
import com.example.waynixgoapp.ui.theme.WaynixColors
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// AUTH STATE MACHINE
// ─────────────────────────────────────────────
sealed class AuthStep {
    object PhoneEntry : AuthStep()
    object CodeEntry : AuthStep()
    object NameEntry : AuthStep()
    object Done : AuthStep()
}

@Composable
fun AuthRoot(onAuthComplete: (phone: String, firstName: String, lastName: String, googleEmail: String) -> Unit) {
    var step by remember { mutableStateOf<AuthStep>(AuthStep.PhoneEntry) }
    var phone by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    var botUrl by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val apiService = remember { ApiService.create() }

    when (step) {
        AuthStep.PhoneEntry -> PhoneEntryScreen(
            phone = phone,
            onChange = { phone = it },
            onSend = {
                scope.launch {
                    try {
                        val resp = apiService.telegramAuthInit(
                            TelegramAuthInitRequest(phone = "+998$phone")
                        )
                        sessionId = resp.sessionId
                        botUrl = resp.botUrl
                        step = AuthStep.CodeEntry
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
        AuthStep.CodeEntry -> CodeEntryScreen(
            phone = phone,
            botUrl = botUrl,
            onBack = { step = AuthStep.PhoneEntry },
            onVerified = { step = AuthStep.NameEntry },
            apiService = apiService,
            sessionId = sessionId
        )
        AuthStep.NameEntry -> NameEntryScreen(
            firstName = firstName,
            lastName = lastName,
            onFirstNameChange = { firstName = it },
            onLastNameChange = { lastName = it },
            onComplete = {
                step = AuthStep.Done
                onAuthComplete("+998$phone", firstName, lastName, "")
            }
        )
        AuthStep.Done -> { }
    }
}

// ─────────────────────────────────────────────
// PHONE ENTRY SCREEN
// ─────────────────────────────────────────────
@Composable
fun PhoneEntryScreen(
    phone: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val isValid = phone.length >= 9

    Box(
        Modifier
            .fillMaxSize()
            .background(WaynixColors.Background)
    ) {
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-30).dp)
                .clip(CircleShape)
                .background(WaynixColors.Teal.copy(alpha = 0.08f))
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(80.dp))

            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(WaynixColors.Teal.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = WaynixColors.Teal, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Ваш номер",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = WaynixColors.TextMain
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Введите номер — мы отправим\nкод подтверждения через Telegram",
                fontSize = 13.sp,
                color = WaynixColors.TextGray,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(32.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = WaynixColors.White,
                border = BorderStroke(1.5.dp, if (isValid) WaynixColors.Teal.copy(alpha = 0.6f) else WaynixColors.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("🇺🇿", fontSize = 18.sp)
                        Text("+998", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = WaynixColors.TextMain)
                    }

                    Box(
                        Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(WaynixColors.Border)
                    )
                    Spacer(Modifier.width(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 9 && it.all { c -> c.isDigit() }) onChange(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("XX XXX XX XX", color = WaynixColors.TextGray.copy(alpha = 0.6f), fontSize = 15.sp)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WaynixColors.TextMain,
                            letterSpacing = 2.sp
                        )
                    )

                    AnimatedVisibility(visible = isValid) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = WaynixColors.Teal, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Пример: 90 123 45 67",
                fontSize = 11.sp,
                color = WaynixColors.TextGray,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onSend,
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WaynixColors.Teal,
                    disabledContainerColor = WaynixColors.Teal.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    "ОТПРАВИТЬ КОД",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────
// CODE ENTRY SCREEN — ввод кода из Telegram бота
// ─────────────────────────────────────────────
@Composable
fun CodeEntryScreen(
    phone: String,
    botUrl: String,
    onBack: () -> Unit,
    onVerified: () -> Unit,
    apiService: ApiService,
    sessionId: String
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun verify() {
        if (code.length != 6 || isLoading) return
        isLoading = true
        errorText = null
        scope.launch {
            try {
                val resp = apiService.telegramAuthVerify(
                    TelegramAuthVerifyRequest(sessionId = sessionId, code = code)
                )
                if (resp.success) {
                    onVerified()
                } else {
                    errorText = "Неверный код"
                    isLoading = false
                }
            } catch (e: Exception) {
                errorText = e.message ?: "Ошибка"
                isLoading = false
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(WaynixColors.Background)
    ) {
        Box(
            Modifier
                .size(180.dp)
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = (-20).dp)
                .clip(CircleShape)
                .background(WaynixColors.Yellow.copy(alpha = 0.15f))
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(56.dp))

            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(WaynixColors.White)
                    .border(1.dp, WaynixColors.Border, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = WaynixColors.TextMain, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(28.dp))

            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(WaynixColors.Yellow.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFB8860B), modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text("Введите код", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = WaynixColors.TextMain)
            Spacer(Modifier.height(4.dp))
            Text(
                "Код отправлен в Telegram-бот @WaynixGo_bot",
                fontSize = 13.sp,
                color = WaynixColors.TextGray,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(16.dp))

            // Кнопка "Открыть Telegram"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0088CC).copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFF0088CC).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(botUrl)
                        )
                        context.startActivity(intent)
                    }
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, tint = Color(0xFF0088CC), modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Открыть Telegram-бот", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0088CC))
                        Text("Нажмите /start и отправьте номер", fontSize = 11.sp, color = WaynixColors.TextGray)
                    }
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF0088CC), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Поле ввода кода
            OutlinedTextField(
                value = code,
                onValueChange = { v ->
                    if (v.length <= 6 && v.all { it.isDigit() }) {
                        code = v
                        errorText = null
                        if (v.length == 6) verify()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Код из Telegram") },
                placeholder = { Text("000000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WaynixColors.Teal,
                    unfocusedBorderColor = WaynixColors.Border,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    color = WaynixColors.TextMain
                )
            )

            if (errorText != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = WaynixColors.Red, modifier = Modifier.size(16.dp))
                    Text(errorText!!, color = WaynixColors.Red, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { verify() },
                enabled = code.length == 6 && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WaynixColors.Teal,
                    disabledContainerColor = WaynixColors.Teal.copy(alpha = 0.4f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("ПОДТВЕРДИТЬ", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────
// NAME ENTRY SCREEN
// ─────────────────────────────────────────────
@Composable
fun NameEntryScreen(
    firstName: String,
    lastName: String,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    val isValid = firstName.length >= 2 && lastName.length >= 2

    Box(
        Modifier
            .fillMaxSize()
            .background(WaynixColors.Background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(80.dp))

            Text(
                "Как вас зовут?",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = WaynixColors.TextMain
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Представьтесь, чтобы попутчики знали вас",
                fontSize = 13.sp,
                color = WaynixColors.TextGray,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text("Имя") },
                placeholder = { Text("Иван") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WaynixColors.Teal,
                    unfocusedBorderColor = WaynixColors.Border,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Фамилия") },
                placeholder = { Text("Иванов") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WaynixColors.Teal,
                    unfocusedBorderColor = WaynixColors.Border,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onComplete,
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WaynixColors.Teal,
                    disabledContainerColor = WaynixColors.Teal.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    "ГОТОВО",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
