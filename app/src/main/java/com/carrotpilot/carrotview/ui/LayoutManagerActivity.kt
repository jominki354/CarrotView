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
 * 레이아웃 관리 Activity
 */
class LayoutManagerActivity : AppCompatActivity() {
    
    private lateinit var layoutManager: LayoutPresetManager
    private lateinit var prefs: AppPreferences
    private lateinit var rootLayout: LinearLayout
    private lateinit var savedLayoutsContainer: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        layoutManager = LayoutPresetManager(this)
        prefs = AppPreferences(this)
        createUI()
    }
    
    private fun createUI() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.BLACK)
        }
        
        // 제목
        val titleText = TextView(this).apply {
            text = "🎨 레이아웃 관리"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        rootLayout.addView(titleText)
        
        // 프리셋 레이아웃 섹션
        createPresetSection()
        
        // 구분선
        addDivider()
        
        // 현재 레이아웃 저장 섹션
        createSaveSection()
        
        // 구분선
        addDivider()
        
        // 저장된 레이아웃 섹션
        createSavedLayoutsSection()
        
        // 뒤로 가기 버튼
        val backButton = Button(this).apply {
            text = "← 뒤로 가기"
            setBackgroundColor(0xFF666666.toInt())
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0)
            }
            setOnClickListener {
                finish()
            }
        }
        rootLayout.addView(backButton)
        
        // ScrollView로 감싸기
        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
        }
        
        setContentView(scrollView)
    }
    
    private fun addDivider() {
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                setMargins(0, 24, 0, 24)
            }
            setBackgroundColor(0xFF333333.toInt())
        }
        rootLayout.addView(divider)
    }
    
    private fun createPresetSection() {
        val sectionTitle = TextView(this).apply {
            text = "📋 프리셋 레이아웃"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(sectionTitle)
        
        val presets = listOf(
            Pair("default", "🏠 기본 레이아웃"),
            Pair("driver_focused", "🚗 운전자 중심"),
            Pair("minimal", "📱 최소화"),
            Pair("racing", "🏁 레이싱 모드")
        )
        
        presets.forEach { (presetId, name) ->
            val button = Button(this).apply {
                text = name
                setBackgroundColor(0xFF2196F3.toInt())
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                setOnClickListener {
                    applyPreset(presetId, name)
                }
            }
            rootLayout.addView(button)
        }
    }
    
    private fun createSaveSection() {
        val sectionTitle = TextView(this).apply {
            text = "💾 현재 레이아웃 저장"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(sectionTitle)
        
        val saveButton = Button(this).apply {
            text = "💾 현재 레이아웃 저장하기"
            setBackgroundColor(0xFF4CAF50.toInt())
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                showSaveDialog()
            }
        }
        rootLayout.addView(saveButton)
    }
    
    private fun createSavedLayoutsSection() {
        val sectionTitle = TextView(this).apply {
            text = "📂 저장된 레이아웃"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        rootLayout.addView(sectionTitle)
        
        savedLayoutsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootLayout.addView(savedLayoutsContainer)
        
        loadSavedLayouts()
    }
    
    private fun applyPreset(presetId: String, name: String) {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        val layout = when (presetId) {
            "default" -> getDefaultLayout(screenWidth, screenHeight)
            "driver_focused" -> getDriverFocusedLayout(screenWidth, screenHeight)
            "minimal" -> getMinimalLayout(screenWidth, screenHeight)
            "racing" -> getRacingLayout(screenWidth, screenHeight)
            else -> getDefaultLayout(screenWidth, screenHeight)
        }
        
        applyLayout(layout)
        Toast.makeText(this, "$name 적용됨", Toast.LENGTH_SHORT).show()
    }
    
    private fun getDefaultLayout(screenWidth: Int, screenHeight: Int): PresetLayoutConfig {
        return PresetLayoutConfig(
            name = "기본",
            components = mapOf(
                "speedometer" to PresetComponentConfig(32f, 32f, 200, 150, 1.0f, true),
                "autopilot" to PresetComponentConfig(screenWidth - 232f, 32f, 180, 100, 1.0f, true)
            )
        )
    }
    
    private fun getDriverFocusedLayout(screenWidth: Int, screenHeight: Int): PresetLayoutConfig {
        return PresetLayoutConfig(
            name = "운전자 중심",
            components = mapOf(
                "speedometer" to PresetComponentConfig(16f, screenHeight / 2f - 100f, 240, 180, 1.2f, true),
                "autopilot" to PresetComponentConfig(16f, 16f, 200, 120, 1.1f, true)
            )
        )
    }
    
    private fun getMinimalLayout(screenWidth: Int, screenHeight: Int): PresetLayoutConfig {
        return PresetLayoutConfig(
            name = "최소화",
            components = mapOf(
                "speedometer" to PresetComponentConfig(screenWidth - 158f, screenHeight - 128f, 150, 120, 0.7f, true),
                "autopilot" to PresetComponentConfig(8f, screenHeight - 88f, 120, 80, 0.6f, true)
            )
        )
    }
    
    private fun getRacingLayout(screenWidth: Int, screenHeight: Int): PresetLayoutConfig {
        val centerX = screenWidth / 2f
        return PresetLayoutConfig(
            name = "레이싱",
            components = mapOf(
                "speedometer" to PresetComponentConfig(centerX - 150f, screenHeight - 216f, 300, 200, 1.5f, true),
                "autopilot" to PresetComponentConfig(centerX - 100f, 16f, 200, 100, 1.0f, true)
            )
        )
    }
    
    private fun showSaveDialog() {
        val input = EditText(this).apply {
            hint = "레이아웃 이름"
            setText("내 레이아웃 ${System.currentTimeMillis() / 1000}")
        }
        
        AlertDialog.Builder(this)
            .setTitle("레이아웃 저장")
            .setView(input)
            .setPositiveButton("저장") { _, _ ->
                val name = input.text.toString().ifEmpty { "내 레이아웃" }
                saveCurrentLayout(name)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun saveCurrentLayout(name: String) {
        // 현재 저장된 컴포넌트 상태 읽기
        val speedState = prefs.getComponentState("speedometer")
        val autopilotState = prefs.getComponentState("autopilot")
        
        if (speedState == null || autopilotState == null) {
            Toast.makeText(this, "저장된 레이아웃이 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        val layout = PresetLayoutConfig(
            name = name,
            components = mapOf(
                "speedometer" to PresetComponentConfig(
                    speedState.x, speedState.y, speedState.width, 
                    speedState.height, speedState.scale, speedState.visible
                ),
                "autopilot" to PresetComponentConfig(
                    autopilotState.x, autopilotState.y, autopilotState.width,
                    autopilotState.height, autopilotState.scale, autopilotState.visible
                )
            )
        )
        
        if (layoutManager.saveLayout(name, layout)) {
            Toast.makeText(this, "레이아웃 '$name' 저장됨", Toast.LENGTH_SHORT).show()
            loadSavedLayouts()
        } else {
            Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadSavedLayouts() {
        savedLayoutsContainer.removeAllViews()
        
        val layoutNames = layoutManager.getLayoutNames()
        
        if (layoutNames.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "저장된 레이아웃이 없습니다"
                textSize = 14f
                setTextColor(0xFF888888.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 16)
            }
            savedLayoutsContainer.addView(emptyText)
            return
        }
        
        layoutNames.forEach { name ->
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
            }
            
            val loadButton = Button(this).apply {
                text = "📂 $name"
                setBackgroundColor(0xFF9C27B0.toInt())
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    loadLayout(name)
                }
            }
            itemLayout.addView(loadButton)
            
            val deleteButton = Button(this).apply {
                text = "🗑️"
                setBackgroundColor(0xFFF44336.toInt())
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 0, 0)
                }
                setOnClickListener {
                    showDeleteConfirmDialog(name)
                }
            }
            itemLayout.addView(deleteButton)
            
            savedLayoutsContainer.addView(itemLayout)
        }
    }
    
    private fun loadLayout(name: String) {
        val layout = layoutManager.loadLayout(name)
        if (layout != null) {
            applyLayout(layout)
            Toast.makeText(this, "레이아웃 '$name' 적용됨", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "레이아웃 로드 실패", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun applyLayout(layout: PresetLayoutConfig) {
        layout.components.forEach { (componentId, config) ->
            val position = com.carrotpilot.carrotview.ui.components.ComponentPosition(
                config.x, config.y, config.width, config.height, config.scale, config.visible
            )
            prefs.saveComponentState(componentId, position)
        }
        
        // 변경사항을 즉시 반영하기 위해 Activity 종료
        finish()
    }
    
    private fun showDeleteConfirmDialog(name: String) {
        AlertDialog.Builder(this)
            .setTitle("레이아웃 삭제")
            .setMessage("'$name' 레이아웃을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                if (layoutManager.deleteLayout(name)) {
                    Toast.makeText(this, "레이아웃 삭제됨", Toast.LENGTH_SHORT).show()
                    loadSavedLayouts()
                } else {
                    Toast.makeText(this, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
