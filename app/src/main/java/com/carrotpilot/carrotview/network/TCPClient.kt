package com.carrotpilot.carrotview.network

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream

/**
 * TCP 클라이언트 - CarrotPilot 서버와 통신
 */
class TCPClient(
    private val config: ConnectionConfig,
    private val onDataReceived: (String) -> Unit,
    private val onStateChanged: (ConnectionState) -> Unit
) {
    private var socket: Socket? = null
    private var inputStream: BufferedInputStream? = null
    private var outputStream: BufferedOutputStream? = null
    
    private var receiveJob: Job? = null
    private var reconnectJob: Job? = null
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    @Volatile
    private var isConnected = false
    
    @Volatile
    private var shouldReconnect = true
    
    @Volatile
    private var reconnectCount = 0
    
    private val stats = ConnectionStats()
    
    companion object {
        private const val TAG = "TCPClient"
    }
    
    /**
     * 서버에 연결
     */
    fun connect() {
        if (isConnected) {
            Log.w(TAG, "Already connected")
            return
        }
        
        scope.launch {
            try {
                Log.d(TAG, "🔌 Attempting connection to ${config.serverAddress}:${config.port}")
                onStateChanged(ConnectionState.Connecting)
                
                // 소켓 생성 및 연결
                socket = Socket().apply {
                    soTimeout = config.readTimeout
                    tcpNoDelay = true
                    keepAlive = true
                }
                
                Log.d(TAG, "📡 Connecting socket with ${config.connectionTimeout}ms timeout...")
                withTimeout(config.connectionTimeout.toLong()) {
                    socket?.connect(
                        InetSocketAddress(config.serverAddress, config.port),
                        config.connectionTimeout
                    )
                }
                
                Log.d(TAG, "✅ Socket connected! Setting up streams...")
                inputStream = BufferedInputStream(socket?.getInputStream())
                outputStream = BufferedOutputStream(socket?.getOutputStream())
                
                // 인증 수행
                if (authenticate()) {
                    isConnected = true
                    reconnectCount = 0  // 연결 성공 시 재시도 카운트 리셋
                    onStateChanged(ConnectionState.Connected(config.serverAddress, config.port))
                    
                    // 데이터 수신 시작
                    startReceiving()
                    
                    Log.i(TAG, "Connected to ${config.serverAddress}:${config.port}")
                } else {
                    throw Exception("Authentication failed")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed: ${e.message}", e)
                handleConnectionError(e)
            }
        }
    }
    
    /**
     * 서버 인증
     */
    private suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 인증 요청 수신
            val authRequest = receiveMessage() ?: return@withContext false
            val authJson = JSONObject(authRequest)
            
            if (authJson.getString("type") != "auth_required") {
                return@withContext false
            }
            
            val challenge = authJson.getString("challenge")
            val timestamp = authJson.getLong("timestamp")
            
            // 인증 응답 전송
            val authResponse = JSONObject().apply {
                put("token", "${config.authToken}_$challenge")
                put("timestamp", System.currentTimeMillis() / 1000)  // 초 단위로 변환
            }
            
            sendMessage(authResponse.toString())
            
            // 인증 결과 수신
            val authResult = receiveMessage() ?: return@withContext false
            val resultJson = JSONObject(authResult)
            
            return@withContext resultJson.getString("type") == "auth_success"
            
        } catch (e: Exception) {
            Log.e(TAG, "Authentication error: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * 데이터 수신 시작
     */
    private fun startReceiving() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            while (isActive && isConnected) {
                try {
                    val message = receiveMessage()
                    if (message != null) {
                        onDataReceived(message)
                    } else {
                        // 연결 끊김
                        throw Exception("Connection closed by server")
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Receive error: ${e.message}", e)
                        handleConnectionError(e)
                    }
                    break
                }
            }
        }
    }
    
    /**
     * 메시지 수신
     */
    private suspend fun receiveMessage(): String? = withContext(Dispatchers.IO) {
        try {
            val input = inputStream ?: return@withContext null
            
            // 메시지 길이 읽기 (4바이트)
            val lengthBytes = ByteArray(4)
            var bytesRead = 0
            while (bytesRead < 4) {
                val read = input.read(lengthBytes, bytesRead, 4 - bytesRead)
                if (read == -1) return@withContext null
                bytesRead += read
            }
            
            val length = ByteBuffer.wrap(lengthBytes).int
            if (length <= 0 || length > 10 * 1024 * 1024) { // 최대 10MB
                throw Exception("Invalid message length: $length")
            }
            
            // 데이터 읽기
            val dataBytes = ByteArray(length)
            bytesRead = 0
            while (bytesRead < length) {
                val read = input.read(dataBytes, bytesRead, length - bytesRead)
                if (read == -1) return@withContext null
                bytesRead += read
            }
            
            // 서버 프로토콜: 데이터만 (압축 플래그 없음)
            return@withContext dataBytes.toString(Charsets.UTF_8)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving message: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * 메시지 전송
     */
    private suspend fun sendMessage(message: String) = withContext(Dispatchers.IO) {
        try {
            val output = outputStream ?: return@withContext
            
            val messageBytes = message.toByteArray(Charsets.UTF_8)
            
            // 서버 프로토콜: [4바이트 길이] + [데이터]
            val length = messageBytes.size
            val lengthBytes = ByteBuffer.allocate(4).putInt(length).array()
            
            output.write(lengthBytes)
            output.write(messageBytes)
            output.flush()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * 연결 오류 처리
     */
    private fun handleConnectionError(error: Exception) {
        isConnected = false
        onStateChanged(ConnectionState.Error(error.message ?: "Unknown error", error))
        
        closeConnection()
        
        // 자동 재연결
        if (config.autoReconnect && shouldReconnect) {
            scheduleReconnect()
        }
    }
    
    /**
     * 재연결 스케줄링
     */
    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            // 첫 3번은 즉시 재시도, 이후는 설정된 간격으로
            val delayTime = if (reconnectCount < 3) {
                500L  // 0.5초 (즉시 재시도)
            } else {
                config.reconnectInterval
            }
            
            delay(delayTime)
            
            if (shouldReconnect && !isConnected) {
                reconnectCount++
                Log.i(TAG, "Attempting to reconnect... (attempt #$reconnectCount)")
                onStateChanged(ConnectionState.Reconnecting)
                connect()
            }
        }
    }
    
    /**
     * 연결 종료
     */
    fun disconnect() {
        shouldReconnect = false
        isConnected = false
        
        receiveJob?.cancel()
        reconnectJob?.cancel()
        
        closeConnection()
        
        onStateChanged(ConnectionState.Disconnected)
        Log.i(TAG, "Disconnected")
    }
    
    /**
     * 소켓 및 스트림 닫기
     */
    private fun closeConnection() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection: ${e.message}", e)
        } finally {
            inputStream = null
            outputStream = null
            socket = null
        }
    }
    
    /**
     * 연결 상태 확인
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}
