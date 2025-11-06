package com.carrotpilot.carrotview.ui.layout

import kotlinx.serialization.Serializable

/**
 * 레이아웃 프리셋용 컴포넌트 설정
 */
@Serializable
data class PresetComponentConfig(
    val x: Float,
    val y: Float,
    val width: Int,
    val height: Int,
    val scale: Float = 1.0f,
    val visible: Boolean = true
)

/**
 * 레이아웃 프리셋 설정
 */
@Serializable
data class PresetLayoutConfig(
    val name: String,
    val components: Map<String, PresetComponentConfig>,
    val version: String = "1.0",
    val createdAt: Long = System.currentTimeMillis()
)
