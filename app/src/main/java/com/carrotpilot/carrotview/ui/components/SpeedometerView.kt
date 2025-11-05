package com.carrotpilot.carrotview.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * Tesla 스타일 속도계 뷰
 */
class SpeedometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentSpeed = 0f
    private var targetSpeed = 0f
    private var cruiseSpeed = 0f
    private var targetCruiseSpeed = 0f
    private var maxSpeed = 200f
    
    private var animatedSpeed = 0f
    private var animatedCruise = 0f

    private val speedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 120f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }

    private val cruisePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A90E2")
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A90E2")
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    init {
        // 부드러운 애니메이션을 위한 지속적인 업데이트
        post(object : Runnable {
            override fun run() {
                // 속도를 부드럽게 보간
                val speedDiff = targetSpeed - animatedSpeed
                animatedSpeed += speedDiff * 0.1f
                
                val cruiseDiff = targetCruiseSpeed - animatedCruise
                animatedCruise += cruiseDiff * 0.1f
                
                invalidate()
                postDelayed(this, 16) // ~60fps
            }
        })
    }
    
    fun setSpeed(speed: Float) {
        targetSpeed = speed
        currentSpeed = speed
    }

    fun setCruiseSpeed(speed: Float) {
        targetCruiseSpeed = speed
        cruiseSpeed = speed
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f - 40f

        // 배경 원호 그리기
        val rectF = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        canvas.drawArc(rectF, 135f, 270f, false, arcPaint)

        // 속도 진행 원호 그리기 (애니메이션된 값 사용)
        val sweepAngle = (animatedSpeed / maxSpeed) * 270f
        canvas.drawArc(rectF, 135f, sweepAngle, false, progressPaint)

        // 현재 속도 텍스트 (애니메이션된 값)
        val speedText = animatedSpeed.toInt().toString()
        canvas.drawText(speedText, centerX, centerY + 20f, speedPaint)

        // 단위 텍스트
        canvas.drawText("km/h", centerX, centerY + 70f, unitPaint)

        // 크루즈 속도 표시 (애니메이션된 값)
        if (animatedCruise > 0) {
            val cruiseText = "크루즈: ${animatedCruise.toInt()}"
            canvas.drawText(cruiseText, centerX, centerY + 120f, cruisePaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = min(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
        setMeasuredDimension(size, size)
    }
}