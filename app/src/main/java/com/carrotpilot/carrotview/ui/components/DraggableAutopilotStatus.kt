package com.carrotpilot.carrotview.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.carrotpilot.carrotview.R

/**
 * 드래그 가능한 오토파일럿 상태 컴포넌트
 */
class DraggableAutopilotStatus @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DraggableUIComponent(context, attrs, defStyleAttr) {
    
    private val indicatorView: View
    private val statusTextView: TextView
    private val stateTextView: TextView
    
    init {
        // 배경 설정
        setBackgroundColor(ContextCompat.getColor(context, R.color.overlay_dark))
        setPadding(16, 16, 16, 16)
        
        // 세로 레이아웃
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        
        // 상태 표시 (인디케이터 + 텍스트)
        val statusRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        // 인디케이터
        indicatorView = View(context).apply {
            val size = 12
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = 8
            }
            setBackgroundResource(R.drawable.circle_indicator)
        }
        statusRow.addView(indicatorView)
        
        // 상태 텍스트
        statusTextView = TextView(context).apply {
            text = "크루즈"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }
        statusRow.addView(statusTextView)
        
        container.addView(statusRow)
        
        // 상태 설명
        stateTextView = TextView(context).apply {
            text = "대기"
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.status_active))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 4
            layoutParams = params
        }
        container.addView(stateTextView)
        
        addView(container)
    }
    
    /**
     * 오토파일럿 상태 업데이트
     */
    fun updateStatus(enabled: Boolean, active: Boolean) {
        when {
            enabled && active -> {
                statusTextView.text = "오토파일럿"
                stateTextView.text = "활성"
                stateTextView.setTextColor(ContextCompat.getColor(context, R.color.status_active))
                indicatorView.setBackgroundColor(ContextCompat.getColor(context, R.color.status_active))
            }
            enabled -> {
                statusTextView.text = "오토파일럿"
                stateTextView.text = "준비"
                stateTextView.setTextColor(ContextCompat.getColor(context, R.color.autopilot_standby))
                indicatorView.setBackgroundColor(ContextCompat.getColor(context, R.color.autopilot_standby))
            }
            else -> {
                statusTextView.text = "크루즈"
                stateTextView.text = "대기"
                stateTextView.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                indicatorView.setBackgroundColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }
}
