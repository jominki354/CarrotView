# CarrotView 통합 테스트 문서

## 개요

이 문서는 CarrotView 앱의 통합 테스트 및 최종 검증에 대한 상세한 설명을 제공합니다. 모든 테스트는 요구사항 문서의 검증 기준에 따라 구현되었습니다.

## 테스트 구조

### 1. 기능 통합 테스트 (7.1)
**위치**: `app/src/test/java/com/carrotpilot/carrotview/integration/DashboardIntegrationTest.kt`
**위치**: `app/src/androidTest/java/com/carrotpilot/carrotview/integration/UIIntegrationTest.kt`

#### 테스트 범위
- 모든 UI 컴포넌트 통합 동작 확인
- 실시간 데이터 업데이트 정확성 검증
- 커스터마이징 기능 전체 테스트
- **요구사항**: 1.3, 3.2, 6.1, 7.5

#### 주요 테스트 케이스
1. **완전한 데이터 흐름 테스트**
   - JSON 파싱부터 UI 업데이트까지 전체 프로세스 검증
   - CarState, ControlsState, LiveTracks, DeviceState 데이터 정확성 확인

2. **UI 컴포넌트 데이터 업데이트 정확성**
   - 실시간 데이터 반영 확인
   - 데이터 타임스탬프 검증
   - UI 상태 동기화 확인

3. **레이아웃 커스터마이징 통합**
   - 드래그 앤 드롭 기능 테스트
   - 레이아웃 저장/로드 기능 검증
   - 프리셋 관리 시스템 테스트

4. **오류 처리 통합**
   - 잘못된 데이터 처리
   - 네트워크 오류 복구
   - 기본값 적용 확인

### 2. 다양한 환경 테스트 (7.2)
**위치**: `app/src/androidTest/java/com/carrotpilot/carrotview/environment/EnvironmentCompatibilityTest.kt`

#### 테스트 범위
- 12.9인치 태블릿 및 스마트폰 테스트
- 세로/가로 모드 전환 테스트
- 다양한 안드로이드 버전 호환성 테스트
- **요구사항**: 3.1, 3.5

#### 주요 테스트 케이스
1. **태블릿 화면 지원**
   - 7인치 이상 태블릿 레이아웃 최적화
   - 12.9인치 대형 태블릿 특별 처리
   - 화면 밀도별 적응 (LDPI ~ XXXHDPI+)

2. **화면 방향 변경 처리**
   - 세로/가로 모드 레이아웃 전환
   - 데이터 보존 확인
   - UI 상태 유지 검증

3. **안드로이드 버전 호환성**
   - API 24 (Android 7.0) ~ API 34 (Android 14) 지원
   - 버전별 특화 기능 테스트
   - 권한 모델 변경 대응

4. **하드웨어 기능 호환성**
   - WiFi, 터치스크린 필수 기능 확인
   - 선택적 기능 (카메라, 블루투스, GPS) 처리

### 3. 안정성 및 성능 테스트 (7.3)
**위치**: `app/src/androidTest/java/com/carrotpilot/carrotview/performance/StabilityPerformanceTest.kt`

#### 테스트 범위
- 장시간 연결 안정성 테스트
- 높은 데이터 전송률에서의 성능 테스트
- 메모리 누수 및 배터리 소모 테스트
- **요구사항**: 4.3, 5.4

#### 주요 테스트 케이스
1. **장기간 연결 안정성**
   - 5분간 지속적인 데이터 업데이트 (10Hz)
   - 95% 이상 성공률 요구
   - 메모리 사용량 모니터링 (200MB 이하)

2. **고빈도 데이터 전송 성능**
   - 50Hz 고빈도 업데이트 처리
   - 평균 처리 시간 < 업데이트 간격의 80%
   - 프레임 드롭률 < 10%

3. **메모리 누수 감지**
   - 반복적인 할당/해제 사이클 테스트
   - 메모리 증가율 < 50%
   - 메모리 사용량 안정화 확인

4. **배터리 소모 최적화**
   - CPU 사용률 < 30%
   - 배터리 최적화 설정 준수
   - 효율적인 데이터 처리 검증

5. **동시 연결 처리**
   - 5개 동시 연결 시뮬레이션
   - 80% 이상 연결 성공률
   - 동시성 오류 방지

