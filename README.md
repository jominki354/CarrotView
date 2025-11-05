# 🥕 CarrotView

CarrotView는 CarrotPilot (OpenPilot 기반 자율주행 시스템)의 실시간 데이터를 모니터링하는 Android 애플리케이션입니다.

## 📱 주요 기능

- **실시간 차량 데이터 모니터링**
  - 속도, 조향각, 가속도 등 주행 데이터
  - GPS 위치 및 고도 정보
  - 배터리 상태 및 온도
  
- **Tesla 스타일 대시보드 UI**
  - 직관적이고 세련된 인터페이스
  - 실시간 데이터 시각화
  - 다크 모드 지원

- **네트워크 통신**
  - CarrotPilot 디바이스와 WiFi 연결
  - 실시간 데이터 스트리밍
  - 자동 재연결 기능

## 🚀 빠른 시작

### 방법 1: 자동 설정 스크립트 (권장)

**Linux/macOS:**
```bash
git clone https://github.com/jominki354/CarrotView.git
cd CarrotView
chmod +x setup_carrotview.sh
./setup_carrotview.sh
```

**Windows (PowerShell - 관리자 권한):**
```powershell
git clone https://github.com/jominki354/CarrotView.git
cd CarrotView
.\setup_carrotview.ps1
```

### 방법 2: 수동 설정

자세한 수동 설정 방법은 [DEVELOPMENT_SETUP.md](DEVELOPMENT_SETUP.md)를 참조하세요.

### 방법 3: AI 자동 설정

AI 어시스턴트를 사용하는 경우 [AI_SETUP_GUIDE.md](AI_SETUP_GUIDE.md)를 참조하세요.

## 📋 필수 요구사항

- **Java JDK 17** 이상
- **Android SDK** (API Level 34)
- **Git**
- **최소 8GB RAM** (권장 16GB)

## 🔧 개발 환경

- **언어**: Kotlin
- **최소 SDK**: API 24 (Android 7.0)
- **타겟 SDK**: API 34 (Android 14)
- **빌드 도구**: Gradle 8.13

## 📖 문서

- [개발 환경 설정 가이드](DEVELOPMENT_SETUP.md) - 수동 설정 방법
- [AI 자동 설정 가이드](AI_SETUP_GUIDE.md) - AI를 위한 자동화 가이드
- [앱 UI 문서](APP_UI_DOCUMENTATION.md) - UI 구조 및 디자인
- [자동 연결 가이드](AUTO_CONNECTION_GUIDE.md) - 네트워크 연결 설정
- [테스트 문서](TEST_DOCUMENTATION.md) - 테스트 방법 및 가이드

## 🏗️ 프로젝트 구조

```
CarrotView/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/carrotpilot/carrotview/
│   │   │   │   ├── network/          # 네트워크 통신
│   │   │   │   ├── ui/              # UI 컴포넌트
│   │   │   │   ├── data/            # 데이터 모델
│   │   │   │   └── MainActivity.kt  # 메인 액티비티
│   │   │   └── res/                 # 리소스 (레이아웃, 이미지)
│   │   └── test/                    # 단위 테스트
│   └── build.gradle.kts             # 앱 빌드 설정
├── test_server.py                   # 테스트 서버
├── setup_carrotview.sh              # 자동 설정 (Linux/macOS)
├── setup_carrotview.ps1             # 자동 설정 (Windows)
└── README.md                        # 이 파일
```

## 🧪 테스트

### 테스트 서버 실행
```bash
python test_server.py
```

### 앱 빌드 및 설치
```bash
# 디버그 APK 빌드
./gradlew assembleDebug

# 디바이스에 설치
./gradlew installDebug

# 또는 한 번에
./gradlew installDebug
```

### 단위 테스트 실행
```bash
./gradlew test
```

## 📱 사용 방법

1. **CarrotPilot 디바이스 준비**
   - CarrotPilot 디바이스의 WiFi 핫스팟 활성화
   - 또는 같은 네트워크에 연결

2. **앱 실행**
   - CarrotView 앱 실행
   - IP 주소 입력 (예: 192.168.43.1)
   - 포트 입력 (기본: 8090)
   - "Connect" 버튼 클릭

3. **데이터 모니터링**
   - 실시간 차량 데이터 확인
   - 대시보드에서 주요 정보 확인

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 코딩 컨벤션
- Kotlin 공식 코딩 컨벤션 준수
- 함수/변수명: camelCase
- 클래스명: PascalCase
- 상수: UPPER_SNAKE_CASE

## 🐛 버그 리포트

버그를 발견하셨나요? [GitHub Issues](https://github.com/jominki354/CarrotView/issues)에 리포트해주세요.

## 📄 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

## 🔗 관련 링크

- [CarrotPilot 공식 사이트](https://carrotpilot.app/)
- [OpenPilot GitHub](https://github.com/commaai/openpilot)
- [Android 개발자 가이드](https://developer.android.com/guide)

## 👨‍💻 개발자

- **jominki354** - [GitHub](https://github.com/jominki354)

## 🙏 감사의 말

- [comma.ai](https://comma.ai/) - OpenPilot 프로젝트
- CarrotPilot 커뮤니티

---

**Made with ❤️ for CarrotPilot Community**
