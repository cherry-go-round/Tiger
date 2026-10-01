# TIGER MASK

***T**arget-**I**nteraction **G**rounded **E**pisode **R**ecorder*\
***M**anual **A**cquisition **S**etting **K**eeper*

수집기에 단 휴대폰으로 카메라 영상, IMU, ARCore pose를 수집해 서버로 보내는 Android **데이터 수집 앱**입니다.

<table align="center">
  <tr>
    <td align="center"><img src="docs/images/demo-collecting.gif" height="270" alt="사람이 손잡이형 수집기를 쥐고 흰 공 앞에서 작업한다. 수집기 위 거치대에 휴대폰이 달려 있다."></td>
    <td align="center"><img src="docs/images/demo-recorded.gif" height="270" alt="휴대폰이 위에서 내려다본 영상. 분홍 표시가 있는 집게 끝이 흰 공에 다가가 집었다가 내려놓는다."></td>
  </tr>
  <tr>
    <td align="center">수집기로 데이터를 수집하는 모습</td>
    <td align="center">수집기의 휴대폰이 녹화한 영상</td>
  </tr>
</table>

## 목차

- [소개](#소개)
- [용어](#용어)
- [주요 기능](#주요-기능)
- [사용 방법](#사용-방법)
- [시작하기](#시작하기)
- [수집 데이터](#수집-데이터)
- [프로젝트 구조](#프로젝트-구조)
- [기술 스택](#기술-스택)
- [관련 문서](#관련-문서)
- [라이선스](#라이선스)

## 소개

### 무엇을 하는 앱인가요

TIGER MASK는 Android 기반 **Capture Session 수집 앱**입니다. Capture Session 한 번 동안 아래 세 가지를
같은 시계에 맞춰 끊김 없이 수집합니다.

- **카메라 영상**: 뒷면 메인 카메라 영상과 프레임마다의 촬영 시각
- **움직임 센서(IMU)**: 가속도계, 자이로스코프, 회전 벡터
- **휴대폰의 위치와 방향**: ARCore가 추정한 카메라 pose

수집에는 **수집기**를 씁니다. 사람이 손에 쥐는 집게 장치이고, 위쪽 거치대에 휴대폰(Galaxy S10)을 단단히
고정해 집게 끝을 내려다보게 합니다.

수집자는 Session 안에서 실제 작업(demonstration)을 수행한 구간을 버튼으로 표시하는데, 이 구간을
Episode라고 합니다. Session을 마치면 앱이 결과를 검사해 Session bundle 하나로 묶고 서버로 보냅니다.
서버는 같은 Session으로 ARCore pose를 그대로 쓰는 방법과 영상·IMU로 후처리하는 방법을 모두 시험하고,
유효한 Episode를 학습 데이터로 씁니다.

### 이름

| 이름 | 풀이 | 뜻 |
| --- | --- | --- |
| **TIGER** | **T**arget-**I**nteraction **G**rounded **E**pisode **R**ecorder | 대상 물체를 다루는 장면을 Episode 단위로 기록한다 |
| **MASK** | **M**anual **A**cquisition **S**etting **K**eeper | 촬영 설정을 사람이 직접 정하고, 녹화 내내 그 값을 지킨다 |

MASK는 TIGER 위에 얹은 촬영 설정 기능이고, 앱 이름은 둘을 합친 TIGER MASK입니다.

### 왜 촬영 설정을 고정하나요

휴대폰 카메라는 장면 밝기에 맞춰 ISO·노출 시간·색을 매 순간 스스로 바꿉니다. 사진에는 좋지만 데이터에는
해롭습니다. 같은 기기에서 자동 설정으로 10초를 찍었더니 설정이 74번 바뀌었고, 0.5초 만에 ISO가 50에서
1218로 올랐습니다. 프레임마다 밝기와 색이 다르면 프레임끼리 비교할 수 없습니다.

초점도 문제입니다. ARCore는 초점을 1 m 거리에 고정해 두므로, 집게 끝처럼 가까운 작업 영역이 흐리게
찍힙니다.

MASK를 쓰면 수집자가 초점·ISO·셔터·화이트 밸런스를 직접 고르고, 앱은 녹화가 끝날 때까지 그 값을
유지합니다. 덕분에 카메라 보정(checkerboard calibration)과 색 기준 촬영, 실제 데이터 수집을 같은 광학
조건에서 할 수 있습니다. 수동 제어를 지원하지 않는 기기에서는 기존처럼 자동 설정으로 찍습니다.

## 용어

| 용어 | 뜻 |
| --- | --- |
| **수집기** | 데이터를 수집할 때 사람이 손에 쥐는 집게 장치. 위쪽 거치대에 휴대폰을 고정한다. |
| **Session** | 녹화 한 번. 시작부터 종료까지 영상·센서·pose를 끊김 없이 기록한다. |
| **Episode** | Session 안에서 실제 작업(demonstration)을 수행한 구간. 한 Session에 여러 개를 둘 수 있다. 화면에서는 "작업 구간"이라고 부른다. |
| **Task / Object** | 무슨 작업을 하는지(예: 컵 집기)와 다루는 물체(예: 머그컵). Session을 시작하기 전에 입력하며, 그 Session의 모든 Episode에 붙는다. |
| **Tracking** | ARCore가 휴대폰의 위치를 놓치지 않고 따라가는 상태. Tracking이 안정돼야 Episode를 시작할 수 있다. |
| **번들(bundle)** | Session 하나의 결과 파일 묶음. 녹화를 마칠 때 검사를 거쳐 만들어지고, 그 뒤로는 수정되지 않는다. |

## 주요 기능

- **동시 기록**: 카메라 영상, IMU 세 종류, ARCore pose를 한 Session으로 함께 기록합니다.
- **구간 표시**: 버튼으로 Episode의 시작과 끝을 표시합니다. Episode 도중 Tracking이 0.5초 넘게 끊기면 그
  Episode를 무효로 처리하고 자동으로 끝냅니다. Session은 계속됩니다.
- **촬영 설정 고정 (MASK)**: 녹화 해상도(1920×1080 / 1280×720), 초점, ISO, 셔터, 화이트 밸런스를 고르고
  녹화 내내 유지합니다. 프레임 속도는 30 fps로 고정합니다. 고른 값은 다음 실행에도 기억합니다.
- **결과 검사**: 녹화를 마칠 때 파일이 빠짐없이 있는지, CSV 머리글이 맞는지, SHA-256 해시가 일치하는지
  검사한 뒤에만 번들을 확정합니다.
- **중단 복구**: 녹화 도중 앱이 꺼져도, 다음 실행 때 남은 녹화를 검사해 정상 Session으로 마감합니다.
- **자동 업로드**: Session을 마치면 곧바로 서버로 올리고, 실패하면 다시 시도할 수 있습니다.
- **다시 보기와 정리**: 찍은 영상을 앱에서 재생하고, 필요 없는 Session을 기기에서 지웁니다.

## 사용 방법

수집 화면은 가로, 목록과 상세 화면은 세로로 고정됩니다. 아래 화면은 에뮬레이터에서 예시 데이터로 찍었습니다.

<p align="center">
  <img src="docs/images/home.png" width="240" alt="홈 화면. Task별로 Session 수가 적힌 카드가 있고 오른쪽 아래에 새 세션 단추가 있다.">
  <img src="docs/images/task-sessions.png" width="240" alt="Task 화면. 한 Task의 Session 카드가 업로드 상태와 함께 나열된다.">
  <img src="docs/images/session-detail.png" width="240" alt="Session 상세 화면. 영상, Task·Object·ID, 업로드 단추가 있다.">
</p>

### 1. 새 세션 열기

홈은 지금까지 찍은 Session을 Task별로 묶어 보여 줍니다. 오른쪽 아래 **새 세션**을 누르면 수집 화면이
열리고, 먼저 Task와 Object를 묻습니다. 입력하고 **준비 완료**를 누릅니다. Task 화면에서 새 세션을 열면
그 Task 이름이 미리 채워져 있습니다.

### 2. 카메라 설정 (선택)

<p align="center">
  <img src="docs/images/camera-settings.jpg" width="720" alt="카메라 설정 시트. 녹화 해상도, 초점, ISO, 셔터, 화이트 밸런스 항목이 있다.">
</p>

왼쪽 위 톱니바퀴를 누르면 카메라 설정이 열립니다. 녹화 해상도, 초점, ISO, 셔터, 화이트 밸런스를 고를 수
있고, 바꾸는 즉시 프리뷰에 반영되므로 화면을 보며 초점을 맞추면 됩니다. Session을 시작하면 톱니바퀴가
사라지고 설정은 잠깁니다.

### 3. 녹화하기

<p align="center">
  <img src="docs/images/capture-workspace.jpg" width="720" alt="녹화 중인 수집 화면. 카메라 프리뷰가 화면을 채우고, 왼쪽 위에 EPISODE ACTIVE 배지, 오른쪽에 닫기·일시 정지·정지 단추가 있다.">
</p>

오른쪽 단추로 조작하고, 왼쪽 위 배지가 지금 상태를 알려 줍니다.

| 누르는 단추 | 바뀌는 상태 | 뜻 |
| --- | --- | --- |
| ▶ 세션 시작 | `INITIALIZING` | 녹화가 시작되고 ARCore가 Tracking을 준비합니다. |
| (기다림) | `READY` | Tracking이 안정됐습니다. 이제 Episode를 시작할 수 있습니다. |
| ▶ 작업 구간 시작 | `EPISODE ACTIVE` | 작업 구간(Episode)을 기록합니다. |
| ⏸ 작업 구간 일시 정지 | `READY` | 작업 구간이 끝납니다. 수집은 계속되므로 자세를 고친 뒤 다음 구간을 시작하면 됩니다. |
| ■ 세션 종료 | `FINALIZING` | 확인을 거쳐 녹화를 마치고 결과를 검사합니다. 진행 중인 작업 구간이 있으면 먼저 끝내야 합니다. |

결과를 검사하는 동안에는 앱을 닫지 마세요.

### 4. 확인하고 올리기

녹화가 마감되면 그 Session의 상세 화면으로 이동하고 업로드가 바로 시작됩니다. 상세 화면에서 영상을
다시 보고 업로드 상태(로컬에만 저장됨 · 업로드 중 · 업로드 완료 · 업로드 실패)를 확인할 수 있습니다.
실패하면 사유와 함께 **업로드 재시도**가 나타납니다.

- 업로드는 앱이 화면에 떠 있는 동안에만 진행됩니다. 도중에 앱을 백그라운드로 보내면 실패로 남으니 상세
  화면에서 다시 시도하세요.
- Session은 Task 화면에서 카드를 길게 누르거나 상세 화면의 메뉴(⋮)에서 지웁니다. **기기에서만** 지워지며
  서버에 올라간 데이터는 남습니다. 업로드 중에는 지울 수 없습니다.

### 촬영 팁

- 어두우면 ARCore가 Tracking을 잡지 못해 `READY`로 넘어가지 않습니다. 조명을 밝게 하세요.
- 수동 노출은 밝기를 잠급니다. 자동이었다면 ISO를 올려 버텼을 장면에서도 Tracking이 끊길 수 있으니, 그럴
  때는 ISO를 올리거나 셔터를 1/60 · 1/30으로 늦춥니다. 1/30까지는 30 fps를 유지합니다.

## 시작하기

### 요구 환경

**실행**

- ARCore를 지원하는 Android 9(API 28) 이상 기기. 개발에는 Galaxy S10(SM-G973N)을 썼습니다.
- 최신 Google Play Services for AR

**빌드**

- JDK 17 이상 (Gradle 9.6이 요구합니다)
- Android SDK Platform 37
- Android Studio를 권장합니다. 저장소에 Gradle Wrapper가 들어 있어 Gradle을 따로 설치하지 않아도 됩니다.

### 업로드 서버 주소 설정

저장소 루트의 `local.properties`에 서버 주소를 적습니다. 이 파일은 git에 올라가지 않습니다.

```properties
tigerUploadBaseUrl=http://<서버 주소>
```

빌드할 때 `-PtigerUploadBaseUrl=<서버 주소>`로 넘겨도 됩니다. 주소 없이 빌드해도 녹화는 되지만 업로드는
할 수 없습니다.

### 빌드와 설치

Android Studio에서 `app` 구성을 실행하거나, 명령줄에서 빌드한 뒤 기기에 설치합니다.

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

macOS·Linux에서는 `./gradlew assembleDebug`를 씁니다. 설치되는 앱의 패키지 이름은
`com.ssafy.s15p21a206.tigermask`입니다.

## 수집 데이터

### 번들 구성

Session 하나가 아래 파일들로 이루어진 번들 하나가 됩니다.

| 파일 | 내용 |
| --- | --- |
| `main_rgb.mp4` | 메인 카메라 영상 |
| `main_frame_timestamps.csv` | 영상 프레임 번호와 그 프레임의 촬영 시각 |
| `accelerometer.csv` | 가속도계 값 |
| `gyroscope.csv` | 자이로스코프 값 |
| `rotation_vector.csv` | 회전 벡터 값 |
| `arcore_poses.csv` | ARCore가 추정한 카메라 위치·방향과 Tracking 상태 |
| `episodes.csv` | Episode마다의 시작·종료 시각과 결과(정상 완료 또는 Tracking 끊김으로 무효) |
| `metadata.json` | Session ID, 카메라 정보(해상도·내부 파라미터), 촬영 설정(요청값과 실제 적용값), 파일 목록과 SHA-256 |

`metadata.json`의 형식은 [Session `metadata.json` 계약](specs/003-session-episode-tracking-gate/contracts/session-metadata.md)에
있습니다.

### 저장과 전송

```text
카메라 + ARCore + IMU
        │  녹화
        ▼
staging/<session_id>/      녹화 중인 파일
        │  마감: 파일 검사
        ▼
completed/<session_id>/    확정된 번들 (이후 수정하지 않음)
        │  업로드: 마감 직후 자동, 실패하면 상세 화면에서 재시도
        ▼
POST {BASE_URL}/sessions
```

번들은 앱 전용 내부 저장소에 있어 다른 앱이나 파일 탐색기에서는 보이지 않습니다. 기기 밖으로 꺼내는
방법은 업로드뿐입니다. 업로드는 번들을 읽기만 하므로 실패하거나 중단돼도 원본은 그대로 남습니다.

앱은 번들을 multipart 요청 하나로 보내고, 같은 Session을 다시 보내도 서버가 중복을 가려내도록
`Idempotency-Key`에 Session ID를 담습니다. 요청과 응답 형식은
[Capture Session Upload 계약](specs/001-episode-recorder/contracts/episode-upload.md)에 있습니다.

## 프로젝트 구조

모듈은 `app` 하나입니다.

```text
app/src/main/java/com/ssafy/s15p21a206/tiger/
├── core/
│   ├── capture/        카메라·ARCore·IMU 기록 (MASK 수동 설정은 capture/manual)
│   ├── session/        번들 마감·검사·복구
│   ├── upload/         서버 전송
│   ├── database/       Room. Session과 Episode 기록
│   ├── model/          데이터 모델
│   └── designsystem/   테마와 공용 컴포넌트
└── feature/
    ├── capture/        수집 화면
    └── session/        홈, Task 화면, Session 상세, 영상 재생
specs/                  기능별 명세·계획·계약 문서
```

## 기술 스택

- Kotlin, Jetpack Compose, Material 3, Navigation Compose
- Android Camera2, ARCore Shared Camera
- Room, Kotlin Coroutines, kotlinx.serialization
- OkHttp, Media3 ExoPlayer
- 글꼴: Pretendard

## 관련 문서

- 기능 명세와 설계: [`specs/001-episode-recorder/`](specs/001-episode-recorder/),
  [`specs/002-capture-control-ux/`](specs/002-capture-control-ux/),
  [`specs/003-session-episode-tracking-gate/`](specs/003-session-episode-tracking-gate/)
- 개발에 참여한다면: [`AGENTS.md`](AGENTS.md)에 빌드·테스트·커밋 규칙이, [`app/AGENTS.md`](app/AGENTS.md)에
  코드·화면 작성 규칙이 있습니다. 처음 clone한 뒤 `.\gradlew.bat installGitHooks`로 git hook을 켜 주세요.

## 라이선스

아직 라이선스를 정하지 않았습니다.
