package com.ssafy.s15p21a206.tiger.feature.capture.state

import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraCapabilities
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraConfig
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution
import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle

/**
 * 상태를 바꾸는 유일한 입구.
 *
 * 수집 상태는 tracking 폴링·권한 콜백·lifecycle 이벤트·ARCore 실패에서 동시에 밀려온다. 각자
 * 상태를 직접 쓰면 순서를 주석으로만 지키게 되므로 여기로 모은다.
 */
internal sealed interface CaptureIntent {
    /** 수집 정보 입력부터 시작한다. [task]는 Task 목록에서 들어왔을 때 미리 채워진 이름이다. */
    data class Open(
        val task: String,
        val resolution: RecordingResolution,
    ) : CaptureIntent

    data object Close : CaptureIntent

    data class EditTask(
        val value: String,
    ) : CaptureIntent

    data class EditObjectName(
        val value: String,
    ) : CaptureIntent

    data class SelectResolution(
        val value: RecordingResolution,
    ) : CaptureIntent

    /** 녹화 카메라의 능력과 불러온 설정을 받았다. 수집 화면을 열 때 한 번 온다. */
    data class ManualCameraProfiled(
        val capabilities: ManualCameraCapabilities,
        val config: ManualCameraConfig,
    ) : CaptureIntent

    /** 사용자가 초점·ISO·셔터·WB를 건드렸다. */
    data class EditManualCamera(
        val value: ManualCameraConfig,
    ) : CaptureIntent

    data class ToggleManualCameraPanel(
        val open: Boolean,
    ) : CaptureIntent

    data object ConfirmMetadata : CaptureIntent

    /** 유휴 프리뷰 Camera2 session을 고른 크기로 다시 연 뒤. */
    data class IdlePreviewResized(
        val value: RecordingResolution,
    ) : CaptureIntent

    data object PreviewFrameArrived : CaptureIntent

    data object PreviewReleased : CaptureIntent

    data class PreviewFailed(
        val notice: String,
    ) : CaptureIntent

    /** 권한 요청과 ARCore 시작을 걸기 직전. */
    data object SessionRequested : CaptureIntent

    data class SessionStarted(
        val bundle: SessionBundle,
        val startedAtNs: Long,
        val resolution: RecordingResolution,
    ) : CaptureIntent

    /** tracking 표본 하나. 값이 변하지 않아도 계속 들어와야 안정화와 유실 마감이 발화한다. */
    data class TrackingSampled(
        val ready: Boolean,
        val episodeActive: Boolean,
    ) : CaptureIntent

    data object EpisodeStarted : CaptureIntent

    data object EpisodeEnded : CaptureIntent

    data object StopRequested : CaptureIntent

    data object StopDismissed : CaptureIntent

    data object FinalizeStarted : CaptureIntent

    data class FinalizeFailed(
        val notice: String,
    ) : CaptureIntent

    data object FinalizeFailureDismissed : CaptureIntent

    data object Finalized : CaptureIntent

    /** `ON_STOP`에서 진행 중인 Session을 `INTERRUPTED`로 마감한 뒤. */
    data object Interrupted : CaptureIntent

    data object BusyReleased : CaptureIntent

    data class Notify(
        val notice: String,
    ) : CaptureIntent

    data object NoticeShown : CaptureIntent
}
