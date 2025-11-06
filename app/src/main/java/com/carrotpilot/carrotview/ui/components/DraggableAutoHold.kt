package com.carrotpilot.carrotview.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * 드래그 가능한 오토홀드 컴포넌트
 */
class DraggableAutoHold @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DraggableUIComponent(context, attrs, defStyleAttr) {
    
    private val container: LinearLayout
    private val autoTextView: TextView
    private val holdTextView: TextView
    
    private var isActive = false
    
    init {
        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 16, 20, 16)
        }
        
        // AUTO 텍스트
        autoTextView = TextView(context).apply {
            text = "AUTO"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        container.addView(autoTextView)
        
        // HOLD 텍스트
        holdTextView = TextView(context).apply {
            text = "HOLD"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 4
            layoutParams = params
        }
        container.addView(holdTextView)
        
        updateBackground(false)
        addView(container)
    }
    
    /**
     * 오토홀드 상태 업데이트
     */
    fun updateStatus(active: Boolean) {
        if (isActive != active) {
            isActive = active
            updateBackground(active)
        }
    }
    
    private fun updateBackground(active: Boolean) {
        val drawable = GradientDrawable().apply {
            if (active) {
                // 활성: 초록색 배경
                setColor(0xFF4CAF50.toInt())
                setStroke(0, Color.TRANSPARENT)
            } else {
                // 비활성: 어두운 배경 + 흰색 테두리
                setColor(0xFF2A2A2A.toInt())
                setStroke(4, 0xFFFFFFFF.toInt())
            }
            cornerRadius = 8f
        }
        container.background = drawable
        
        // 그림자 효과
        container.elevation = if (active) 8f else 4f
    }
}