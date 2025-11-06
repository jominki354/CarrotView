package com.carrotpilot.carrotview.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.carrotpilot.carrotview.data.preferences.AppPreferences
import com.carrotpilot.carrotview.network.ConnectionState
import com.carrotpilot.carrotview.ui.components.*
import com.carrotpilot.carrotview.ui.controller.DashboardController
import com.carrotpilot.carrotview.data.models.*
import kotlinx.coroutines.launch

/**
 * 편집 가능한 Tesla 스타일 대시보드
 */
class EditableDashboardActivity : AppCompatActivity() {
    
    private lateinit var dashboardController: DashboardController
    private lateinit var prefs: AppPreferences
    private lateinit var rootLayout: FrameLayout
    
    // 드래그 가능한 컴포넌트들
    private lateinit var speedometer: DraggableSpeedometer
    private lateinit var autopilotStatus: DraggableAutopilotStatus
    private lateinit var visualizationView: TeslaPerspectiveView
    
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
        
        // 속도계 (왼쪽 상단)
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
        
        // 오토파일럿 상태 (오른쪽 상단)
        autopilotStatus = DraggableAutopilotStatus(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END or Gravity.TOP
                setMargins(0, 32, 32, 0)
            }
        }
        rootLayout.addView(autopilotStatus)
        
        // 편집 모드 토글 버튼 (상단 중앙)
        editModeButton = Button(this).apply {
            text = "🔓 편집"
            textSize = 12f
            setBackgroundColor(0x88000000.toInt())
            setTextColor(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                setMargins(0, 16, 0, 0)
            }
            setOnClickListener {
                toggleEditMode()
            }
        }
        rootLayout.addView(editModeButton)
        
        // 레이아웃 관리 버튼 (상단 오른쪽)
        val layoutManagerButton = Button(this).apply {
            text = "🎨"
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
        rootLayout.addView(layoutManagerButton)
        
        setContentView(rootLayout)
    }
    
    private fun toggleEditMode() {
        isEditMode = !isEditMode
        
        // 모든 드래그 가능한 컴포넌트의 편집 모드 설정
        speedometer.isEditMode = isEditMode
        autopilotStatus.isEditMode = isEditMode
        
        // 버튼 텍스트 변경
        editModeButton.text = if (isEditMode) "🔒 저장" else "🔓 편집"
        
        if (isEditMode) {
            Toast.makeText(this, "편집 모드: 컴포넌트를 드래그하세요", Toast.LENGTH_SHORT).show()
        } else {
            // 레이아웃 저장
            saveLayout()
            Toast.makeText(this, "레이아웃 저장됨", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openLayoutManager() {
        val intent = android.content.Intent(this, LayoutManagerActivity::class.java)
        startActivity(intent)
    }
    
    private fun saveLayout() {
        val speedPos = speedometer.savePosition()
        val autopilotPos = autopilotStatus.savePosition()
        
        // SharedPreferences에 저장
        prefs.saveComponentPosition("speedometer", speedPos.x, speedPos.y)
        prefs.saveComponentPosition("autopilot", autopilotPos.x, autopilotPos.y)
    }
    
    private fun restoreLayout() {
        // SharedPreferences에서 복원
        val speedPos = prefs.getComponentPosition("speedometer")
        val autopilotPos = prefs.getComponentPosition("autopilot")
        
        if (speedPos != null) {
            speedometer.x = speedPos.first
            speedometer.y = speedPos.second
        }
        
        if (autopilotPos != null) {
            autopilotStatus.x = autopilotPos.first
            autopilotStatus.y = autopilotPos.second
        }
    }
    
    private fun setupListeners() {
        // 데이터 업데이트 리스너
        dashboardController.setDataUpdateListener { data ->
            runOnUiThread {
                isConnected = true
                updateUIWithRealData(data)
            }
        }
        
        // 연결 상태 리스너
        dashboardController.setConnectionStateListener { state ->
            runOnUiThread {
                when (state) {
                    is ConnectionState.Connected -> {
                        isConnected = true
                    }
                    is ConnectionState.Disconnected -> {
                        isConnected = false
                    }
                    else -> {}
                }
            }
        }
    }
    
    private fun updateUIWithRealData(data: DrivingData) {
        val speedKmh = data.carState.vEgo * 3.6f
        val cruiseKmh = data.carState.vCruise * 3.6f
        
        // 속도계 업데이트
        speedometer.updateSpeed(speedKmh, cruiseKmh)
        
        // 오토파일럿 상태 업데이트
        autopilotStatus.updateStatus(data.controlsState.enabled, data.controlsState.active)
        
        // 차량 시각화 업데이트
        // visualizationView.updateData(data)  // TODO: 나중에 구현
    }
    
    private fun startAutoConnection() {
        lifecycleScope.launch {
            kotlinx.coroutines.delay(500)
            dashboardController.discoverAndConnect()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        dashboardController.cleanup()
    }
}
