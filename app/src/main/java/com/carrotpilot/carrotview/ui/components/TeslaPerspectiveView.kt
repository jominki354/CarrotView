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
import kotlin.math.pow

/**
 * Tesla 스타일 3D 원근 시각화 뷰
 * - 3D 원근 투영
 * - 밝고 선명한 색상
 * - 글로우 효과
 * - 실제 Tesla와 동일한 디자인
 */
class TeslaPerspectiveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var extendedData: ExtendedDrivingData? = null
    private var targetSteeringAngle = 0f
    private var currentSteeringAngle = 0f
    
    private val vehiclePositions = mutableMapOf<Int, AnimatedPosition>()
    private var frameTime = 0L
    
    // 3D 원근 설정 (더 누워진 각도)
    private val perspectiveRatio = 0.7f  // 원근 강도 증가
    private val vanishingPointY = 0.1f   // 소실점 더 위로
    
    // Tesla 정확한 색상
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#000000")  // 순수 검정
        style = Paint.Style.FILL
    }

    private val lanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")  // 순수 흰색
        style = Paint.Style.STROKE
        strokeWidth = 12f
        alpha = 255
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#80FFFFFF"))
    }

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")  // 흰색으로 변경
        style = Paint.Style.STROKE
        strokeWidth = 18f
        alpha = 200  // 약간 투명
        strokeCap = Paint.Cap.ROUND
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#80FFFFFF"))
    }

    private val myCarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")  // 흰색으로 변경
        style = Paint.Style.FILL
        setShadowLayer(20f, 0f, 4f, Color.parseColor("#80000000"))
    }

    private val leadCarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC")  // 밝은 회색
        style = Paint.Style.FILL
        setShadowLayer(25f, 0f, 4f, Color.parseColor("#80000000"))
    }

    private val otherCarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888")  // 어두운 회색
        style = Paint.Style.FILL
        setShadowLayer(15f, 0f, 2f, Color.parseColor("#60000000"))
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        // 텍스트 그림자 (가독성)
        setShadowLayer(8f, 0f, 2f, Color.BLACK)
    }

    private data class AnimatedPosition(
        var currentX: Float,
        var currentY: Float,
        var targetX: Float,
        var targetY: Float,
        var currentVRel: Float,
        var targetVRel: Float
    )
    
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
    
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)  // 그림자 효과를 위해 필요
        
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

    fun updateData(data: ExtendedDrivingData) {
        extendedData = data
        targetSteeringAngle = data.carState.steeringAngleDeg
        
        // 데이터가 업데이트되고 뷰가 보이면 animator 시작
        if (visibility == VISIBLE && !animator.isRunning) {
            animator.start()
        }
        
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
        
        val currentTrackIds = data.radarTracks.map { it.trackId }.toSet()
        vehiclePositions.keys.retainAll(currentTrackIds)
    }
    
    private fun updateAnimations(deltaTime: Float) {
        val steeringDiff = targetSteeringAngle - currentSteeringAngle
        currentSteeringAngle += steeringDiff * 5f * deltaTime
        
        vehiclePositions.values.forEach { pos ->
            val smoothFactor = 8f * deltaTime
            pos.currentX += (pos.targetX - pos.currentX) * smoothFactor
            pos.currentY += (pos.targetY - pos.currentY) * smoothFactor
            pos.currentVRel += (pos.targetVRel - pos.currentVRel) * smoothFactor
        }
    }

    /**
     * 3D 원근 투영 변환 (더 누워진 각도)
     */
    private fun apply3DPerspective(worldX: Float, worldY: Float, centerX: Float, bottomY: Float): PointF {
        val normalizedDistance = worldY / 80f  // 더 가까운 범위
        val perspectiveScale = 1.0f - (normalizedDistance * perspectiveRatio)
        
        // 더 극적인 Y 압축 (누워진 효과)
        val screenY = bottomY - (worldY * 12f)  // 더 길게
        val vanishingY = height * vanishingPointY
        
        // X 좌표에 강한 원근 적용
        val screenX = centerX + (worldX * 120f * perspectiveScale)
        
        // Y 좌표를 더 극적으로 압축
        val adjustedY = vanishingY + (screenY - vanishingY) * (0.15f + perspectiveScale * 0.85f)
        
        return PointF(screenX, adjustedY)
    }

    /**
     * 거리에 따른 스케일 계산
     */
    private fun getScaleForDistance(distance: Float): Float {
        val normalizedDistance = distance / 100f
        return 1.0f - (normalizedDistance * 0.7f)  // 멀수록 작게 (최대 70% 축소)
    }

    /**
     * 거리에 따른 투명도 계산
     */
    private fun getAlphaForDistance(distance: Float): Int {
        val normalizedDistance = distance / 100f
        return (255 * (1.0f - normalizedDistance * 0.5f)).toInt()  // 멀수록 흐리게
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val data = extendedData ?: return

        val centerX = width / 2f
        val bottomY = height.toFloat()

        // 1. 순수 검정 배경
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), roadPaint)

        // 2. 차선 그리기 (3D 원근)
        data.modelV2?.let { model ->
            model.leftLane?.let { drawLane3D(canvas, it, centerX, bottomY, true) }
            model.rightLane?.let { drawLane3D(canvas, it, centerX, bottomY, false) }
        }

        // 3. 예상 경로 그리기 (3D 원근, 글로우)
        data.modelV2?.path?.let { drawPath3D(canvas, it, centerX, bottomY) }

        // 4. 주변 차량 그리기 (3D 원근, 글로우)
        drawVehicles3D(canvas, data, centerX, bottomY)

        // 5. 내 차량 그리기 (화면 하단 중앙에 고정, 크게)
        drawMyVehicle3D(canvas, centerX, bottomY - 220f)  // 더 위로 올려서 잘 보이게
    }

    private fun drawLane3D(canvas: Canvas, lane: LaneLine, centerX: Float, bottomY: Float, isLeft: Boolean) {
        if (lane.points.isEmpty()) return

        val path = Path()
        var first = true

        for (point in lane.points) {
            val screenPos = apply3DPerspective(point.y, point.x, centerX, bottomY)
            
            if (screenPos.y < 0 || screenPos.y > height) continue

            if (first) {
                path.moveTo(screenPos.x, screenPos.y)
                first = false
            } else {
                path.lineTo(screenPos.x, screenPos.y)
            }
        }

        // 거리에 따른 투명도
        val avgDistance = lane.points.map { it.x }.average().toFloat()
        lanePaint.alpha = getAlphaForDistance(avgDistance)
        
        // 점선 효과
        lanePaint.pathEffect = DashPathEffect(floatArrayOf(40f, 30f), 0f)
        canvas.drawPath(path, lanePaint)
        lanePaint.pathEffect = null
    }

    private fun drawPath3D(canvas: Canvas, pathPlan: PathPlan, centerX: Float, bottomY: Float) {
        if (pathPlan.points.isEmpty()) return

        val path = Path()
        var first = true

        for (point in pathPlan.points) {
            val screenPos = apply3DPerspective(point.y, point.x, centerX, bottomY)
            
            if (screenPos.y < 0 || screenPos.y > height) continue

            if (first) {
                path.moveTo(screenPos.x, screenPos.y)
                first = false
            } else {
                path.lineTo(screenPos.x, screenPos.y)
            }
        }

        pathPaint.alpha = 255
        canvas.drawPath(path, pathPaint)
    }

    private fun drawVehicles3D(canvas: Canvas, data: ExtendedDrivingData, centerX: Float, bottomY: Float) {
        data.radarTracks.forEach { track ->
            val pos = vehiclePositions[track.trackId]
            if (pos != null) {
                drawVehicle3D(canvas, pos.currentX, pos.currentY, centerX, bottomY, track, false)
            }
        }

        data.modelV2?.leadOne?.let { lead ->
            drawVehicle3D(canvas, lead.yRel, lead.dRel, centerX, bottomY, lead, true)
        }
    }

    private fun drawVehicle3D(canvas: Canvas, yRel: Float, dRel: Float, centerX: Float, bottomY: Float, track: RadarTrack, isLead: Boolean) {
        val screenPos = apply3DPerspective(yRel, dRel, centerX, bottomY)
        
        if (screenPos.y < 0 || screenPos.y > height) return

        val scale = getScaleForDistance(dRel)
        val vehicleWidth = 140f * scale  // 100f -> 140f
        val vehicleHeight = 200f * scale  // 150f -> 200f

        val rect = RectF(
            screenPos.x - vehicleWidth / 2,
            screenPos.y - vehicleHeight / 2,
            screenPos.x + vehicleWidth / 2,
            screenPos.y + vehicleHeight / 2
        )

        val paint = if (isLead) leadCarPaint else otherCarPaint
        paint.alpha = getAlphaForDistance(dRel)
        
        canvas.drawRoundRect(rect, 12f * scale, 12f * scale, paint)

        // 거리 표시
        if (dRel < 60f) {
            textPaint.textSize = 28f * scale
            textPaint.alpha = getAlphaForDistance(dRel)
            canvas.drawText("${dRel.toInt()}m", screenPos.x, screenPos.y - vehicleHeight / 2 - 15f * scale, textPaint)
        }
    }

    private fun drawMyVehicle3D(canvas: Canvas, x: Float, y: Float) {
        // openpilot처럼 훨씬 크게
        val vehicleWidth = 280f
        val vehicleHeight = 400f

        val left = x - vehicleWidth / 2
        val top = y - vehicleHeight / 2
        val right = x + vehicleWidth / 2
        val bottom = y + vehicleHeight / 2

        // 실제 자동차 모양 그리기
        
        // 1. 메인 차체 (둥근 사각형)
        val bodyRect = RectF(left + 30f, top + 80f, right - 30f, bottom - 20f)
        canvas.drawRoundRect(bodyRect, 40f, 40f, myCarPaint)

        // 2. 후드 (앞부분)
        val hoodRect = RectF(left + 40f, top + 20f, right - 40f, top + 120f)
        canvas.drawRoundRect(hoodRect, 30f, 30f, myCarPaint)

        // 3. 앞유리 (어두운 색)
        val windshieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#666666")
            style = Paint.Style.FILL
            setShadowLayer(10f, 0f, 2f, Color.parseColor("#40000000"))
        }
        val windshieldRect = RectF(
            left + 50f,
            top + 100f,
            right - 50f,
            top + 200f
        )
        canvas.drawRoundRect(windshieldRect, 20f, 20f, windshieldPaint)

        // 4. 사이드 미러 (좌우)
        val mirrorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 2f, Color.parseColor("#60000000"))
        }
        // 왼쪽 미러
        canvas.drawRoundRect(
            RectF(left - 10f, top + 140f, left + 30f, top + 180f),
            10f, 10f, mirrorPaint
        )
        // 오른쪽 미러
        canvas.drawRoundRect(
            RectF(right - 30f, top + 140f, right + 10f, top + 180f),
            10f, 10f, mirrorPaint
        )

        // 5. 헤드라이트 (LED 스타일)
        val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.FILL
            setShadowLayer(15f, 0f, 0f, Color.parseColor("#80FFFFFF"))
        }
        // 왼쪽 헤드라이트
        canvas.drawRoundRect(
            RectF(left + 50f, top + 30f, left + 90f, top + 60f),
            8f, 8f, headlightPaint
        )
        // 오른쪽 헤드라이트
        canvas.drawRoundRect(
            RectF(right - 90f, top + 30f, right - 50f, top + 60f),
            8f, 8f, headlightPaint
        )

        // 6. 테일라이트 (빨간색)
        val taillightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF4444")
            style = Paint.Style.FILL
            setShadowLayer(12f, 0f, 0f, Color.parseColor("#80FF0000"))
        }
        // 왼쪽 테일라이트
        canvas.drawRoundRect(
            RectF(left + 40f, bottom - 40f, left + 70f, bottom - 25f),
            5f, 5f, taillightPaint
        )
        // 오른쪽 테일라이트
        canvas.drawRoundRect(
            RectF(right - 70f, bottom - 40f, right - 40f, bottom - 25f),
            5f, 5f, taillightPaint
        )

        // 조향각 표시 (화살표)
        if (abs(currentSteeringAngle) > 5f) {
            val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00D563")
                style = Paint.Style.STROKE
                strokeWidth = 8f
                strokeCap = Paint.Cap.ROUND
                setShadowLayer(10f, 0f, 0f, Color.parseColor("#8000D563"))
            }

            val arrowLength = 80f
            val arrowX = x + (currentSteeringAngle * 3f)
            canvas.drawLine(x, top - 30f, arrowX, top - arrowLength, arrowPaint)
            
            // 화살표 머리
            val arrowHeadSize = 20f
            canvas.drawLine(arrowX, top - arrowLength, arrowX - arrowHeadSize, top - arrowLength + arrowHeadSize, arrowPaint)
            canvas.drawLine(arrowX, top - arrowLength, arrowX + arrowHeadSize, top - arrowLength + arrowHeadSize, arrowPaint)
        }
    }
}