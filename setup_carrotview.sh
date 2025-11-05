#!/bin/bash
set -e

echo "🚀 CarrotView 자동 환경 설정 시작..."

# 1. Java 설치 확인
if ! command -v java &> /dev/null; then
    echo "📦 Java 설치 중..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install openjdk@17
        echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
    else
        sudo apt update && sudo apt install openjdk-17-jdk -y
    fi
else
    echo "✅ Java 이미 설치됨"
fi

# 2. Android SDK 설치
if [ ! -d "$HOME/Android/Sdk" ]; then
    echo "📦 Android SDK 설치 중..."
    mkdir -p ~/Android/Sdk/cmdline-tools
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        curl -L -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-mac-9477386_latest.zip
    else
        curl -L -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
    fi
    
    unzip -q /tmp/cmdtools.zip -d ~/Android/Sdk/cmdline-tools
    mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
    
    export ANDROID_HOME=$HOME/Android/Sdk
    export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
    
    # 환경변수 영구 설정
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.zshrc
        echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.zshrc
    else
        echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
        echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
    fi
    
    echo "📝 SDK 라이선스 동의 중..."
    yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
    
    echo "📦 필수 SDK 패키지 설치 중..."
    $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
else
    echo "✅ Android SDK 이미 설치됨"
    export ANDROID_HOME=$HOME/Android/Sdk
    export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
fi

# 3. 프로젝트 설정
echo "⚙️  local.properties 생성 중..."
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 4. Gradle 권한 설정
chmod +x gradlew

# 5. 빌드
echo "🔨 프로젝트 빌드 중..."
./gradlew clean build --stacktrace

echo ""
echo "✅ 환경 설정 완료!"
echo "📱 APK 위치: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "🎯 다음 단계:"
echo "  - 실제 디바이스 연결 또는 에뮬레이터 실행"
echo "  - ./gradlew installDebug 로 앱 설치"
echo "  - python test_server.py 로 테스트 서버 실행"
