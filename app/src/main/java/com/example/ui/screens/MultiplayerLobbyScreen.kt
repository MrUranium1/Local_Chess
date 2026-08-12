package com.example.ui.screens

import com.example.viewmodel.ChessViewModel
import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.GameMode
import com.example.model.PieceColor
import com.example.network.BluetoothManager
import com.example.network.ConnectionState
import com.example.network.DiscoveredWifiRoom
import com.example.network.WifiManager
import com.example.ui.theme.*
import kotlin.random.Random

enum class HostColorChoice {
    WHITE, BLACK, RANDOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerLobbyScreen(
    initialMode: GameMode,
    bluetoothManager: BluetoothManager,
    wifiManager: WifiManager,
    playerName: String,
    viewModel: ChessViewModel,
    onStartGame: (GameMode, PieceColor, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(if (initialMode == GameMode.BLUETOOTH) 0 else 1) }

    val btConnState by bluetoothManager.connectionState.collectAsState()
    val btDiscoveredDevices by bluetoothManager.discoveredDevices.collectAsState()

    val wifiConnState by wifiManager.connectionState.collectAsState()
    val wifiDiscoveredRooms by wifiManager.discoveredRooms.collectAsState()

    // Host Settings State
    var hostColorChoice by remember { mutableStateOf(HostColorChoice.WHITE) }
    var isPasswordProtected by remember { mutableStateOf(false) }
    var hostPassword by remember { mutableStateOf("") }
    var isHostSettingsExpanded by remember { mutableStateOf(false) }

    // Join / Password prompt state
    var manualIpInput by remember { mutableStateOf("") }
    var showJoinPasswordDialog by remember { mutableStateOf(false) }
    var pendingConnectRoom by remember { mutableStateOf<DiscoveredWifiRoom?>(null) }
    var pendingConnectBtDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var joinPasswordInput by remember { mutableStateOf("") }
    var passwordErrorMessage by remember { mutableStateOf<String?>(null) }

    // Check runtime permissions
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
    }

    // Function to calculate final host color
    fun getResolvedHostColor(): PieceColor {
        return when (hostColorChoice) {
            HostColorChoice.WHITE -> PieceColor.WHITE
            HostColorChoice.BLACK -> PieceColor.BLACK
            HostColorChoice.RANDOM -> if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        }
    }

    val activeConnState = if (selectedTab == 0) btConnState else wifiConnState
    val isHost = if (selectedTab == 0) bluetoothManager.isHost else wifiManager.isHost

    // Sync host settings with ViewModel
    LaunchedEffect(isPasswordProtected, hostPassword, hostColorChoice) {
        viewModel.setHostPassword(isPasswordProtected, hostPassword)
        viewModel.setHostPreferredColor(getResolvedHostColor())
    }

    // Client sends JOIN message upon socket connection
    LaunchedEffect(activeConnState) {
        if (activeConnState is ConnectionState.Connected && !isHost) {
            val mode = if (selectedTab == 0) GameMode.BLUETOOTH else GameMode.WIFI
            viewModel.sendJoinRequest(mode)
        }
    }

