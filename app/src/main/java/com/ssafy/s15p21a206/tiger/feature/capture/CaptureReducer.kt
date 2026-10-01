package com.ssafy.s15p21a206.tiger.feature.capture

import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraCapabilities
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraConfig
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution
import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle

/**
 * 상태 전이.
 *
 * 받을 수 없는 상태에서 온 intent는 상태를 그대로 돌려준다. 예를 들어 [CaptureIntent.EpisodeStarted]는
 * tracking이 안정화된 뒤에만 뜻이 있으므로 [CaptureWorkspaceControlState.Ready]에서만 받는다.
 * 무시하는 쪽이 맞는 것은, 이 값들이 사용자 조작이 아니라 비동기 경로에서도 밀려오기 때문이다.
 */
internal fun CaptureUiState.reduce(intent: CaptureIntent): CaptureUiState =
    when (intent) {
        is CaptureIntent.Open -> openedWorkspace(intent.task, intent.resolution)
        CaptureIntent.Close -> CaptureUiState()
        is CaptureIntent.EditTask -> copy(task = intent.value)
        is CaptureIntent.EditObjectName -> copy(objectName = intent.value)
        is CaptureIntent.SelectResolution -> withResolution(intent.value)
        is CaptureIntent.ManualCameraProfiled -> withManualCameraProfile(intent.capabilities, intent.config)
        is CaptureIntent.EditManualCamera -> withManualCameraConfig(intent.value)
        is CaptureIntent.ToggleManualCameraPanel -> withCameraPanelOpen(intent.open)
        CaptureIntent.ConfirmMetadata -> copy(showMetadataDialog = false)
        is CaptureIntent.IdlePreviewResized -> copy(idlePreviewSize = intent.value)
        CaptureIntent.PreviewFrameArrived -> if (previewFailed) this else copy(previewReady = true)
        CaptureIntent.PreviewReleased -> copy(previewReady = false)
        is CaptureIntent.PreviewFailed -> copy(previewReady = false, previewFailed = true, notice = intent.notice)
        CaptureIntent.SessionRequested -> copy(busy = true, previewReady = false)
        is CaptureIntent.SessionStarted -> withSessionStarted(intent.bundle, intent.startedAtNs, intent.resolution)
        is CaptureIntent.TrackingSampled -> withTrackingSample(intent.ready, intent.episodeActive)
        CaptureIntent.EpisodeStarted ->
            if (phase == CaptureWorkspaceControlState.Ready) copy(phase = CaptureWorkspaceControlState.EpisodeActive) else this
        CaptureIntent.EpisodeEnded ->
            if (phase == CaptureWorkspaceControlState.EpisodeActive) copy(phase = CaptureWorkspaceControlState.Ready) else this
        CaptureIntent.StopRequested -> copy(showStopConfirmation = true)
        CaptureIntent.StopDismissed -> copy(showStopConfirmation = false)
        CaptureIntent.FinalizeStarted -> copy(phase = CaptureWorkspaceControlState.Finalizing, showStopConfirmation = false)
        is CaptureIntent.FinalizeFailed -> copy(finalizeFailure = intent.notice)
        CaptureIntent.FinalizeFailureDismissed -> withoutSession().copy(finalizeFailure = null)
        CaptureIntent.Finalized -> withoutSession().copy(busy = false)
        CaptureIntent.Interrupted -> withoutSession().copy(busy = false)
        CaptureIntent.BusyReleased -> copy(busy = false)
        is CaptureIntent.Notify -> copy(notice = intent.notice)
        CaptureIntent.NoticeShown -> copy(notice = "")
    }

/** 새로 연 작업 공간. 직전 Session이 남긴 값을 물려받지 않도록 처음부터 만든다. */
private fun openedWorkspace(
    task: String,
    resolution: RecordingResolution,
): CaptureUiState =
    CaptureUiState(
        open = true,
        task = task,
        resolution = resolution,
        idlePreviewSize = resolution,
        showMetadataDialog = true,
    )

