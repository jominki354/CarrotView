# CarrotView 네트워크 통합 완료

## 구현된 기능

### 1. 네트워크 통신 모듈 ✅

**위치**: `CarrotView/app/src/main/java/com/carrotpilot/carrotview/network/`

#### ConnectionState.kt
- 연결 상태 관리 (Disconnected, Connecting, Connected, Error, Reconnecting)
- 연결 설정 (ConnectionConfig)
- 연결 통계 (ConnectionStats)

#### TCPClient.kt
- TCP 소켓 연결
- 서버 인증 (토큰 기반)
- 데이터 수신 (압축 지원)
- 자동 재연결
- 연결 타임아웃 처리

**주요 기능**:
- ✅ CarrotPilot 서버 연결
- ✅ 강화된 인증 메커니즘 (타임스탬프 + 챌린지)
- ✅ gzip 압축 데이터 자동 해제
- ✅ 자동 재연결 (5초 간격)
- ✅ 연결 상태 실시간 업데이트

#### NetworkManager.kt
- 네트워크 연결 관리
- CarrotPilot 자동 발견
- 데이터 수신 및 파싱
- 연결 상태 Flow
- 주행 데이터 Flow

**주요 기능**:
- ✅ CarrotPilot 자동 발견 (서브넷 스캔)
- ✅ 수동 IP 연결
- ✅ 실시간 데이터 수신
- ✅ 데이터 유효성 검증
- ✅ 네트워크 상태 확인 (WiFi/Ethernet)
- ✅ 로컬 IP 주소 감지

### 2. DashboardController 업데이트 ✅

**위치**: `CarrotView/app/src/main/java/com/carrotpilot/carrotview/ui/controller/DashboardController.kt`

**추가된 기능**:
- ✅ NetworkManager 통합
- ✅ 연결 상태 관찰 (Kotlin Flow)
- ✅ 주행 데이터 자동 업데이트
- ✅ 연결/재연결/해제 메서드
- ✅ 자동 발견 및 연결

### 3. MainActivity 업데이트 ✅

**위치**: `CarrotView/app/src/main/java/com/carrotpilot/carrotview/ui/MainActivity.kt`

**추가된 UI**:
- ✅ 연결 상태 표시
- ✅ 서버 IP 주소 입력
- ✅ 연결 버튼
- ✅ 자동 발견 버튼
- ✅ 연결 해제 버튼
- ✅ 실시간 데이터 표시

### 4. DataParser 업데이트 ✅

**위치**: `CarrotView/app/src/main/java/com/carrotpilot/carrotview/data/parser/DataParser.kt`

**추가된 기능**:
- ✅ `parseJson()` 메서드 (NetworkManager용)
- ✅ Object로 변경 (싱글톤)

## 사용 방법

### 1. 앱 설치

```bash
cd CarrotView
.\gradlew.bat installDebug
```

### 2. CarrotPilot 데이터 전송 서비스 시작

CarrotPilot 기기에서:

```bash
cd /data/openpilot
python3 selfdrive/carrot/start_data_transmitter.py
```

또는 시스템 서비스로 실행:

```bash
sudo systemctl start carrotview-transmitter.service
```

### 3. 앱에서 연결

#### 방법 1: 자동 발견
1. 앱 실행
2. "자동 발견 및 연결" 버튼 클릭
3. 자동으로 CarrotPilot 검색 및 연결

#### 방법 2: 수동 연결
1. 앱 실행
2. CarrotPilot의 IP 주소 입력 (예: 192.168.1.100)
3. "연결" 버튼 클릭

### 4. 실시간 데이터 확인

연결되면 자동으로 실시간 주행 데이터가 표시됩니다:
- 차량 속도
- 크루즈 설정 속도
- 기어 상태
- 오토파일럿 상태
- 주변 차량 정보
- 디바이스 상태

## 네트워크 프로토콜

### 연결 과정

1. **TCP 연결**: 클라이언트가 서버 포트 8080에 연결
2. **인증 요청**: 서버가 챌린지 전송
   ```json
   {
     "type": "auth_required",
     "timestamp": 1699123456789,
     "challenge": "carrotview_1699123456"
   }
   ```
3. **인증 응답**: 클라이언트가 토큰 전송
   ```json
   {
     "token": "carrotview2024_carrotview_1699123456",
     "timestamp": 1699123456789
   }
   ```
4. **인증 성공**: 서버가 확인 전송
   ```json
   {
     "type": "auth_success",
     "server_version": "1.0",
     "compression_supported": true
   }
   ```
5. **데이터 스트림**: 실시간 주행 데이터 전송 시작

