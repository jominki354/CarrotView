#!/usr/bin/env python3
"""
CarrotView 라이브 데모 서버 - 실시간 데이터 전송
"""

import json
import time
import socket
import threading
import random
from typing import Dict, Any, List


class LiveCarrotPilotSimulator:
    """실시간 CarrotPilot 시뮬레이터"""
    
    def __init__(self):
        self.speed = 0.0
        self.cruise_speed = 25.0
        self.autopilot_enabled = False
        self.gear = "park"
        self.steering_angle = 0.0
        self.battery = 85
        self.scenario_time = 0
        
    def update_scenario(self):
        """시나리오 기반 데이터 업데이트"""
        self.scenario_time += 0.1
        
        # 시나리오 1: 정차 -> 출발 -> 자율주행 활성화
        if self.scenario_time < 10:
            # 정차 상태
            self.speed = 0
            self.gear = "park"
            self.autopilot_enabled = False
            
        elif self.scenario_time < 20:
            # 출발
            self.gear = "drive"
            self.speed = min(self.speed + 0.5, 15.0)  # 점진적 가속
            self.steering_angle += random.uniform(-2, 2)
            self.steering_angle = max(-30, min(30, self.steering_angle))
            
        elif self.scenario_time < 30:
            # 자율주행 활성화
            if not self.autopilot_enabled:
                self.autopilot_enabled = True
                print("🤖 자율주행 모드 활성화!")
            
            # 크루즈 속도로 수렴
            speed_diff = self.cruise_speed - self.speed
            self.speed += speed_diff * 0.05
            
            # 자율주행 시 부드러운 조향
            self.steering_angle *= 0.95
            
        elif self.scenario_time < 50:
            # 안정적인 자율주행
            self.speed = self.cruise_speed + random.uniform(-1, 1)
            self.steering_angle += random.uniform(-1, 1)
            self.steering_angle = max(-10, min(10, self.steering_angle))
            
        else:
            # 시나리오 리셋
            self.scenario_time = 0
            self.autopilot_enabled = False
            print("🔄 시나리오 리셋")
        
        # 배터리 소모 시뮬레이션
        if self.speed > 0:
            self.battery -= 0.001
            self.battery = max(0, self.battery)
    
    def generate_live_tracks(self) -> List[Dict]:
        """주변 차량 시뮬레이션"""
        tracks = []
        
        # 자율주행 모드일 때 더 많은 차량 감지
        max_tracks = 8 if self.autopilot_enabled else 3
        num_tracks = random.randint(0, max_tracks)
        
        for i in range(num_tracks):
            # 거리별 차량 분포
            if i == 0:  # 가장 가까운 차량
                distance = random.uniform(15, 40)
            else:
                distance = random.uniform(20, 100)
            
            tracks.append({
                "trackId": i + 1,
                "dRel": round(distance, 1),
                "yRel": round(random.uniform(-3.5, 3.5), 1),
                "vRel": round(random.uniform(-15, 10), 1)
            })
        
        return sorted(tracks, key=lambda x: x["dRel"])
    
    def get_current_data(self) -> Dict[str, Any]:
        """현재 상태 데이터 반환"""
        self.update_scenario()
        
        # 경고 상태 결정
        alert_text = ""
        alert_status = "normal"
        
        if not self.autopilot_enabled and self.speed > 20:
            alert_text = "고속 수동 주행 중"
            alert_status = "userPrompt"
        elif self.autopilot_enabled:
            if self.speed < 5:
                alert_text = "자율주행 대기 중"
                alert_status = "userPrompt"
            else:
                alert_text = "자율주행 활성"
                alert_status = "normal"
        
        return {
            "timestamp": int(time.time() * 1000),
            "carState": {
                "vEgo": round(self.speed, 2),
                "vCruise": round(self.cruise_speed, 2),
                "gearShifter": self.gear,
                "doorOpen": False,
                "seatbeltLatched": True,
                "steeringAngleDeg": round(self.steering_angle, 1)
            },
            "controlsState": {
                "enabled": self.autopilot_enabled,
                "active": self.autopilot_enabled and self.speed > 5,
                "alertText": alert_text,
                "alertStatus": alert_status
            },
            "liveTracks": self.generate_live_tracks(),
            "deviceState": {
                "batteryPercent": int(self.battery),
                "thermalStatus": "green" if self.battery > 20 else "yellow"
            }
        }


