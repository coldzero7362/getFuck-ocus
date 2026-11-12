# 화면 캡처 모음

에뮬레이터(1080×2400, Android, 다크 런처)에서 2026-11-12에 `ui/improvements` 브랜치 빌드로 찍은 화면이다.
다른 세션에서 UI·문구를 검토하거나 수정할 때 참고용으로 쓴다. 앱에 쓰인 문구 전체는 [`../strings.json`](../strings.json)에 있다.

> 화면이 바뀌면 다시 찍어야 한다. 캡처 파일 이름의 번호는 둘러본 순서일 뿐 앱 구조와는 무관하다 (11번은 12번과 같아서 지웠다).

## 목록

| 파일 | 화면 | 상태 |
|---|---|---|
| `01-home.png` | 오늘의 과제(홈) | 진행 중 과제 히어로 카드 |
| `02-home-tasks.png` | 홈 아래쪽 | 다른 과제 목록(완료 배지·상태 줄) |
| `03-history-calendar.png` | 기록 탭 | 월 달력 |
| `04-history-day-streak.png` | 기록 탭 아래쪽 | 날짜별 상세 + 연속 기록 리스트 |
| `05-nfc-tags-empty.png` | NFC 태그 탭 | 등록된 태그 없음 |
| `06-nfc-tag-add-wait.png` | NFC 태그 추가 | 태그 대기 중 |
| `07-nfc-tag-add-confirm.png` | NFC 태그 추가 | 태그 읽음 → 이름 확인 |
| `08-nfc-tag-add-ready.png` | NFC 태그 추가 | 저장 직전 |
| `09-nfc-tags-list.png` | NFC 태그 탭 | 태그 1개 등록됨 |
| `10-edit-new-basic-method.png` | 과제 만들기 | 기본 정보·인증 방식 |
| `12-edit-new-alarm-miss.png` | 과제 만들기 | 일정·알림·놓쳤을 때 |
| `13-edit-nag-messages-photos.png` | 과제 만들기 | 억제 화면 문구·사진 목록 |
| `14-edit-nfc-tag-pick.png` | 과제 만들기 | NFC 인증 → 태그 선택 |
| `15-edit-place-empty.png` | 과제 만들기 | 위치 인증, 장소 미지정 |
| `16-place-picker.png` | 장소 선택 지도 | OSM 초록 필터·도트 핀·반경 원 |
| `17-edit-place-set.png` | 과제 만들기 | 장소 지정됨(주소·반경·초기화) |
| `18-settings-top.png` | 설정 | 위쪽 |
| `19-settings-permissions.png` | 설정 | 권한 목록 |
| `20-allowed-apps.png` | 허용 앱 | 잠금 중에도 쓸 앱 |
| `21-capture-camera.png` | 사진 인증 촬영 | 카메라 미리보기 |
| `22-home-locked.png` | 홈 | 잠금(LOCK) 걸린 상태 |
| `23-lock-screen.png` | 잠금 화면 | 위쪽 |
| `24-lock-screen-bottom.png` | 잠금 화면 | 아래쪽(긴급전화·비상 해제) |
| `25-lock-blocks-other-app.png` | 다른 앱을 연 순간 | 잠금 화면이 덮음 |
| `26-nag-overlay-home.png` | 런처 위 억제 화면 | 문구가 크기·속도 랜덤으로 흘러감 |
| `27-home-nag.png` | 홈 | 억제(NAG) 걸린 상태 |
| `28-nag-emergency.png` | 억제 중 비상 해제 화면 | 위쪽(앱 안에서도 문구가 지나감) |
| `29-nag-emergency-bottom.png` | 억제 중 비상 해제 화면 | 아래쪽 |

탭 체크 과제의 인증 화면은 누르는 즉시 통과해서 따로 찍지 않았다.

## 다시 찍는 법 (PowerShell)

- adb: `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`
- 캡처: `adb shell screencap -p /sdcard/s.png` 후 `adb pull`. PowerShell `>` 로 받으면 바이너리가 깨진다.
- 탭: `adb shell uiautomator dump` 결과에서 text/content-desc로 좌표를 찾아 `input tap`.
- 잠금·억제 상태 재현: `shared_prefs/lock.xml` 을 `run-as` 로 바꾸고 `run-as com.getfuckocus kill <pid>` 로 프로세스만 재시작.
  **force-stop 금지** — 접근성 서비스가 풀린다.

## 문구 JSON

```bash
python -I tools/extract_strings.py . docs/strings.json
```

`resources`(strings.xml)와 `kotlin`(파일별 `{line, text}`, 한글이 든 문자열 리터럴만)으로 나뉜다.
