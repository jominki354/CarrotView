package com.carrotpilot.carrotview.data.parser

import com.carrotpilot.carrotview.data.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

/**
 * JSON 데이터 파서
 */
object DataParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    /**
     * JSON 문자열을 DrivingData 객체로 파싱
     */
    fun parseJson(jsonString: String): DrivingData {
        return json.decodeFromString<DrivingData>(jsonString)
    }
    
    /**
     * JSON 문자열을 DrivingData 객체로 파싱 (Result 반환)
     */
    fun parseData(jsonString: String): Result<DrivingData> {
        return try {
            val data = json.decodeFromString<DrivingData>(jsonString)
            Result.success(data)
        } catch (e: SerializationException) {
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * DrivingData 객체를 JSON 문자열로 변환
     */
    fun serializeData(data: DrivingData): Result<String> {
        return try {
            val jsonString = json.encodeToString(DrivingData.serializer(), data)
            Result.success(jsonString)
        } catch (e: SerializationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}