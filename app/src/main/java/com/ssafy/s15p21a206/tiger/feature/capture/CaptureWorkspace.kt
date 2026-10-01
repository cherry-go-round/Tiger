package com.ssafy.s15p21a206.tiger.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.core.android.LockLandscapeWhileVisible
import com.ssafy.s15p21a206.tiger.core.capture.camera.RecordingResolutionStore
import com.ssafy.s15p21a206.tiger.core.session.SessionRepository
import com.ssafy.s15p21a206.tiger.feature.capture.preview.CapturePreviewSurface
import com.ssafy.s15p21a206.tiger.feature.capture.settings.CaptureCameraPanel
import com.ssafy.s15p21a206.tiger.feature.capture.settings.CaptureCameraSheet
import com.ssafy.s15p21a206.tiger.feature.capture.settings.CaptureSettingsControls

/**
 * 수집 작업 공간. 조회 흐름 위에 모달로 얹힌다.
 *
 * 목적지가 아닌 이유는 진입 경로가 하나뿐이고, 뒤로 가기가 "이전 화면으로"가 아니라 "종료할까요?"이며,
 * 안에 또 모달을 품기 때문이다. NavHost 바깥에 있어 수집 상태가 백스택 조작과 무관하게 남는다.
 *
 * **여기는 수명만 맡는다.** 카메라 세션·ARCore·마감은 [rememberCaptureDriver]가 맡고, 그리는 것은
 * [CaptureWorkspaceContent]가 맡는다. 어디로 갈지는 [onCompleted]로 부모가 정한다.
 *
 * [state]는 부모가 소유한다. 부모가 [CaptureIntent.Open]으로 작업 공간을 열기 때문이다.
 */
@Suppress("FunctionName")
@Composable
internal fun CaptureWorkspace(
    state: CaptureUiState,
    onIntent: (CaptureIntent) -> Unit,
    repository: SessionRepository,
    resolutionStore: RecordingResolutionStore,
    /** 마감으로 세션이 저장됐다. `startUpload`는 앱이 전면에 있어 전송을 걸어도 되는지다. */
    onCompleted: (sessionId: String, startUpload: Boolean) -> Unit,
) {
    val driver =
        rememberCaptureDriver(
            state = state,
            onIntent = onIntent,
            repository = repository,
            resolutionStore = resolutionStore,
            onCompleted = onCompleted,
        )

    if (!state.open) return
    DisposableEffect(Unit) {
        onDispose(driver.releaseIdlePreview)
    }
    // 카메라 센서가 90도 눕혀 장착돼 있어 세로 화면에서는 프리뷰가 옆으로 누운 채 비율까지 어긋난다.
    LockLandscapeWhileVisible()
    CaptureWorkspaceContent(state, onIntent, driver)
}

/**
 * 작업 공간이 그리는 것. 상태와 조작([driver])만 받고 카메라도 저장소도 모르므로 기기 없이 그릴 수 있다.
 *
 * 프리뷰 위에 상단 줄, 설정 시트, 알림, 우측 제어를 얹고, 그 위를 덮는 판(수집 정보 입력, 마감, 정지
 * 확인)을 띄운다.
 */
@Suppress("FunctionName")
@Composable
internal fun CaptureWorkspaceContent(
    state: CaptureUiState,
    onIntent: (CaptureIntent) -> Unit,
    driver: CaptureDriver,
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CapturePreviewSurface(
            bufferSize = state.idlePreviewSize,
            applyTransform = state.phase == CaptureWorkspaceControlState.Idle,
            onSurfaceAvailable = driver.onSurfaceAvailable,
            onSurfaceDestroyed = driver.onSurfaceDestroyed,
            onFrame = { onIntent(CaptureIntent.PreviewFrameArrived) },
            modifier = Modifier.align(Alignment.Center).aspectRatio(PREVIEW_ASPECT_RATIO),
        )
        if (state.chromeVisible) {
            // 거치 기준선을 판 위의 것들보다 먼저 그려 배지나 스낵바를 가르지 않게 한다.
            CaptureCenterGuide()
            CaptureTopBar(
                state = state,
                onOpenSettings = { onIntent(CaptureIntent.ToggleManualCameraPanel(true)) },
                onExit = driver.requestExit,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        if (state.manualCamera.panelOpen && state.chromeVisible) {
            CaptureSettingsSheet(
                state = state,
                controls = driver.captureSettings,
                onClose = { onIntent(CaptureIntent.ToggleManualCameraPanel(false)) },
            )
        }
        SnackbarHost(driver.snackbarHostState, Modifier.align(Alignment.TopCenter).padding(top = 80.dp))
        if (!state.showMetadataDialog && state.chromeVisible) {
            CaptureWorkspaceControls(
                policy = state.policy,
                onPlay = driver.play,
                onPause = driver.pause,
                onStop = driver.requestExit,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
    CaptureWorkspaceModals(state, onIntent, driver)
}

@Suppress("FunctionName")
@Composable
private fun CaptureSettingsSheet(
    state: CaptureUiState,
    controls: CaptureSettingsControls,
    onClose: () -> Unit,
) {
    CaptureCameraSheet(onDismiss = onClose) {
        CaptureCameraPanel(
            state = state.manualCamera,
            resolution = state.resolution,
            enabled = state.captureSettingsEditable,
            onChange = controls.editManualCamera,
            onResolutionChange = controls.selectResolution,
            onFixWhiteBalance = controls.fixWhiteBalance,
            onClearWhiteBalance = controls.releaseWhiteBalance,
            onClose = onClose,
        )
    }
}

/** 작업 공간을 덮는 판. 수집 정보 입력, 마감 중·실패, 정지 확인이다. */
@Suppress("FunctionName")
@Composable
private fun CaptureWorkspaceModals(
    state: CaptureUiState,
    onIntent: (CaptureIntent) -> Unit,
    driver: CaptureDriver,
) {
    if (state.showMetadataDialog) {
        CaptureMetadataDialog(
            task = state.task,
            objectName = state.objectName,
            onTaskChange = { onIntent(CaptureIntent.EditTask(it)) },
            onObjectNameChange = { onIntent(CaptureIntent.EditObjectName(it)) },
            onConfirm = driver.confirmMetadata,
            onCancel = { onIntent(CaptureIntent.Close) },
        )
    }
    if (!state.chromeVisible) {
        CaptureFinalizingOverlay(
            failure = state.finalizeFailure,
            onDismissFailure = { onIntent(CaptureIntent.FinalizeFailureDismissed) },
        )
    }
    if (state.showStopConfirmation) {
        CaptureStopConfirmation(
            onConfirm = driver.confirmStop,
            onDismiss = { onIntent(CaptureIntent.StopDismissed) },
        )
    }
}

/**
 * 프리뷰와 영상의 가로세로 비. 두 녹화 해상도 모두 16:9라 선택과 무관하게 같다.
 *
 * 늘이거나 잘라내지 않고 이 비로 맞춰야 저장되는 영상과 화각이 같다. 화면이 세로면 위아래에 검은 영역이 남는다.
 */
private const val PREVIEW_ASPECT_RATIO = 16f / 9f
