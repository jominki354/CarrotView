package com.carrotpilot.carrotview.data.models

import kotlinx.serialization.Serializable

/**
 * 확장된 주행 데이터 - Tesla 스타일 시각화를 위한 모든 데이터
 */

/**
 * 차선 데이터
 */
@Serializable
data class LaneLine(
    val points: List<Point2D>,  // 차선 포인트들
    val prob: Float = 1.0f,     // 신뢰도
    val std: Float = 0.0f       // 표준편차
)

@Serializable
data class Point2D(
    val x: Float,  // 전방 거리 (m)
    val y: Float   // 좌우 위치 (m)
)

/**
 * 도로 가장자리
 */
@Serializable
data class RoadEdge(
    val points: List<Point2D>,
    val prob: Float = 1.0f
)

/**
 * 예상 주행 경로
 */
@Serializable
data class PathPlan(
    val points: List<Point2D>,
    val prob: Float = 1.0f,
    val validLen: Float = 0.0f  // 유효 길이 (m)
)

/**
 * 주변 차량 상세 정보
 */
@Serializable
data class RadarTrack(
    val trackId: Int,
    val dRel: Float,           // 거리 (m)
    val yRel: Float,           // 좌우 위치 (m)
    val vRel: Float,           // 상대 속도 (m/s)
    val aRel: Float = 0.0f,    // 상대 가속도 (m/s²)
    val oncoming: Boolean = false,  // 반대편 차량 여부
    val stationary: Boolean = false, // 정지 차량 여부
    val prob: Float = 1.0f     // 신뢰도
)

/**
 * 신호등/표지판 정보
 */
@Serializable
data class RoadSign(
    val type: String,          // 신호등, 정지표지판 등
    val distance: Float,       // 거리 (m)
    val state: String = ""     // 신호등 색상 등
)

/**
 * AI 모델 예측 데이터
 */
@Serializable
data class ModelV2Data(
    val leftLane: LaneLine? = null,      // 왼쪽 차선
    val rightLane: LaneLine? = null,     // 오른쪽 차선
    val leftEdge: RoadEdge? = null,      // 왼쪽 도로 가장자리
    val rightEdge: RoadEdge? = null,     // 오른쪽 도로 가장자리
    val path: PathPlan? = null,          // 예상 주행 경로
    val leadOne: RadarTrack? = null,     // 앞차 #1
    val leadTwo: RadarTrack? = null      // 앞차 #2
)

/**
 * 횡방향 제어 계획
 */
@Serializable
data class LateralPlan(
    val desiredCurvature: Float = 0.0f,  // 목표 곡률
    val laneWidth: Float = 3.7f,         // 차선 폭 (m)
    val dPathPoints: List<Point2D> = emptyList()  // 계획된 경로
)

/**
 * 종방향 제어 계획
 */
@Serializable
data class LongitudinalPlan(
    val speeds: List<Float> = emptyList(),     // 계획된 속도들
    val accels: List<Float> = emptyList(),     // 계획된 가속도들
    val distances: List<Float> = emptyList()   // 거리들
)

/**
 * 캘리브레이션 데이터
 */
@Serializable
data class LiveCalibration(
    val rpyCalib: List<Float> = listOf(0f, 0f, 0f),  // Roll, Pitch, Yaw
    val validBlocks: Int = 0
)

/**
 * 확장된 전체 주행 데이터
 */
@Serializable
data class ExtendedDrivingData(
    val timestamp: Long,
    val carState: CarState,
    val controlsState: ControlsState,
    val liveTracks: List<LiveTrack>,
    
    // 확장 데이터
    val modelV2: ModelV2Data? = null,
    val radarTracks: List<RadarTrack> = emptyList(),
    val lateralPlan: LateralPlan? = null,
    val longitudinalPlan: LongitudinalPlan? = null,
    val liveCalibration: LiveCalibration? = null,
    val roadSigns: List<RoadSign> = emptyList()
)