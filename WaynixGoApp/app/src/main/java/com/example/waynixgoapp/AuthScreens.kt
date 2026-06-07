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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.*
import com.example.waynixgoapp.ui.components.noRippleClickable
import com.example.waynixgoapp.ui.theme.WaynixColors
import com.example.waynixgoapp.ui.theme.LocalWaynixStrings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.FirebaseException
import java.util.concurrent.TimeUnit
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// AUTH STATE MACHINE
// ─────────────────────────────────────────────
sealed class AuthStep {
    object Onboarding : AuthStep()
    object PhoneEntry : AuthStep()
    object OtpVerify : AuthStep()
    object GoogleLink : AuthStep()
    object NameEntry : AuthStep()
    object Done : AuthStep()
}

@Composable
fun AuthRoot(onAuthComplete: (phone: String, firstName: String, lastName: String, googleEmail: String) -> Unit) {
    var step by remember { mutableStateOf<AuthStep>(AuthStep.Onboarding) }
    var phone by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var storedVerificationId by remember { mutableStateOf("") }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var googleEmail by remember { mutableStateOf("") }

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val strings = LocalWaynixStrings.current

    val sendOtp = { phoneNumber: String ->
        if (activity != null) {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-retrieval or Instant verification
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener(activity) { task ->
                            if (task.isSuccessful) {
                                step = AuthStep.GoogleLink
                            } else {
                                Toast.makeText(context, strings.invalidCode, Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(context, strings.connectionFailed, Toast.LENGTH_LONG).show()
                }
                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    storedVerificationId = verificationId
                    resendToken = token
                    step = AuthStep.OtpVerify
                }
            }
            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
            resendToken?.let { options.setForceResendingToken(it) }
            PhoneAuthProvider.verifyPhoneNumber(options.build())
        }
    }

    when (step) {
        AuthStep.Onboarding -> OnboardingScreen(
            onGoogleSignIn = { step = AuthStep.PhoneEntry },
            onPhoneSignIn  = { step = AuthStep.PhoneEntry }
        )
        AuthStep.PhoneEntry -> PhoneEntryScreen(
            phone    = phone,
            onChange = { phone = it },
            onBack   = { step = AuthStep.Onboarding },
            onSend   = { 
                val fullPhone = "+998$phone"
                sendOtp(fullPhone)
            }
        )
        AuthStep.OtpVerify -> OtpScreen(
            phone  = phone,
            onBack = { step = AuthStep.PhoneEntry },
            verificationId = storedVerificationId,
            activity = activity,
            onResend = {
                sendOtp("+998$phone")
            },
            onVerified = { step = AuthStep.GoogleLink }
        )
        AuthStep.GoogleLink -> GoogleLinkScreen(
            onLinked = { email ->
                googleEmail = email
                step = AuthStep.NameEntry
            },
            onSkip = { step = AuthStep.NameEntry }
        )
        AuthStep.NameEntry -> NameEntryScreen(
            firstName = firstName,
            lastName = lastName,
            onFirstNameChange = { firstName = it },
            onLastNameChange = { lastName = it },
            onComplete = {
                step = AuthStep.Done
                onAuthComplete("+998$phone", firstName, lastName, googleEmail)
            }
        )
        AuthStep.Done -> { }
    }
}