    // Handle Auth Rejection (wrong password / password required)
    LaunchedEffect(Unit) {
        viewModel.authRejectedEvent.collect { reason ->
            passwordErrorMessage = reason
            showJoinPasswordDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multiplayer Match Lobby", color = GoldPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        bluetoothManager.disconnect()
                        wifiManager.disconnect()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GeoTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Permission Grant Banner if permissions missing
            if (!hasPermissions) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberAccent),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldPrimary))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = GoldPrimary)
                            Text(
                                text = "Permissions Required",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF121410)
                            )
                        }
                        Text(
                            text = "Bluetooth and Location access are required to search and pair with nearby devices.",
                            fontSize = 12.sp,
                            color = Color(0xFF121410)
                        )
                        Button(
                            onClick = { permissionLauncher.launch(requiredPermissions) },
                            modifier = Modifier.height(38.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                        ) {
                            Text("Grant Permissions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tab Selector (Bluetooth vs Wi-Fi)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SlateDarkCard,
                contentColor = GoldPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Bluetooth", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) GoldPrimary else GeoTextSecondary)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Local Wi-Fi", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) GoldPrimary else GeoTextSecondary)
                        }
                    }
                )
            }

            // Connection Status Banner
            ConnectionStatusBanner(
                state = activeConnState,
                onCancel = {
                    if (selectedTab == 0) bluetoothManager.disconnect() else wifiManager.disconnect()
                }
            )

            // COLLAPSIBLE HOST CONFIGURATION CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GeoBorderColor, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isHostSettingsExpanded = !isHostSettingsExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "Host Match Settings (Host Only)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                                val sideText = when (hostColorChoice) {
                                    HostColorChoice.WHITE -> "Side: White ♔"
                                    HostColorChoice.BLACK -> "Side: Black ♚"
                                    HostColorChoice.RANDOM -> "Side: Random 🎲"
                                }
                                val lockText = if (isPasswordProtected) "• 🔒 Password On" else "• Open"
                                Text(
                                    text = "$sideText $lockText",
                                    fontSize = 11.sp,
                                    color = GeoTextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { isHostSettingsExpanded = !isHostSettingsExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isHostSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Settings",
                                tint = GeoTextSecondary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isHostSettingsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Divider(color = GeoBorderColor.copy(alpha = 0.5f), thickness = 1.dp)

                            // Host Color Picker
                            Text(
                                text = "Host Side Selection:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GeoTextPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = hostColorChoice == HostColorChoice.WHITE,
                                    onClick = { hostColorChoice = HostColorChoice.WHITE },
                                    label = { Text("♔ White", fontSize = 12.sp, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = hostColorChoice == HostColorChoice.BLACK,
                                    onClick = { hostColorChoice = HostColorChoice.BLACK },
                                    label = { Text("♚ Black", fontSize = 12.sp, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = hostColorChoice == HostColorChoice.RANDOM,
                                    onClick = { hostColorChoice = HostColorChoice.RANDOM },
                                    label = { Text("🎲 Random", fontSize = 12.sp, maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GoldPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }

                            Text(
                                text = "* Opponent joining your room automatically receives the opposite color.",
                                fontSize = 11.sp,
                                color = GeoTextSecondary
                            )

                            // Host Password Options
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                    Text("Require Password to Join:", fontSize = 12.sp, color = GeoTextPrimary)
                                }

                                Switch(
                                    checked = isPasswordProtected,
                                    onCheckedChange = { isPasswordProtected = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = GoldPrimary,
                                        checkedTrackColor = AmberAccent
                                    )
                                )
                            }

                            AnimatedVisibility(visible = isPasswordProtected) {
                                OutlinedTextField(
                                    value = hostPassword,
                                    onValueChange = { hostPassword = it },
                                    label = { Text("Set Room Password", fontSize = 12.sp) },
                                    placeholder = { Text("Enter room password...", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPrimary,
                                        unfocusedBorderColor = GeoBorderColor,
                                        focusedTextColor = GeoTextPrimary,
                                        unfocusedTextColor = GeoTextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // TAB CONTENT (HOST vs JOIN ACTIONS)
            if (selectedTab == 0) {
                // BLUETOOTH LOBBY
                BluetoothLobbyContent(
                    bluetoothManager = bluetoothManager,
                    connState = btConnState,
                    discoveredDevices = btDiscoveredDevices,
                    onConnectDevice = { device ->
                        pendingConnectBtDevice = device
                        pendingConnectRoom = null
                        if (isPasswordProtected) {
                            showJoinPasswordDialog = true
                        } else {
                            viewModel.setClientJoinPassword("")
                            bluetoothManager.connectToDevice(device)
                        }
                    }
                )
            } else {
                // WI-FI LOBBY
                WifiLobbyContent(
                    context = context,
                    wifiManager = wifiManager,
                    connState = wifiConnState,
                    discoveredRooms = wifiDiscoveredRooms,
                    manualIpInput = manualIpInput,
                    isHostPasswordProtected = isPasswordProtected,
                    hostPassword = hostPassword,
                    onManualIpChange = { manualIpInput = it },
                    onConnectRoom = { room ->
                        pendingConnectRoom = room
                        pendingConnectBtDevice = null
                        if (room.isPasswordProtected) {
                            showJoinPasswordDialog = true
                        } else {
                            viewModel.setClientJoinPassword("")
                            wifiManager.connectToRoom(room.hostAddress, room.port)
                        }
                    }
                )
            }
        }
    }

    // JOIN ROOM PASSWORD DIALOG
    if (showJoinPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showJoinPasswordDialog = false
                pendingConnectRoom = null
                pendingConnectBtDevice = null
                joinPasswordInput = ""
                passwordErrorMessage = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary)
                    Text("Password Protected Room", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GeoTextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This room requires a password to join.",
                        fontSize = 13.sp,
                        color = GeoTextSecondary
                    )
                    OutlinedTextField(
                        value = joinPasswordInput,
                        onValueChange = {
                            joinPasswordInput = it
                            passwordErrorMessage = null
                        },
                        label = { Text("Enter Room Password", fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = GeoBorderColor,
                            focusedTextColor = GeoTextPrimary
                        )
                    )
                    if (passwordErrorMessage != null) {
                        Text(passwordErrorMessage!!, fontSize = 12.sp, color = CrimsonError)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinPasswordInput.isBlank()) {
                            passwordErrorMessage = "Password cannot be empty!"
                            return@Button
                        }
                        viewModel.setClientJoinPassword(joinPasswordInput)
                        showJoinPasswordDialog = false
                        val targetRoom = pendingConnectRoom
                        val targetDevice = pendingConnectBtDevice
                        if (targetRoom != null) {
                            wifiManager.connectToRoom(targetRoom.hostAddress, targetRoom.port)
                        } else if (targetDevice != null) {
                            bluetoothManager.connectToDevice(targetDevice)
                        } else {
                            val mode = if (selectedTab == 0) GameMode.BLUETOOTH else GameMode.WIFI
                            viewModel.sendJoinRequest(mode)
                        }
                        joinPasswordInput = ""
                    },
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) {
                    Text("Join Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showJoinPasswordDialog = false
                        pendingConnectRoom = null
                        pendingConnectBtDevice = null
                        joinPasswordInput = ""
                        passwordErrorMessage = null
                    }
                ) {
                    Text("Cancel", color = GeoTextSecondary, fontSize = 13.sp)
                }
            },
            containerColor = SlateDarkCard
        )
    }
}

@Composable
fun ConnectionStatusBanner(
    state: ConnectionState,
    onCancel: () -> Unit
) {
    val (text, bgColor, textColor) = when (state) {
        is ConnectionState.Idle -> Triple("Ready to Host or Join Match", SlateDarkCard, GeoTextSecondary)
        is ConnectionState.Discovering -> Triple("Scanning for nearby match rooms...", AmberAccent, GoldPrimary)
        is ConnectionState.Advertising -> Triple("Hosting Match & Waiting for Opponent...", AmberAccent, GoldPrimary)
        is ConnectionState.Connecting -> Triple("Connecting to Host...", AmberAccent, GoldPrimary)
        is ConnectionState.Connected -> Triple("Connected to ${state.deviceName}! Starting match...", GoldPrimary, Color.White)
        is ConnectionState.Error -> Triple("Status: ${state.message}", CrimsonError.copy(alpha = 0.15f), CrimsonError)
    }

    val isActiveState = state is ConnectionState.Discovering ||
            state is ConnectionState.Connecting ||
            state is ConnectionState.Advertising ||
            state is ConnectionState.Error

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, GeoBorderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (state is ConnectionState.Discovering || state is ConnectionState.Connecting || state is ConnectionState.Advertising) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = textColor, strokeWidth = 2.dp)
                } else if (state is ConnectionState.Connected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (isActiveState) {
                TextButton(
                    onClick = onCancel,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimsonError)
                }
            }
        }
    }
}

