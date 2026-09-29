# 피클 사운드 APK 만들기 (GitHub에서 자동 빌드, PC 설치 불필요)

1. github.com 가입/로그인 → 오른쪽 위 + → New repository → 이름 `PickleSound` → Create
2. "uploading an existing file" 클릭 → 이 폴더(PickleSound) 안의 **모든 파일과 폴더**를 끌어다 놓기
   (`.github` 폴더가 꼭 포함돼야 함) → Commit changes
3. 상단 **Actions** 탭 → "Build APK" 가 5~8분 돌고 초록 체크가 되면 완료
4. 저장소 첫 화면 오른쪽 **Releases** → 최신 빌드 → `PickleSound.apk` 다운로드

## 워치에 설치 (Wear Installer 2)
- 워치: 개발자 옵션 → ADB 디버깅 + 무선 디버깅 켜기 (폰과 같은 Wi-Fi)
- 폰: Wear Installer 2 에서 워치 IP:포트 입력(처음엔 페어링 코드) → 받은 PickleSound.apk 선택 → 설치

코드를 고쳐서 다시 올리면 새 APK가 자동으로 만들어지고, 같은 키로 서명되므로 기존 앱 위에 업데이트 설치됩니다.
