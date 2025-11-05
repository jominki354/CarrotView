# CarrotView AI 자동 환경 설정 가이드

이 문서는 AI 어시스턴트가 새로운 컴퓨터에서 CarrotView 프로젝트를 자동으로 설정할 수 있도록 작성되었습니다.

## 🤖 AI를 위한 자동 설정 지침

### 1. 시스템 환경 감지
AI는 먼저 다음을 확인해야 합니다:
```bash
# OS 확인
uname -a  # Linux/Mac
systeminfo  # Windows

# 설치된 도구 확인
java -version
javac -version
git --version
```

### 2. 필수 소프트웨어 자동 설치

#### Java JDK 17 설치
**Windows (PowerShell):**
```powershell
# Chocolatey를 통한 설치
choco install openjdk17 -y

# 또는 수동 다운로드
Invoke-WebRequest -Uri "https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_windows-x64_bin.zip" -OutFile "jdk17.zip"
Expand-Archive -Path "jdk17.zip" -DestinationPath "C:\Program Files\Java\"
```

**macOS:**
```bash
# Homebrew를 통한 설치
brew install openjdk@17

# 환경변수 설정
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
```

#### Android SDK 자동 설치 (Command Line Tools)

**모든 플랫폼 공통:**
```bash
# Android Command Line Tools 다운로드
# Windows
curl -o commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip

# macOS
curl -o commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-mac-9477386_latest.zip

# Linux
curl -o commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip

# 압축 해제 및 설정
mkdir -p ~/Android/Sdk/cmdline-tools
unzip commandlinetools.zip -d ~/Android/Sdk/cmdline-tools
mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
```

**환경변수 설정:**
```bash
# Linux/macOS (~/.bashrc 또는 ~/.zshrc)
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Windows (PowerShell - 영구 설정)
[System.Environment]::SetEnvironmentVariable('ANDROID_HOME', "$env:USERPROFILE\Android\Sdk", 'User')
[System.Environment]::SetEnvironmentVariable('PATH', "$env:PATH;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:ANDROID_HOME\platform-tools", 'User')
```

**필수 SDK 패키지 설치:**
```bash
# SDK Manager를 통한 자동 설치
sdkmanager --licenses  # 라이선스 동의 (자동: yes | sdkmanager --licenses)
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
sdkmanager "emulator" "system-images;android-34;google_apis;x86_64"  # 에뮬레이터 (선택)
```

### 3. 프로젝트 클론 및 설정

```bash
# 저장소 클론
git clone https://github.com/jominki354/CarrotView.git
cd CarrotView

# local.properties 자동 생성
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Windows의 경우
echo "sdk.dir=$env:ANDROID_HOME" | Out-File -FilePath local.properties -Encoding utf8
```

### 4. Gradle 빌드 자동화

```bash
# Gradle Wrapper 권한 설정 (Linux/macOS)
chmod +x gradlew

# 의존성 다운로드 및 빌드
./gradlew build --stacktrace

# Windows
.\gradlew.bat build --stacktrace
```

### 5. 빌드 검증

```bash
# APK 생성 확인
./gradlew assembleDebug

# 생성된 APK 위치
ls -la app/build/outputs/apk/debug/app-debug.apk
```

## 🔧 AI 자동 문제 해결 스크립트

### 전체 자동 설정 스크립트 (Linux/macOS)

```bash
#!/bin/bash
set -e

echo "🚀 CarrotView 자동 환경 설정 시작..."

# 1. Java 설치 확인
if ! command -v java &> /dev/null; then
    echo "📦 Java 설치 중..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install openjdk@17
    else
        sudo apt update && sudo apt install openjdk-17-jdk -y
    fi
fi

# 2. Android SDK 설치
if [ ! -d "$HOME/Android/Sdk" ]; then
    echo "📦 Android SDK 설치 중..."
    mkdir -p ~/Android/Sdk/cmdline-tools
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        curl -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-mac-9477386_latest.zip
    else
        curl -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
    fi
    
    unzip /tmp/cmdtools.zip -d ~/Android/Sdk/cmdline-tools
    mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
    
    export ANDROID_HOME=$HOME/Android/Sdk
    export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
    
    yes | sdkmanager --licenses
    sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
fi

# 3. 프로젝트 설정
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 4. 빌드
echo "🔨 프로젝트 빌드 중..."
chmod +x gradlew
./gradlew build --stacktrace

echo "✅ 환경 설정 완료!"
```

