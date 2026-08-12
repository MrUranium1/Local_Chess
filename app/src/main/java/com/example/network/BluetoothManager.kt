package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.UUID

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Discovering : ConnectionState()
    object Advertising : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val deviceName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter

    private val appUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices

    var isHost: Boolean = false
        private set

    private val _incomingMessages = MutableSharedFlow<NetworkMessage>()
    val incomingMessages: SharedFlow<NetworkMessage> = _incomingMessages

    private var serverSocket: BluetoothServerSocket? = null
    private var activeSocket: BluetoothSocket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    private var ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val isBluetoothAvailable: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    fun getPairedDevices(): List<BluetoothDevice> {
        if (!isBluetoothEnabled) return emptyList()
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun startDiscovery() {
        if (!isBluetoothEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is turned off.")
            return
        }
        try {
            _discoveredDevices.value = emptyList()
            _connectionState.value = ConnectionState.Discovering
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter?.startDiscovery()
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Failed to start Bluetooth discovery: ${e.message}")
        }
    }

    fun stopDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            if (_connectionState.value is ConnectionState.Discovering) {
                _connectionState.value = ConnectionState.Idle
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Error stopping discovery", e)
        }
    }

    fun addDiscoveredDevice(device: BluetoothDevice) {
        val current = _discoveredDevices.value.toMutableList()
        if (current.none { it.address == device.address }) {
            current.add(device)
            _discoveredDevices.value = current
        }
    }

    fun hostRoom(roomName: String = "Chess Match") {
        isHost = true
        if (!isBluetoothEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is disabled")
            return
        }

        stopDiscovery()
        disconnect()

        _connectionState.value = ConnectionState.Advertising

        ioScope.launch {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(roomName, appUuid)
                val socket = serverSocket?.accept() // Blocks until client connects
                serverSocket?.close()
                serverSocket = null

                if (socket != null) {
                    activeSocket = socket
                    setupSocketStreams(socket)
                    _connectionState.value = ConnectionState.Connected(socket.remoteDevice.name ?: "Peer Device")
                }
            } catch (e: Exception) {
                if (_connectionState.value !is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Error("Hosting failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        isHost = false
        if (!isBluetoothEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is disabled")
            return
        }

        stopDiscovery()
        disconnect()

        _connectionState.value = ConnectionState.Connecting

        ioScope.launch {
            try {
                val socket = device.createRfcommSocketToServiceRecord(appUuid)
                socket.connect()
                activeSocket = socket
                setupSocketStreams(socket)
                _connectionState.value = ConnectionState.Connected(device.name ?: "Peer Device")
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error("Connection failed: ${e.localizedMessage}")
            }
        }
    }

    private fun setupSocketStreams(socket: BluetoothSocket) {
        try {
            reader = BufferedReader(InputStreamReader(socket.inputStream))
            writer = PrintWriter(OutputStreamWriter(socket.outputStream), true)

            // Start listening thread
            ioScope.launch {
                listenForIncomingData()
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Stream setup failed: ${e.localizedMessage}")
        }
    }

    private suspend fun listenForIncomingData() {
        try {
            var line: String? = null
            while (currentCoroutineContext().isActive && reader?.readLine().also { line = it } != null) {
                val json = line ?: continue
                val msg = NetworkMessage.fromJson(json)
                if (msg != null) {
                    _incomingMessages.emit(msg)
                }
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Read loop ended", e)
        } finally {
            if (_connectionState.value is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.Error("Connection lost")
            }
        }
    }

    fun sendMessage(msg: NetworkMessage) {
        ioScope.launch {
            try {
                writer?.println(msg.toJson())
            } catch (e: Exception) {
                Log.e("BluetoothManager", "Failed to send message", e)
            }
        }
    }

    fun disconnect() {
        try {
            serverSocket?.close()
            serverSocket = null
            activeSocket?.close()
            activeSocket = null
            reader?.close()
            reader = null
            writer?.close()
            writer = null
            _connectionState.value = ConnectionState.Idle
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Error during disconnect", e)
        }
    }
}
