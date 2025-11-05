@echo off
echo ========================================
echo CarrotView 통합 테스트 실행 스크립트
echo ========================================
echo.

echo 1. 단위 테스트 실행 중...
call gradlew.bat test
if %ERRORLEVEL% neq 0 (
    echo 단위 테스트 실패!
    pause
    exit /b 1
)
echo 단위 테스트 완료!
echo.

echo 2. 안드로이드 기기 연결 확인...
adb devices
echo.

echo 3. 계측 테스트 실행 중...
echo    (안드로이드 기기가 연결되어 있어야 합니다)
call gradlew.bat connectedAndroidTest
if %ERRORLEVEL% neq 0 (
    echo 계측 테스트 실패!
    echo 안드로이드 기기가 연결되어 있고 USB 디버깅이 활성화되어 있는지 확인하세요.
    pause
    exit /b 1
)
echo 계측 테스트 완료!
echo.

echo ========================================
echo 모든 통합 테스트가 성공적으로 완료되었습니다!
echo ========================================
echo.
echo 테스트 결과는 다음 위치에서 확인할 수 있습니다:
echo - 단위 테스트: app/build/reports/tests/testDebugUnitTest/
echo - 계측 테스트: app/build/reports/androidTests/connected/
echo.
pause