package com.ssafy.s15p21a206.tiger.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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

/**
 * 수집 작업 공간. 조회 흐름 위에 모달로 얹힌다.
 *
 * 목적지가 아닌 이유는 진입 경로가 하나뿐이고, 뒤로 가기가 "이전 화면으로"가 아니라 "종료할까요?"이며,
 * 안에 또 모달을 품기 때문이다. NavHost 바깥에 있어 수집 상태가 백스택 조작과 무관하게 남는다.
 *
 * **여기는 그리기만 한다.** 카메라 세션·ARCore·마감은 [rememberCaptureDriver]가 맡고, 어디로 갈지는
 * [onCompleted]로 부모가 정한다. 이 화면은 목적지도 카메라도 모른다.
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
            val closePanel = { onIntent(CaptureIntent.ToggleManualCameraPanel(false)) }
            CaptureCameraSheet(onDismiss = closePanel) {
                CaptureCameraPanel(
                    state = state.manualCamera,
                    resolution = state.resolution,
                    enabled = state.captureSettingsEditable,
                    onChange = driver.manualCamera.edit,
                    onResolutionChange = driver.selectResolution,
                    onFixWhiteBalance = driver.manualCamera.fixWhiteBalance,
                    onClearWhiteBalance = driver.manualCamera.releaseWhiteBalance,
                    onClose = closePanel,
                )
            }
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
 * 프리뷰 위 상단 줄. 왼쪽 자리와 닫기 버튼을 한 Row에 담는다.
 *
 * 서로 다른 align으로 두면 배지가 길어질 때 닫기 버튼 아래로 파고든다. 그래서 왼쪽 자리는
 * `weight(fill = false)`로 제 너비만 쓴다. 기준은 프리뷰가 아니라 화면이다. 우측 제어와 좌표계를
 * 맞추고, 레터박스가 생기는 기기에서는 검은 띠 위에 얹혀 영상을 가리지 않는다.
 *
 * 왼쪽 자리는 Session 전에는 촬영 조건을 여는 톱니바퀴, 시작한 뒤에는 tracking 상태 배지가 쓴다.
 * 시작하면 조건이 잠기고, 그 전에는 상태가 늘 IDLE이라 배지가 알려 줄 것이 없다.
 */
@Suppress("FunctionName")
@Composable
private fun CaptureTopBar(
    state: CaptureUiState,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(top = 20.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.phase == CaptureWorkspaceControlState.Idle) {
                CaptureCameraSettingsButton(enabled = state.captureSettingsEditable, onClick = onOpenSettings)
            } else {
                CaptureWorkspaceStatus(state = state.phase)
            }
        }
        CaptureWorkspaceExitControls(policy = state.policy, onExit = onExit)
    }
}

/**
 * 프리뷰와 영상의 가로세로 비. 두 녹화 해상도 모두 16:9라 선택과 무관하게 같다.
 *
 * 늘이거나 잘라내지 않고 이 비로 맞춰야 저장되는 영상과 화각이 같다. 화면이 세로면 위아래에 검은 영역이 남는다.
 */
private const val PREVIEW_ASPECT_RATIO = 16f / 9f
