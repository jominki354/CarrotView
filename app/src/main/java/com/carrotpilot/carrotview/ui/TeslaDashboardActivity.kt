package com.carrotpilot.carrotview.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.carrotpilot.carrotview.R
import com.carrotpilot.carrotview.data.preferences.AppPreferences
import com.carrotpilot.carrotview.network.ConnectionState
import com.carrotpilot.carrotview.ui.components.TeslaPerspectiveView
import com.carrotpilot.carrotview.ui.controller.DashboardController
import com.carrotpilot.carrotview.data.models.*
import kotlinx.coroutines.launch

/**
 * Tesla 스타일 대시보드 메인 액티비티
 */
class TeslaDashboardActivity : AppCompatActivity() {
    
    private lateinit var dashboardController: DashboardController
    private lateinit var prefs: AppPreferences
    private lateinit var visualizationView: TeslaPerspectiveView
    private lateinit var currentSpeed: TextView
    private lateinit var cruiseSpeed: TextView
    private lateinit var autopilotStatus: TextView
    private lateinit var autopilotState: TextView
    private lateinit var autopilotIndicator: View
    private lateinit var alertText: TextView
    
    private var isConnected = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tesla_dashboard)
        
        // 설정 초기화
        prefs = AppPreferences(this)
        
        // 뷰 초기화
        visualizationView = findViewById(R.id.visualizationView)
        currentSpeed = findViewById(R.id.currentSpeed)
        cruiseSpeed = findViewById(R.id.cruiseSpeed)
        autopilotStatus = findViewById(R.id.autopilotStatus)
        autopilotState = findViewById(R.id.autopilotState)
        autopilotIndicator = findViewById(R.id.autopilotIndicator)
        alertText = findViewById(R.id.alertText)
        
        // 전체 화면 모드
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        // 화면 항상 켜짐
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // 컨트롤러 초기화
        dashboardController = DashboardController(this)
        dashboardController.initializeNetwork(this)
        
        // 리스너 설정
        setupListeners()
        
        // 연결 안 됨 상태로 시작
        showDisconnectedState()
        
        // 백그라운드에서 자동 연결 시도
        startAutoConnection()
    }
    
    private fun setupListeners() {
        // 데이터 업데이트 리스너
        dashboardController.setDataUpdateListener { data ->
            runOnUiThread {
                isConnected = true
                
                // 주행 상태 확인
                val isDriving = data.controlsState.enabled
                val isActive = data.controlsState.active
                
                when {
                    // 크루즈 활성화 - 주행 중 (시각화 표시)
                    isActive -> {
                        hideDisconnectedState()
                        updateUIWithRealData(data)
                    }
                    // 차량 연결됨 - 주행 준비 완료
                    isDriving -> {
                        showReadyState()
                        updateUIWithRealData(data)  // 속도 등은 업데이트
                    }
                    // 차량 미연결 - 대기 중
                    else -> {
                        showWaitingState()
                    }
                }
            }
        }
        
        // 연결 상태 리스너
        dashboardController.setConnectionStateListener { state ->
            runOnUiThread {
                when (state) {
                    is ConnectionState.Connected -> {
                        isConnected = true
                        showWaitingState()  // 연결 직후는 대기 상태
                    }
                    is ConnectionState.Disconnected,
                    is ConnectionState.Error -> {
                        isConnected = false
                        showDisconnectedState()
                    }
                    is ConnectionState.Connecting,
                    is ConnectionState.Reconnecting -> {
                        showConnectingState()
                    }
                }
            }
        }
    }
    
    private fun startAutoConnection() {
        lifecycleScope.launch {
            android.util.Log.d("TeslaDashboard", "🔍 자동으로 CarrotPilot 검색 중...")
            
            // 자동 검색 및 연결
            val discovered = dashboardController.discoverAndConnect()
            
            if (discovered) {
                android.util.Log.d("TeslaDashboard", "✅ CarrotPilot 자동 연결 성공!")
            } else {
                android.util.Log.w("TeslaDashboard", "❌ CarrotPilot을 찾을 수 없습니다")
                // 실패 시 재시도
                kotlinx.coroutines.delay(5000)
                startAutoConnection()
            }
        }
    }
    
    private fun showDisconnectedState() {
        // 시각화 숨기기
        visualizationView.visibility = View.INVISIBLE
        
        // 속도 표시 숨기기
        currentSpeed.text = "--"
        cruiseSpeed.text = "--"
        
        // 크루즈 상태
        autopilotIndicator.setBackgroundColor(getColor(R.color.status_inactive))
        autopilotState.text = "비활성"
        autopilotState.setTextColor(getColor(R.color.text_tertiary))
        
        // 중앙에 "연결 안 됨" 표시
        alertText.visibility = View.VISIBLE
        alertText.text = "연결 안 됨\n\nCarrotPilot 연결 대기 중..."
        alertText.setTextColor(getColor(R.color.text_secondary))
        alertText.textSize = 24f
        alertText.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        alertText.gravity = android.view.Gravity.CENTER
    }
    
    private fun showConnectingState() {
        visualizationView.visibility = View.INVISIBLE
        alertText.visibility = View.VISIBLE
        alertText.text = "연결 중..."
        alertText.setTextColor(getColor(R.color.text_secondary))
        alertText.textSize = 24f
        alertText.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        alertText.gravity = android.view.Gravity.CENTER
    }
    
    private fun showWaitingState() {
        visualizationView.visibility = View.INVISIBLE
        alertText.visibility = View.VISIBLE
        alertText.text = "주행 준비 중\n\n차량 시동 및 카메라 활성화 대기 중..."
        alertText.setTextColor(getColor(R.color.text_secondary))
        alertText.textSize = 24f
        alertText.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        alertText.gravity = android.view.Gravity.CENTER
    }
    
    private fun showReadyState() {
        visualizationView.visibility = View.INVISIBLE
        alertText.visibility = View.VISIBLE
        alertText.text = "주행 준비 완료\n\n크루즈 활성화 대기 중..."
        alertText.setTextColor(getColor(R.color.status_active))
        alertText.textSize = 24f
        alertText.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        alertText.gravity = android.view.Gravity.CENTER
    }
    
    private fun hideDisconnectedState() {
        // 시각화 보이기
        visualizationView.visibility = View.VISIBLE
        
        // 연결 메시지 숨기기
        alertText.visibility = View.GONE
        alertText.textSize = 16f
    }
    
    private fun updateUIWithRealData(data: DrivingData) {
        // ExtendedDrivingData로 변환
        val extendedData = convertToExtendedData(data)
        
        // 속도 업데이트
        val speedKmh = data.carState.vEgo * 3.6f
        val cruiseKmh = data.carState.vCruise * 3.6f
        currentSpeed.text = speedKmh.toInt().toString()
        cruiseSpeed.text = cruiseKmh.toInt().toString()
        
        // 시각화 뷰 업데이트
        visualizationView.updateData(extendedData)
        
        // 크루즈 상태
        if (data.controlsState.enabled) {
            if (data.controlsState.active) {
                autopilotIndicator.setBackgroundResource(R.drawable.circle_indicator)
                autopilotState.text = "활성"
                autopilotState.setTextColor(getColor(R.color.status_active))
            } else {
                autopilotIndicator.setBackgroundColor(getColor(R.color.autopilot_standby))
                autopilotState.text = "대기"
                autopilotState.setTextColor(getColor(R.color.autopilot_standby))
            }
        } else {
            autopilotIndicator.setBackgroundColor(getColor(R.color.status_inactive))
            autopilotState.text = "비활성"
            autopilotState.setTextColor(getColor(R.color.text_tertiary))
        }
        
        // 경고 메시지
        if (data.controlsState.alertText.isNotEmpty()) {
            alertText.visibility = View.VISIBLE
            alertText.text = data.controlsState.alertText
            alertText.textSize = 16f
            alertText.setTextColor(when (data.controlsState.alertStatus) {
                "critical" -> getColor(R.color.status_critical)
                "warning" -> getColor(R.color.status_warning)
                else -> getColor(R.color.text_primary)
            })
        } else if (isConnected) {
            alertText.visibility = View.GONE
        }
    }
    
    private fun convertToExtendedData(data: DrivingData): ExtendedDrivingData {
        // DrivingData를 ExtendedDrivingData로 변환
        // liveTracks를 radarTracks로 변환
        val radarTracks = data.liveTracks.map { track ->
            RadarTrack(
                trackId = track.trackId,
                dRel = track.dRel,
                yRel = track.yRel,
                vRel = track.vRel,
                aRel = 0f,  // 가속도 정보 없음
                prob = 0.9f
            )
        }
        
        // 기본 ModelV2Data 생성 (실제 데이터가 없으면 빈 데이터)
        val modelV2 = ModelV2Data(
            leftLane = LaneLine(points = emptyList(), prob = 0f),
            rightLane = LaneLine(points = emptyList(), prob = 0f),
            path = PathPlan(points = emptyList(), prob = 0f, validLen = 0f),
            leadOne = radarTracks.firstOrNull()
        )
        
        return ExtendedDrivingData(
            timestamp = data.timestamp,
            carState = data.carState,
            controlsState = data.controlsState,
            liveTracks = data.liveTracks,
            modelV2 = modelV2,
            radarTracks = radarTracks,
            lateralPlan = LateralPlan(laneWidth = 3.7f)
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dashboardController.cleanup()
    }
}