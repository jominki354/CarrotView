package com.carrotpilot.carrotview.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.carrotpilot.carrotview.data.models.LiveTrack

/**
 * Tesla 스타일 차량 시각화 뷰 - 주변 환경 표시
 */
class VehicleVisualizationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var liveTracks = listOf<LiveTrack>()
    private var steeringAngle = 0f

    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2C2C2C")
        style = Paint.Style.FILL
    }

    private val lanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
    }

    private val carPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A90E2")
        style = Paint.Style.FILL
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6B6B")
        style = Paint.Style.FILL
    }

    private val trackStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4444")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun updateTracks(tracks: List<LiveTrack>) {
        liveTracks = tracks
        invalidate()
    }

    fun setSteeringAngle(angle: Float) {
        steeringAngle = angle
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val bottomY = height.toFloat()

        // 도로 배경
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), roadPaint)

        // 차선 그리기
        val laneWidth = width / 4f
        for (i in 1..3) {
            val x = i * laneWidth
            canvas.drawLine(x, 0f, x, height.toFloat(), lanePaint)
        }

        // 주변 차량 그리기
        liveTracks.forEach { track ->
            drawVehicle(canvas, centerX, bottomY, track)
        }

        // 내 차량 그리기 (하단 중앙)
        drawMyVehicle(canvas, centerX, bottomY - 100f)
    }

    private fun drawVehicle(canvas: Canvas, centerX: Float, bottomY: Float, track: LiveTrack) {
        // 거리를 화면 좌표로 변환 (멀수록 위쪽)
        val scale = 5f // 1m = 5px
        val y = bottomY - (track.dRel * scale) - 100f
        val x = centerX + (track.yRel * 50f) // 좌우 위치

        // 화면 범위 체크
        if (y < 0 || y > height) return

        val vehicleWidth = 60f
        val vehicleHeight = 100f

        // 차량 사각형
        val rect = RectF(
            x - vehicleWidth / 2,
            y - vehicleHeight / 2,
            x + vehicleWidth / 2,
            y + vehicleHeight / 2
        )

        // 거리에 따라 색상 변경 (가까울수록 빨강)
        val alpha = if (track.dRel < 20f) 255 else 180
        trackPaint.alpha = alpha
        trackStrokePaint.alpha = alpha

        canvas.drawRoundRect(rect, 10f, 10f, trackPaint)
        canvas.drawRoundRect(rect, 10f, 10f, trackStrokePaint)

        // 거리 표시
        val distPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${track.dRel.toInt()}m", x, y, distPaint)
    }

    private fun drawMyVehicle(canvas: Canvas, x: Float, y: Float) {
        val vehicleWidth = 80f
        val vehicleHeight = 120f

        val rect = RectF(
            x - vehicleWidth / 2,
            y - vehicleHeight / 2,
            x + vehicleWidth / 2,
            y + vehicleHeight / 2
        )

        canvas.drawRoundRect(rect, 15f, 15f, carPaint)

        // 조향각 표시 (간단한 화살표)
        if (kotlin.math.abs(steeringAngle) > 5f) {
            val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }

            val arrowLength = 40f
            val arrowX = x + (steeringAngle * 2f)
            canvas.drawLine(x, y - vehicleHeight / 2 - 20f, arrowX, y - vehicleHeight / 2 - 60f, arrowPaint)
        }
    }
}