// ─────────────────────────────────────────────
// GOOGLE LINK SCREEN — 2FA-style: link a Google account to the phone user.
// Phone is the primary login; Google is the secondary provider.
// ─────────────────────────────────────────────
@Composable
fun GoogleLinkScreen(
    onLinked: (email: String) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalWaynixStrings.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

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
                    .background(WaynixColors.Yellow.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = Color(0xFFB8860B),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                strings.linkGoogleTitle,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = WaynixColors.TextMain
            )
            Spacer(Modifier.height(4.dp))
            Text(
                strings.linkGoogleDesc,
                fontSize = 13.sp,
                color = WaynixColors.TextGray,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WaynixColors.Teal.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, WaynixColors.Teal.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = WaynixColors.Teal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        strings.whyLinkGoogle,
                        fontSize = 11.sp,
                        color = WaynixColors.Teal,
                        lineHeight = 16.sp
                    )
                }
            }

            AnimatedVisibility(visible = errorText != null) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = WaynixColors.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            errorText.orEmpty(),
                            color = WaynixColors.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (isLoading) return@Button
                    isLoading = true
                    errorText = null
                    scope.launch {
                        when (val r = com.example.waynixgoapp.auth.GoogleLinker
                            .linkGoogleToCurrentUser(context)) {
                            is com.example.waynixgoapp.auth.GoogleLinker.Result.Success -> {
                                isLoading = false
                                onLinked(r.email)
                            }
                            com.example.waynixgoapp.auth.GoogleLinker.Result.AlreadyLinkedToOtherUser -> {
                                isLoading = false
                                errorText = strings.googleAlreadyLinkedToOther
                            }
                            com.example.waynixgoapp.auth.GoogleLinker.Result.Cancelled -> {
                                isLoading = false
                            }
                            com.example.waynixgoapp.auth.GoogleLinker.Result.NoCurrentUser -> {
                                isLoading = false
                                errorText = strings.googleLinkFailed
                            }
                            is com.example.waynixgoapp.auth.GoogleLinker.Result.Error -> {
                                r.cause.printStackTrace()
                                isLoading = false
                                errorText = strings.googleLinkFailed
                            }
                        }
                    }
                },
                enabled = !isLoading,
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
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    GoogleGLogo(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        strings.continueWithGoogle,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onSkip,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    strings.skipForNow,
                    color = WaynixColors.TextGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─────────────────────────────────────────────
// ONBOARDING / WELCOME SCREEN
// ─────────────────────────────────────────────
@Composable
fun OnboardingScreen(
    onGoogleSignIn: () -> Unit,
    onPhoneSignIn: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(WaynixColors.Background)
    ) {
        // Декоративные круги
        Box(
            Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = (-60).dp)
                .clip(CircleShape)
                .background(WaynixColors.Teal.copy(alpha = 0.08f))
        )
        Box(
            Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .clip(CircleShape)
                .background(WaynixColors.Yellow.copy(alpha = 0.18f))
        )
        Box(
            Modifier
                .size(160.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 30.dp)
                .clip(CircleShape)
                .background(WaynixColors.Teal.copy(alpha = 0.06f))
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // Логотип
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(WaynixColors.Yellow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Navigation,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            val strings = LocalWaynixStrings.current
            Text(
                strings.appName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = 3.sp,
                color = WaynixColors.TextMain
            )

            Spacer(Modifier.height(6.dp))

            Text(
                strings.findCompanion,
                fontSize = 14.sp,
                color = WaynixColors.TextGray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(56.dp))

            // Иллюстрация-плашка
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = WaynixColors.White,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OnboardingFeatureRow(
                        icon  = Icons.Filled.DirectionsCar,
                        title = strings.fastSearch,
                        desc  = strings.fastSearchDesc
                    )
                    HorizontalDivider(color = WaynixColors.Border)
                    OnboardingFeatureRow(
                        icon  = Icons.Filled.PeopleAlt,
                        title = strings.reliableCompanions,
                        desc  = strings.reliableCompanionsDesc
                    )
                    HorizontalDivider(color = WaynixColors.Border)
                    OnboardingFeatureRow(
                        icon  = Icons.Filled.LocationOn,
                        title = strings.allDistricts,
                        desc  = strings.allDistrictsDesc
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Кнопка Google
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = WaynixColors.White,
                border = BorderStroke(1.5.dp, WaynixColors.Border),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable(onClick = onGoogleSignIn)
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GoogleGLogo(modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Google",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = WaynixColors.TextMain
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Кнопка телефон
            Button(
                onClick = onPhoneSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WaynixColors.Teal)
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                val strings = LocalWaynixStrings.current
                Text(
                    strings.loginWithNumber,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Продолжая, вы соглашаетесь с Условиями\nиспользования и Политикой конфиденциальности",
                fontSize = 11.sp,
                color = WaynixColors.TextGray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(WaynixColors.Teal.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = WaynixColors.Teal, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = WaynixColors.TextMain)
            Text(desc, fontSize = 11.sp, color = WaynixColors.TextGray)
        }
    }
}

// Google «G» логотип через Canvas/Box

@Composable
fun GoogleGLogo(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(Color(0xFFFFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        // Упрощённый вариант — разноцветный текст «G»
        Text(
            "G",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF4285F4) // Google синий
        )
    }
}

// ─────────────────────────────────────────────
// PHONE ENTRY SCREEN
// ─────────────────────────────────────────────
@Composable
fun PhoneEntryScreen(
    phone: String,
    onChange: (String) -> Unit,
    onBack: () -> Unit,
    onSend: () -> Unit
) {
    val isValid = phone.length >= 9

    Box(
        Modifier
            .fillMaxSize()
            .background(WaynixColors.Background)
    ) {
        // Декоративный круг сверху
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
            Spacer(Modifier.height(56.dp))

            // Back button
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(WaynixColors.White)
                    .border(1.dp, WaynixColors.Border, CircleShape)
                    .noRippleClickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = WaynixColors.TextMain, modifier = Modifier.size(18.dp))
            }

            Spacer(Modifier.height(28.dp))

            // Иконка телефона
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
                "Введите номер — мы отправим\nкод подтверждения",
                fontSize = 13.sp,
                color = WaynixColors.TextGray,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(32.dp))

            // Поле ввода с флагом и кодом страны
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
                    // Флаг + код
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("🇺🇿", fontSize = 18.sp)
                        Text("+998", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = WaynixColors.TextMain)
                    }

                    // Разделитель
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
                            focusedBorderColor   = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor   = Color.Transparent,
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
// OTP VERIFICATION SCREEN
// ─────────────────────────────────────────────
@Composable
fun OtpScreen(
    phone: String,
    onBack: () -> Unit,
    verificationId: String = "",
    activity: android.app.Activity? = null,
    onResend: () -> Unit,
    onVerified: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = LocalWaynixStrings.current

    // Фейковый таймер обратного отсчёта
    var secondsLeft by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft--
        }
    }

    fun verify() {
        if (otp.length == 6 && verificationId.isNotEmpty() && activity != null) {
            isVerifying = true
            val credential = com.google.firebase.auth.PhoneAuthProvider.getCredential(verificationId, otp)
            com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        onVerified()
                    } else {
                        hasError = true
                        otp = ""
                        isVerifying = false
                    }
                }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(WaynixColors.Background)
    ) {
        // Декор
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
                    .noRippleClickable(onClick = onBack),
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

            Text(strings.enterOtp, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = WaynixColors.TextMain)
            Spacer(Modifier.height(4.dp))
            Text(
                "${strings.weSentSms}+998 $phone",
                fontSize = 13.sp,
                color = WaynixColors.TextGray,
                lineHeight = 18.sp
            )

            // Уведомление-заглушка
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = WaynixColors.Teal.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, WaynixColors.Teal.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Sms, contentDescription = null, tint = WaynixColors.Teal, modifier = Modifier.size(18.dp))
                    Text(
                        strings.waitSms,
                        fontSize = 11.sp,
                        color = WaynixColors.Teal,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // OTP поля
            OtpInputRow(
                otp      = otp,
                hasError = hasError,
                onOtpChange = {
                    hasError = false
                    otp = it
                    if (it.length == 6) verify()
                }
            )

            AnimatedVisibility(visible = hasError) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = WaynixColors.Red, modifier = Modifier.size(16.dp))
                    Text(strings.invalidCode, color = WaynixColors.Red, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Повторная отправка
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (secondsLeft > 0) {
                    Text(
                        "Отправить повторно через ${secondsLeft}с",
                        fontSize = 12.sp,
                        color = WaynixColors.TextGray
                    )
                } else {
                    Text(
                        "Отправить повторно",
                        fontSize = 12.sp,
                        color = WaynixColors.Teal,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.noRippleClickable {
                            secondsLeft = 60
                            otp = ""
                            hasError = false
                            onResend()
                        }
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { verify() },
                enabled = otp.length == 6 && !isVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WaynixColors.Teal,
                    disabledContainerColor = WaynixColors.Teal.copy(alpha = 0.4f)
                )
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(strings.confirmCode, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp, color = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                strings.testCodeHint,
                fontSize = 11.sp,
                color = WaynixColors.TextGray.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// 6 отдельных OTP-ячеек
@Composable
fun OtpInputRow(
    otp: String,
    hasError: Boolean,
    onOtpChange: (String) -> Unit
) {
    // Один скрытый input + визуальные ячейки — стандартный подход
    val boxColor = if (hasError) WaynixColors.Red else WaynixColors.Teal

    Box(Modifier.fillMaxWidth()) {
        // Скрытое поле ввода на весь экран — прозрачное, поверх ячеек
        OutlinedTextField(
            value = otp,
            onValueChange = { v ->
                if (v.length <= 6 && v.all { it.isDigit() }) onOtpChange(v)
            },
            modifier = Modifier
                .matchParentSize()
                .alpha(0.01f), // почти невидимый, но фокусируемый
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )

        // Визуальные ячейки поверх
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(6) { index ->
                val char = otp.getOrNull(index)?.toString() ?: ""
                val isFocused = index == otp.length
                val borderColor = when {
                    hasError   -> WaynixColors.Red
                    isFocused  -> WaynixColors.Teal
                    char.isNotEmpty() -> boxColor.copy(alpha = 0.6f)
                    else       -> WaynixColors.Border
                }
                val bgColor = when {
                    hasError && char.isNotEmpty() -> WaynixColors.Red.copy(alpha = 0.06f)
                    char.isNotEmpty()             -> WaynixColors.Teal.copy(alpha = 0.06f)
                    else                          -> WaynixColors.White
                }
                Box(
                    Modifier
                        .weight(1f)
                        .aspectRatio(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(
                            width = if (isFocused) 2.dp else 1.5.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (char.isNotEmpty()) {
                        Text(
                            char,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (hasError) WaynixColors.Red else WaynixColors.TextMain
                        )
                    } else if (isFocused) {
                        // Мигающий курсор (упрощённый)
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(24.dp)
                                .background(WaynixColors.Teal)
                        )
                    }
                }
            }
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

            val strings = LocalWaynixStrings.current
            Text(
                strings.whatIsYourName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = WaynixColors.TextMain
            )
            Spacer(Modifier.height(4.dp))
            Text(
                strings.introduceYourself,
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
                val strings = LocalWaynixStrings.current
                Text(
                    strings.done,
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

