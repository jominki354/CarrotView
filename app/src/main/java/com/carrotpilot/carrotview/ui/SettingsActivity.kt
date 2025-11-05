package com.carrotpilot.carrotview.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.carrotpilot.carrotview.data.preferences.AppPreferences

/**
 * 설정 화면
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var prefs: AppPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = AppPreferences(this)
        
        createUI()
    }
    
    private fun createUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        
        // 제목
        layout.addView(TextView(this).apply {
            text = "CarrotView 설정"
            textSize = 20f
            setPadding(0, 0, 0, 40)
        })
        
        // 자동 연결 설정
        layout.addView(TextView(this).apply {
            text = "연결 설정"
            textSize = 16f
            setPadding(0, 20, 0, 10)
        })
        
        val autoConnectSwitch = Switch(this).apply {
            text = "앱 시작 시 자동 연결"
            isChecked = prefs.autoConnect
            setOnCheckedChangeListener { _, isChecked ->
                prefs.autoConnect = isChecked
                Toast.makeText(
                    this@SettingsActivity,
                    if (isChecked) "자동 연결 활성화" else "자동 연결 비활성화",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        layout.addView(autoConnectSwitch)
        
        val autoReconnectSwitch = Switch(this).apply {
            text = "연결 끊김 시 자동 재연결"
            isChecked = prefs.autoReconnect
            setOnCheckedChangeListener { _, isChecked ->
                prefs.autoReconnect = isChecked
                Toast.makeText(
                    this@SettingsActivity,
                    if (isChecked) "자동 재연결 활성화" else "자동 재연결 비활성화",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        layout.addView(autoReconnectSwitch)
        
        // 서버 설정
        layout.addView(TextView(this).apply {
            text = "서버 설정"
            textSize = 16f
            setPadding(0, 40, 0, 10)
        })
        
        layout.addView(TextView(this).apply {
            text = "서버 포트"
            textSize = 14f
        })
        
        val portInput = EditText(this).apply {
            setText(prefs.serverPort.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        layout.addView(portInput)
        
        layout.addView(TextView(this).apply {
            text = "인증 토큰"
            textSize = 14f
            setPadding(0, 20, 0, 0)
        })
        
        val tokenInput = EditText(this).apply {
            setText(prefs.authToken)
        }
        layout.addView(tokenInput)
        
        // 저장 버튼
        val saveButton = Button(this).apply {
            text = "설정 저장"
            setOnClickListener {
                try {
                    val port = portInput.text.toString().toInt()
                    if (port in 1024..65535) {
                        prefs.serverPort = port
                        prefs.authToken = tokenInput.text.toString()
                        
                        Toast.makeText(
                            this@SettingsActivity,
                            "설정이 저장되었습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        finish()
                    } else {
                        Toast.makeText(
                            this@SettingsActivity,
                            "포트는 1024~65535 사이여야 합니다",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@SettingsActivity,
                        "잘못된 포트 번호입니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        layout.addView(saveButton)
        
        // 초기화 버튼
        val resetButton = Button(this).apply {
            text = "기본값으로 초기화"
            setOnClickListener {
                prefs.resetToDefaults()
                Toast.makeText(
                    this@SettingsActivity,
                    "설정이 초기화되었습니다",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
        layout.addView(resetButton)
        
        // 정보
        layout.addView(TextView(this).apply {
            text = "\n💡 팁:\n" +
                   "• 자동 연결을 활성화하면 앱 시작 시 자동으로 C3에 연결됩니다\n" +
                   "• 마지막 연결 주소를 먼저 시도하고, 실패 시 자동 검색합니다\n" +
                   "• 자동 재연결은 연결이 끊어졌을 때 자동으로 재연결을 시도합니다"
            textSize = 12f
            setPadding(0, 40, 0, 0)
        })
        
        setContentView(layout)
    }
}