class CarrotViewServer:
    """CarrotView 데이터 서버"""
    
    def __init__(self, port=8080):
        self.port = port
        self.running = False
        self.clients = []
        self.server_socket = None
        self.simulator = LiveCarrotPilotSimulator()
        self.data_count = 0
        
    def start_server(self):
        """서버 시작"""
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(5)
            
            self.running = True
            
            print(f"🚀 CarrotView 라이브 서버 시작됨!")
            print(f"📡 포트: {self.port}")
            print(f"🌐 로컬 주소: localhost:{self.port}")
            print(f"📱 안드로이드 에뮬레이터: 10.0.2.2:{self.port}")
            print("=" * 50)
            
            # 클라이언트 연결 처리 스레드
            threading.Thread(target=self._accept_clients, daemon=True).start()
            
            # 데이터 전송 스레드
            threading.Thread(target=self._broadcast_data, daemon=True).start()
            
            return True
            
        except Exception as e:
            print(f"❌ 서버 시작 실패: {e}")
            return False
    
    def _accept_clients(self):
        """클라이언트 연결 수락"""
        while self.running:
            try:
                client_socket, address = self.server_socket.accept()
                self.clients.append(client_socket)
                print(f"📲 새 클라이언트 연결: {address[0]}:{address[1]}")
                
            except Exception as e:
                if self.running:
                    print(f"연결 오류: {e}")
    
    def _broadcast_data(self):
        """데이터 브로드캐스트"""
        while self.running:
            try:
                # 실시간 데이터 생성
                data = self.simulator.get_current_data()
                json_data = json.dumps(data, ensure_ascii=False)
                message = json_data.encode('utf-8') + b'\n'
                
                # 모든 클라이언트에게 전송
                disconnected_clients = []
                for client in self.clients:
                    try:
                        client.send(message)
                    except:
                        disconnected_clients.append(client)
                
                # 연결 끊어진 클라이언트 제거
                for client in disconnected_clients:
                    if client in self.clients:
                        self.clients.remove(client)
                        client.close()
                        print("📵 클라이언트 연결 해제됨")
                
                # 상태 출력 (5초마다)
                self.data_count += 1
                if self.data_count % 50 == 0:  # 10Hz * 5초
                    self._print_status(data)
                
                time.sleep(0.1)  # 10Hz 전송
                
            except Exception as e:
                print(f"데이터 전송 오류: {e}")
                time.sleep(1)
    
    def _print_status(self, data):
        """상태 출력"""
        speed_kmh = data['carState']['vEgo'] * 3.6
        autopilot = "🤖 자율주행" if data['controlsState']['enabled'] else "👤 수동"
        active = "✅ 활성" if data['controlsState']['active'] else "⏸️ 대기"
        tracks = len(data['liveTracks'])
        battery = data['deviceState']['batteryPercent']
        
        print(f"📊 {speed_kmh:5.1f}km/h | {autopilot} {active} | 🚗{tracks:2d}대 | 🔋{battery:3d}% | 📱{len(self.clients):2d}개")
        
        if data['controlsState']['alertText']:
            print(f"⚠️  {data['controlsState']['alertText']}")
    
    def stop_server(self):
        """서버 중지"""
        self.running = False
        
        for client in self.clients:
            client.close()
        
        if self.server_socket:
            self.server_socket.close()
        
        print("\n🛑 서버 중지됨")


def main():
    """메인 함수"""
    print("🚗 CarrotView 라이브 데모 서버")
    print("=" * 50)
    
    server = CarrotViewServer()
    
    if server.start_server():
        print("\n📋 사용 방법:")
        print("1. 안드로이드 기기/에뮬레이터에서 CarrotView 앱 실행")
        print("2. 설정 → 서버 주소 입력:")
        print("   - 로컬 테스트: localhost:8080")
        print("   - 에뮬레이터: 10.0.2.2:8080")
        print("   - 실제 기기: [PC IP]:8080")
        print("3. 연결 버튼 클릭")
        print("4. 실시간 Tesla 스타일 대시보드 확인!")
        print("\n🎬 자동 시나리오:")
        print("   정차 → 출발 → 자율주행 활성화 → 안정 주행 → 반복")
        print("\n종료: Ctrl+C")
        print("=" * 50)
        
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            print("\n\n👋 사용자가 서버를 중지했습니다.")
            server.stop_server()
    else:
        print("❌ 서버 시작 실패")


if __name__ == "__main__":
    main()