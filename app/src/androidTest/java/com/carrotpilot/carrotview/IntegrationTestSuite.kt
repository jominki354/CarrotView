package com.carrotpilot.carrotview

import com.carrotpilot.carrotview.integration.UIIntegrationTest
import com.carrotpilot.carrotview.environment.EnvironmentCompatibilityTest
import com.carrotpilot.carrotview.performance.StabilityPerformanceTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * 통합 테스트 스위트 - 모든 통합 테스트를 실행
 * 
 * 실행 방법:
 * ./gradlew connectedAndroidTest
 * 
 * 또는 특정 테스트만 실행:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.carrotpilot.carrotview.IntegrationTestSuite
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    UIIntegrationTest::class,
    EnvironmentCompatibilityTest::class,
    StabilityPerformanceTest::class
)
class IntegrationTestSuite