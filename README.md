# getFuck-ocus
자기 통제를 위한 과제 앱 개발 예제입니다.

과제를 사진으로 인증하지 못하면 스스로 폰 사용을 제한하는 **개인용** Android 앱.

## 열기
1. Android Studio에서 이 폴더를 열거나 `gradlew assembleDebug`로 빌드한다 (JDK 17, Android SDK 35).
2. 기기에서 실행 → 홈 화면 배너 또는 설정 화면에서 접근성의 "getFuck-ocus 잠금"을 켠다.

## 동작
- 과제는 매일 **시작 시각 + 소요 시간**으로 정한다. 카드의 재생 버튼으로 오늘 회차를 바로 시작할 수도 있다.
- 시간 안에 앱 카메라로 찍어서 인증한다. 촬영 시각, 위치(선택), EXIF, ML Kit 라벨(선택)을 확인한다.
- 마감까지 통과하지 못하면 잠금 예외 앱을 뺀 나머지 앱이 잠긴다. 늦게라도 인증하거나, 비상 해제(15분 대기), 또는 3시간이 지나면 풀린다.
- 진행 중인 회차나 잠금의 원인이 된 과제는 이름만 바꿀 수 있다. 조건을 바꿔 약속에서 빠져나가지 못하게 하기 위해서다.
- 마감 판정은 접근성 서비스(1분마다)와 마감 알람 두 곳에서 한다. 서비스가 꺼져 있으면 알림으로 알린다.
- 마감 판정과 인증 시각은 시계를 뒤로 돌려도 줄어들지 않는 시각을 쓴다 (`safety/DeviceClock.kt`).

## 구조
```
app/src/main/java/com/getfuckocus/
├─ data/     Room: 과제(TaskEntity), 인증 시도(AttemptEntity), 마이그레이션
├─ task/     회차 계산(TaskSchedule), 마감 판정(DeadlineEvaluator)
├─ verify/   CameraX 촬영 → 시각/위치/EXIF/ML Kit 라벨 판정
├─ lock/     잠금 상태, 접근성 서비스(앱 차단), 잠금 화면
├─ safety/   안전 상한, 허용 목록, 시계 조작 대응, 사진 보존 기간
└─ ui/       Compose 화면, components/(앱 전용 UI 부품), theme/
tools/make_icon.py   design/logo_source.webp에서 앱 아이콘을 만든다
design/DESIGN.md     UI 디자인 언어 기준 문서 (`npx getdesign add nintendo-2001`로 받은 "콘솔 크롬" 스타일)
```

## 브랜치
- `main`: 항상 빌드되는 상태. 직접 커밋하지 않고 PR로만 병합한다 (PR마다 GitHub Actions가 `assembleDebug`를 돌린다).
- 작업 브랜치: `feat/…`, `fix/…`, `ui/…`, `chore/…`. 주제 하나에 하나, 병합 후 삭제한다.
- Room 스키마를 바꾸는 작업은 마이그레이션까지 한 브랜치에 담는다.
- 폰에 깔 만한 시점마다 `v0.1.0` 같은 태그를 붙이고 `versionName`/`versionCode`를 올린다.

## 안전 원칙 (코드로 강제)
- 본인 기기에서 본인이 설정. 원격 제어, 아이콘 숨김, 은밀한 실행 없음. 동작 중 상시 알림.
- 긴급전화, 전화, 문자, 알람, 설정, 홈 화면, 키보드는 항상 허용.
- 비상 해제(15분 대기)와 최대 잠금 시간(3시간 후 자동 해제)이 항상 있음.
- 앱 삭제와 접근성 해제를 막지 않음.
- 접근성 서비스는 앞에 뜬 앱의 패키지명만 확인 (`canRetrieveWindowContent=false`).
- 사진과 위치는 기기에만 저장(`allowBackup=false`)하고 7일 뒤 삭제. 서버 없음.
- 돈이나 타인 승인을 해제 조건으로 쓰지 않음.
