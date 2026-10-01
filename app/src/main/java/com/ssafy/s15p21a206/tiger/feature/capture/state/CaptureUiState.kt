package com.ssafy.s15p21a206.tiger.feature.capture.state

import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraCapabilities
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraConfig
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraUnsupportedReason
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingFormat
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution
import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import com.ssafy.s15p21a206.tiger.feature.capture.preview.IdlePreview

/**
 * 수동 촬영 설정 패널의 상태.
 *
 * [config] 하나가 프리뷰와 녹화가 함께 쓰는 값이다. 화면용 값과 카메라용 값을 따로 들지 않는 것이
 * 이 기능의 요점이므로, 여기에도 사본을 만들지 않는다.
 */
internal data class ManualCameraUiState(
    /** 기기가 무엇을 지원하는지. 아직 읽기 전이면 null이다. */
    val capabilities: ManualCameraCapabilities? = null,
    val config: ManualCameraConfig? = null,
    val panelOpen: Boolean = false,
) {
    /** 수동 설정을 쓸 수 없는 이유. 패널은 조작 대신 이 이유의 문구를 보여 준다. */
    val unsupportedReason: ManualCameraUnsupportedReason? get() = capabilities?.unsupportedReason

    val supported: Boolean get() = capabilities != null && unsupportedReason == null

    /**
     * 녹화에 걸 값.
     *
     * 지원하지 않는 기기에서는 null이라 수집이 기존 자동 동작 그대로 돈다. 수동 설정 하나 때문에
     * 수집 자체를 막지 않는다.
     */
    val appliedConfig: ManualCameraConfig? get() = config.takeIf { supported }

    val whiteBalanceFixed: Boolean get() = config?.awbFixed == true
}

/**
 * 수집 작업 공간의 상태 전부.
 *
 * 수집 단계는 [phase] 하나가 든다. 불리언 여럿으로 나누면 합법이 아닌 조합이 타입으로 막히지 않는다.
 *
 * 프리뷰 Surface와 SurfaceTexture에 해당하는 값은 여기 없다. 수명이 `TextureView`에 묶여 있어
 * 상태로 올리면 backing view가 사라진 뒤의 null·release를 직접 관리해야 한다. [IdlePreview]가
 * 들고, 화면은 Surface가 생기고 사라졌다는 사실만 알린다.
 */
internal data class CaptureUiState(
    /** 작업 공간을 띄우고 있는지. `false`면 나머지 값은 다음 [CaptureIntent.Open]이 새로 채운다. */
    val open: Boolean = false,
    val phase: CaptureWorkspaceControlState = CaptureWorkspaceControlState.Idle,
    val task: String = "",
    val objectName: String = "",
    /** 이번 Session으로 녹화할 크기. 수집이 시작되면 ARCore가 실제로 고른 값으로 갱신된다. */
    val resolution: RecordingResolution = RecordingFormat.DEFAULT_RESOLUTION,
    /** 유휴 프리뷰 Camera2 session이 열려 있는 크기. [resolution]과 어긋나면 session을 다시 연다. */
    val idlePreviewSize: RecordingResolution = RecordingFormat.DEFAULT_RESOLUTION,
    val previewReady: Boolean = false,
    val previewFailed: Boolean = false,
    /** 비동기 작업이 도는 동안 제어를 잠근다. */
    val busy: Boolean = false,
    val showMetadataDialog: Boolean = false,
    val showStopConfirmation: Boolean = false,
    /** 마감 실패는 지나가는 알림이 아니라 확인을 받아야 걷히는 판이다. */
    val finalizeFailure: String? = null,
    /** Snackbar로 한 번 보여 주고 비우는 문구. */
    val notice: String = "",
    val activeBundle: SessionBundle? = null,
    val recordingStartNs: Long = 0L,
    /** 이번 Session에 쓸 촬영 조건. Session이 시작되면 잠긴다. */
    val manualCamera: ManualCameraUiState = ManualCameraUiState(),
) {
    /** 수집을 시작하거나 Episode를 시작할 수 있는 상태인지. */
    val ready: Boolean
        get() = previewReady && !previewFailed && task.isNotBlank() && objectName.isNotBlank() && !showMetadataDialog

    val policy: CaptureControlPolicy
        get() = CaptureControlPolicy(state = phase, ready = ready, busy = busy)

    /** 촬영 조건(해상도·수동 설정)을 바꿀 수 있는지. Session이 시작되면 잠기고, Session 요청이 걸린 사이에도 받지 않는다. */
    val captureSettingsEditable: Boolean
        get() = phase == CaptureWorkspaceControlState.Idle && !busy

    /** 마감 중이거나 마감이 실패한 동안은 판이 덮으므로 작업 공간의 것들을 걷는다. */
    val chromeVisible: Boolean
        get() = phase != CaptureWorkspaceControlState.Finalizing && finalizeFailure == null
}
