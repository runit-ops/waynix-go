package com.example.waynixgoapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.waynixgoapp.ui.components.noRippleClickable
import com.example.waynixgoapp.ui.theme.*

// ─────────────────────────────────────────────
// BOTTOM NAVIGATION BAR
// ─────────────────────────────────────────────
@Composable
fun WaynixBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalWaynixStrings.current
    Surface(
        shadowElevation = 8.dp,
        color = WaynixColors.White,
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Dimens.bottomBarH),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomBarItem(
                icon = Icons.Filled.Home,
                label = strings.home,
                selected = selected == 0,
                onClick = { onSelect(0) },
                modifier = Modifier.weight(1f)
            )

            // Central FAB padding
            Box(
                Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(Dimens.fabSize)
                        .clip(CircleShape)
                        .background(WaynixColors.Teal)
                        .noRippleClickable { onSelect(2) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Post",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            BottomBarItem(
                icon = Icons.Outlined.DirectionsCar,
                label = strings.myAds,
                selected = selected == 1,
                onClick = { onSelect(1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────
// BOTTOM BAR ITEM
// ─────────────────────────────────────────────
@Composable
fun BottomBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) WaynixColors.Teal else WaynixColors.TextGray
    Column(
        modifier
            .noRippleClickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(Dimens.iconMd))
        if (label.isNotEmpty()) {
            Text(label, fontSize = 10.sp, color = color)
        }
    }
}

// ─────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun BottomBarPreview() {
    WaynixBottomBar(selected = 0, onSelect = {})
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun BottomBarItemPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        BottomBarItem(Icons.Filled.Home, "Главная", true, {})
        BottomBarItem(Icons.Outlined.DirectionsCar, "Мои авто", false, {})
    }
}