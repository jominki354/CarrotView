package com.carrotpilot.carrotview.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.carrotpilot.carrotview.data.models.*
import kotlin.math.min
import kotlin.math.abs

/**
 * Tesla 스타일 완전한 주행 시각화 뷰
 * - 부드러운 60FPS 애니메이션
 * - 실시간 차선 표시
 * - 주변 차량 위치 및 속도
 * - 예상 주행 경로
 * - 도로 가장자리
 * - 신호등/표지판
 */
class TeslaStyleVisualizationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 데이터
    private var extendedData: ExtendedDrivingData? = null
    private var targetSteeringAngle = 0f
    private var currentSteeringAngle = 0f
    
    // 부드러운 애니메이션을 위한 보간 데이터
    private val vehiclePositions = mutableMapOf<Int, AnimatedPosition>()
    
    // 애니메이션 프레임 카운터
    private var frameTime = 0L
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = Long.MAX_VALUE
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val currentTime = System.currentTimeMillis()
            if (frameTime > 0) {
                val deltaTime = (currentTime - frameTime) / 1000f
                updateAnimations(deltaTime)
            }
            frameTime = currentTime
            invalidate()
        }
    }
    
    // 애니메이션 위치 데이터 클래스
    private data class AnimatedPosition(
        var currentX: Float,
        var currentY: Float,
        var targetX: Float,
        var targetY: Float,
        var currentVRel: Float,
        var targetVRel: Float
    )
    
    init {
        // 뷰가 attach될 때 애니메이션 시작
        addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                // 뷰가 보일 때만 animator 시작
                if (visibility == VISIBLE && extendedData != null) {
                    animator.start()
                }
            }
            
            override fun onViewDetachedFromWindow(v: View) {
                animator.cancel()
            }
        })
    }
    
    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        
        // 뷰가 보이지 않으면 animator 중지
        if (visibility != VISIBLE) {
            if (animator.isRunning) {
                animator.cancel()
            }
        } else if (extendedData != null && !animator.isRunning) {
            // 뷰가 보이고 데이터가 있으면 animator 시작
            animator.start()
        }
    }

    // 스케일 설정 (더 크게 보이도록)
    private val metersPerPixel = 0.08f  // 1픽셀 = 0.08m (이전 0.15에서 축소)
    private val viewDistance = 80f       // 80m 앞까지 표시 (더 가까이)

    // 페인트 객체들
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        style = Paint.Style.FILL
    }

    private val lanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 8f  // 3 -> 8 (더 굵게)
        alpha = 220
    }

    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#666666")
        style = Paint.Style.STROKE
        strokeWidth = 6f  // 4 -> 6
    }

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#10B981")  // 더 선명한 초록색
        style = Paint.Style.STROKE
        strokeWidth = 12f  // 6 -> 12 (더 굵게)
        alpha = 230
    }

    private val myCarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A90E2")
        style = Paint.Style.FILL
    }

    private val leadCarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6B6B")
        style = Paint.Style.FILL
    }

    private val otherCarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFA500")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    fun updateData(data: ExtendedDrivingData) {
        extendedData = data
        targetSteeringAngle = data.carState.steeringAngleDeg
        
        // 차량 위치 업데이트 (부드러운 전환을 위해)
        data.radarTracks.forEach { track ->
            val pos = vehiclePositions.getOrPut(track.trackId) {
                AnimatedPosition(
                    track.yRel, track.dRel,
                    track.yRel, track.dRel,
                    track.vRel, track.vRel
                )
            }
            pos.targetX = track.yRel
            pos.targetY = track.dRel
            pos.targetVRel = track.vRel
        }
        
        // 더 이상 존재하지 않는 차량 제거
        val currentTrackIds = data.radarTracks.map { it.trackId }.toSet()
        vehiclePositions.keys.retainAll(currentTrackIds)
        
        // 데이터가 업데이트되고 뷰가 보이면 animator 시작
        if (visibility == VISIBLE && !animator.isRunning) {
            animator.start()
        }
    }
    
    private fun updateAnimations(deltaTime: Float) {
        // 조향각 부드럽게 보간
        val steeringDiff = targetSteeringAngle - currentSteeringAngle
        currentSteeringAngle += steeringDiff * 5f * deltaTime // 5배 속도로 따라감
        
        // 차량 위치 부드럽게 보간
        vehiclePositions.values.forEach { pos ->
            val smoothFactor = 8f * deltaTime // 8배 속도
            pos.currentX += (pos.targetX - pos.currentX) * smoothFactor
            pos.currentY += (pos.targetY - pos.currentY) * smoothFactor
            pos.currentVRel += (pos.targetVRel - pos.currentVRel) * smoothFactor
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val data = extendedData ?: return

        val centerX = width / 2f
        val bottomY = height.toFloat()

        // 1. 도로 배경
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), roadPaint)

        // 2. 도로 가장자리 그리기
        data.modelV2?.let { model ->
            model.leftEdge?.let { drawRoadEdge(canvas, it, centerX, bottomY, true) }
            model.rightEdge?.let { drawRoadEdge(canvas, it, centerX, bottomY, false) }
        }

        // 3. 차선 그리기
        data.modelV2?.let { model ->
            model.leftLane?.let { drawLaneLine(canvas, it, centerX, bottomY) }
            model.rightLane?.let { drawLaneLine(canvas, it, centerX, bottomY) }
        }

        // 4. 예상 주행 경로 그리기
        data.modelV2?.path?.let { drawPath(canvas, it, centerX, bottomY) }

        // 5. 주변 차량 그리기
        drawVehicles(canvas, data, centerX, bottomY)

        // 6. 내 차량 그리기
        drawMyVehicle(canvas, centerX, bottomY - 80f)

        // 7. 정보 오버레이
        drawInfoOverlay(canvas, data)
    }

    private fun drawRoadEdge(canvas: Canvas, edge: RoadEdge, centerX: Float, bottomY: Float, isLeft: Boolean) {
        if (edge.points.isEmpty()) return

        val path = Path()
        var first = true

        for (point in edge.points) {
            val screenX = centerX + (point.y / metersPerPixel)
            val screenY = bottomY - (point.x / metersPerPixel)

            if (screenY < 0 || screenY > height) continue

            if (first) {
                path.moveTo(screenX, screenY)
                first = false
            } else {
                path.lineTo(screenX, screenY)
            }
        }

        edgePaint.alpha = (edge.prob * 255).toInt()
        canvas.drawPath(path, edgePaint)
    }

    private fun drawLaneLine(canvas: Canvas, lane: LaneLine, centerX: Float, bottomY: Float) {
        if (lane.points.isEmpty()) return

        val path = Path()
        var first = true

        for (point in lane.points) {
            val screenX = centerX + (point.y / metersPerPixel)
            val screenY = bottomY - (point.x / metersPerPixel)

            if (screenY < 0 || screenY > height) continue

            if (first) {
                path.moveTo(screenX, screenY)
                first = false
            } else {
                path.lineTo(screenX, screenY)
            }
        }

        // 신뢰도에 따라 투명도 조절
        lanePaint.alpha = (lane.prob * 255).toInt()
        
        // 점선 효과
        lanePaint.pathEffect = DashPathEffect(floatArrayOf(30f, 20f), 0f)
        canvas.drawPath(path, lanePaint)
        lanePaint.pathEffect = null
    }

    private fun drawPath(canvas: Canvas, pathPlan: PathPlan, centerX: Float, bottomY: Float) {
        if (pathPlan.points.isEmpty()) return

        val path = Path()
        var first = true

        for (point in pathPlan.points) {
            val screenX = centerX + (point.y / metersPerPixel)
            val screenY = bottomY - (point.x / metersPerPixel)

            if (screenY < 0 || screenY > height) continue

            if (first) {
                path.moveTo(screenX, screenY)
                first = false
            } else {
                path.lineTo(screenX, screenY)
            }
        }

        pathPaint.alpha = (pathPlan.prob * 200).toInt()
        canvas.drawPath(path, pathPaint)
    }

    private fun drawVehicles(canvas: Canvas, data: ExtendedDrivingData, centerX: Float, bottomY: Float) {
        // 레이더 트랙 그리기 (더 정확한 데이터)
        data.radarTracks.forEach { track ->
            drawRadarVehicle(canvas, track, centerX, bottomY)
        }

        // LiveTracks 그리기 (비전 기반)
        data.liveTracks.forEach { track ->
            drawLiveTrackVehicle(canvas, track, centerX, bottomY)
        }

        // Lead 차량 강조 표시
        data.modelV2?.let { model ->
            model.leadOne?.let { drawLeadVehicle(canvas, it, centerX, bottomY, 1) }
            model.leadTwo?.let { drawLeadVehicle(canvas, it, centerX, bottomY, 2) }
        }
    }

    private fun drawRadarVehicle(canvas: Canvas, track: RadarTrack, centerX: Float, bottomY: Float) {
        val screenX = centerX + (track.yRel / metersPerPixel)
        val screenY = bottomY - (track.dRel / metersPerPixel) - 100f

        if (screenY < 0 || screenY > height) return

        val vehicleWidth = 90f  // 50 -> 90 (더 크게)
        val vehicleHeight = 140f  // 80 -> 140 (더 크게)

        val rect = RectF(
            screenX - vehicleWidth / 2,
            screenY - vehicleHeight / 2,
            screenX + vehicleWidth / 2,
            screenY + vehicleHeight / 2
        )

        // 차량 타입에 따라 색상 변경
        val paint = when {
            track.stationary -> otherCarPaint.apply { alpha = 150 }
            track.oncoming -> otherCarPaint.apply { alpha = 200 }
            else -> otherCarPaint.apply { alpha = 255 }
        }

        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // 거리 표시
        val distText = "${track.dRel.toInt()}m"
        canvas.drawText(distText, screenX, screenY - vehicleHeight / 2 - 10f, smallTextPaint)

        // 상대 속도 표시 (화살표)
        if (abs(track.vRel) > 1f) {
            drawSpeedArrow(canvas, screenX, screenY + vehicleHeight / 2 + 5f, track.vRel)
        }
    }

    private fun drawLiveTrackVehicle(canvas: Canvas, track: LiveTrack, centerX: Float, bottomY: Float) {
        val screenX = centerX + (track.yRel / metersPerPixel)
        val screenY = bottomY - (track.dRel / metersPerPixel) - 80f

        if (screenY < 0 || screenY > height) return

        val vehicleWidth = 45f
        val vehicleHeight = 75f

        val rect = RectF(
            screenX - vehicleWidth / 2,
            screenY - vehicleHeight / 2,
            screenX + vehicleWidth / 2,
            screenY + vehicleHeight / 2
        )

        otherCarPaint.alpha = 200
        canvas.drawRoundRect(rect, 8f, 8f, otherCarPaint)
    }

    private fun drawLeadVehicle(canvas: Canvas, track: RadarTrack, centerX: Float, bottomY: Float, leadNum: Int) {
        val screenX = centerX + (track.yRel / metersPerPixel)
        val screenY = bottomY - (track.dRel / metersPerPixel) - 80f

        if (screenY < 0 || screenY > height) return

        val vehicleWidth = 60f
        val vehicleHeight = 90f

        val rect = RectF(
            screenX - vehicleWidth / 2,
            screenY - vehicleHeight / 2,
            screenX + vehicleWidth / 2,
            screenY + vehicleHeight / 2
        )

        // Lead 차량은 빨간색으로 강조
        leadCarPaint.alpha = 255
        canvas.drawRoundRect(rect, 10f, 10f, leadCarPaint)

        // 테두리 강조
        val strokePaint = Paint(leadCarPaint).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(rect, 10f, 10f, strokePaint)

        // Lead 번호 표시
        canvas.drawText("LEAD $leadNum", screenX, screenY, textPaint)

        // 거리와 속도 표시
        val distText = "${track.dRel.toInt()}m"
        val speedText = "${(track.vRel * 3.6).toInt()}km/h"
        canvas.drawText(distText, screenX, screenY - vehicleHeight / 2 - 30f, textPaint)
        canvas.drawText(speedText, screenX, screenY + vehicleHeight / 2 + 30f, smallTextPaint)
    }

    private fun drawSpeedArrow(canvas: Canvas, x: Float, y: Float, vRel: Float) {
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (vRel < 0) Color.RED else Color.GREEN
            style = Paint.Style.FILL
        }

        val arrowSize = min(abs(vRel) * 3f, 20f)
        val path = Path()

        if (vRel < 0) {
            // 다가오는 화살표 (아래)
            path.moveTo(x, y + arrowSize)
            path.lineTo(x - arrowSize / 2, y)
            path.lineTo(x + arrowSize / 2, y)
        } else {
            // 멀어지는 화살표 (위)
            path.moveTo(x, y - arrowSize)
            path.lineTo(x - arrowSize / 2, y)
            path.lineTo(x + arrowSize / 2, y)
        }
        path.close()

        canvas.drawPath(path, arrowPaint)
    }

    private fun drawMyVehicle(canvas: Canvas, x: Float, y: Float) {
        val vehicleWidth = 110f  // 70 -> 110 (더 크게)
        val vehicleHeight = 160f  // 100 -> 160 (더 크게)

        val rect = RectF(
            x - vehicleWidth / 2,
            y - vehicleHeight / 2,
            x + vehicleWidth / 2,
            y + vehicleHeight / 2
        )

        canvas.drawRoundRect(rect, 12f, 12f, myCarPaint)

        // 조향각 표시
        if (abs(currentSteeringAngle) > 3f) {
            val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }

            val arrowLength = 50f
            val arrowX = x + (currentSteeringAngle * 1.5f)
            canvas.drawLine(x, y - vehicleHeight / 2 - 10f, arrowX, y - vehicleHeight / 2 - arrowLength, arrowPaint)

            // 화살표 머리
            val headSize = 15f
            canvas.drawLine(arrowX, y - vehicleHeight / 2 - arrowLength, 
                arrowX - headSize, y - vehicleHeight / 2 - arrowLength + headSize, arrowPaint)
            canvas.drawLine(arrowX, y - vehicleHeight / 2 - arrowLength, 
                arrowX + headSize, y - vehicleHeight / 2 - arrowLength + headSize, arrowPaint)
        }
    }

    private fun drawInfoOverlay(canvas: Canvas, data: ExtendedDrivingData) {
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
        }

        var yPos = 40f

        // 차선 폭 정보
        data.lateralPlan?.let { plan ->
            canvas.drawText("차선 폭: ${String.format("%.1f", plan.laneWidth)}m", 20f, yPos, overlayPaint)
            yPos += 35f
        }

        // 추적 차량 수
        val trackCount = data.radarTracks.size + data.liveTracks.size
        canvas.drawText("추적 차량: ${trackCount}대", 20f, yPos, overlayPaint)
        yPos += 35f

        // Lead 차량 정보
        data.modelV2?.leadOne?.let { lead ->
            canvas.drawText("앞차: ${lead.dRel.toInt()}m", 20f, yPos, overlayPaint)
        }
    }
}