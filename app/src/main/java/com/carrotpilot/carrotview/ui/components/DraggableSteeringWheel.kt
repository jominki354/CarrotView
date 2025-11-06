package com.carrotpilot.carrotview.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 드래그 가능한 조향각 핸들 컴포넌트
 */
class DraggableSteeringWheel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DraggableUIComponent(context, attrs, defStyleAttr) {
    
    private val wheelView: SteeringWheelView
    private val angleTextView: TextView
    
    init {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        
        // 핸들 뷰
        wheelView = SteeringWheelView(context).apply {
            layoutParams = LinearLayout.LayoutParams(120, 120)
        }
        container.addView(wheelView)
        
        // 각도 텍스트
        angleTextView = TextView(context).apply {
            text = "0°"
            textSize = 20f
            setTextColor(0xFFE0E0E0.toInt())
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 8
            layoutParams = params
        }
        container.addView(angleTextView)
        
        addView(container)
    }
    
    /**
     * 조향각 업데이트
     */
    fun updateSteeringAngle(angleDegrees: Float) {
        wheelView.setAngle(angleDegrees)
        angleTextView.text = String.format("%.0f°", angleDegrees)
    }
    
    /**
     * 조향각 핸들 그리기 뷰
     */
    private class SteeringWheelView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
    ) : android.view.View(context, attrs) {
        
        private var angle = 0f
        
        private val wheelPaint = Paint().apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
        }
        
        private val centerPaint = Paint().apply {
            color = 0xFF00BCD4.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        private val shadowPaint = Paint().apply {
            color = 0x40000000
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        
        fun setAngle(degrees: Float) {
            angle = degrees
            invalidate()
        }
        
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            
            val centerX = width / 2f
            val centerY = height / 2f
            val radius = (Math.min(width, height) / 2f) - 20f
            
            canvas.save()
            canvas.rotate(angle, centerX, centerY)
            
            // 그림자
            canvas.drawCircle(centerX, centerY + 2, radius, shadowPaint)
            
            // 외곽 원
            canvas.drawCircle(centerX, centerY, radius, wheelPaint)
            
            // 핸들 그립 (위)
            canvas.drawLine(
                centerX - 15f, centerY - radius + 10f,
                centerX + 15f, centerY - radius + 10f,
                wheelPaint
            )
            
            // 중앙 점
            canvas.drawCircle(centerX, centerY, 12f, centerPaint)
            
            canvas.restore()
        }
    }
}