### 전체 자동 설정 스크립트 (Windows PowerShell)

```powershell
# setup_carrotview.ps1
Write-Host "🚀 CarrotView 자동 환경 설정 시작..." -ForegroundColor Green

# 1. Java 설치 확인
try {
    java -version 2>&1 | Out-Null
} catch {
    Write-Host "📦 Java 설치 중..." -ForegroundColor Yellow
    choco install openjdk17 -y
}

# 2. Android SDK 설치
$androidHome = "$env:USERPROFILE\Android\Sdk"
if (-not (Test-Path $androidHome)) {
    Write-Host "📦 Android SDK 설치 중..." -ForegroundColor Yellow
    
    New-Item -ItemType Directory -Path "$androidHome\cmdline-tools" -Force
    
    $cmdToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
    $cmdToolsZip = "$env:TEMP\cmdtools.zip"
    
    Invoke-WebRequest -Uri $cmdToolsUrl -OutFile $cmdToolsZip
    Expand-Archive -Path $cmdToolsZip -DestinationPath "$androidHome\cmdline-tools" -Force
    Move-Item "$androidHome\cmdline-tools\cmdline-tools" "$androidHome\cmdline-tools\latest" -Force
    
    $env:ANDROID_HOME = $androidHome
    $env:PATH += ";$androidHome\cmdline-tools\latest\bin;$androidHome\platform-tools"
    
    # 라이선스 동의
    echo y | & "$androidHome\cmdline-tools\latest\bin\sdkmanager.bat" --licenses
    
    # 필수 패키지 설치
    & "$androidHome\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
}

# 3. 프로젝트 설정
"sdk.dir=$androidHome" | Out-File -FilePath "local.properties" -Encoding utf8

# 4. 빌드
Write-Host "🔨 프로젝트 빌드 중..." -ForegroundColor Yellow
.\gradlew.bat build --stacktrace

Write-Host "✅ 환경 설정 완료!" -ForegroundColor Green
```

## 📋 AI 체크리스트

AI가 환경 설정 시 확인해야 할 항목:

- [ ] OS 타입 확인 (Windows/macOS/Linux)
- [ ] Java JDK 17 설치 확인
- [ ] JAVA_HOME 환경변수 설정
- [ ] Android SDK 설치 확인
- [ ] ANDROID_HOME 환경변수 설정
- [ ] SDK 라이선스 동의
- [ ] 필수 SDK 패키지 설치 (platform-tools, platforms;android-34, build-tools;34.0.0)
- [ ] local.properties 파일 생성
- [ ] Gradle 빌드 성공 확인
- [ ] APK 생성 확인

## 🐛 일반적인 문제 및 자동 해결

### 문제 1: SDK 라이선스 미동의
```bash
# 자동 동의
yes | sdkmanager --licenses
```

### 문제 2: Gradle 데몬 오류
```bash
./gradlew --stop
./gradlew clean build --no-daemon
```

### 문제 3: 네트워크 프록시 문제
```bash
# gradle.properties에 추가
echo "systemProp.http.proxyHost=proxy.company.com" >> gradle.properties
echo "systemProp.http.proxyPort=8080" >> gradle.properties
```

### 문제 4: 메모리 부족
```bash
# gradle.properties에 추가
echo "org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m" >> gradle.properties
```

## 🎯 AI 실행 예시

AI가 새 컴퓨터에서 실행할 명령어 순서:

```bash
# 1. 시스템 확인
uname -a
java -version || echo "Java 필요"

# 2. 저장소 클론
git clone https://github.com/jominki354/CarrotView.git
cd CarrotView

# 3. 자동 설정 스크립트 실행
chmod +x setup_carrotview.sh
./setup_carrotview.sh

# 4. 빌드 확인
./gradlew assembleDebug

# 5. 성공 메시지
echo "✅ CarrotView 환경 설정 완료!"
echo "📱 APK 위치: app/build/outputs/apk/debug/app-debug.apk"
```

## 📚 추가 참고 자료

- **Android Command Line Tools**: https://developer.android.com/studio#command-tools
- **Gradle 문서**: https://docs.gradle.org/current/userguide/userguide.html
- **OpenJDK 다운로드**: https://adoptium.net/

---

**이 문서는 AI가 자동으로 환경을 설정할 수 있도록 작성되었습니다.**
**AI는 이 가이드를 따라 사용자 개입 없이 개발 환경을 구축할 수 있습니다.**