## 테스트 실행 방법

### 전체 테스트 실행
```bash
# 모든 단위 테스트 실행
./gradlew test

# 모든 계측 테스트 실행 (실제 기기 필요)
./gradlew connectedAndroidTest

# 통합 테스트 스위트 실행
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.carrotpilot.carrotview.IntegrationTestSuite
```

### 개별 테스트 클래스 실행
```bash
# 기능 통합 테스트
./gradlew test --tests "com.carrotpilot.carrotview.integration.DashboardIntegrationTest"

# 환경 호환성 테스트
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.carrotpilot.carrotview.environment.EnvironmentCompatibilityTest

# 성능 테스트
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.carrotpilot.carrotview.performance.StabilityPerformanceTest
```

### 특정 테스트 메서드 실행
```bash
# 장기간 안정성 테스트만 실행
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.carrotpilot.carrotview.performance.StabilityPerformanceTest -Pandroid.testInstrumentationRunnerArguments.method=testLongTermConnectionStability
```

## 테스트 환경 요구사항

### 최소 요구사항
- **안드로이드 기기**: API 24 (Android 7.0) 이상
- **메모리**: 최소 2GB RAM
- **저장공간**: 최소 1GB 여유 공간
- **네트워크**: WiFi 연결 가능

### 권장 요구사항
- **다양한 화면 크기**: 스마트폰 + 태블릿 (7인치, 12.9인치)
- **다양한 안드로이드 버전**: API 24, 28, 30, 33, 34
- **다양한 제조사**: Samsung, LG, Google Pixel 등

## 테스트 결과 해석

### 성공 기준
1. **기능 통합 테스트**
   - 모든 데이터 파싱 테스트 통과
   - UI 업데이트 정확성 100%
   - 레이아웃 커스터마이징 기능 정상 동작

2. **환경 호환성 테스트**
   - 지원하는 모든 화면 크기에서 정상 동작
   - 세로/가로 모드 전환 시 데이터 보존
   - 모든 지원 안드로이드 버전에서 실행 가능

3. **성능 테스트**
   - 장기간 연결 성공률 ≥ 95%
   - 고빈도 업데이트 프레임 드롭률 < 10%
   - 메모리 증가율 < 50%
   - CPU 사용률 < 30%

### 실패 시 대응 방안
1. **성능 이슈**
   - 데이터 처리 로직 최적화
   - UI 업데이트 빈도 조절
   - 메모리 관리 개선

2. **호환성 이슈**
   - 특정 버전/기기별 예외 처리 추가
   - 레이아웃 리소스 추가 생성
   - 권한 처리 로직 수정

3. **안정성 이슈**
   - 오류 처리 로직 강화
   - 재연결 메커니즘 개선
   - 타임아웃 설정 조정

## 지속적인 테스트 개선

### 테스트 커버리지 확장
- 새로운 기능 추가 시 해당 테스트 케이스 추가
- 사용자 피드백 기반 테스트 시나리오 보완
- 실제 CarrotPilot 연동 테스트 추가

### 자동화 개선
- CI/CD 파이프라인 통합
- 다양한 기기에서의 자동 테스트
- 성능 회귀 테스트 자동화

### 모니터링 강화
- 실제 사용 환경에서의 성능 모니터링
- 크래시 리포트 분석
- 사용자 행동 패턴 분석

## 문제 해결 가이드

### 일반적인 테스트 실패 원인
1. **기기 리소스 부족**: 메모리 부족, 저장공간 부족
2. **네트워크 연결 문제**: WiFi 불안정, 방화벽 차단
3. **권한 문제**: 필요한 권한 미승인
4. **버전 호환성**: 지원하지 않는 API 사용

### 디버깅 팁
1. **로그 확인**: `adb logcat` 명령으로 상세 로그 확인
2. **메모리 프로파일링**: Android Studio Memory Profiler 사용
3. **네트워크 모니터링**: 네트워크 트래픽 분석
4. **UI 테스트**: Layout Inspector로 UI 구조 확인

이 테스트 문서는 CarrotView의 품질과 안정성을 보장하기 위한 포괄적인 가이드입니다. 모든 테스트를 정기적으로 실행하여 앱의 품질을 유지하시기 바랍니다.