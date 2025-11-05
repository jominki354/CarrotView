package com.carrotpilot.carrotview.data.models

import kotlinx.serialization.Serializable

/**
 * 주행 데이터 모델
 */
@Serializable
data class DrivingData(
    val timestamp: Long,
    val carState: CarState,
    val controlsState: ControlsState,
    val liveTracks: List<LiveTrack>,
    val deviceState: DeviceState
)

@Serializable
data class CarState(
    val vEgo: Float,  // 현재 속도 (m/s)
    val vCruise: Float,  // 크루즈 설정 속도 (m/s)
    val gearShifter: String,  // 기어 상태
    val doorOpen: Boolean,  // 문 열림 상태
    val seatbeltLatched: Boolean,  // 안전벨트 착용 상태
    val steeringAngleDeg: Float  // 조향각 (도)
)

@Serializable
data class ControlsState(
    val enabled: Boolean,  // 오토파일럿 활성화 여부
    val active: Boolean,  // 크루즈 활성화 여부
    val alertText: String,  // 경고 메시지
    val alertStatus: String  // 경고 상태 (normal, warning, critical)
)

@Serializable
data class LiveTrack(
    val trackId: Int,  // 추적 객체 ID
    val dRel: Float,  // 상대 거리 (m)
    val yRel: Float,  // 상대 횡방향 위치 (m)
    val vRel: Float  // 상대 속도 (m/s)
)

@Serializable
data class DeviceState(
    val batteryPercent: Int,  // 배터리 잔량 (%)
    val thermalStatus: String  // 열 상태 (green, yellow, red)
)

/**
 * 레이아웃 설정 모델
 */
@Serializable
data class LayoutConfig(
    val name: String,
    val components: List<ComponentConfig>
)

@Serializable
data class ComponentConfig(
    val componentType: String,
    val position: Point,
    val size: Size,
    val visible: Boolean,
    val zIndex: Int
)

@Serializable
data class Point(
    val x: Int,
    val y: Int
)

@Serializable
data class Size(
    val width: Int,
    val height: Int
)