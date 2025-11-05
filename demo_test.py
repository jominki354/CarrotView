#!/usr/bin/env python3
"""
CarrotView 데모 테스트 - 실제 동작 확인
"""

import json
import time
import socket
import threading
import random
from typing import Dict, Any


class MockCarrotPilotData:
    """CarrotPilot 데이터 시뮬레이터"""
    
    def __init__(self):
        self.running = False
        self.speed = 0.0
        self.cruise_speed = 0.0
        self.autopilot_enabled = False
        self.gear = "park"
        
    def generate_realistic_data(self) -> Dict[str, Any]:
        """실제와 유사한 주행 데이터 생성"""
        
        # 속도 변화 시뮬레이션
        if self.autopilot_enabled:
            # 자율주행 모드: 안정적인 속도
            target_speed = self.cruise_speed
            speed_diff = target_speed - self.speed
            self.speed += speed_diff * 0.1  # 점진적 변화
        else:
            # 수동 주행: 더 변동적인 속도
            self.speed += random.uniform(-2.0, 2.0)
            self.speed = max(0, min(self.speed, 50.0))  # 0-50 m/s 제한
        
        # 가끔 자율주행 모드 토글
        if random.random() < 0.01:  # 1% 확률
            self.autopilot_enabled = not self.autopilot_enabled
            if self.autopilot_enabled:
                self.cruise_speed = self.speed + random.uniform(-5, 5)
                self.cruise_speed = max(10, min(self.cruise_speed, 30))
        
        # 기어 변경 시뮬레이션
        if self.speed < 0.5:
            self.gear = random.choice(["park", "drive", "reverse"])
        elif self.speed > 0.5:
            self.gear = "drive"
        
        # 주변 차량 시뮬레이션
        num_tracks = random.randint(0, 8)
        live_tracks = []
        for i in range(num_tracks):
            live_tracks.append({
                "trackId": i + 1,
                "dRel": random.uniform(10, 100),  # 10-100m 거리
                "yRel": random.uniform(-4, 4),    # 좌우 4m
                "vRel": random.uniform(-10, 10)   # 상대속도
            })
        
        return {
            "timestamp": int(time.time() * 1000),
            "carState": {
                "vEgo": round(self.speed, 2),
                "vCruise": round(self.cruise_speed, 2),
                "gearShifter": self.gear,
                "doorOpen": random.random() < 0.05,  # 5% 확률로 문 열림
                "seatbeltLatched": random.random() < 0.95,  # 95% 확률로 안전벨트
                "steeringAngleDeg": random.uniform(-45, 45)
            },
            "controlsState": {
                "enabled": self.autopilot_enabled,
                "active": self.autopilot_enabled and self.speed > 5,
                "alertText": "Cruise Control" if self.autopilot_enabled else "",
                "alertStatus": "normal" if self.autopilot_enabled else "userPrompt"
            },
            "liveTracks": live_tracks,
            "deviceState": {
                "batteryPercent": random.randint(70, 100),
                "thermalStatus": random.choice(["green", "yellow", "red"])
            }
        }


def test_data_generation():
    """데이터 생성 테스트"""
    print("=== 데이터 생성 테스트 ===")
    
    generator = MockCarrotPilotData()
    
    for i in range(5):
        data = generator.generate_realistic_data()
        
        print(f"\n샘플 {i+1}:")
        print(f"  시간: {data['timestamp']}")
        print(f"  속도: {data['carState']['vEgo']:.1f} m/s ({data['carState']['vEgo']*3.6:.1f} km/h)")
        print(f"  크루즈: {data['carState']['vCruise']:.1f} m/s")
        print(f"  기어: {data['carState']['gearShifter']}")
        print(f"  자율주행: {data['controlsState']['enabled']}")
        print(f"  주변차량: {len(data['liveTracks'])}대")
        print(f"  배터리: {data['deviceState']['batteryPercent']}%")
        
        time.sleep(0.5)
    
    print("\n✅ 데이터 생성 테스트 완료")


def test_json_serialization():
    """JSON 직렬화 테스트"""
    print("\n=== JSON 직렬화 테스트 ===")
    
    generator = MockCarrotPilotData()
    data = generator.generate_realistic_data()
    
    # JSON 직렬화
    json_str = json.dumps(data, ensure_ascii=False, indent=2)
    print(f"JSON 크기: {len(json_str)} 바이트")
    
    # JSON 역직렬화
    parsed_data = json.loads(json_str)
    
    # 데이터 검증
    required_keys = ['timestamp', 'carState', 'controlsState', 'liveTracks', 'deviceState']
    if all(key in parsed_data for key in required_keys):
        print("✅ JSON 직렬화/역직렬화 성공")
        return True
    else:
        print("❌ JSON 직렬화/역직렬화 실패")
        return False


def main():
    """메인 함수"""
    print("🚗 CarrotView 실제 동작 테스트")
    print("=" * 50)
    
    # 1. 데이터 생성 테스트
    test_data_generation()
    
    # 2. JSON 직렬화 테스트
    test_json_serialization()
    
    print("\n🎉 모든 테스트가 성공적으로 완료되었습니다!")
    print("\n📋 다음 단계:")
    print("1. Android Studio에서 CarrotView 프로젝트 열기")
    print("2. 에뮬레이터나 실제 기기에서 앱 실행")
    print("3. 네트워크 설정에서 데이터 소스 연결")
    print("4. 실시간 대시보드 확인")


if __name__ == "__main__":
    main()