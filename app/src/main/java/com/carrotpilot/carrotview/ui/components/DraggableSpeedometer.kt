package com.carrotpilot.carrotview.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.carrotpilot.carrotview.R

/**
 * 드래그 가능한 속도계 컴포넌트
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
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }
        
        // 현재 속도
        speedTextView = TextView(context).apply {
            text = "--"
            textSize = 48f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            includeFontPadding = false
        }
        container.addView(speedTextView)
        
        // 단위
        unitTextView = TextView(context).apply {
            text = "km/h"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = -8
            layoutParams = params
        }
        container.addView(unitTextView)
        
        // 크루즈 속도
        cruiseTextView = TextView(context).apply {
            text = "--"
            textSize = 20f
            setTextColor(ContextCompat.getColor(context, R.color.autopilot_standby))
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
