#!/usr/bin/env python3
"""
CarrotView 클라이언트 테스트 - 서버 연결 및 데이터 수신 테스트
"""

import socket
import json
import time
import threading


class CarrotViewTestClient:
    """CarrotView 테스트 클라이언트"""
    
    def __init__(self, host='localhost', port=8080):
        self.host = host
        self.port = port
        self.socket = None
        self.running = False
        self.data_count = 0
        
    def connect(self):
        """서버에 연결"""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.host, self.port))
            self.running = True
            
            print(f"✅ 서버 연결 성공: {self.host}:{self.port}")
            return True
            
        except Exception as e:
            print(f"❌ 서버 연결 실패: {e}")
            return False
    
    def receive_data(self):
        """데이터 수신 및 처리"""
        buffer = ""
        
        while self.running:
            try:
                data = self.socket.recv(4096).decode('utf-8')
                if not data:
                    break
                
                buffer += data
                
                # 줄바꿈으로 구분된 JSON 메시지 처리
                while '\n' in buffer:
                    line, buffer = buffer.split('\n', 1)
                    if line.strip():
                        self.process_message(line.strip())
                        
            except Exception as e:
                if self.running:
                    print(f"데이터 수신 오류: {e}")
                break
    
    def process_message(self, message):
        """수신된 메시지 처리"""
        try:
            data = json.loads(message)
            self.data_count += 1
            
            # 5초마다 상태 출력
            if self.data_count % 50 == 0:
                self.print_data_summary(data)
                
        except json.JSONDecodeError as e:
            print(f"JSON 파싱 오류: {e}")
    
    def print_data_summary(self, data):
        """데이터 요약 출력"""
        car_state = data.get('carState', {})
        controls_state = data.get('controlsState', {})
        live_tracks = data.get('liveTracks', [])
        device_state = data.get('deviceState', {})
        
        speed_ms = car_state.get('vEgo', 0)
        speed_kmh = speed_ms * 3.6
        cruise_speed = car_state.get('vCruise', 0) * 3.6
        gear = car_state.get('gearShifter', 'unknown')
        steering = car_state.get('steeringAngleDeg', 0)
        
        autopilot_enabled = controls_state.get('enabled', False)
        autopilot_active = controls_state.get('active', False)
        alert_text = controls_state.get('alertText', '')
        
        battery = device_state.get('batteryPercent', 0)
        thermal = device_state.get('thermalStatus', 'unknown')
        
        print(f"\n📊 데이터 수신 #{self.data_count}")
        print(f"🚗 속도: {speed_kmh:.1f} km/h (크루즈: {cruise_speed:.1f} km/h)")
        print(f"⚙️  기어: {gear} | 조향: {steering:.1f}°")
        print(f"🤖 자율주행: {'✅ 활성' if autopilot_active else '⏸️ 대기' if autopilot_enabled else '❌ 비활성'}")
        if alert_text:
            print(f"⚠️  알림: {alert_text}")
        print(f"🚙 주변차량: {len(live_tracks)}대")
        print(f"🔋 배터리: {battery}% ({thermal})")
        
        # 가장 가까운 차량 정보
        if live_tracks:
            closest = min(live_tracks, key=lambda x: x.get('dRel', float('inf')))
            print(f"🚨 가장 가까운 차량: {closest.get('dRel', 0):.1f}m")
    
    def disconnect(self):
        """연결 해제"""
        self.running = False
        if self.socket:
            self.socket.close()
        print("🔌 연결 해제됨")


def main():
    """메인 함수"""
    print("📱 CarrotView 클라이언트 테스트")
    print("=" * 40)
    
    client = CarrotViewTestClient()
    
    if client.connect():
        print("📡 데이터 수신 시작... (Ctrl+C로 중지)")
        print("=" * 40)
        
        # 데이터 수신 스레드 시작
        receive_thread = threading.Thread(target=client.receive_data, daemon=True)
        receive_thread.start()
        
        try:
            # 메인 스레드에서 대기
            while client.running:
                time.sleep(1)
                
        except KeyboardInterrupt:
            print("\n\n👋 사용자가 클라이언트를 중지했습니다.")
            
        finally:
            client.disconnect()
    else:
        print("❌ 서버에 연결할 수 없습니다.")
        print("💡 먼저 live_demo_server.py를 실행하세요.")


if __name__ == "__main__":
    main()