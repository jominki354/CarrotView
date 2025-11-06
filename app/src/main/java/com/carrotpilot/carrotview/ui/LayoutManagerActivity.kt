package com.carrotpilot.carrotview.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.carrotpilot.carrotview.data.preferences.AppPreferences
import com.carrotpilot.carrotview.ui.layout.LayoutPresetManager
import com.carrotpilot.carrotview.ui.layout.PresetLayoutConfig
import com.carrotpilot.carrotview.ui.layout.PresetComponentConfig

/**
 * 레이아웃 관리 Activity (간소화 버전)
 */
class LayoutManagerActivity : AppCompatActivity() {
    
    private lateinit var layoutManager: LayoutPresetManager
    private lateinit var prefs: AppPreferences
    private lateinit var rootLayout: LinearLayout
    
    companion object {
        private const val LAYOUT_SLOT_COUNT = 5
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        layoutManager = LayoutPresetManager(this)
        prefs = AppPreferences(this)
        createUI()
    }
    
    private fun createUI() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFF121212.toInt())
        }
        
        // 제목
        val titleText = TextView(this).apply {
            text = "레이아웃 관리"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(titleText)
        
        // 설명
        val descText = TextView(this).apply {
            text = "레이아웃을 슬롯에 저장하고 불러올 수 있습니다"
            textSize = 13f
            setTextColor(0xFFAAAAAA.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        rootLayout.addView(descText)
        
        // 기본값 초기화 버튼
        val resetButton = Button(this).apply {
            text = "기본 레이아웃으로 초기화"
            textSize = 13f
            setBackgroundColor(0xFFFF5722.toInt())
            setTextColor(Color.WHITE)
            setPadding(16, 12, 16, 12)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 24)
            }
            setOnClickListener {
                resetToDefaultLayout()
            }
        }
        rootLayout.addView(resetButton)
        
        // 레이아웃 슬롯들
        createLayoutSlots()
        
        // 뒤로 가기 버튼
        val backButton = Button(this).apply {
            text = "뒤로 가기"
            textSize = 14f
            setBackgroundColor(0xFF424242.toInt())
            setTextColor(Color.WHITE)
            setPadding(16, 14, 16, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24, 0, 0)
            }
            setOnClickListener {
                finish()
            }
        }
        rootLayout.addView(backButton)
        
        // ScrollView로 감싸기
        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
            setBackgroundColor(0xFF121212.toInt())
        }
        
        setContentView(scrollView)
    }
    
    private fun createLayoutSlots() {
        for (slotNumber in 1..LAYOUT_SLOT_COUNT) {
            val slotName = "layout_slot_$slotNumber"
            val savedLayout = layoutManager.loadLayout(slotName)
            
            // 슬롯 컨테이너
            val slotContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 14, 16, 14)
                setBackgroundColor(0xFF1E1E1E.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 12)
                }
            }
            
            // 슬롯 제목
            val slotTitle = TextView(this).apply {
                text = "레이아웃 $slotNumber"
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(0, 0, 0, 8)
            }
            slotContainer.addView(slotTitle)
            
            // 상태 표시
            val statusText = TextView(this).apply {
                text = if (savedLayout != null) {
                    "저장됨"
                } else {
                    "비어있음"
                }
                textSize = 12f
                setTextColor(if (savedLayout != null) 0xFF4CAF50.toInt() else 0xFF757575.toInt())
                setPadding(0, 0, 0, 10)
            }
            slotContainer.addView(statusText)
            
            // 버튼 컨테이너
            val buttonContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            // 불러오기 버튼
            val loadButton = Button(this).apply {
                text = "불러오기"
                textSize = 13f
                setBackgroundColor(if (savedLayout != null) 0xFF1976D2.toInt() else 0xFF424242.toInt())
                setTextColor(Color.WHITE)
                isEnabled = savedLayout != null
                setPadding(12, 10, 12, 10)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(0, 0, 6, 0)
                }
                setOnClickListener {
                    loadLayoutFromSlot(slotName, slotNumber)
                }
            }
            buttonContainer.addView(loadButton)
            
            // 저장 버튼
            val saveButton = Button(this).apply {
                text = "저장"
                textSize = 13f
                setBackgroundColor(0xFF4CAF50.toInt())
                setTextColor(Color.WHITE)
                setPadding(12, 10, 12, 10)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(6, 0, 6, 0)
                }
                setOnClickListener {
                    saveLayoutToSlot(slotName, slotNumber, statusText, loadButton)
                }
            }
            buttonContainer.addView(saveButton)
            
            // 삭제 버튼
            val deleteButton = Button(this).apply {
                text = "삭제"
                textSize = 13f
                setBackgroundColor(0xFFF44336.toInt())
                setTextColor(Color.WHITE)
                isEnabled = savedLayout != null
                setPadding(12, 10, 12, 10)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    0.7f
                ).apply {
                    setMargins(6, 0, 0, 0)
                }
                setOnClickListener {
                    deleteLayoutFromSlot(slotName, slotNumber, statusText, loadButton, this)
                }
            }
            buttonContainer.addView(deleteButton)
            
            slotContainer.addView(buttonContainer)
            rootLayout.addView(slotContainer)
        }
    }
    
    private fun loadLayoutFromSlot(slotName: String, slotNumber: Int) {
        val layout = layoutManager.loadLayout(slotName)
        if (layout != null) {
            applyLayout(layout)
            Toast.makeText(this, "레이아웃 $slotNumber 적용됨", Toast.LENGTH_SHORT).show()
            finish()  // 적용 후 메인 화면으로
        } else {
            Toast.makeText(this, "레이아웃을 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveLayoutToSlot(slotName: String, slotNumber: Int, statusText: TextView, loadButton: Button) {
        // 현재 저장된 컴포넌트 상태 읽기
        val speedState = prefs.getComponentState("speedometer")
        val wheelState = prefs.getComponentState("steering_wheel")
        val holdState = prefs.getComponentState("auto_hold")
        
        if (speedState == null) {
            Toast.makeText(this, "저장할 레이아웃이 없습니다\n먼저 메인 화면에서 레이아웃을 편집하세요", Toast.LENGTH_LONG).show()
            return
        }
        
        val components = mutableMapOf<String, PresetComponentConfig>()
        
        // 속도계 (필수)
        components["speedometer"] = PresetComponentConfig(
            speedState.x, speedState.y, speedState.width, 
            speedState.height, speedState.scale, speedState.visible
        )
        
        // 조향각 (선택)
        wheelState?.let {
            components["steering_wheel"] = PresetComponentConfig(
                it.x, it.y, it.width, it.height, it.scale, it.visible
            )
        }
        
        // 오토홀드 (선택)
        holdState?.let {
            components["auto_hold"] = PresetComponentConfig(
                it.x, it.y, it.width, it.height, it.scale, it.visible
            )
        }
        
        val layout = PresetLayoutConfig(
            name = "레이아웃 $slotNumber",
            components = components
        )
        
        if (layoutManager.saveLayout(slotName, layout)) {
            Toast.makeText(this, "레이아웃 $slotNumber 저장됨", Toast.LENGTH_SHORT).show()
            statusText.text = "✓ 저장됨 (레이아웃 $slotNumber)"
            statusText.setTextColor(0xFF4CAF50.toInt())
            loadButton.isEnabled = true
            loadButton.setBackgroundColor(0xFF2196F3.toInt())
        } else {
            Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun deleteLayoutFromSlot(slotName: String, slotNumber: Int, statusText: TextView, loadButton: Button, deleteButton: Button) {
        AlertDialog.Builder(this)
            .setTitle("레이아웃 삭제")
            .setMessage("레이아웃 $slotNumber 을(를) 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                if (layoutManager.deleteLayout(slotName)) {
                    Toast.makeText(this, "레이아웃 $slotNumber 삭제됨", Toast.LENGTH_SHORT).show()
                    statusText.text = "비어있음"
                    statusText.setTextColor(0xFF888888.toInt())
                    loadButton.isEnabled = false
                    loadButton.setBackgroundColor(0xFF444444.toInt())
                    deleteButton.isEnabled = false
                } else {
                    Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun applyLayout(layout: PresetLayoutConfig) {
        layout.components.forEach { (componentId, config) ->
            val position = com.carrotpilot.carrotview.ui.components.ComponentPosition(
                config.x, config.y, config.width, config.height, config.scale, config.visible
            )
            prefs.saveComponentState(componentId, position)
        }
    }
    
    private fun resetToDefaultLayout() {
        AlertDialog.Builder(this)
            .setTitle("기본 레이아웃으로 초기화")
            .setMessage("현재 레이아웃을 기본값으로 초기화하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
            .setPositiveButton("초기화") { _, _ ->
                // 기본 레이아웃 생성
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                
                val defaultLayout = PresetLayoutConfig(
                    name = "기본",
                    components = mapOf(
                        "speedometer" to PresetComponentConfig(
                            x = 16f,
                            y = 16f,
                            width = 250,
                            height = 200,
                            scale = 1.0f,
                            visible = true
                        ),
                        "steering_wheel" to PresetComponentConfig(
                            x = screenWidth - 170f,
                            y = 16f,
                            width = 150,
                            height = 180,
                            scale = 1.0f,
                            visible = true
                        ),
                        "auto_hold" to PresetComponentConfig(
                            x = 16f,
                            y = 400f,
                            width = 120,
                            height = 100,
                            scale = 1.0f,
                            visible = true
                        )
                    )
                )
                
                applyLayout(defaultLayout)
                Toast.makeText(this, "기본 레이아웃으로 초기화되었습니다", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
