package com.carrotpilot.carrotview.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.carrotpilot.carrotview.R
import com.carrotpilot.carrotview.data.preferences.AppPreferences
import com.carrotpilot.carrotview.network.ConnectionState
import com.carrotpilot.carrotview.ui.components.*
import com.carrotpilot.carrotview.ui.controller.DashboardController
import com.carrotpilot.carrotview.data.models.*
import kotlinx.coroutines.launch

/**
 * Tesla 스타일 대시보드 메인 액티비티
 */
class TeslaDashboardActivity : AppCompatActivity() {
    
    private lateinit var dashboardController: DashboardController
    private lateinit var prefs: AppPreferences
    private lateinit var rootLayout: FrameLayout
    private lateinit var visualizationView: TeslaPerspectiveView
    private lateinit var alertText: TextView
    private lateinit var versionInfo: TextView
    
    // 드래그 가능한 컴포넌트들
    private lateinit var speedometer: DraggableSpeedometer
    private lateinit var autopilotStatusView: DraggableAutopilotStatus
    
    // 편집 모드
    private var isEditMode = false
    private lateinit var editModeButton: Button
    
    private var isConnected = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 설정 초기화
        prefs = AppPreferences(this)
        
        // 프로그래밍 방식으로 UI 생성
        createUI()
        
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
        
        // 저장된 레이아웃 복원
        restoreLayout()
        
        // 연결 안 됨 상태로 시작
        showDisconnectedState()
        
