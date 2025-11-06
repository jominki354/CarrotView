package com.carrotpilot.carrotview.ui.layout

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 레이아웃 프리셋 관리자
 */
class LayoutPresetManager(private val context: Context) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private val prefs = context.getSharedPreferences("layout_presets", Context.MODE_PRIVATE)
    
    /**
     * 레이아웃 저장
     */
    fun saveLayout(name: String, layout: PresetLayoutConfig): Boolean {
        return try {
            val jsonString = json.encodeToString(layout)
            prefs.edit().putString(name, jsonString).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 레이아웃 로드
     */
    fun loadLayout(name: String): PresetLayoutConfig? {
        return try {
            val jsonString = prefs.getString(name, null) ?: return null
            json.decodeFromString<PresetLayoutConfig>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 저장된 레이아웃 목록 가져오기
     */
    fun getLayoutNames(): List<String> {
        return prefs.all.keys.toList()
    }
    
    /**
     * 레이아웃 삭제
     */
    fun deleteLayout(name: String): Boolean {
        return try {
            prefs.edit().remove(name).apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}