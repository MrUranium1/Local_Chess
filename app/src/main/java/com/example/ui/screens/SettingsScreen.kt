package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.BoardTheme
import com.example.model.BotLevel
import com.example.model.TimeControl
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentBoardTheme: BoardTheme,
    currentTimeControl: TimeControl,
    currentBotLevel: BotLevel,
    isDarkMode: Boolean,
    currentBgImageUri: String?,
    currentBgDimOpacity: Float,
    onSelectBoardTheme: (BoardTheme) -> Unit,
    onSelectTimeControl: (TimeControl) -> Unit,
    onSelectBotLevel: (BotLevel) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onSelectBgImageUri: (String?) -> Unit,
    onChangeBgDimOpacity: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectBgImageUri(uri.toString())
        }
    }

    val timeControls = listOf(
        TimeControl(3, 0, "3 min Blitz"),
        TimeControl(5, 0, "5 min Blitz"),
        TimeControl(10, 0, "10 min Rapid"),
        TimeControl(15, 0, "15 min Rapid"),
        TimeControl(0, 0, "Unlimited")
    )

    val containerBgColor = if (!currentBgImageUri.isNullOrBlank()) Color.Transparent else SlateDarkBackground

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Settings", color = GoldPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = containerBgColor)
            )
        },
        containerColor = containerBgColor
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // CUSTOM BACKGROUND IMAGE SECTION
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Custom Background Image", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeoBorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SlateDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Current active image preview & controls
                        if (!currentBgImageUri.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.5.dp, GoldPrimary, RoundedCornerShape(8.dp))
                                    ) {
                                        AsyncImage(
                                            model = currentBgImageUri,
                                            contentDescription = "Active Background",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Custom Wallpaper Active",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GeoTextPrimary
                                        )
                                        Text(
                                            text = "Displayed behind chess board",
                                            fontSize = 12.sp,
                                            color = GeoTextSecondary
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onSelectBgImageUri(null) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(CrimsonError)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = GeoBorderColor.copy(alpha = 0.5f))
                        }

                        // Pick image button
                        Button(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Gallery Image", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Dimming Opacity Slider
                        if (!currentBgImageUri.isNullOrBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Background Dim Level", fontSize = 13.sp, color = GeoTextPrimary)
                                    Text("${(currentBgDimOpacity * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                                }
                                Slider(
                                    value = currentBgDimOpacity,
                                    onValueChange = onChangeBgDimOpacity,
                                    valueRange = 0.0f..0.8f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GoldPrimary,
                                        activeTrackColor = GoldPrimary,
                                        inactiveTrackColor = SlateDarkSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // APPEARANCE / DARK MODE SELECTION
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Appearance", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeoBorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SlateDarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = GoldPrimary
                            )
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GeoTextPrimary
                                )
                                Text(
                                    text = if (isDarkMode) "Dark theme active" else "Light theme active",
                                    fontSize = 12.sp,
                                    color = GeoTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = onToggleDarkMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GoldPrimary,
                                uncheckedThumbColor = GeoTextSecondary,
                                uncheckedTrackColor = SlateDarkSurface
                            )
                        )
                    }
                }
            }

            // BOARD THEME SELECTION
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Board Theme", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BoardTheme.values().forEach { theme ->
                        val isSelected = currentBoardTheme == theme
                        val (lightCol, darkCol) = when (theme) {
                            BoardTheme.WOOD -> Pair(WoodLightSquare, WoodDarkSquare)
                            BoardTheme.SLATE -> Pair(SlateLightSquare, SlateDarkSquare)
                            BoardTheme.EMERALD -> Pair(EmeraldLightSquare, EmeraldDarkSquare)
                            BoardTheme.OCEAN -> Pair(OceanLightSquare, OceanDarkSquare)
                            BoardTheme.AMETHYST -> Pair(AmethystLightSquare, AmethystDarkSquare)
                            BoardTheme.CORAL -> Pair(CoralLightSquare, CoralDarkSquare)
                            BoardTheme.SAND -> Pair(SandLightSquare, SandDarkSquare)
                            BoardTheme.CYBER -> Pair(CyberLightSquare, CyberDarkSquare)
                        }

                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SlateDarkCard)
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) GoldPrimary else GeoBorderColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectBoardTheme(theme) },
                            contentAlignment = Alignment.Center
                        ) {
                            // 2x2 Mini Board Swatch Preview
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                Row(modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(lightCol))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(darkCol))
                                }
                                Row(modifier = Modifier.weight(1f)) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(darkCol))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(lightCol))
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // TIME CONTROL SELECTION
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Default Time Control", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GeoBorderColor, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateDarkCard)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        timeControls.forEach { tc ->
                            val isSelected = currentTimeControl.initialMinutes == tc.initialMinutes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectTimeControl(tc) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tc.displayName,
                                    fontSize = 14.sp,
                                    color = if (isSelected) GoldPrimary else GeoTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = GoldPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // BOT DIFFICULTY LEVEL
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Chess Bot Difficulty", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BotLevel.values().forEach { level ->
                        val isSelected = currentBotLevel == level
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectBotLevel(level) },
                            label = { Text(level.name, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}
