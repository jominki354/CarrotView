package com.carrotpilot.carrotview.ui.controller

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.carrotpilot.carrotview.data.models.DrivingData
import com.carrotpilot.carrotview.network.ConnectionState
import com.carrotpilot.carrotview.network.NetworkManager
import com.carrotpilot.carrotview.ui.layout.CustomLayoutManager
import com.carrotpilot.carrotview.ui.layout.LayoutPresetManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 대시보드 컨트롤러 - 네트워크 연동 포함
 */
class DashboardController(private val context: Context) {
    
    private val layoutManager = CustomLayoutManager(context)
    private val presetManager = LayoutPresetManager(context)
    private val networkManager = NetworkManager(context)
    
    private var currentData: DrivingData? = null
    private var dataUpdateListener: ((DrivingData) -> Unit)? = null
    private var connectionStateListener: ((ConnectionState) -> Unit)? = null
    
    companion object {
        private const val TAG = "DashboardController"
    }
    
    /**
     * 네트워크 연결 초기화
     */
    fun initializeNetwork(lifecycleOwner: LifecycleOwner) {
        // 연결 상태 관찰
        lifecycleOwner.lifecycleScope.launch {
            networkManager.connectionState.collectLatest { state ->
                connectionStateListener?.invoke(state)
                Log.d(TAG, "Connection state: $state")
            }
        }
        
        // 주행 데이터 관찰
        lifecycleOwner.lifecycleScope.launch {
            networkManager.drivingData.collectLatest { data ->
                data?.let {
                    updateData(it)
                }
            }
        }
    }
    
    /**
     * CarrotPilot에 연결
     */
    fun connectToCarrotPilot(serverAddress: String, port: Int = 8090) {
        networkManager.connect(serverAddress, port)
    }
    
    /**
     * 현재 연결 상태 가져오기
     */
    fun getConnectionState(): ConnectionState {
        return networkManager?.connectionState?.value ?: ConnectionState.Disconnected
    }
    
    /**
     * CarrotPilot 자동 발견 및 연결
     */
    suspend fun discoverAndConnect(): Boolean {
        val serverAddress = networkManager.discoverCarrotPilot()
        return if (serverAddress != null) {
            networkManager.connect(serverAddress)
            true
        } else {
            false
        }
    }
    
    /**
     * 연결 해제
     */
    fun disconnect() {
        networkManager.disconnect()
    }
    
    /**
     * 재연결
     */
    fun reconnect() {
        networkManager.reconnect()
    }
    
    /**
     * 데이터 업데이트
     */
    fun updateData(data: DrivingData) {
        currentData = data
        dataUpdateListener?.invoke(data)
    }
    
    /**
     * 데이터 업데이트 리스너 설정
     */
    fun setDataUpdateListener(listener: (DrivingData) -> Unit) {
        dataUpdateListener = listener
    }
    
    /**
     * 연결 상태 리스너 설정
     */
    fun setConnectionStateListener(listener: (ConnectionState) -> Unit) {
        connectionStateListener = listener
    }
    
    /**
     * 현재 데이터 가져오기
     */
    fun getCurrentData(): DrivingData? {
        return currentData
    }
    
    /**
     * 네트워크 관리자 가져오기
     */
    fun getNetworkManager(): NetworkManager {
        return networkManager
    }
    
    /**
     * 레이아웃 관리자 가져오기
     */
    fun getLayoutManager(): CustomLayoutManager {
        return layoutManager
    }
    
    /**
     * 프리셋 관리자 가져오기
     */
    fun getPresetManager(): LayoutPresetManager {
        return presetManager
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        networkManager.cleanup()
    }
}