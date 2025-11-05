package com.carrotpilot.carrotview.network

/**
 * 네트워크 연결 상태
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val serverAddress: String, val port: Int) : ConnectionState()
    data class Error(val message: String, val exception: Throwable? = null) : ConnectionState()
    object Reconnecting : ConnectionState()
}

/**
 * 연결 설정
 */
data class ConnectionConfig(
    val serverAddress: String,
    val port: Int = 8090,
    val authToken: String = "carrotview2024",
    val autoReconnect: Boolean = true,
    val reconnectInterval: Long = 2000, // 2초 (빠른 재연결)
    val connectionTimeout: Int = 3000, // 3초 (빠른 실패)
    val readTimeout: Int = 10000 // 10초 (빠른 타임아웃 감지)
)

/**
 * 연결 통계
 */
data class ConnectionStats(
    val connectedTime: Long = 0,
    val bytesReceived: Long = 0,
    val messagesReceived: Long = 0,
    val reconnectCount: Int = 0,
    val lastError: String? = null
)
