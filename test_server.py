#!/usr/bin/env python3
"""
CarrotView 테스트 TCP 서버
실제 CarrotPilot 데이터 전송 서비스를 시뮬레이션
"""

import json
import time
import socket
import threading
import random
import struct


class TestTCPServer:
    """테스트용 TCP 서버"""
    
    def __init__(self, port=8080):
        self.port = port
        self.running = False
        self.clients = []
        self.server_socket = None
        
        # 시뮬레이션 데이터
        self.speed = 0.0  # m/s (시작은 정지 상태)
        self.cruise_speed = 25.0  # m/s (약 90 km/h)
        self.autopilot_enabled = False  # 시작은 비활성
        self.autopilot_active = False  # 크루즈 비활성
        self.simulation_time = 0  # 시뮬레이션 시간
        
    def start(self):
        """서버 시작"""
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind(('0.0.0.0', self.port))
        self.server_socket.listen(5)
        self.running = True
        
        print(f"✅ TCP 서버 시작: 포트 {self.port}")
        print(f"📱 앱에서 연결하세요: {self.get_local_ip()}:{self.port}")
        
        # 클라이언트 수락 스레드
        accept_thread = threading.Thread(target=self.accept_clients, daemon=True)
        accept_thread.start()
        
        # 데이터 전송 스레드
        broadcast_thread = threading.Thread(target=self.broadcast_data, daemon=True)
        broadcast_thread.start()
        
    def get_local_ip(self):
        """로컬 IP 주소 가져오기"""
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except:
            return "127.0.0.1"
    
    def accept_clients(self):
        """클라이언트 연결 수락"""
        while self.running:
            try:
                self.server_socket.settimeout(1.0)
                client_socket, address = self.server_socket.accept()
                print(f"🔗 클라이언트 연결: {address}")
                
                # 인증 처리
                if self.authenticate_client(client_socket):
                    self.clients.append(client_socket)
                    print(f"✅ 인증 성공: {address}")
                else:
                    print(f"❌ 인증 실패: {address}")
                    client_socket.close()
                    
            except socket.timeout:
                continue
            except Exception as e:
                if self.running:
                    print(f"❌ 연결 오류: {e}")
    
    def authenticate_client(self, client_socket):
        """클라이언트 인증"""
        try:
            # 인증 요청 전송
            auth_request = {
                "type": "auth_required",
                "timestamp": int(time.time()),
                "challenge": f"carrotview_{int(time.time())}"
            }
            self.send_message(client_socket, json.dumps(auth_request))
            
            # 인증 응답 수신
            response = self.receive_message(client_socket)
            if response:
                response_data = json.loads(response)
                expected_token = f"carrotview2024_{auth_request['challenge']}"
                
                if response_data.get('token') == expected_token:
                    # 인증 성공 응답
                    success_response = {
                        "type": "auth_success",
                        "server_version": "1.0",
                        "compression_supported": True
                    }
                    self.send_message(client_socket, json.dumps(success_response))
                    return True
            
            return False
            
        except Exception as e:
            print(f"인증 오류: {e}")
            return False
    
    def send_message(self, client_socket, message):
        """메시지 전송 (TCPClient 프로토콜: 길이 + 압축플래그 + 데이터)"""
        try:
            message_bytes = message.encode('utf-8')
            compression_flag = 0x00  # 압축 안 함
            
            # 전체 길이 = 압축플래그(1바이트) + 메시지
            total_length = 1 + len(message_bytes)
            
            # 길이 전송 (4바이트)
            client_socket.send(struct.pack('>I', total_length))
            # 압축 플래그 전송 (1바이트)
            client_socket.send(bytes([compression_flag]))
            # 메시지 전송
            client_socket.send(message_bytes)
            
        except Exception as e:
            print(f"전송 오류: {e}")
    
    def receive_message(self, client_socket):
        """메시지 수신"""
        try:
            # 길이 수신 (4바이트)
            length_bytes = client_socket.recv(4)
            if len(length_bytes) != 4:
                return None
            
            length = struct.unpack('>I', length_bytes)[0]
            
            # 메시지 수신
            message_bytes = b''
            while len(message_bytes) < length:
                chunk = client_socket.recv(length - len(message_bytes))
                if not chunk:
                    return None
                message_bytes += chunk
            
            return message_bytes.decode('utf-8')
            
        except Exception as e:
            print(f"수신 오류: {e}")
            return None
    
    def generate_data(self):
        """테스트 데이터 생성 - 상태만 전송 (실제 데이터는 CarrotPilot에서)"""
        # 랜덤 데이터 없이 상태만 전송
        
        return {
            "timestamp": int(time.time() * 1000),
            "carState": {
                "vEgo": 0.0,  # 실제 CarrotPilot 데이터 대기
                "vCruise": 0.0,
                "gearShifter": "park",
                "doorOpen": False,
                "seatbeltLatched": True,
                "steeringAngleDeg": 0.0
            },
            "controlsState": {
                "enabled": self.autopilot_enabled,
                "active": self.autopilot_active,
                "alertText": "",
                "alertStatus": "normal"
            },
            "liveTracks": [],  # 빈 배열 - 실제 데이터 없음
            "deviceState": {
                "batteryPercent": 100,
                "thermalStatus": "green"
            }
        }
    
    def broadcast_data(self):
        """데이터 브로드캐스트"""
        while self.running:
            try:
                if self.clients:
                    data = self.generate_data()
                    json_str = json.dumps(data)
                    
                    # 압축 플래그 없이 전송
                    message = b'\x00' + json_str.encode('utf-8')
                    
                    # 모든 클라이언트에게 전송
                    disconnected = []
                    for client in self.clients:
                        try:
                            # 길이 전송
                            length = len(message)
                            client.send(struct.pack('>I', length))
                            # 데이터 전송
                            client.send(message)
                        except Exception as e:
                            print(f"❌ 전송 실패: {e}")
                            disconnected.append(client)
                    
                    # 연결 끊긴 클라이언트 제거
                    for client in disconnected:
                        self.clients.remove(client)
                        print(f"🔌 클라이언트 연결 해제")
                
                time.sleep(0.1)  # 10Hz
                
            except Exception as e:
                print(f"브로드캐스트 오류: {e}")
    
    def stop(self):
        """서버 중지"""
        self.running = False
        for client in self.clients:
            client.close()
        if self.server_socket:
            self.server_socket.close()
        print("🛑 서버 중지")


