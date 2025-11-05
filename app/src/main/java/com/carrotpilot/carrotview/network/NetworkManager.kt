package com.carrotpilot.carrotview.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.carrotpilot.carrotview.data.models.DrivingData
import com.carrotpilot.carrotview.data.parser.DataParser
import com.carrotpilot.carrotview.data.preferences.AppPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.Socket
import java.net.InetSocketAddress

/**
 * 네트워크 관리자 - CarrotPilot 연결 및 데이터 수신 관리
 */
class NetworkManager(private val context: Context) {
    
    private var tcpClient: TCPClient? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs = AppPreferences(context)
    
    // 연결 상태
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // 수신된 주행 데이터
    private val _drivingData = MutableStateFlow<DrivingData?>(null)
    val drivingData: StateFlow<DrivingData?> = _drivingData.asStateFlow()
    
    // 연결 통계
    private var messagesReceived = 0L
    private var bytesReceived = 0L
    private var lastDataTime = 0L
    
    companion object {
        private const val TAG = "NetworkManager"
    }
    
    /**
     * CarrotPilot 서버에 연결
     */
    fun connect(serverAddress: String, port: Int? = null) {
        if (tcpClient?.isConnected() == true) {
            Log.w(TAG, "Already connected")
            return
        }
        
        // 네트워크 연결 확인
        if (!isNetworkAvailable()) {
            _connectionState.value = ConnectionState.Error("네트워크 연결이 없습니다")
            return
        }
        
        // 설정에서 기본값 가져오기
        val config = ConnectionConfig(
            serverAddress = serverAddress,
            port = port ?: prefs.serverPort,
            authToken = prefs.authToken,
            autoReconnect = prefs.autoReconnect,
            reconnectInterval = prefs.reconnectInterval,
            connectionTimeout = 3000,  // 3초로 단축
            readTimeout = 10000  // 10초로 단축
        )
        
        // 연결 설정 저장
        prefs.saveConnectionConfig(config)
        
        tcpClient = TCPClient(
            config = config,
            onDataReceived = { data -> handleDataReceived(data) },
            onStateChanged = { state -> handleStateChanged(state) }
        )
        
        tcpClient?.connect()
    }
    
    /**
     * CarrotPilot 자동 발견
     */
    suspend fun discoverCarrotPilot(): String? = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Discovering CarrotPilot...")
            
            // 마지막 연결 주소가 있으면 먼저 시도
            val lastAddress = prefs.lastServerAddress
            if (lastAddress.isNotEmpty() && isCarrotPilotServer(lastAddress)) {
                Log.i(TAG, "CarrotPilot found at last address: $lastAddress")
                return@withContext lastAddress
            }
            
            // 로컬 네트워크에서 CarrotPilot 검색
            val localIp = getLocalIpAddress()
            if (localIp == null) {
                Log.w(TAG, "Could not determine local IP address")
                return@withContext null
            }
            
            val subnet = localIp.substringBeforeLast(".")
            val discoveredIp = findCarrotPilotInSubnet(subnet)
            
            if (discoveredIp != null) {
                Log.i(TAG, "CarrotPilot discovered at: $discoveredIp")
                prefs.lastServerAddress = discoveredIp
            } else {
                Log.w(TAG, "CarrotPilot not found in subnet")
            }
            