        // 백그라운드에서 자동 연결 시도
        startAutoConnection()
    }
    
    private fun createUI() {
        // 루트 레이아웃
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        
        // 차량 시각화 (배경)
        visualizationView = TeslaPerspectiveView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootLayout.addView(visualizationView)
        
        // 속도계 (왼쪽 상단) - 드래그 가능
        speedometer = DraggableSpeedometer(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START or Gravity.TOP
                setMargins(32, 32, 0, 0)
            }
        }
        rootLayout.addView(speedometer)
        
        // 오토파일럿 상태 (오른쪽 상단) - 드래그 가능
        autopilotStatusView = DraggableAutopilotStatus(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                setMargins(0, 32, 32, 0)
            }
        }
        rootLayout.addView(autopilotStatusView)
        
        // 버전 정보 (상단 중앙)
        versionInfo = TextView(this).apply {
            val buildTime = try {
                val timestamp = com.carrotpilot.carrotview.BuildConfig.BUILD_TIME.toLong()
                java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
            } catch (e: Exception) {
                "Unknown"
            }
            text = "v${com.carrotpilot.carrotview.BuildConfig.VERSION_NAME} | $buildTime"
            textSize = 10f
            setTextColor(0xFF00BCD4.toInt())
            alpha = 0.7f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                setMargins(0, 8, 0, 0)
            }
        }
        rootLayout.addView(versionInfo)
        
        // 중앙 경고 메시지
        alertText = TextView(this).apply {
            textSize = 16f
            setTextColor(getColor(R.color.status_warning))
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        rootLayout.addView(alertText)
        
        // 편집 모드 토글 버튼 (상단 중앙 오른쪽)
        editModeButton = Button(this).apply {
            text = "🔓"
            textSize = 12f
            setBackgroundColor(0x88000000.toInt())
            setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                setMargins(100, 16, 0, 0)
            }
            setOnClickListener {
                toggleEditMode()
            }
        }
        rootLayout.addView(editModeButton)
        
        // 설정 버튼 (상단 오른쪽 끝)
        val settingsButton = Button(this).apply {
            text = "⚙️"
            textSize = 12f
            setBackgroundColor(0x88000000.toInt())
            setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                setMargins(0, 16, 16, 0)
            }
            setOnClickListener {
                openLayoutManager()
            }
        }
        rootLayout.addView(settingsButton)
        
        // 표시/숨김 토글 버튼들 (편집 모드에서만 표시)
        createVisibilityToggleButtons()
        
        setContentView(rootLayout)
    }
    
    private fun createVisibilityToggleButtons() {
        // 속도계 표시/숨김 버튼
        val speedToggleButton = Button(this).apply {
            text = "👁️ 속도계"
            textSize = 10f
            setBackgroundColor(0x88000000.toInt())
            setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START or Gravity.BOTTOM
                setMargins(16, 0, 0, 16)
            }
            visibility = View.GONE
            tag = "visibility_toggle"
            setOnClickListener {
                speedometer.toggleVisibility()
                text = if (speedometer.visibility == View.VISIBLE) "👁️ 속도계" else "👁️‍🗨️ 속도계"
            }
        }
        rootLayout.addView(speedToggleButton)
        
        // 오토파일럿 표시/숨김 버튼
        val autopilotToggleButton = Button(this).apply {
            text = "👁️ 오토파일럿"
            textSize = 10f
            setBackgroundColor(0x88000000.toInt())
            setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.BOTTOM
                setMargins(0, 0, 16, 16)
            }
            visibility = View.GONE
            tag = "visibility_toggle"
            setOnClickListener {
                autopilotStatusView.toggleVisibility()
                text = if (autopilotStatusView.visibility == View.VISIBLE) "👁️ 오토파일럿" else "👁️‍🗨️ 오토파일럿"
            }
        }
        rootLayout.addView(autopilotToggleButton)
    }
    
    private fun toggleEditMode() {
        isEditMode = !isEditMode
        
        // 모든 드래그 가능한 컴포넌트의 편집 모드 설정
        speedometer.isEditMode = isEditMode
        autopilotStatusView.isEditMode = isEditMode
        
        // 표시/숨김 토글 버튼들 표시/숨김
        for (i in 0 until rootLayout.childCount) {
            val child = rootLayout.getChildAt(i)
            if (child.tag == "visibility_toggle") {
                child.visibility = if (isEditMode) View.VISIBLE else View.GONE
            }
        }
        
        // 버튼 텍스트 변경
        editModeButton.text = if (isEditMode) "🔒" else "🔓"
        
        if (isEditMode) {
            Toast.makeText(this, "편집 모드: 드래그/핀치/토글 가능", Toast.LENGTH_SHORT).show()
        } else {
            // 레이아웃 저장
            saveLayout()
            Toast.makeText(this, "레이아웃 저장됨", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openLayoutManager() {
        val intent = Intent(this, LayoutManagerActivity::class.java)
        startActivity(intent)
    }
    
    private fun saveLayout() {
        val speedPos = speedometer.savePosition()
        val autopilotPos = autopilotStatusView.savePosition()
        
        // SharedPreferences에 전체 상태 저장
        prefs.saveComponentState("speedometer", speedPos)
        prefs.saveComponentState("autopilot", autopilotPos)
    }
    
    private fun restoreLayout() {
        // SharedPreferences에서 전체 상태 복원
        val speedState = prefs.getComponentState("speedometer")
        val autopilotState = prefs.getComponentState("autopilot")
        
        speedState?.let { speedometer.restorePosition(it) }
        autopilotState?.let { autopilotStatusView.restorePosition(it) }
    }
    
    override fun onResume() {
        super.onResume()
        // 레이아웃 관리에서 돌아왔을 때 레이아웃 다시 로드
        restoreLayout()
    }
    
    private fun setupListeners() {
        // 데이터 업데이트 리스너
        dashboardController.setDataUpdateListener { data ->
            runOnUiThread {
                isConnected = true
                
                // 주행 상태 확인
                val isDriving = data.controlsState.enabled
                val hasSpeed = data.carState.vEgo > 0.5  // 0.5 m/s (약 2 km/h) 이상
                
                when {
                    // 오픈파일럿 활성화 또는 속도가 있으면 주행 화면 표시
                    isDriving || hasSpeed -> {
                        hideDisconnectedState()
                        updateUIWithRealData(data)
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
        
        // 속도 표시 초기화
        speedometer.updateSpeed(0f, 0f)
        
        // 오토파일럿 상태 초기화
        autopilotStatusView.updateStatus(false, false)
        
        // 중앙에 "연결 안 됨" 표시
        alertText.visibility = View.VISIBLE
        alertText.text = "연결 안 됨\n\nCarrotPilot 연결 대기 중..."
        alertText.setTextColor(getColor(R.color.text_secondary))
        alertText.textSize = 24f
        alertText.gravity = Gravity.CENTER
    }
    
    private fun showConnectingState() {
        visualizationView.visibility = View.INVISIBLE
        alertText.visibility = View.VISIBLE
        alertText.text = "연결 중..."
        alertText.setTextColor(getColor(R.color.text_secondary))
        alertText.textSize = 24f
        alertText.gravity = Gravity.CENTER
    }
    
    private fun showWaitingState() {
        visualizationView.visibility = View.INVISIBLE
        alertText.visibility = View.VISIBLE
        alertText.text = "주행 준비 중\n\n차량 시동 및 카메라 활성화 대기 중..."
        alertText.setTextColor(getColor(R.color.text_secondary))
        alertText.textSize = 24f
        alertText.gravity = Gravity.CENTER
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
        speedometer.updateSpeed(speedKmh, cruiseKmh)
        
        // 오토파일럿 상태 업데이트
        autopilotStatusView.updateStatus(data.controlsState.enabled, data.controlsState.active)
        
        // 시각화 뷰 업데이트
        visualizationView.updateData(extendedData)
        
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
        val radarTracks = data.liveTracks.map { track ->
            RadarTrack(
                trackId = track.trackId,
                dRel = track.dRel,
                yRel = track.yRel,
                vRel = track.vRel,
                aRel = 0f,
                prob = 0.9f
            )
        }
        
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