def main():
    print("🚗 CarrotView 테스트 서버")
    print("=" * 50)
    
    server = TestTCPServer(port=8080)
    server.start()
    
    print("\n서버 실행 중...")
    print("\n📋 명령어:")
    print("  1 - 차량 연결 (enabled=True, active=False)")
    print("  2 - 크루즈 활성화 (enabled=True, active=True, speed=20)")
    print("  0 - 대기 상태 (enabled=False, active=False)")
    print("  q - 종료")
    print()
    
    try:
        while True:
            cmd = input("명령 입력: ").strip()
            
            if cmd == '0':
                server.autopilot_enabled = False
                server.autopilot_active = False
                server.speed = 0.0
                print("✅ 대기 상태로 변경")
            elif cmd == '1':
                server.autopilot_enabled = True
                server.autopilot_active = False
                server.speed = 0.0
                print("✅ 차량 연결됨 (크루즈 대기)")
            elif cmd == '2':
                server.autopilot_enabled = True
                server.autopilot_active = True
                server.speed = 20.0
                print("✅ 크루즈 활성화 (주행 중)")
            elif cmd == 'q':
                break
            else:
                print("❌ 잘못된 명령")
                
    except KeyboardInterrupt:
        pass
    
    print("\n\n종료 중...")
    server.stop()


if __name__ == "__main__":
    main()
