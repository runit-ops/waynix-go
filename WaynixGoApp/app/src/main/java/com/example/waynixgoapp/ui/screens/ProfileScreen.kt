package com.example.waynixgoapp.ui.screens

import androidx.compose.ui.platform.LocalContext
import com.example.waynixgoapp.data.UserPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waynixgoapp.ui.components.WaynixTopBar
import com.example.waynixgoapp.ui.components.noRippleClickable
import com.example.waynixgoapp.ui.theme.Dimens
import com.example.waynixgoapp.ui.theme.LocalWaynixStrings
import com.example.waynixgoapp.ui.theme.WaynixColors

data class ProfileState(
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val language: String = "RU",
    val showLogout: Boolean = false
)

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    onLanguageChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val strings = LocalWaynixStrings.current
    
    var state by remember { 
        mutableStateOf(
            ProfileState(
                firstName = prefs.name,
                lastName = prefs.lastName,
                phone = prefs.phone,
                language = prefs.language
            )
        ) 
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WaynixColors.Background,
        topBar = {
            WaynixTopBar(
                title = strings.profile,
                trailingContent = {
                    Box(
                        Modifier
                            .size(Dimens.avatarSize)
                            .clip(RoundedCornerShape(Dimens.radiusLarge))
                            .background(WaynixColors.Border)
                            .noRippleClickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = WaynixColors.TextMain,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = Dimens.paddingMd,
                end = Dimens.paddingMd,
                top = paddingValues.calculateTopPadding() + Dimens.paddingSm,
                bottom = paddingValues.calculateBottomPadding() + Dimens.paddingLg
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "profile_block") {
                ProfileMainBlock(
                    firstName = state.firstName,
                    lastName = state.lastName,
                    phone = state.phone
                )
            }
            item(key = "settings") {
                SettingsBlock(
                    language = state.language,
                    onLanguageChange = { newLang ->
                        state = state.copy(language = newLang)
                        prefs.language = newLang
                        onLanguageChange(newLang)
                    }
                )
            }
            item(key = "support") {
                SupportBlock()
            }
            item(key = "logout") {
                LogoutButton(onClick = { state = state.copy(showLogout = true) })
            }
        }
    }

    if (state.showLogout) {
        AlertDialog(
            onDismissRequest = { state = state.copy(showLogout = false) },
            title = { Text(strings.areYouSureLogout, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            confirmButton = {
                TextButton(onClick = {
                    state = state.copy(showLogout = false)
                    onLogout()
                }) {
                    Text(strings.logout, color = WaynixColors.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { state = state.copy(showLogout = false) }) {
                    Text(strings.cancel, color = WaynixColors.TextMain)
                }
            },
            containerColor = WaynixColors.White,
            shape = RoundedCornerShape(Dimens.paddingMd)
        )
    }
}

@Composable
fun ProfileMainBlock(
    firstName: String,
    lastName: String,
    phone: String
) {
    val gradient = Brush.linearGradient(
        colors = listOf(WaynixColors.Teal, Color(0xFF00796B))
    )

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Decorative circles
            Box(
                Modifier
                    .size(150.dp)
                    .offset(x = 180.dp, y = (-50).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = WaynixColors.Teal,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Column {
                    Text(
                        text = "$firstName $lastName",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = phone,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = WaynixColors.TextGray,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsBlock(language: String, onLanguageChange: (String) -> Unit) {
    val strings = LocalWaynixStrings.current
    
    Column {
        ProfileSectionLabel(strings.settings.uppercase())
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WaynixColors.White,
            shadowElevation = 2.dp
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WaynixColors.Teal.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Language,
                        contentDescription = null,
                        tint = WaynixColors.Teal,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(strings.language, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = WaynixColors.TextMain)
                Spacer(Modifier.weight(1f))
                
                // Language toggles
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(WaynixColors.Background)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("UZ", "RU").forEach { lang ->
                        val isSelected = language == lang
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) WaynixColors.White else Color.Transparent)
                                .noRippleClickable { onLanguageChange(lang) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = lang,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) WaynixColors.Teal else WaynixColors.TextGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupportBlock() {
    val strings = LocalWaynixStrings.current
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WaynixColors.Yellow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.SupportAgent,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(strings.supportCenter, fontWeight = FontWeight.ExtraBold, color = Color.Black, fontSize = 15.sp)
                Text(strings.sendSuggestions, color = Color.Black.copy(alpha = 0.7f), fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Black)
            }
        }
    }
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    val strings = LocalWaynixStrings.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WaynixColors.White,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .noRippleClickable(onClick = onClick),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, WaynixColors.Red.copy(alpha = 0.1f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = WaynixColors.Red,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                strings.logout,
                color = WaynixColors.Red,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }
    }
}
