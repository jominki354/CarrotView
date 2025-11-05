# CarrotView 초보자 가이드 🚗

## 완전 자동 설정 (SSH 불필요!)

### 방법 1: 자동 시작 설정 (권장)

**한 번만 설정하면 됩니다!**

1. **C3에 파일 복사:**
   - USB 케이블로 C3 연결
   - `openpilot/selfdrive/carrot/` 폴더를 C3의 `/data/openpilot/selfdrive/carrot/`에 복사

2. **C3에서 설치 스크립트 실행 (한 번만):**
   ```bash
   # Termux 앱 또는 SSH로 접속
   cd /data/openpilot/selfdrive/carrot
   chmod +x install_autostart.sh
   ./install_autostart.sh
   ```

3. **C3 재부팅**

4. **완료!** 이제 C3가 부팅될 때마다 자동으로 서비스가 시작됩니다.

---

### 방법 2: 수동 시작 (간단)

**C3 부팅 후 한 번만 실행:**

```bash
cd /data/openpilot/selfdrive/carrot
chmod +x remote_start.sh
./remote_start.sh
```

---

### 방법 3: 앱에서 자동 연결 (개발 중)

**미래 버전에서 지원 예정:**
- 앱 실행 → 자동으로 C3 검색
- 서비스 미실행 시 자동 시작
- 완전 자동화!

---

## 사용 방법

1. **C3와 핸드폰을 같은 Wi-Fi에 연결**
   - C3 핫스팟 사용 권장
   - 또는 같은 Wi-Fi 네트워크

2. **CarrotView 앱 실행**
   - 자동으로 C3 검색
   - 연결 성공!

3. **차량 시동 후 주행**
   - 주행 준비 중 → 주행 준비 완료 → 시각화 표시

---

## 문제 해결

### "연결 안 됨" 표시
- C3와 핸드폰이 같은 네트워크에 있는지 확인
- C3에서 서비스가 실행 중인지 확인: `pgrep -f carrotview_transmitter`

### 시각화가 표시되지 않음
- 차량 시동이 켜져 있는지 확인
- 크루즈 컨트롤이 활성화되어 있는지 확인

### 서비스 재시작
```bash
pkill -f carrotview_transmitter
cd /data/openpilot/selfdrive/carrot
./remote_start.sh
```

---

## 설정 변경

`/data/openpilot/selfdrive/carrot_settings.json` 파일 수정:

```json
{
  "enabled": true,          // 서비스 활성화
  "port": 8080,            // 포트 번호
  "rate": 10.0,            // 업데이트 주기 (Hz)
  "max_clients": 3         // 최대 연결 수
}
```

---

## 고급 사용자

### SSH 접속
```bash
ssh comma@192.168.137.23
# 비밀번호: C3 설정에서 확인
```

### 로그 확인
```bash
tail -f /data/carrotview.log
```

### 서비스 상태 확인
```bash
ps aux | grep carrotview
```
