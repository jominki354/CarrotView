package com.carrotpilot.carrotview

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * 간단한 테스트 액티비티 - 빌드 테스트용
 */
class SimpleTestActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 간단한 텍스트뷰 생성
        val textView = TextView(this)
        textView.text = "CarrotView 테스트 앱이 성공적으로 실행되었습니다!"
        textView.textSize = 18f
        textView.setPadding(50, 50, 50, 50)
        
        setContentView(textView)
    }
}