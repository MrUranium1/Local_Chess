package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.GameMode
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    playerName: String,
    onUpdatePlayerName: (String) -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember(playerName) { mutableStateOf(playerName) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "♔ CHESS",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = GeoTextPrimary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = GeoTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Card with Generated Art
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDarkCard)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.chess_app_icon_1786347481229),
                        contentDescription = "Chess Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.4f
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SlateDarkBackground.copy(alpha = 0.9f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "GRANDMASTER CHESS",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            Text(
                                text = "Local Bluetooth & Wi-Fi Network Match",
                                fontSize = 13.sp,
                                color = GeoTextSecondary
                            )
                        }
                    }
                }
            }

            // Player Profile Name Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoBorderColor, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDarkSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AmberAccent)
                                .border(1.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (isEditingName) {
                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                singleLine = true,
                                modifier = Modifier.width(160.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = GeoBorderColor,
                                    focusedTextColor = GeoTextPrimary
                                )
                            )
                        } else {
                            Column {
                                Text(
                                    text = playerName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoTextPrimary
                                )
                                Text(
                                    text = "Player Profile",
                                    fontSize = 11.sp,
                                    color = GeoTextSecondary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isEditingName) {
                                if (tempName.isNotBlank()) onUpdatePlayerName(tempName.trim())
                                isEditingName = false
                            } else {
                                isEditingName = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditingName) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = GoldPrimary
                        )
                    }
                }
            }

            Text(
                text = "Select Game Mode",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GeoTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Game Mode Buttons
            ModeOptionCard(
                title = "Bluetooth Local Match",
                subtitle = "Play with nearby device via Bluetooth",
                icon = Icons.Default.Bluetooth,
                badge = null,
                accentColor = Color(0xFF38BDF8),
                onClick = { onSelectMode(GameMode.BLUETOOTH) },
                testTag = "mode_bluetooth"
            )

            ModeOptionCard(
                title = "Wi-Fi Local Network Match",
                subtitle = "Connect with device on same Wi-Fi network",
                icon = Icons.Default.Wifi,
                badge = null,
                accentColor = EmeraldSuccess,
                onClick = { onSelectMode(GameMode.WIFI) },
                testTag = "mode_wifi"
            )

            ModeOptionCard(
                title = "Local Pass & Play",
                subtitle = "Two players on a single device screen",
                icon = Icons.Default.People,
                badge = null,
                accentColor = GoldPrimary,
                onClick = { onSelectMode(GameMode.LOCAL_PASS_PLAY) },
                testTag = "mode_pass_play"
            )

            ModeOptionCard(
                title = "Play vs Chess Bot",
                subtitle = "Practice against computer AI (Easy/Med/Hard)",
                icon = Icons.Default.SmartToy,
                badge = null,
                accentColor = Color(0xFFA855F7),
                onClick = { onSelectMode(GameMode.VS_BOT) },
                testTag = "mode_vs_bot"
            )
        }
    }
}

@Composable
fun ModeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String? = null,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GeoBorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDarkCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmberAccent)
                        .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeoTextPrimary
                        )
                        if (!badge.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AmberAccent)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }
                        }
                    }
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = GeoTextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GeoTextSecondary
            )
        }
    }
}