### 데이터 형식

```
[4바이트 길이][1바이트 압축플래그][JSON 데이터]
```

- **길이**: 빅엔디안 32비트 정수
- **압축플래그**: 
  - `0x00`: 압축 안 됨
  - `0x01`: gzip 압축됨
- **JSON 데이터**: UTF-8 인코딩된 주행 데이터

### 예제 JSON 데이터

```json
{
  "timestamp": 1699123456789,
  "carState": {
    "vEgo": 25.5,
    "vCruise": 30.0,
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
      "vRel": -5.0,
      "aRel": 0.0
    }
  ],
  "deviceState": {
    "batteryPercent": 85,
    "thermalStatus": "green"
  }
}
```

## 보안 기능

### 1. 인증
- 토큰 기반 인증
- 타임스탬프 검증 (30초 이내)
- 챌린지-응답 메커니즘

### 2. 네트워크 제한
- 로컬 네트워크만 허용 (WiFi/Ethernet)
- 외부 인터넷 연결 차단

### 3. 데이터 검증
- 타임스탬프 유효성 확인
- 속도 데이터 범위 검증
- 잘못된 데이터 필터링

## 성능 최적화

### 1. 데이터 압축
- gzip 압축 자동 감지 및 해제
- 압축률에 따라 선택적 압축

### 2. 자동 재연결
- 연결 끊김 시 자동 재연결 (5초 간격)
- 재연결 횟수 제한 없음

### 3. 비동기 처리
- Kotlin Coroutines 사용
- 메인 스레드 블로킹 방지
- Flow를 통한 반응형 데이터 스트림

## 오류 처리

### 1. 연결 오류
- 자동 재연결 시도
- 연결 상태 UI 표시
- 오류 메시지 표시

### 2. 데이터 파싱 오류
- 잘못된 JSON 무시
- 로그 기록
- 마지막 유효 데이터 유지

### 3. 네트워크 오류
- 네트워크 상태 확인
- WiFi 연결 확인
- 타임아웃 처리

## 테스트

### 1. 로컬 테스트

CarrotPilot 없이 테스트하려면 `demo_test.py` 사용:

```bash
cd CarrotView
python demo_test.py
```

그 다음 앱에서 `127.0.0.1` 또는 로컬 IP로 연결

### 2. 실제 테스트

1. CarrotPilot 데이터 전송 서비스 시작
2. 같은 WiFi 네트워크에 안드로이드 기기 연결
3. 앱에서 자동 발견 또는 수동 연결
4. 실시간 데이터 확인

## 다음 단계

### 1. UI 개선 (권장)
- Tesla 스타일 그래픽 UI 적용
- TeslaDashboardActivity와 통합
- 실시간 차량 시각화

### 2. 설정 화면 (권장)
- 연결 설정 저장
- 레이아웃 프리셋 선택
- 테마 설정

### 3. 추가 기능 (선택)
- 연결 히스토리
- 데이터 로깅
- 성능 모니터링

## 문제 해결

### 연결 안 됨
1. CarrotPilot 데이터 전송 서비스 실행 확인
2. 같은 WiFi 네트워크 연결 확인
3. 방화벽 설정 확인
4. 포트 8080 사용 가능 확인

### 데이터 수신 안 됨
1. 연결 상태 확인
2. CarrotPilot 로그 확인
3. 네트워크 대역폭 확인

### 자동 발견 실패
1. WiFi 연결 확인
2. 서브넷 확인 (192.168.x.x)
3. 수동 IP 입력 시도

## 빌드 정보

- **빌드 상태**: ✅ 성공
- **APK 위치**: `CarrotView/app/build/outputs/apk/debug/app-debug.apk`
- **빌드 시간**: 2024년 11월 5일
- **Kotlin 버전**: 1.9.x
- **최소 SDK**: API 24 (Android 7.0)
- **타겟 SDK**: API 34 (Android 14)

## 구현 완료 요약

✅ **NetworkManager** - CarrotPilot 연결 및 데이터 수신
✅ **TCPClient** - TCP 소켓 통신 및 인증
✅ **ConnectionState** - 연결 상태 관리
✅ **DashboardController 통합** - 네트워크와 UI 연동
✅ **MainActivity 업데이트** - 연결 UI 추가
✅ **DataParser 업데이트** - JSON 파싱
✅ **빌드 성공** - APK 생성 완료

**전체 완성도**: 약 **85%** (네트워크 통합 완료)

남은 작업:
- Tesla 스타일 UI와 네트워크 연동
- 설정 화면 구현
- 실제 환경 테스트
