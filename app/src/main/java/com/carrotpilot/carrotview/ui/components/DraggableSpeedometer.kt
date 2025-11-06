package com.carrotpilot.carrotview.ui.components

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 드래그 가능한 속도계 컴포넌트 (차량용 최적화)
 */
class DraggableSpeedometer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DraggableUIComponent(context, attrs, defStyleAttr) {
    
    private val speedTextView: TextView
    private val unitTextView: TextView
    private val cruiseTextView: TextView
    
    init {
        // 세로 레이아웃
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            setPadding(24, 24, 24, 24)
        }
        
        // 현재 속도 (매우 크게)
        speedTextView = TextView(context).apply {
            text = "--"
            textSize = 80f
            setTextColor(0xFFFFFFFF.toInt())  // 순백색
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            setShadowLayer(8f, 0f, 4f, 0x40000000)  // 그림자
            includeFontPadding = false
        }
        container.addView(speedTextView)
        
        // 단위 (km/h)
        unitTextView = TextView(context).apply {
            text = "km/h"
            textSize = 24f
            setTextColor(0xFFB0B0B0.toInt())  // 밝은 회색
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = -12
            layoutParams = params
        }
        container.addView(unitTextView)
        
        // 크루즈 속도 (크게)
        cruiseTextView = TextView(context).apply {
            text = "--"
            textSize = 36f
            setTextColor(0xFF00BCD4.toInt())  // 시안 (오토파일럿 색상)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 8
            layoutParams = params
        }
        container.addView(cruiseTextView)
        
        addView(container)
    }
    
    /**
     * 속도 업데이트
     */
    fun updateSpeed(speedKmh: Float, cruiseKmh: Float) {
        speedTextView.text = String.format("%.0f", speedKmh)
        cruiseTextView.text = String.format("%.0f", cruiseKmh)
    }
}
