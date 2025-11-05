@echo off
echo ========================================
echo CarrotView 빌드 및 설치
echo ========================================
echo.

echo [1/3] 프로젝트 빌드 중...
call gradlew.bat assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 빌드 실패!
    pause
    exit /b 1
)

echo.
echo [2/3] 연결된 디바이스 확인 중...
adb devices
echo.

echo [3/3] 앱 설치 중...
call gradlew.bat installDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 설치 실패!
    echo.
    echo 💡 팁:
    echo   - USB 디버깅이 활성화되어 있는지 확인하세요
    echo   - adb devices 명령으로 디바이스가 인식되는지 확인하세요
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✅ 빌드 및 설치 완료!
echo ========================================
echo.
echo 📱 이제 디바이스에서 CarrotView 앱을 실행하세요.
echo.
pause
