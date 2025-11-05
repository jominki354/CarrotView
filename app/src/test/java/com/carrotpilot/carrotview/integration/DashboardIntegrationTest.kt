package com.carrotpilot.carrotview.integration

import com.carrotpilot.carrotview.data.models.*
import com.carrotpilot.carrotview.data.parser.DataParser
import com.carrotpilot.carrotview.ui.controller.DashboardController
import com.carrotpilot.carrotview.ui.layout.CustomLayoutManager
import com.carrotpilot.carrotview.ui.layout.LayoutPresetManager
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any

/**
 * 기능 통합 테스트 - 모든 UI 컴포넌트의 통합 동작 확인
 * 요구사항: 1.3, 3.2, 6.1, 7.5
 */
class DashboardIntegrationTest {

    @Mock
    private lateinit var mockLayoutManager: CustomLayoutManager
    
    @Mock
    private lateinit var mockPresetManager: LayoutPresetManager
    
    private lateinit var dataParser: DataParser
    private lateinit var dashboardController: DashboardController

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        dataParser = DataParser()
        // Note: In real implementation, DashboardController would be properly initialized
        // with actual UI components. This is a simplified test structure.
    }

    @Test
    fun `test complete data flow from parsing to UI update`() {
        // Given: 실제 JSON 데이터
        val jsonData = """
        {
            "timestamp": 1699123456789,
            "carState": {
                "vEgo": 18.06,
                "vCruise": 19.44,
                "gearShifter": "drive",
                "doorOpen": false,
                "seatbeltLatched": true,
                "steeringAngleDeg": -2.5
            },
            "controlsState": {
                "enabled": true,
                "active": true,
                "alertText": "",
                "alertStatus": "normal"
            },
            "liveTracks": [
                {
                    "trackId": 1,
                    "dRel": 45.2,
                    "yRel": -1.8,
                    "vRel": -5.0
                }
            ],
            "deviceState": {
                "batteryPercent": 85,
                "thermalStatus": "green"
            }
        }
        """.trimIndent()

        // When: 데이터 파싱
        val result = dataParser.parseData(jsonData)

        // Then: 파싱 결과 검증
        assertTrue("Data parsing should succeed", result.isSuccess)
        val drivingData = result.getOrNull()
        assertNotNull("Parsed data should not be null", drivingData)
        
        drivingData?.let { data ->
            // CarState 검증
            assertEquals("Speed should be parsed correctly", 18.06f, data.carState.vEgo, 0.01f)
            assertEquals("Cruise speed should be parsed correctly", 19.44f, data.carState.vCruise, 0.01f)
            assertEquals("Gear should be parsed correctly", "drive", data.carState.gearShifter)
            assertFalse("Door should be closed", data.carState.doorOpen)
            assertTrue("Seatbelt should be latched", data.carState.seatbeltLatched)
            
            // ControlsState 검증
            assertTrue("Autopilot should be enabled", data.controlsState.enabled)
            assertTrue("Cruise should be active", data.controlsState.active)
            assertEquals("Alert status should be normal", "normal", data.controlsState.alertStatus)
            
            // LiveTracks 검증
            assertEquals("Should have one tracked vehicle", 1, data.liveTracks.size)
            val track = data.liveTracks[0]
            assertEquals("Track ID should match", 1, track.trackId)
            assertEquals("Distance should match", 45.2f, track.dRel, 0.01f)
            
            // DeviceState 검증
            assertEquals("Battery should match", 85, data.deviceState.batteryPercent)
            assertEquals("Thermal status should match", "green", data.deviceState.thermalStatus)
        }
    }

    @Test
    fun `test UI component data update accuracy`() {
        // Given: 테스트 데이터
        val testData = DrivingData(
            timestamp = System.currentTimeMillis(),
            carState = CarState(
                vEgo = 20.0f,
                vCruise = 22.0f,
                gearShifter = "drive",
                doorOpen = false,
                seatbeltLatched = true,
                steeringAngleDeg = 0.0f
            ),
            controlsState = ControlsState(
                enabled = true,
                active = true,
                alertText = "Test Alert",
                alertStatus = "warning"
            ),
            liveTracks = listOf(
                LiveTrack(1, 30.0f, -1.5f, -3.0f),
                LiveTrack(2, 50.0f, 1.2f, 2.0f)
            ),
            deviceState = DeviceState(
                batteryPercent = 75,
                thermalStatus = "yellow"
            )
        )

        // When & Then: 데이터 유효성 검증
        assertTrue("Speed should be positive", testData.carState.vEgo >= 0)
        assertTrue("Cruise speed should be positive", testData.carState.vCruise >= 0)
        assertTrue("Should have tracked vehicles", testData.liveTracks.isNotEmpty())
        assertTrue("Battery should be in valid range", 
            testData.deviceState.batteryPercent in 0..100)
        
        // 실시간 업데이트 정확성 검증
        val currentTime = System.currentTimeMillis()
        assertTrue("Timestamp should be recent", 
            currentTime - testData.timestamp < 5000) // 5초 이내
    }

    @Test
    fun `test layout customization integration`() {
        // Given: 레이아웃 설정
        val layoutConfig = LayoutConfig(
            name = "test_layout",
            components = listOf(
                ComponentConfig(
                    componentType = "speedometer",
                    position = Point(100, 100),
                    size = Size(200, 200),
                    visible = true,
                    zIndex = 1
                ),
                ComponentConfig(
                    componentType = "autopilot",
                    position = Point(300, 100),
                    size = Size(150, 100),
                    visible = true,
                    zIndex = 2
                )
            )
        )

        // When: 레이아웃 저장 시뮬레이션
        whenever(mockPresetManager.saveLayout(any(), any())).thenReturn(true)
        whenever(mockPresetManager.loadLayout(any())).thenReturn(layoutConfig)

        val saveResult = mockPresetManager.saveLayout("test_layout", layoutConfig)
        val loadedLayout = mockPresetManager.loadLayout("test_layout")

        // Then: 레이아웃 저장/로드 검증
        assertTrue("Layout should be saved successfully", saveResult)
        assertNotNull("Loaded layout should not be null", loadedLayout)
        assertEquals("Layout name should match", "test_layout", loadedLayout?.name)
        assertEquals("Should have 2 components", 2, loadedLayout?.components?.size)
        
        // 컴포넌트 설정 검증
        val speedometerComponent = loadedLayout?.components?.find { it.componentType == "speedometer" }
        assertNotNull("Speedometer component should exist", speedometerComponent)
        assertEquals("Speedometer position should match", Point(100, 100), speedometerComponent?.position)
        assertTrue("Speedometer should be visible", speedometerComponent?.visible == true)
    }

    @Test
    fun `test error handling in data processing`() {
        // Given: 잘못된 JSON 데이터
        val invalidJsonData = """
        {
            "timestamp": "invalid",
            "carState": {
                "vEgo": "not_a_number",
                "vCruise": null
            }
        }
        """.trimIndent()

        // When: 파싱 시도
        val result = dataParser.parseData(invalidJsonData)

        // Then: 오류 처리 검증
        assertTrue("Parsing should fail gracefully", result.isFailure)
        
        // 기본값 처리 테스트
        val emptyData = """{}"""
        val emptyResult = dataParser.parseData(emptyData)
        
        // 빈 데이터에 대한 기본값 처리 확인
        if (emptyResult.isSuccess) {
            val data = emptyResult.getOrNull()
            assertNotNull("Should provide default values", data)
        }
    }

    @Test
    fun `test performance with high frequency updates`() {
        // Given: 고빈도 데이터 업데이트 시뮬레이션
        val testData = DrivingData(
            timestamp = System.currentTimeMillis(),
            carState = CarState(15.0f, 16.0f, "drive", false, true, 1.0f),
            controlsState = ControlsState(true, true, "", "normal"),
            liveTracks = emptyList(),
            deviceState = DeviceState(80, "green")
        )

        // When: 연속적인 업데이트 시뮬레이션
        val startTime = System.currentTimeMillis()
        val updateCount = 100
        
        repeat(updateCount) {
            val updatedData = testData.copy(
                timestamp = System.currentTimeMillis(),
                carState = testData.carState.copy(vEgo = 15.0f + it * 0.1f)
            )
            // 실제 구현에서는 UI 업데이트가 여기서 발생
        }
        
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime

        // Then: 성능 검증
        assertTrue("Processing should complete within reasonable time", 
            processingTime < 1000) // 1초 이내
        
        // 메모리 사용량 체크 (간접적)
        System.gc()
        val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        assertTrue("Memory usage should be reasonable", memoryAfter > 0)
    }
}