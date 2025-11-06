package com.carrotpilot.carrotview.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.carrotpilot.carrotview.network.ConnectionConfig

/**
 * 앱 설정 관리
 */
class AppPreferences(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "carrotview_prefs"
        
        // 네트워크 설정
        private const val KEY_SERVER_ADDRESS = "server_address"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        private const val KEY_RECONNECT_INTERVAL = "reconnect_interval"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        
        // 기본값
        private const val DEFAULT_PORT = 8090
        private const val DEFAULT_AUTH_TOKEN = "carrotview2024"
        private const val DEFAULT_AUTO_RECONNECT = true
        private const val DEFAULT_RECONNECT_INTERVAL = 2000L  // 2초로 단축
        
        // UI 설정
        private const val KEY_THEME = "theme"
        private const val KEY_LAYOUT_PRESET = "layout_preset"
        
        // 마지막 연결 정보
        private const val KEY_LAST_SERVER_ADDRESS = "last_server_address"
        
        // 레이아웃 설정
        private const val KEY_COMPONENT_X = "component_x_"
        private const val KEY_COMPONENT_Y = "component_y_"
    }
    
    // 서버 주소
    var serverAddress: String
        get() = prefs.getString(KEY_SERVER_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER_ADDRESS, value).apply()
    
    // 서버 포트
    var serverPort: Int
        get() = prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT)
        set(value) = prefs.edit().putInt(KEY_SERVER_PORT, value).apply()
    
    // 인증 토큰
    var authToken: String
        get() = prefs.getString(KEY_AUTH_TOKEN, DEFAULT_AUTH_TOKEN) ?: DEFAULT_AUTH_TOKEN
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()
    
    // 자동 재연결
    var autoReconnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECONNECT, DEFAULT_AUTO_RECONNECT)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RECONNECT, value).apply()
    
    // 재연결 간격 (밀리초)
    var reconnectInterval: Long
        get() = prefs.getLong(KEY_RECONNECT_INTERVAL, DEFAULT_RECONNECT_INTERVAL)
        set(value) = prefs.edit().putLong(KEY_RECONNECT_INTERVAL, value).apply()
    
    // 테마
    var theme: String
        get() = prefs.getString(KEY_THEME, "dark") ?: "dark"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()
    
    // 레이아웃 프리셋
    var layoutPreset: String
        get() = prefs.getString(KEY_LAYOUT_PRESET, "tesla") ?: "tesla"
        set(value) = prefs.edit().putString(KEY_LAYOUT_PRESET, value).apply()
    
    // 마지막 연결 서버 주소
    var lastServerAddress: String
        get() = prefs.getString(KEY_LAST_SERVER_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SERVER_ADDRESS, value).apply()
    
    // 앱 시작 시 자동 연결
    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()
    
    /**
     * 기본값으로 초기화
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 연결 설정 가져오기
     */
    fun getConnectionConfig(): ConnectionConfig {
        return ConnectionConfig(
            serverAddress = serverAddress.ifEmpty { lastServerAddress },
            port = serverPort,
            authToken = authToken,
            autoReconnect = autoReconnect,
            reconnectInterval = reconnectInterval
        )
    }
    
    /**
     * 연결 설정 저장
     */
    fun saveConnectionConfig(config: ConnectionConfig) {
        serverAddress = config.serverAddress
        serverPort = config.port
        authToken = config.authToken
        autoReconnect = config.autoReconnect
        reconnectInterval = config.reconnectInterval
        lastServerAddress = config.serverAddress
    }
    
    /**
     * 컴포넌트 위치 저장
     */
    fun saveComponentPosition(componentId: String, x: Float, y: Float) {
        prefs.edit().apply {
            putFloat(KEY_COMPONENT_X + componentId, x)
            putFloat(KEY_COMPONENT_Y + componentId, y)
            apply()
        }
    }
    
    /**
     * 컴포넌트 위치 복원
     */
    fun getComponentPosition(componentId: String): Pair<Float, Float>? {
        val x = prefs.getFloat(KEY_COMPONENT_X + componentId, -1f)
        val y = prefs.getFloat(KEY_COMPONENT_Y + componentId, -1f)
        
        return if (x >= 0 && y >= 0) {
            Pair(x, y)
        } else {
            null
        }
    }
    
    /**
     * 컴포넌트 전체 상태 저장
     */
    fun saveComponentState(componentId: String, position: com.carrotpilot.carrotview.ui.components.ComponentPosition) {
        prefs.edit().apply {
            putFloat(KEY_COMPONENT_X + componentId, position.x)
            putFloat(KEY_COMPONENT_Y + componentId, position.y)
            putInt("component_width_$componentId", position.width)
            putInt("component_height_$componentId", position.height)
            putFloat("component_scale_$componentId", position.scale)
            putBoolean("component_visible_$componentId", position.visible)
            apply()
        }
    }
    
    /**
     * 컴포넌트 전체 상태 복원
     */
    fun getComponentState(componentId: String): com.carrotpilot.carrotview.ui.components.ComponentPosition? {
        val x = prefs.getFloat(KEY_COMPONENT_X + componentId, -1f)
        val y = prefs.getFloat(KEY_COMPONENT_Y + componentId, -1f)
        
        if (x < 0 || y < 0) return null
        
        val width = prefs.getInt("component_width_$componentId", -1)
        val height = prefs.getInt("component_height_$componentId", -1)
        val scale = prefs.getFloat("component_scale_$componentId", 1.0f)
        val visible = prefs.getBoolean("component_visible_$componentId", true)
        
        return if (width > 0 && height > 0) {
            com.carrotpilot.carrotview.ui.components.ComponentPosition(x, y, width, height, scale, visible)
        } else {
            null
        }
    }
}
