# CarrotView 자동 환경 설정 스크립트 (Windows PowerShell)
# 관리자 권한으로 실행 권장

Write-Host "🚀 CarrotView 자동 환경 설정 시작..." -ForegroundColor Green

# 1. Java 설치 확인
Write-Host "`n📋 Java 확인 중..." -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1
    Write-Host "✅ Java 이미 설치됨: $($javaVersion[0])" -ForegroundColor Green
} catch {
    Write-Host "📦 Java 설치 필요" -ForegroundColor Yellow
    Write-Host "Chocolatey가 설치되어 있다면 자동 설치를 시도합니다..." -ForegroundColor Yellow
    
    try {
        choco install openjdk17 -y
        Write-Host "✅ Java 설치 완료" -ForegroundColor Green
    } catch {
        Write-Host "❌ Chocolatey가 없습니다. 수동으로 Java를 설치해주세요:" -ForegroundColor Red
        Write-Host "   https://adoptium.net/" -ForegroundColor Yellow
        exit 1
    }
}

# 2. Android SDK 설치
$androidHome = "$env:USERPROFILE\Android\Sdk"
Write-Host "`n📋 Android SDK 확인 중..." -ForegroundColor Cyan

if (-not (Test-Path $androidHome)) {
    Write-Host "📦 Android SDK 설치 중..." -ForegroundColor Yellow
    
    # 디렉토리 생성
    New-Item -ItemType Directory -Path "$androidHome\cmdline-tools" -Force | Out-Null
    
    # Command Line Tools 다운로드
    $cmdToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
    $cmdToolsZip = "$env:TEMP\cmdtools.zip"
    
    Write-Host "   다운로드 중: $cmdToolsUrl" -ForegroundColor Yellow
    Invoke-WebRequest -Uri $cmdToolsUrl -OutFile $cmdToolsZip -UseBasicParsing
    
    Write-Host "   압축 해제 중..." -ForegroundColor Yellow
    Expand-Archive -Path $cmdToolsZip -DestinationPath "$androidHome\cmdline-tools" -Force
    
    # cmdline-tools를 latest로 이동
    if (Test-Path "$androidHome\cmdline-tools\cmdline-tools") {
        Move-Item "$androidHome\cmdline-tools\cmdline-tools" "$androidHome\cmdline-tools\latest" -Force
    }
    
    # 환경변수 설정
    [System.Environment]::SetEnvironmentVariable('ANDROID_HOME', $androidHome, 'User')
    $currentPath = [System.Environment]::GetEnvironmentVariable('PATH', 'User')
    $newPath = "$currentPath;$androidHome\cmdline-tools\latest\bin;$androidHome\platform-tools"
    [System.Environment]::SetEnvironmentVariable('PATH', $newPath, 'User')
    
    # 현재 세션에도 적용
    $env:ANDROID_HOME = $androidHome
    $env:PATH += ";$androidHome\cmdline-tools\latest\bin;$androidHome\platform-tools"
    
    # SDK Manager 경로
    $sdkmanager = "$androidHome\cmdline-tools\latest\bin\sdkmanager.bat"
    
    # 라이선스 동의
    Write-Host "   📝 SDK 라이선스 동의 중..." -ForegroundColor Yellow
    $yesInput = "y`ny`ny`ny`ny`ny`ny`ny`ny`n"
    $yesInput | & $sdkmanager --licenses 2>&1 | Out-Null
    
    # 필수 패키지 설치
    Write-Host "   📦 필수 SDK 패키지 설치 중..." -ForegroundColor Yellow
    & $sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" 2>&1 | Out-Null
    
    Write-Host "✅ Android SDK 설치 완료" -ForegroundColor Green
} else {
    Write-Host "✅ Android SDK 이미 설치됨: $androidHome" -ForegroundColor Green
    $env:ANDROID_HOME = $androidHome
    $env:PATH += ";$androidHome\cmdline-tools\latest\bin;$androidHome\platform-tools"
}

# 3. 프로젝트 설정
Write-Host "`n⚙️  local.properties 생성 중..." -ForegroundColor Cyan
$sdkPath = $androidHome -replace '\\', '\\'
"sdk.dir=$sdkPath" | Out-File -FilePath "local.properties" -Encoding utf8
Write-Host "✅ local.properties 생성 완료" -ForegroundColor Green

# 4. Gradle 빌드
Write-Host "`n🔨 프로젝트 빌드 중..." -ForegroundColor Cyan
Write-Host "   (첫 빌드는 시간이 걸릴 수 있습니다...)" -ForegroundColor Yellow

try {
    .\gradlew.bat clean build --stacktrace
    Write-Host "`n✅ 빌드 성공!" -ForegroundColor Green
} catch {
    Write-Host "`n❌ 빌드 실패" -ForegroundColor Red
    Write-Host "   에러 로그를 확인하고 다시 시도해주세요." -ForegroundColor Yellow
    exit 1
}

# 완료 메시지
Write-Host "`n" -NoNewline
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host ""
Write-Host "✅ CarrotView 환경 설정 완료!" -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "=" -NoNewline -ForegroundColor Green
Write-Host "`n"

Write-Host "📱 APK 위치: " -NoNewline -ForegroundColor Cyan
Write-Host "app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Yellow

Write-Host "`n🎯 다음 단계:" -ForegroundColor Cyan
Write-Host "  1. 실제 디바이스 연결 또는 에뮬레이터 실행" -ForegroundColor White
Write-Host "  2. " -NoNewline -ForegroundColor White
Write-Host ".\gradlew.bat installDebug" -NoNewline -ForegroundColor Yellow
Write-Host " 로 앱 설치" -ForegroundColor White
Write-Host "  3. " -NoNewline -ForegroundColor White
Write-Host "python test_server.py" -NoNewline -ForegroundColor Yellow
Write-Host " 로 테스트 서버 실행" -ForegroundColor White

Write-Host "`n💡 팁: PowerShell을 재시작하면 환경변수가 완전히 적용됩니다." -ForegroundColor Yellow