@Composable
fun BluetoothLobbyContent(
    bluetoothManager: BluetoothManager,
    connState: ConnectionState,
    discoveredDevices: List<BluetoothDevice>,
    onConnectDevice: (BluetoothDevice) -> Unit
) {
    val pairedDevices = remember { bluetoothManager.getPairedDevices() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bluetooth Instructions Hint Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GeoBorderColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDarkSurface)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                Text(
                    text = "Ensure Bluetooth is ON. One player taps 'Host Match' while the other scans or picks from Paired Devices.",
                    fontSize = 11.sp,
                    color = GeoTextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // Host & Scan Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { bluetoothManager.hostRoom() },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CellTower, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Host Match", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
            }

            OutlinedButton(
                onClick = { bluetoothManager.startDiscovery() },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoTextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoBorderColor)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoldPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Devices", color = GeoTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.BluetoothConnected, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Text("Paired Bluetooth Devices", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
            }

            if (pairedDevices.isEmpty()) {
                Text("No paired Bluetooth devices found. Pair devices in Android settings or scan below.", fontSize = 12.sp, color = GeoTextSecondary)
            } else {
                pairedDevices.forEach { device ->
                    DeviceItemCard(
                        deviceName = device.name ?: "Unknown Device",
                        address = device.address,
                        badgeText = "Paired",
                        onClick = { onConnectDevice(device) }
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Icon(Icons.Default.Radar, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                Text("Discovered Nearby Devices", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
            }

            if (discoveredDevices.isEmpty()) {
                Text("Tap 'Scan Devices' above to discover nearby un-paired devices.", fontSize = 12.sp, color = GeoTextSecondary)
            } else {
                discoveredDevices.forEach { device ->
                    DeviceItemCard(
                        deviceName = device.name ?: "Discovered Device",
                        address = device.address,
                        badgeText = "Discovered",
                        onClick = { onConnectDevice(device) }
                    )
                }
            }
        }
    }
}

@Composable
fun WifiLobbyContent(
    context: Context,
    wifiManager: WifiManager,
    connState: ConnectionState,
    discoveredRooms: List<DiscoveredWifiRoom>,
    manualIpInput: String,
    isHostPasswordProtected: Boolean,
    hostPassword: String,
    onManualIpChange: (String) -> Unit,
    onConnectRoom: (DiscoveredWifiRoom) -> Unit
) {
    val localIp = remember { wifiManager.getLocalIpAddress() }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Local IP Display Card with Copy Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GeoBorderColor, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDarkCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmberAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WifiTethering, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Your Wi-Fi IP Address:", fontSize = 11.sp, color = GeoTextSecondary)
                        Text(localIp, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    }
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("IP Address", localIp)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "IP Address Copied!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy IP", tint = GoldPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Host & Scan Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val roomTitle = if (isHostPasswordProtected) "Chess Room [🔒 Locked]" else "Chess Room [Open]"
                    wifiManager.hostRoom(roomTitle)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Host Wi-Fi Room", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
            }

            OutlinedButton(
                onClick = { wifiManager.startRoomDiscovery() },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoTextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, GeoBorderColor)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoldPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Rooms", color = GeoTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                }
            }
        }

        // Direct IP Join Block
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GeoBorderColor, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SlateDarkCard)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Direct IP Join (Same Local Wi-Fi)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = manualIpInput,
                        onValueChange = onManualIpChange,
                        placeholder = { Text("e.g. 192.168.1.105", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = GeoBorderColor,
                            focusedTextColor = GeoTextPrimary
                        )
                    )

                    Button(
                        onClick = {
                            if (manualIpInput.isNotBlank()) {
                                wifiManager.connectToRoom(manualIpInput.trim())
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                    ) {
                        Text("Connect", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Text("Discovered Wi-Fi Rooms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (discoveredRooms.isEmpty()) {
                Text("No active Wi-Fi chess rooms discovered yet. Tap 'Scan Rooms' or enter Host IP directly above.", fontSize = 12.sp, color = GeoTextSecondary)
            } else {
                discoveredRooms.forEach { room ->
                    val isLocked = room.serviceName.contains("Locked") || room.serviceName.contains("🔒")
                    DeviceItemCard(
                        deviceName = room.serviceName,
                        address = "${room.hostAddress}:${room.port}",
                        isLocked = isLocked,
                        onClick = { onConnectRoom(room.copy(isPasswordProtected = isLocked)) }
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceItemCard(
    deviceName: String,
    address: String,
    isLocked: Boolean = false,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GeoBorderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SlateDarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(deviceName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GeoTextPrimary)
                    if (isLocked) {
                        Icon(Icons.Default.Lock, contentDescription = "Password Protected", tint = GoldPrimary, modifier = Modifier.size(14.dp))
                    }
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AmberAccent)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(badgeText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        }
                    }
                }
                Text(address, fontSize = 11.sp, color = GeoTextSecondary)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AmberAccent,
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

