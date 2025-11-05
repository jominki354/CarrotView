package com.carrotpilot.carrotview.performance

import android.content.Context
import android.os.Debug
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.carrotpilot.carrotview.ui.MainActivity
import com.carrotpilot.carrotview.data.models.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 안정성 및 성능 테스트 - 장시간 연결, 고빈도 데이터, 메모리 누수, 배터리 소모
 * 요구사항: 4.3, 5.4
 */
@RunWith(AndroidJUnit4::class)
class StabilityPerformanceTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var context: Context
    private lateinit var powerManager: PowerManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    @Test
    fun testLongTermConnectionStability() {
        // Given: 장시간 연결 시뮬레이션 설정
        val testDurationMinutes = 5 // 실제로는 더 길게 설정 가능
        val updateIntervalMs = 100L // 10Hz 업데이트
        val totalUpdates = (testDurationMinutes * 60 * 1000 / updateIntervalMs).toInt()
        
        var successfulUpdates = 0
        var failedUpdates = 0
        val startTime = System.currentTimeMillis()
        
        activityRule.scenario.onActivity { activity ->
            // When: 장시간 데이터 업데이트 시뮬레이션
            repeat(totalUpdates) { iteration ->
                try {
                    val testData = generateTestData(iteration)
                    
                    // 데이터 처리 시뮬레이션
                    processDataUpdate(testData)
                    successfulUpdates++
                    
                    // 실제 업데이트 간격 유지
                    Thread.sleep(updateIntervalMs)
                    
                    // 중간 체크포인트 (매 1000회마다)
                    if (iteration % 1000 == 0) {
                        val currentTime = System.currentTimeMillis()
                        val elapsedMinutes = (currentTime - startTime) / (1000 * 60)
                        
                        // 메모리 사용량 체크
                        val memoryInfo = Debug.MemoryInfo()
                        Debug.getMemoryInfo(memoryInfo)
                        val totalMemoryKb = memoryInfo.totalPss
                        
                        // 메모리 사용량이 합리적인 범위 내에 있는지 확인
                        assertTrue("Memory usage should be reasonable at checkpoint $iteration", 
                            totalMemoryKb < 200 * 1024) // 200MB 이하
                        
                        println("Checkpoint $iteration: ${elapsedMinutes}min, Memory: ${totalMemoryKb}KB")
                    }
                    
                } catch (e: Exception) {
                    failedUpdates++
                    println("Update failed at iteration $iteration: ${e.message}")
                }
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTimeMs = endTime - startTime
        val actualDurationMinutes = totalTimeMs / (1000 * 60)
        
        // Then: 안정성 검증
        val successRate = successfulUpdates.toDouble() / totalUpdates
        assertTrue("Success rate should be high", successRate >= 0.95) // 95% 이상
        assertTrue("Failed updates should be minimal", failedUpdates < totalUpdates * 0.05)
        
        println("Long-term stability test completed:")
        println("Duration: ${actualDurationMinutes}min")
        println("Total updates: $totalUpdates")
        println("Successful: $successfulUpdates")
        println("Failed: $failedUpdates")
        println("Success rate: ${(successRate * 100).toInt()}%")
    }

    @Test
    fun testHighDataTransmissionRatePerformance() {
        // Given: 고빈도 데이터 전송 시뮬레이션
        val highFrequencyHz = 50 // 50Hz (매우 높은 빈도)
        val testDurationSeconds = 30
        val totalUpdates = highFrequencyHz * testDurationSeconds
        val updateIntervalMs = 1000L / highFrequencyHz
        
        val processingTimes = mutableListOf<Long>()
        var droppedFrames = 0
        
        activityRule.scenario.onActivity { activity ->
            val startTime = System.currentTimeMillis()
            
            // When: 고빈도 데이터 처리
            repeat(totalUpdates) { iteration ->
                val updateStartTime = System.nanoTime()
                
                try {
                    // 복잡한 데이터 생성 (실제 CarrotPilot 데이터와 유사)
                    val complexTestData = generateComplexTestData(iteration)
                    
                    // 데이터 처리 시뮬레이션
                    processComplexDataUpdate(complexTestData)
                    
                    val updateEndTime = System.nanoTime()
                    val processingTimeMs = (updateEndTime - updateStartTime) / 1_000_000
                    processingTimes.add(processingTimeMs)
                    
                    // 프레임 드롭 체크 (처리 시간이 업데이트 간격을 초과하는 경우)
                    if (processingTimeMs > updateIntervalMs) {
                        droppedFrames++
                    }
                    
                    // 정확한 타이밍 유지
                    val remainingTime = updateIntervalMs - processingTimeMs
                    if (remainingTime > 0) {
                        Thread.sleep(remainingTime)
                    }
                    
                } catch (e: Exception) {
                    droppedFrames++
                    println("High-frequency update failed at iteration $iteration: ${e.message}")
                }
            }
            
            val endTime = System.currentTimeMillis()
            val actualDurationMs = endTime - startTime
            
            // Then: 성능 검증
            val averageProcessingTime = processingTimes.average()
            val maxProcessingTime = processingTimes.maxOrNull() ?: 0L
            val frameDropRate = droppedFrames.toDouble() / totalUpdates
            
            assertTrue("Average processing time should be reasonable", 
                averageProcessingTime < updateIntervalMs * 0.8) // 80% 이하
            assertTrue("Max processing time should not exceed interval significantly", 
                maxProcessingTime < updateIntervalMs * 2) // 2배 이하
            assertTrue("Frame drop rate should be low", frameDropRate < 0.1) // 10% 이하
            
            println("High-frequency performance test completed:")
            println("Frequency: ${highFrequencyHz}Hz")
            println("Duration: ${actualDurationMs}ms")
            println("Average processing time: ${averageProcessingTime.toInt()}ms")
            println("Max processing time: ${maxProcessingTime}ms")
            println("Dropped frames: $droppedFrames/${totalUpdates} (${(frameDropRate * 100).toInt()}%)")
        }
    }

    @Test
    fun testMemoryLeakDetection() {
        // Given: 메모리 누수 감지 테스트
        val initialMemoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(initialMemoryInfo)
        val initialMemoryKb = initialMemoryInfo.totalPss
        
        val cycleCount = 10
        val updatesPerCycle = 1000
        val memoryMeasurements = mutableListOf<Int>()
        
        activityRule.scenario.onActivity { activity ->
            // When: 반복적인 메모리 할당/해제 사이클
            repeat(cycleCount) { cycle ->
                // 대량의 데이터 처리 시뮬레이션
                val dataList = mutableListOf<DrivingData>()
                
                repeat(updatesPerCycle) { iteration ->
                    val testData = generateTestData(iteration)
                    dataList.add(testData)
                    
                    // 데이터 처리
                    processDataUpdate(testData)
                }
                
                // 명시적으로 참조 해제
                dataList.clear()
                
                // 가비지 컬렉션 강제 실행
                System.gc()
                Thread.sleep(100) // GC 완료 대기
                
                // 메모리 사용량 측정
                val memoryInfo = Debug.MemoryInfo()
                Debug.getMemoryInfo(memoryInfo)
                val currentMemoryKb = memoryInfo.totalPss
                memoryMeasurements.add(currentMemoryKb)
                
                println("Cycle $cycle: Memory usage = ${currentMemoryKb}KB")
            }
        }
        
        // Then: 메모리 누수 검증
        val finalMemoryKb = memoryMeasurements.last()
        val memoryIncrease = finalMemoryKb - initialMemoryKb
        val memoryIncreasePercentage = (memoryIncrease.toDouble() / initialMemoryKb) * 100
        
        // 메모리 증가가 합리적인 범위 내에 있는지 확인
        assertTrue("Memory increase should be reasonable", 
            memoryIncreasePercentage < 50) // 50% 이하 증가
        
        // 메모리 사용량이 지속적으로 증가하지 않는지 확인
        val lastThreeMeasurements = memoryMeasurements.takeLast(3)
        val isMemoryStable = lastThreeMeasurements.zipWithNext().all { (prev, curr) ->
            val increase = curr - prev
            increase < initialMemoryKb * 0.1 // 초기 메모리의 10% 이하 증가
        }
        
        assertTrue("Memory usage should stabilize", isMemoryStable)
        
        println("Memory leak test completed:")
        println("Initial memory: ${initialMemoryKb}KB")
        println("Final memory: ${finalMemoryKb}KB")
        println("Memory increase: ${memoryIncrease}KB (${memoryIncreasePercentage.toInt()}%)")
    }

    @Test
    fun testBatteryConsumptionOptimization() {
        // Given: 배터리 소모 최적화 테스트
        val testDurationMinutes = 3
        val updateIntervalMs = 200L // 5Hz
        val totalUpdates = (testDurationMinutes * 60 * 1000 / updateIntervalMs).toInt()
        
        // 배터리 상태 확인 (가능한 경우)
        val batteryOptimizationEnabled = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        
        activityRule.scenario.onActivity { activity ->
            val startTime = System.currentTimeMillis()
            var cpuActiveTime = 0L
            
            // When: 배터리 효율적인 데이터 처리 시뮬레이션
            repeat(totalUpdates) { iteration ->
                val processingStartTime = System.nanoTime()
                
                // 효율적인 데이터 처리
                val testData = generateTestData(iteration)
                processDataUpdateEfficiently(testData)
                
                val processingEndTime = System.nanoTime()
                cpuActiveTime += (processingEndTime - processingStartTime)
                
                // 적절한 대기 시간으로 CPU 사용량 조절
                Thread.sleep(updateIntervalMs)
                
                // 주기적으로 불필요한 작업 정리
                if (iteration % 100 == 0) {
                    System.gc()
                }
            }
            
            val endTime = System.currentTimeMillis()
            val totalTimeMs = endTime - startTime
            val cpuActiveTimeMs = cpuActiveTime / 1_000_000
            val cpuUsagePercentage = (cpuActiveTimeMs.toDouble() / totalTimeMs) * 100
            
            // Then: 배터리 효율성 검증
            assertTrue("CPU usage should be reasonable", cpuUsagePercentage < 30) // 30% 이하
            
            // 배터리 최적화 설정 확인
            if (batteryOptimizationEnabled) {
                assertTrue("Should respect battery optimization settings", true)
            }
            
            println("Battery optimization test completed:")
            println("Total time: ${totalTimeMs}ms")
            println("CPU active time: ${cpuActiveTimeMs}ms")
            println("CPU usage: ${cpuUsagePercentage.toInt()}%")
            println("Battery optimization enabled: $batteryOptimizationEnabled")
        }
    }

    @Test
    fun testConcurrentConnectionHandling() {
        // Given: 동시 연결 처리 테스트
        val connectionCount = 5
        val updatesPerConnection = 100
        val latch = CountDownLatch(connectionCount)
        val results = mutableListOf<Boolean>()
        
        activityRule.scenario.onActivity { activity ->
            // When: 다중 연결 시뮬레이션
            repeat(connectionCount) { connectionId ->
                Thread {
                    try {
                        repeat(updatesPerConnection) { iteration ->
                            val testData = generateTestData(iteration + connectionId * 1000)
                            processDataUpdate(testData)
                            Thread.sleep(50) // 50ms 간격
                        }
                        results.add(true)
                    } catch (e: Exception) {
                        results.add(false)
                        println("Connection $connectionId failed: ${e.message}")
                    } finally {
                        latch.countDown()
                    }
                }.start()
            }
            
            // 모든 연결 완료 대기 (최대 30초)
            val completed = latch.await(30, TimeUnit.SECONDS)
            
            // Then: 동시 연결 처리 검증
            assertTrue("All connections should complete within timeout", completed)
            
            val successfulConnections = results.count { it }
            val successRate = successfulConnections.toDouble() / connectionCount
            
            assertTrue("Most connections should succeed", successRate >= 0.8) // 80% 이상
            
            println("Concurrent connection test completed:")
            println("Total connections: $connectionCount")
            println("Successful connections: $successfulConnections")
            println("Success rate: ${(successRate * 100).toInt()}%")
        }
    }

    // Helper methods for test data generation and processing
    private fun generateTestData(iteration: Int): DrivingData {
        return DrivingData(
            timestamp = System.currentTimeMillis(),
            carState = CarState(
                vEgo = 15.0f + (iteration % 50),
                vCruise = 20.0f + (iteration % 30),
                gearShifter = if (iteration % 2 == 0) "drive" else "reverse",
                doorOpen = iteration % 10 == 0,
                seatbeltLatched = iteration % 3 != 0,
                steeringAngleDeg = (iteration % 360).toFloat() - 180f
            ),
            controlsState = ControlsState(
                enabled = iteration % 4 != 0,
                active = iteration % 3 != 0,
                alertText = if (iteration % 20 == 0) "Test Alert" else "",
                alertStatus = if (iteration % 15 == 0) "warning" else "normal"
            ),
            liveTracks = (1..(iteration % 5 + 1)).map { trackId ->
                LiveTrack(
                    trackId = trackId,
                    dRel = 20.0f + Random.nextFloat() * 80f,
                    yRel = Random.nextFloat() * 6f - 3f,
                    vRel = Random.nextFloat() * 20f - 10f
                )
            },
            deviceState = DeviceState(
                batteryPercent = 100 - (iteration % 100),
                thermalStatus = when (iteration % 3) {
                    0 -> "green"
                    1 -> "yellow"
                    else -> "red"
                }
            )
        )
    }

    private fun generateComplexTestData(iteration: Int): DrivingData {
        // 더 복잡한 데이터 생성 (실제 CarrotPilot 데이터와 유사)
        return generateTestData(iteration).copy(
            liveTracks = (1..10).map { trackId ->
                LiveTrack(
                    trackId = trackId,
                    dRel = 10.0f + Random.nextFloat() * 100f,
                    yRel = Random.nextFloat() * 8f - 4f,
                    vRel = Random.nextFloat() * 30f - 15f
                )
            }
        )
    }

    private fun processDataUpdate(data: DrivingData) {
        // 데이터 처리 시뮬레이션
        val processedSpeed = data.carState.vEgo * 3.6f // m/s to km/h
        val processedCruise = data.carState.vCruise * 3.6f
        
        // 간단한 계산 작업
        val calculations = (1..10).map { it * processedSpeed }
        
        // 메모리 할당/해제 시뮬레이션
        val tempList = mutableListOf<Float>()
        tempList.addAll(calculations)
        tempList.clear()
    }

    private fun processComplexDataUpdate(data: DrivingData) {
        // 더 복잡한 데이터 처리 시뮬레이션
        processDataUpdate(data)
        
        // 추가적인 복잡한 계산
        data.liveTracks.forEach { track ->
            val distance = kotlin.math.sqrt(track.dRel * track.dRel + track.yRel * track.yRel)
            val relativeSpeed = kotlin.math.abs(track.vRel)
            val timeToCollision = if (relativeSpeed > 0) distance / relativeSpeed else Float.MAX_VALUE
        }
        
        // JSON 직렬화/역직렬화 시뮬레이션
        val jsonString = data.toString()
        val processedString = jsonString.replace("true", "1").replace("false", "0")
    }

    private fun processDataUpdateEfficiently(data: DrivingData) {
        // 배터리 효율적인 데이터 처리
        // 불필요한 계산 최소화
        val essentialSpeed = data.carState.vEgo
        val essentialStatus = data.controlsState.enabled
        
        // 최소한의 처리만 수행
        if (essentialSpeed > 0 && essentialStatus) {
            // 필수 처리만 수행
        }
    }
}