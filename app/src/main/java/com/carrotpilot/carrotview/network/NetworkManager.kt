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
            connectionTimeout = 2000,  // 2초로 단축 (빠른 연결)
            readTimeout = 8000  // 8초로 단축
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
     * CarrotPilot 자동 발견 - 모든 IP 대역 지원 (확장 검색)
     */
    suspend fun discoverCarrotPilot(): String? = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🔍 Discovering CarrotPilot...")
            
            // 1. 마지막 연결 주소가 있으면 먼저 시도
            val lastAddress = prefs.lastServerAddress
            if (lastAddress.isNotEmpty() && isCarrotPilotServer(lastAddress)) {
                Log.i(TAG, "✅ CarrotPilot found at last address: $lastAddress")
                return@withContext lastAddress
            }
            
            // 2. 로컬 서브넷 검색
            val localIp = getLocalIpAddress()
            if (localIp != null) {
                Log.i(TAG, "📍 Local IP: $localIp")
                val subnet = localIp.substringBeforeLast(".")
                val discoveredIp = findCarrotPilotInSubnet(subnet)
                
                if (discoveredIp != null) {
                    Log.i(TAG, "✅ CarrotPilot discovered in local subnet: $discoveredIp")
                    prefs.lastServerAddress = discoveredIp
                    return@withContext discoveredIp
                }
            }
            
            // 3. 일반적인 사설 IP 대역 검색 (로컬 서브넷에서 못 찾은 경우)
            Log.i(TAG, "🔍 Searching common private IP ranges...")
            val commonSubnets = listOf(
                "192.168.43",   // Android 핫스팟 기본
                "192.168.1",    // 가장 일반적인 홈 네트워크
                "192.168.0",    // 두 번째로 일반적
                "10.0.0",       // 일부 라우터
                "10.0.1",       // 일부 라우터
                "172.16.0",     // 기업 네트워크
                "192.168.100"   // 일부 ISP
            )
            
            for (subnet in commonSubnets) {
                if (localIp != null && subnet == localIp.substringBeforeLast(".")) {
                    continue  // 이미 검색한 서브넷은 스킵
                }
                
                Log.d(TAG, "🔍 Checking subnet: $subnet.0/24")
                val discoveredIp = findCarrotPilotInSubnet(subnet)
                if (discoveredIp != null) {
                    Log.i(TAG, "✅ CarrotPilot discovered at: $discoveredIp")
                    prefs.lastServerAddress = discoveredIp
                    return@withContext discoveredIp
                }
            }
            
            Log.w(TAG, "❌ CarrotPilot not found in any subnet")
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Discovery error: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * 서브넷에서 CarrotPilot 찾기 - 모든 IP 대역 지원 (빠른 검색)
     */
    private suspend fun findCarrotPilotInSubnet(subnet: String): String? = withContext(Dispatchers.IO) {
        // 우선순위 IP 목록 (일반적인 CarrotPilot IP)
        val priorityIps = listOf(
            "$subnet.1",      // 게이트웨이/핫스팟 (가장 일반적)
            "$subnet.100",    // 일반적인 고정 IP
            "$subnet.10",     // 일부 라우터
            "$subnet.254",    // 마지막 주소
            "$subnet.2"       // 두 번째 주소
        )
        
        // 우선순위 IP 먼저 순차 확인 (빠른 발견)
        for (ip in priorityIps) {
            if (isCarrotPilotServer(ip)) {
                Log.i(TAG, "✅ Found CarrotPilot at priority IP: $ip")
                return@withContext ip
            }
        }
        
        // 병렬로 전체 서브넷 스캔 (x.x.x.1 ~ x.x.x.254)
        Log.i(TAG, "🔍 Scanning full subnet: $subnet.0/24")
        val jobs = mutableListOf<Deferred<String?>>()
        
        for (i in 1..254) {
            val ip = "$subnet.$i"
            if (priorityIps.contains(ip)) continue  // 이미 확인한 IP는 스킵
            
            val job = async {
                if (isCarrotPilotServer(ip)) ip else null
            }
            jobs.add(job)
        }
        
        // 첫 번째로 발견된 서버 반환
        for (job in jobs) {
            val result = job.await()
            if (result != null) {
                Log.i(TAG, "✅ Found CarrotPilot at: $result")
                // 나머지 작업 취소
                jobs.forEach { it.cancel() }
                return@withContext result
            }
        }
        
        Log.w(TAG, "❌ CarrotPilot not found in subnet $subnet")
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
                socket.soTimeout = 800  // 0.8초 타임아웃 (빠른 검색)
                socket.connect(InetSocketAddress(ip, port), 800)
                
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
                // 로그 레벨을 낮춤 (너무 많은 로그 방지)
                return@withContext false
            }
            
        } catch (e: Exception) {
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
                
                // 데이터 유효성 검증 (완화된 검증)
                if (isValidData(drivingData)) {
                    _drivingData.value = drivingData
                    
                    // 통계 업데이트
                    messagesReceived++
                    bytesReceived += data.length
                    lastDataTime = System.currentTimeMillis()
                    
                    Log.d(TAG, "✅ Data received: vEgo=${String.format("%.1f", drivingData.carState.vEgo * 3.6f)} km/h, " +
                            "enabled=${drivingData.controlsState.enabled}, " +
                            "active=${drivingData.controlsState.active}, " +
                            "alert=${drivingData.controlsState.alertText}")
                } else {
                    Log.w(TAG, "⚠️ Invalid data received: vEgo=${drivingData.carState.vEgo}, timestamp=${drivingData.timestamp}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error parsing data: ${e.message}", e)
                Log.e(TAG, "Raw data: ${data.take(200)}...")  // 처음 200자만 로그
            }
        }
    }
    
    /**
     * 데이터 유효성 검증 (완화된 검증)
     */
    private fun isValidData(data: DrivingData): Boolean {
        // 타임스탬프 확인 (현재 시간 기준 ±5분으로 완화)
        val currentTime = System.currentTimeMillis()
        val timeDiff = Math.abs(data.timestamp - currentTime)
        if (timeDiff > 300000) {  // 5분
            Log.w(TAG, "⚠️ Timestamp out of range: ${timeDiff}ms difference")
            // 타임스탬프가 이상해도 데이터는 받아들임 (경고만 출력)
        }
        
        // 속도 데이터 확인 (더 넓은 범위)
        if (data.carState.vEgo < -1 || data.carState.vEgo > 200) {  // m/s (720 km/h까지 허용)
            Log.w(TAG, "⚠️ Speed out of range: ${data.carState.vEgo} m/s")
            return false
        }
        
        // 기본적인 데이터 구조만 확인
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
