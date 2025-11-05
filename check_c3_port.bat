@echo off
echo C3 포트 8080 확인 중...
echo.

REM C3 IP 주소
set C3_IP=192.168.137.23

echo [1] Ping 테스트
ping -n 1 %C3_IP%

echo.
echo [2] 포트 8080 확인
powershell -Command "Test-NetConnection -ComputerName %C3_IP% -Port 8080"

echo.
echo [3] 텔넷 테스트 (수동)
echo telnet %C3_IP% 8080 명령어로 직접 확인 가능
echo.

pause
