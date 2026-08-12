package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager as SystemWifiManager
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
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.NetworkInterface

data class DiscoveredWifiRoom(
    val serviceName: String,
    val hostAddress: String,
    val port: Int,
    val isPasswordProtected: Boolean = false
)

class WifiManager(private val context: Context) {

    private val SERVICE_TYPE = "_chessgame._tcp."
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredWifiRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<DiscoveredWifiRoom>> = _discoveredRooms

    var isHost: Boolean = false
        private set

    private val _incomingMessages = MutableSharedFlow<NetworkMessage>()
    val incomingMessages: SharedFlow<NetworkMessage> = _incomingMessages

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses.toList()
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WifiManager", "Error getting local IP", e)
        }
        return "127.0.0.1"
    }

    fun hostRoom(roomName: String = "Chess Room", port: Int = 8888) {
        isHost = true
        disconnect()
        _connectionState.value = ConnectionState.Advertising

        ioScope.launch {
            try {
                val sSocket = ServerSocket(port)
                serverSocket = sSocket
                val actualPort = sSocket.localPort

                registerNsdService(roomName, actualPort)

                _connectionState.value = ConnectionState.Advertising

                val clientSocket = sSocket.accept() // Blocks until client connects
                activeSocket = clientSocket
                setupStreams(clientSocket)

                unregisterNsdService()

                _connectionState.value = ConnectionState.Connected(
                    clientSocket.inetAddress.hostAddress ?: "Wi-Fi Player"
                )
            } catch (e: Exception) {
                if (_connectionState.value !is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Error("Wi-Fi Host Failed: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun registerNsdService(serviceName: String, port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d("WifiManager", "NSD Service registered: ${NsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e("WifiManager", "NSD Registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e("WifiManager", "Failed to register NSD service", e)
        }
    }

    fun startRoomDiscovery() {
        stopRoomDiscovery()
        _discoveredRooms.value = emptyList()
        _connectionState.value = ConnectionState.Discovering

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("WifiManager", "NSD Discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("chessgame")) {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e("WifiManager", "NSD Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val host = serviceInfo.host?.hostAddress ?: return
                            val room = DiscoveredWifiRoom(
                                serviceName = serviceInfo.serviceName,
                                hostAddress = host,
                                port = serviceInfo.port
                            )
                            val current = _discoveredRooms.value.toMutableList()
                            if (current.none { it.hostAddress == host && it.port == serviceInfo.port }) {
                                current.add(room)
                                _discoveredRooms.value = current
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                val current = _discoveredRooms.value.filterNot { it.serviceName == service.serviceName }
                _discoveredRooms.value = current
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Discovery failed: ${e.localizedMessage}")
        }
    }

    fun stopRoomDiscovery() {
        try {
            discoveryListener?.let {
                nsdManager.stopServiceDiscovery(it)
                discoveryListener = null
            }
            if (_connectionState.value is ConnectionState.Discovering) {
                _connectionState.value = ConnectionState.Idle
            }
        } catch (e: Exception) {
            Log.e("WifiManager", "Error stopping NSD discovery", e)
        }
    }

    fun connectToRoom(hostIp: String, port: Int = 8888) {
        isHost = false
        stopRoomDiscovery()
        disconnect()

        _connectionState.value = ConnectionState.Connecting

        ioScope.launch {
            try {
                val inetAddr = InetAddress.getByName(hostIp)
                val socket = Socket(inetAddr, port)
                activeSocket = socket
                setupStreams(socket)

                _connectionState.value = ConnectionState.Connected(hostIp)
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error("Wi-Fi Connection Failed: ${e.localizedMessage}")
            }
        }
    }

    private fun setupStreams(socket: Socket) {
        try {
            reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)

            ioScope.launch {
                listenForData()
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Stream setup failed: ${e.localizedMessage}")
        }
    }

    private suspend fun listenForData() {
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
            Log.e("WifiManager", "Wi-Fi read loop ended", e)
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
                Log.e("WifiManager", "Failed to send message", e)
            }
        }
    }

    private fun unregisterNsdService() {
        try {
            registrationListener?.let {
                nsdManager.unregisterService(it)
                registrationListener = null
            }
        } catch (e: Exception) {
            Log.e("WifiManager", "Error unregistering NSD", e)
        }
    }

    fun disconnect() {
        try {
            unregisterNsdService()
            stopRoomDiscovery()
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
            Log.e("WifiManager", "Error during Wi-Fi disconnect", e)
        }
    }
}
