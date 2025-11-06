package com.carrotpilot.carrotview.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.carrotpilot.carrotview.R
import com.carrotpilot.carrotview.data.preferences.AppPreferences
import com.carrotpilot.carrotview.network.ConnectionState
import com.carrotpilot.carrotview.ui.controller.DashboardController
import com.carrotpilot.carrotview.data.models.*
import kotlinx.coroutines.launch

/**
 * 메인 액티비티 - 네트워크 연결 포함
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var dashboardController: DashboardController
    private lateinit var prefs: AppPreferences
    private lateinit var statusTextView: TextView
    private lateinit var connectionStatusTextView: TextView
    private lateinit var serverAddressInput: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var autoDiscoverButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 설정 초기화
        prefs = AppPreferences(this)
        
        // UI 생성
        createUI()
        
        // 컨트롤러 초기화
        dashboardController = DashboardController(this)
        
        // 네트워크 초기화
        dashboardController.initializeNetwork(this)
        
        // 리스너 설정
        setupListeners()
        
        // 마지막 연결 주소 복원
        restoreLastConnection()
        
        // 테스트 데이터로 초기화 (연결 전)
        initializeWithTestData()
        
        // 자동 연결 시도 (설정에서 활성화된 경우)
        if (prefs.autoConnect) {
            autoConnectOnStartup()
        }
    }
    
    /**
     * 앱 시작 시 자동 연결 (빠른 연결)
     */
    private fun autoConnectOnStartup() {
        lifecycleScope.launch {
            // 잠시 대기 (UI 초기화 완료 후)
            kotlinx.coroutines.delay(300)
            
            connectionStatusTextView.text = "연결 상태: 자동 연결 중..."
            
            // 마지막 연결 주소가 있으면 먼저 시도
            val lastAddress = prefs.lastServerAddress
            if (lastAddress.isNotEmpty()) {
                connectionStatusTextView.text = "연결 상태: $lastAddress 연결 중..."
                dashboardController.connectToCarrotPilot(lastAddress)
                
                // 연결 성공 여부 확인 (1초만 대기 - 빠른 실패)
                kotlinx.coroutines.delay(1000)
                
                val currentState = dashboardController.getConnectionState()
                if (currentState is com.carrotpilot.carrotview.network.ConnectionState.Connected) {
                    // 연결 성공
                    return@launch
                }
                
                // 연결 실패 시 즉시 자동 발견 시작
                connectionStatusTextView.text = "연결 상태: 이전 주소 실패, 검색 중..."
            }
            
            // 자동 발견 시도
            connectionStatusTextView.text = "연결 상태: CarrotPilot 검색 중..."
            val success = dashboardController.discoverAndConnect()
            
            if (!success) {
                connectionStatusTextView.text = "연결 상태: 자동 연결 실패 (수동으로 연결하세요)"
            }
        }
    }
    
    private fun restoreLastConnection() {
        val lastAddress = prefs.lastServerAddress
        if (lastAddress.isNotEmpty()) {
            serverAddressInput.setText(lastAddress)
        }
    }
    
    private fun createUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        
        // 연결 상태 표시
        connectionStatusTextView = TextView(this).apply {
            text = "연결 상태: 연결 안 됨"
            textSize = 14f
            setPadding(0, 0, 0, 20)
        }
        layout.addView(connectionStatusTextView)
        
        // 서버 주소 입력
        serverAddressInput = EditText(this).apply {
            hint = "서버 IP 주소 (예: 192.168.1.100)"
            textSize = 14f
        }
        layout.addView(serverAddressInput)
        
        // 연결 버튼
        connectButton = Button(this).apply {
            text = "연결"
            setOnClickListener {
                val address = serverAddressInput.text.toString()
                if (address.isNotEmpty()) {
                    dashboardController.connectToCarrotPilot(address)
                }
            }
        }
        layout.addView(connectButton)
        
        // 자동 발견 버튼
        autoDiscoverButton = Button(this).apply {
            text = "자동 발견 및 연결"
            setOnClickListener {
                lifecycleScope.launch {
                    connectionStatusTextView.text = "연결 상태: CarrotPilot 검색 중..."
                    val success = dashboardController.discoverAndConnect()
                    if (!success) {
                        connectionStatusTextView.text = "연결 상태: CarrotPilot을 찾을 수 없습니다"
                    }
                }
            }
        }
        layout.addView(autoDiscoverButton)
        
        // 연결 해제 버튼
        disconnectButton = Button(this).apply {
            text = "연결 해제"
            isEnabled = false
            setOnClickListener {
                dashboardController.disconnect()
            }
        }
        layout.addView(disconnectButton)
        
        // 설정 버튼
        val settingsButton = Button(this).apply {
            text = "⚙️ 설정"
            setOnClickListener {
                val intent = android.content.Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        layout.addView(settingsButton)
        
        // 상태 표시
        statusTextView = TextView(this).apply {
            text = "CarrotView 대시보드\n\n초기화 중..."
            textSize = 14f
            setPadding(0, 40, 0, 0)
        }
        layout.addView(statusTextView)
        
        setContentView(layout)
    }
    
    private fun setupListeners() {
        // 데이터 업데이트 리스너
        dashboardController.setDataUpdateListener { data ->
            updateUI(data)
        }
        
        // 연결 상태 리스너
        dashboardController.setConnectionStateListener { state ->
            updateConnectionStatus(state)
        }
    }
    
    private fun updateConnectionStatus(state: ConnectionState) {
        runOnUiThread {
            when (state) {
                is ConnectionState.Disconnected -> {
                    connectionStatusTextView.text = "연결 상태: 연결 안 됨"
                    connectButton.isEnabled = true
                    disconnectButton.isEnabled = false
                    autoDiscoverButton.isEnabled = true
                    showDisconnectedMessage()
                }
                is ConnectionState.Connecting -> {
                    connectionStatusTextView.text = "연결 상태: 연결 중..."
                    connectButton.isEnabled = false
                    disconnectButton.isEnabled = false
                    autoDiscoverButton.isEnabled = false
                    showConnectingMessage()
                }
                is ConnectionState.Connected -> {
                    connectionStatusTextView.text = "연결 상태: 연결됨 (${state.serverAddress}:${state.port})"
                    connectButton.isEnabled = false
                    disconnectButton.isEnabled = true
                    autoDiscoverButton.isEnabled = false
                }
                is ConnectionState.Error -> {
                    connectionStatusTextView.text = "연결 상태: 오류 - ${state.message}"
                    connectButton.isEnabled = true
                    disconnectButton.isEnabled = false
                    autoDiscoverButton.isEnabled = true
                    showDisconnectedMessage()
                }
                is ConnectionState.Reconnecting -> {
                    connectionStatusTextView.text = "연결 상태: 재연결 중..."
                    connectButton.isEnabled = false
                    disconnectButton.isEnabled = true
                    autoDiscoverButton.isEnabled = false
                    showReconnectingMessage()
                }
            }
        }
    }
    
    private fun showDisconnectedMessage() {
        statusTextView.text = buildString {
            appendLine()
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("        연결 안 됨")
            appendLine()
            appendLine("  CarrotPilot에 연결되지 않았습니다")
            appendLine()
            appendLine("  위의 버튼을 사용하여 연결하세요:")
            appendLine("  • 자동 발견 및 연결")
            appendLine("  • 또는 IP 주소 입력 후 연결")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
    
    private fun showConnectingMessage() {
        statusTextView.text = buildString {
            appendLine()
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("        연결 중...")
            appendLine()
            appendLine("  CarrotPilot에 연결하는 중입니다")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
    
    private fun showReconnectingMessage() {
        statusTextView.text = buildString {
            appendLine()
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("        재연결 중...")
            appendLine()
            appendLine("  CarrotPilot에 재연결하는 중입니다")
            appendLine()
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }
    
    private fun initializeWithTestData() {
        // 연결 안 됐을 때는 테스트 데이터 표시하지 않음
        showDisconnectedMessage()
    }
    
    private fun updateUI(data: DrivingData) {
        runOnUiThread {
            val speedKmh = data.carState.vEgo * 3.6f
            val cruiseKmh = data.carState.vCruise * 3.6f
            
            // 주행 상태 판단
            val drivingStatus = when {
                !data.controlsState.enabled && !data.controlsState.active -> "⚪ 대기 중"
                !data.controlsState.enabled && data.controlsState.active -> "🟡 크루즈 ON (오픈파일럿 OFF)"
                data.controlsState.enabled && !data.controlsState.active -> "🟠 오픈파일럿 준비 중"
                data.controlsState.enabled && data.controlsState.active -> "🟢 주행 중 (오픈파일럿 활성)"
                else -> "❓ 알 수 없음"
            }
            
            // 버전 정보 생성
            val buildTime = try {
                val timestamp = com.carrotpilot.carrotview.BuildConfig.BUILD_TIME.toLong()
                java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
            } catch (e: Exception) {
                "Unknown"
            }
            val versionInfo = "v${com.carrotpilot.carrotview.BuildConfig.VERSION_NAME} ($buildTime)"
            
            val statusText = buildString {
                appendLine("CarrotView 대시보드")
                appendLine("=" * 30)
                appendLine()
                appendLine("📊 주행 상태: $drivingStatus")
                appendLine()
                appendLine("🚗 차량 상태")
                appendLine("  현재 속도: ${String.format("%.1f", speedKmh)} km/h")
                appendLine("  크루즈 설정 속도: ${String.format("%.1f", cruiseKmh)} km/h")
                appendLine("  기어: ${data.carState.gearShifter}")
                appendLine("  조향각: ${String.format("%.1f", data.carState.steeringAngleDeg)}°")
                appendLine("  문 열림: ${if (data.carState.doorOpen) "예" else "아니오"}")
                appendLine("  안전벨트: ${if (data.carState.seatbeltLatched) "착용" else "미착용"}")
                appendLine()
                appendLine("        [$versionInfo]")  // 버전 정보 (중앙 정렬)
                appendLine()
                appendLine("🚙 크루즈 제어")
                appendLine("  오픈파일럿: ${if (data.controlsState.enabled) "✅ 활성화" else "❌ 비활성화"}")
                appendLine("  크루즈: ${if (data.controlsState.active) "✅ ON" else "❌ OFF"}")
                appendLine("  경고: ${data.controlsState.alertText}")
                appendLine("  상태: ${data.controlsState.alertStatus}")
                appendLine()
                
                // 경고 메시지가 있으면 강조 표시
                if (data.controlsState.alertText.isNotEmpty() && data.controlsState.alertText != "None") {
                    appendLine("⚠️ 알림: ${data.controlsState.alertText}")
                    appendLine()
                }
                
                appendLine("🎯 추적 객체: ${data.liveTracks.size}개")
                data.liveTracks.take(3).forEach { track ->
                    appendLine("  #${track.trackId}: ${String.format("%.1f", track.dRel)}m, ${String.format("%.1f", track.vRel * 3.6f)} km/h")
                }
                appendLine()
                appendLine("🔋 디바이스")
                appendLine("  배터리: ${data.deviceState.batteryPercent}%")
                appendLine("  열 상태: ${data.deviceState.thermalStatus}")
                appendLine()
                appendLine("🕐 마지막 업데이트: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(data.timestamp))}")
            }
            
            statusTextView.text = statusText
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dashboardController.cleanup()
    }
    
    private operator fun String.times(count: Int): String {
        return this.repeat(count)
    }
}