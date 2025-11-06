#!/usr/bin/env python3
"""
C3 CarrotPilot 테스트 모드
차량 연결 없이 CarrotView 테스트를 위한 가짜 상태 생성
"""

import sys
import time

# openpilot 경로 추가
if '/data/openpilot' not in sys.path:
    sys.path.insert(0, '/data/openpilot')

try:
    from openpilot.common.params import Params
except ImportError:
    from common.params import Params


class TestModeController:
    """테스트 모드 컨트롤러"""
    
    def __init__(self):
        self.params = Params()
        
    def set_test_mode(self, mode):
        """
        테스트 모드 설정
        mode:
            0 - 대기 중 (enabled=False, active=False)
            1 - 주행 준비 (enabled=True, active=False)
            2 - 크루즈 활성 (enabled=True, active=True)
        """
        import os
        test_file = "/data/openpilot/selfdrive/carrot/test_mode.txt"
        
        if mode == 0:
            print("🔵 대기 중 모드")
            with open(test_file, 'w') as f:
                f.write("enabled=0\nactive=0\n")
        elif mode == 1:
            print("🟡 주행 준비 모드 (enabled=True, active=False)")
            with open(test_file, 'w') as f:
                f.write("enabled=1\nactive=0\n")
        elif mode == 2:
            print("🟢 크루즈 활성 모드 (enabled=True, active=True)")
            with open(test_file, 'w') as f:
                f.write("enabled=1\nactive=1\n")
        else:
            print("❌ 잘못된 모드")
            return False
        
        print("✅ 테스트 모드 설정 완료")
        print("⚠️  data_transmitter를 재시작해야 적용됩니다:")
        print("   pkill -f data_transmitter")
        
        return True
    
    def set_speed(self, speed_kmh):
        """속도 설정 (km/h)"""
        speed_ms = speed_kmh / 3.6
        print(f"🚗 속도 설정: {speed_kmh} km/h ({speed_ms:.2f} m/s)")
        # vEgo는 실제 차량에서만 업데이트되므로 여기서는 설정 불가
        # 대신 CarrotView 앱에서 속도 > 0이면 주행 화면 표시
    
    def get_current_state(self):
        """현재 상태 확인"""
        import os
        test_file = "/data/openpilot/selfdrive/carrot/test_mode.txt"
        
        enabled = False
        active = False
        
        try:
            if os.path.exists(test_file):
                with open(test_file, 'r') as f:
                    content = f.read()
                    enabled = "enabled=1" in content
                    active = "active=1" in content
        except:
            pass
        
        print("\n📊 현재 테스트 상태:")
        print(f"  TestEnabled: {enabled}")
        print(f"  TestActive: {active}")
        
        if not enabled and not active:
            print("  상태: 🔵 대기 중")
        elif enabled and not active:
            print("  상태: 🟡 주행 준비")
        elif enabled and active:
            print("  상태: 🟢 크루즈 활성")
        
        return enabled, active


def main():
    print("=" * 50)
    print("🥕 CarrotPilot 테스트 모드")
    print("=" * 50)
    print()
    
    controller = TestModeController()
    
    # 현재 상태 확인
    controller.get_current_state()
    
    print("\n📋 명령어:")
    print("  0 - 대기 중 (enabled=False, active=False)")
    print("  1 - 주행 준비 (enabled=True, active=False)")
    print("  2 - 크루즈 활성 (enabled=True, active=True)")
    print("  s - 현재 상태 확인")
    print("  q - 종료")
    print()
    
    try:
        while True:
            cmd = input("명령 입력: ").strip().lower()
            
            if cmd == 'q':
                break
            elif cmd == 's':
                controller.get_current_state()
            elif cmd in ['0', '1', '2']:
                controller.set_test_mode(int(cmd))
                time.sleep(0.5)
                controller.get_current_state()
            else:
                print("❌ 잘못된 명령")
    
    except KeyboardInterrupt:
        pass
    
    print("\n👋 종료")


if __name__ == "__main__":
    main()
