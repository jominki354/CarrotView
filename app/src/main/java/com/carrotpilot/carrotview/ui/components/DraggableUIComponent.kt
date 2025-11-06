package com.carrotpilot.carrotview.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.carrotpilot.carrotview.R

/**
 * 드래그 가능한 UI 컴포넌트 베이스 클래스
 */
abstract class DraggableUIComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    // 편집 모드 상태
    var isEditMode: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    
    // 드래그 상태
    private var isDragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var dX = 0f
    private var dY = 0f
    
    // 크기 조절 상태
    private var isScaling = false
    private var currentScale = 1.0f
    private var initialWidth = 0
    private var initialHeight = 0
    private val minScale = 0.5f
    private val maxScale = 2.0f
    
    // 스케일 제스처 감지기
    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            if (!isEditMode) return false
            
            isScaling = true
            initialWidth = width
            initialHeight = height
            alpha = 0.7f
            return true
        }
        
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!isEditMode) return false
            
            currentScale *= detector.scaleFactor
            currentScale = currentScale.coerceIn(minScale, maxScale)
            
            val newWidth = (initialWidth * currentScale).toInt()
            val newHeight = (initialHeight * currentScale).toInt()
            
            layoutParams = layoutParams.apply {
                width = newWidth
                height = newHeight
            }
            
            return true
        }
        
        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
            alpha = 1.0f
            onScaleEnd()
        }
    })
    
    // 편집 모드 시각적 효과
    private val editModePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = ContextCompat.getColor(context, R.color.autopilot_active)
        isAntiAlias = true
    }
    
    private val editModeBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0x33FFFFFF  // 반투명 흰색
        isAntiAlias = true
    }
    
    init {
        setWillNotDraw(false)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEditMode) {
            return super.onTouchEvent(event)
        }
        
        // 스케일 제스처 먼저 처리
        scaleGestureDetector.onTouchEvent(event)
        
        // 스케일 중이면 드래그 무시
        if (isScaling) {
            return true
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                dX = x - lastTouchX
                dY = y - lastTouchY
                isDragging = true
                alpha = 0.7f
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && event.pointerCount == 1) {  // 단일 터치만 드래그
                    val newX = event.rawX + dX
                    val newY = event.rawY + dY
                    
                    // 화면 경계 체크
                    val parent = parent as? View
                    if (parent != null) {
                        val maxX = parent.width - width.toFloat()
                        val maxY = parent.height - height.toFloat()
                        
                        // 스냅 기능 적용
                        val snappedPosition = applySnap(newX, newY, parent)
                        x = snappedPosition.first.coerceIn(0f, maxX)
                        y = snappedPosition.second.coerceIn(0f, maxY)
                    } else {
                        x = newX
                        y = newY
                    }
                    
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    alpha = 1.0f
                    onDragEnd()
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 편집 모드일 때 테두리 표시
        if (isEditMode) {
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            
            // 반투명 배경
            canvas.drawRect(rect, editModeBackgroundPaint)
            
            // 테두리
            canvas.drawRect(rect, editModePaint)
            
            // 모서리 핸들 (크기 조절용 - 나중에 구현)
            drawCornerHandles(canvas)
        }
    }
    
    private fun drawCornerHandles(canvas: Canvas) {
        val handleSize = 20f
        val handlePaint = Paint().apply {
            style = Paint.Style.FILL
            color = ContextCompat.getColor(context, R.color.autopilot_active)
            isAntiAlias = true
        }
        
        // 네 모서리에 핸들 그리기
        canvas.drawCircle(0f, 0f, handleSize, handlePaint)
        canvas.drawCircle(width.toFloat(), 0f, handleSize, handlePaint)
        canvas.drawCircle(0f, height.toFloat(), handleSize, handlePaint)
        canvas.drawCircle(width.toFloat(), height.toFloat(), handleSize, handlePaint)
    }
    
    /**
     * 드래그 종료 시 호출
     */
    protected open fun onDragEnd() {
        // 서브클래스에서 오버라이드 가능
    }
    
    /**
     * 스케일 종료 시 호출
     */
    protected open fun onScaleEnd() {
        // 서브클래스에서 오버라이드 가능
    }
    
    /**
     * 스냅 기능 적용
     */
    private fun applySnap(x: Float, y: Float, parent: View): Pair<Float, Float> {
        val snapThreshold = 30f  // 30px 이내면 스냅
        
        var snappedX = x
        var snappedY = y
        
        // 화면 가장자리에 스냅
        if (x < snapThreshold) snappedX = 0f
        if (y < snapThreshold) snappedY = 0f
        if (x > parent.width - width - snapThreshold) snappedX = parent.width - width.toFloat()
        if (y > parent.height - height - snapThreshold) snappedY = parent.height - height.toFloat()
        
        // 화면 중앙에 스냅
        val centerX = (parent.width - width) / 2f
        val centerY = (parent.height - height) / 2f
        
        if (kotlin.math.abs(x - centerX) < snapThreshold) snappedX = centerX
        if (kotlin.math.abs(y - centerY) < snapThreshold) snappedY = centerY
        
        return Pair(snappedX, snappedY)
    }
    
    /**
     * 현재 위치와 크기 저장
     */
    fun savePosition(): ComponentPosition {
        return ComponentPosition(x, y, width, height, currentScale, visibility == View.VISIBLE)
    }
    
    /**
     * 위치와 크기 복원
     */
    fun restorePosition(position: ComponentPosition) {
        x = position.x
        y = position.y
        currentScale = position.scale
        visibility = if (position.visible) View.VISIBLE else View.GONE
        
        layoutParams = layoutParams.apply {
            width = position.width
            height = position.height
        }
    }
    
    /**
     * 표시/숨김 토글
     */
    fun toggleVisibility() {
        visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }
}

/**
 * 컴포넌트 위치 및 상태 정보
 */
data class ComponentPosition(
    val x: Float,
    val y: Float,
    val width: Int,
    val height: Int,
    val scale: Float = 1.0f,
    val visible: Boolean = true
)
