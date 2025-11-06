@echo off
REM CarrotView Service를 C3에서 기본 활성화하는 스크립트 (Windows)

echo 🥕 CarrotView Service 활성화 중...

REM ADB로 C3에 연결
set DEVICE_ID=ac960474

REM CarrotViewEnabled 파라미터를 1로 설정
adb -s %DEVICE_ID% shell "echo -n '1' > /data/params/d/CarrotViewEnabled"

REM 설정 확인
adb -s %DEVICE_ID% shell "cat /data/params/d/CarrotViewEnabled" > temp_enabled.txt
set /p ENABLED=<temp_enabled.txt
del temp_enabled.txt

if "%ENABLED%"=="1" (
    echo ✅ CarrotView Service가 활성화되었습니다!
    
    REM 서비스 재시작
    echo 🔄 서비스 재시작 중...
    adb -s %DEVICE_ID% shell "pkill -f carrot"
    timeout /t 2 /nobreak >nul
    
    REM 서비스 상태 확인
    echo 📊 서비스 상태:
    adb -s %DEVICE_ID% shell "ps aux | grep carrot | grep -v grep"
) else (
    echo ❌ 활성화 실패
)

pause