            return@withContext discoveredIp
            
        } catch (e: Exception) {
            Log.e(TAG, "Discovery error: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * 서브넷에서 CarrotPilot 찾기
     */
    private suspend fun findCarrotPilotInSubnet(subnet: String): String? = withContext(Dispatchers.IO) {
        val jobs = mutableListOf<Deferred<String?>>()
        
        // 병렬로 IP 스캔 (192.168.x.1 ~ 192.168.x.254)
        for (i in 1..254) {
            val ip = "$subnet.$i"
            val job = async {
                if (isCarrotPilotServer(ip)) ip else null
            }
            jobs.add(job)
        }
        
        // 첫 번째로 발견된 서버 반환
        for (job in jobs) {
            val result = job.await()
            if (result != null) {
                // 나머지 작업 취소
                jobs.forEach { it.cancel() }
                return@withContext result
            }
        }
        
        return@withContext null
    }
    
    /**
     * CarrotPilot 서버인지 확인 (빠른 검색)
     */
    private suspend fun isCarrotPilotServer(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val port = prefs.serverPort
            android.util.Log.d(TAG, "🔍 Checking $ip:$port...")
            
            // 포트 체크 및 CarrotPilot 서버 확인
            val socket = Socket()
            try {
                socket.soTimeout = 2000  // 2초 타임아웃
                socket.connect(InetSocketAddress(ip, port), 2000)
                
                // 서버가 인증 요청을 보내는지 확인 (CarrotPilot 서버는 연결 시 즉시 auth_required 전송)
                val input = socket.getInputStream()
                val lengthBytes = ByteArray(4)
                val read = input.read(lengthBytes, 0, 4)
                
                socket.close()
                
                if (read == 4) {
                    android.util.Log.d(TAG, "✅ Found CarrotPilot at $ip:$port")
                    return@withContext true
                } else {
                    android.util.Log.d(TAG, "❌ Not CarrotPilot server at $ip:$port")
                    return@withContext false
                }
            } catch (e: Exception) {
                try { socket.close() } catch (_: Exception) {}
                android.util.Log.d(TAG, "❌ No service at $ip:$port - ${e.message}")
                return@withContext false
            }
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error checking $ip: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * CarrotPilot 서버인지 확인 (인증 포함 - 사용 안 함)
     */
    private suspend fun isCarrotPilotServerWithAuth(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // TCP 연결 시도
            val testConfig = ConnectionConfig(
                serverAddress = ip,
                port = prefs.serverPort,
                authToken = prefs.authToken,
                autoReconnect = false,
                connectionTimeout = 2000
            )
            
            var isCarrotPilot = false
            val testClient = TCPClient(
                config = testConfig,
                onDataReceived = { },
                onStateChanged = { state ->
                    if (state is ConnectionState.Connected) {
                        isCarrotPilot = true
                    }
                }
            )
            
            testClient.connect()
            delay(2000) // 연결 대기
            testClient.disconnect()
            
            return@withContext isCarrotPilot
            
        } catch (e: Exception) {
            return@withContext false
        }
    }
    
    /**
     * 로컬 IP 주소 가져오기
     */
    private fun getLocalIpAddress(): String? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return null
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null
            
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return null
            }
            
            // WiFi 인터페이스의 IP 주소 찾기
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.name.startsWith("wlan")) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                            return address.hostAddress
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP: ${e.message}", e)
        }
        
        return null
    }
    
    /**
     * 네트워크 연결 확인
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
    
    /**
     * 데이터 수신 처리
     */
    private fun handleDataReceived(data: String) {
        scope.launch {
            try {
                // JSON 파싱
                val drivingData = DataParser.parseJson(data)
                
                // 데이터 유효성 검증
                if (isValidData(drivingData)) {
                    _drivingData.value = drivingData
                    
                    // 통계 업데이트
                    messagesReceived++
                    bytesReceived += data.length
                    lastDataTime = System.currentTimeMillis()
                    
                    Log.d(TAG, "Data received: vEgo=${drivingData.carState.vEgo}, " +
                            "enabled=${drivingData.controlsState.enabled}")
                } else {
                    Log.w(TAG, "Invalid data received")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing data: ${e.message}", e)
            }
        }
    }
    
    /**
     * 데이터 유효성 검증
     */
    private fun isValidData(data: DrivingData): Boolean {
        // 타임스탬프 확인 (현재 시간 기준 ±30초)
        val currentTime = System.currentTimeMillis()
        if (Math.abs(data.timestamp - currentTime) > 30000) {
            return false
        }
        
        // 속도 데이터 확인
        if (data.carState.vEgo < 0 || data.carState.vEgo > 120) {
            return false
        }
        
        return true
    }
    
    /**
     * 연결 상태 변경 처리
     */
    private fun handleStateChanged(state: ConnectionState) {
        _connectionState.value = state
        
        when (state) {
            is ConnectionState.Connected -> {
                Log.i(TAG, "Connected to ${state.serverAddress}:${state.port}")
            }
            is ConnectionState.Disconnected -> {
                Log.i(TAG, "Disconnected")
                _drivingData.value = null
            }
            is ConnectionState.Error -> {
                Log.e(TAG, "Connection error: ${state.message}")
            }
            is ConnectionState.Reconnecting -> {
                Log.i(TAG, "Reconnecting...")
            }
            is ConnectionState.Connecting -> {
                Log.i(TAG, "Connecting...")
            }
        }
    }
    
    /**
     * 연결 해제
     */
    fun disconnect() {
        tcpClient?.disconnect()
        tcpClient = null
        _drivingData.value = null
    }
    
    /**
     * 수동 재연결
     */
    fun reconnect() {
        val currentState = _connectionState.value
        if (currentState is ConnectionState.Connected) {
            val serverAddress = currentState.serverAddress
            val port = currentState.port
            disconnect()
            connect(serverAddress, port)
        }
    }
    
    /**
     * 연결 통계 가져오기
     */
    fun getConnectionStats(): ConnectionStats {
        val connectedTime = if (_connectionState.value is ConnectionState.Connected) {
            System.currentTimeMillis() - lastDataTime
        } else {
            0
        }
        
        return ConnectionStats(
            connectedTime = connectedTime,
            bytesReceived = bytesReceived,
            messagesReceived = messagesReceived,
            reconnectCount = 0,
            lastError = (_connectionState.value as? ConnectionState.Error)?.message
        )
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        disconnect()
        tcpClient?.cleanup()
        scope.cancel()
    }
}