/**
 * 녹화 해상도를 고른다. 초점·ISO와 같은 촬영 조건이라 Session이 시작되면 바뀌지 않는다.
 *
 * Session 시작 직전에 ARCore가 실제로 고른 값으로 맞추는 갱신도 이 전이로 오지만, 그때는 아직 Idle이라
 * 막히지 않는다.
 */
private fun CaptureUiState.withResolution(value: RecordingResolution): CaptureUiState =
    if (phase == CaptureWorkspaceControlState.Idle) copy(resolution = value) else this

private fun CaptureUiState.withManualCameraProfile(
    capabilities: ManualCameraCapabilities,
    config: ManualCameraConfig,
): CaptureUiState = copy(manualCamera = manualCamera.copy(capabilities = capabilities, config = capabilities.coerce(config)))

/**
 * 수동 설정 값을 바꾼다. Session이 시작된 뒤에는 받지 않는다.
 *
 * 화면에서도 막지만 전이로 한 번 더 막는다. 이 값이 촬영 도중 흔들리지 않는 것이 이 기능의 전부다.
 */
private fun CaptureUiState.withManualCameraConfig(value: ManualCameraConfig): CaptureUiState {
    val capabilities = manualCamera.capabilities
    if (phase != CaptureWorkspaceControlState.Idle || capabilities == null) return this
    return copy(manualCamera = manualCamera.copy(config = capabilities.coerce(value)))
}

/**
 * 카메라 설정 시트를 열거나 닫는다. 촬영 중에는 열지 않고, 닫는 요청은 언제나 받는다.
 *
 * 시트는 모달이라 열려 있는 동안 정지를 누를 수 없고, 촬영 중에는 값도 잠겨 있어 열어 볼 일이 없다.
 */
private fun CaptureUiState.withCameraPanelOpen(open: Boolean): CaptureUiState =
    if (open && phase != CaptureWorkspaceControlState.Idle) this else copy(manualCamera = manualCamera.copy(panelOpen = open))

/**
 * Session이 시작됐다. [resolution]은 ARCore가 실제로 고른 크기로, 후보 config가 없어 기본값으로
 * 물러났으면 고른 값과 다르다. 촬영 조건은 이제 잠기므로 열린 설정 시트를 접는다.
 */
private fun CaptureUiState.withSessionStarted(
    bundle: SessionBundle,
    startedAtNs: Long,
    resolution: RecordingResolution,
): CaptureUiState =
    copy(
        phase = CaptureWorkspaceControlState.Initializing,
        resolution = resolution,
        idlePreviewSize = resolution,
        activeBundle = bundle,
        recordingStartNs = startedAtNs,
        busy = false,
        manualCamera = manualCamera.copy(panelOpen = false),
    )

/** tracking 표본으로 단계를 맞춘다. Session이 돌고 있을 때만 받는다. Idle에는 아직 Session이 없고 Finalizing은 닫는 중이다. */
private fun CaptureUiState.withTrackingSample(
    ready: Boolean,
    episodeActive: Boolean,
): CaptureUiState {
    if (phase == CaptureWorkspaceControlState.Idle || phase == CaptureWorkspaceControlState.Finalizing) return this
    val sampled =
        when {
            episodeActive -> CaptureWorkspaceControlState.EpisodeActive
            ready -> CaptureWorkspaceControlState.Ready
            else -> CaptureWorkspaceControlState.Initializing
        }
    return copy(phase = sampled)
}

/**
 * 진행 중이던 Session을 놓고 Idle로 돌아간다. 마감·중단·마감 실패 확인이 같은 자리로 돌아온다.
 * 마감 실패는 확인을 받은 뒤에야 걷히고, Session이 저장되지 않았으므로 역시 여기로 온다.
 *
 * Task·Object·해상도·촬영 조건은 남긴다. 같은 작업 공간에서 이어 찍을 때 다시 입력하지 않는다.
 */
private fun CaptureUiState.withoutSession(): CaptureUiState =
    copy(phase = CaptureWorkspaceControlState.Idle, activeBundle = null, recordingStartNs = 0L)
