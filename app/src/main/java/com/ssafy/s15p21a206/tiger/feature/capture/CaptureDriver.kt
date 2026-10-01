package com.ssafy.s15p21a206.tiger.feature.capture

import android.Manifest
import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ssafy.s15p21a206.tiger.core.capture.camera.RecordingResolutionStore
import com.ssafy.s15p21a206.tiger.core.session.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * 수집 작업 공간이 부르는 조작 묶음. 화면은 무엇을 그릴지만 알고 조작은 여기로 넘긴다.
 *
 * 실제 일은 [IdlePreview], [CaptureSettingsControls], [CaptureSessionActions]가 한다. [rememberCaptureDriver]가
 * 그 셋을 이어 만든다.
 */
internal class CaptureDriver(
    /** 작업 공간의 알림을 띄우는 자리. 문구는 상태의 `notice`에서 온다. */
    val snackbarHostState: SnackbarHostState,
    val onSurfaceAvailable: (Surface, SurfaceTexture) -> Unit,
    val onSurfaceDestroyed: () -> Unit,
    val play: () -> Unit,
    val pause: () -> Unit,
    val requestExit: () -> Unit,
    val confirmStop: () -> Unit,
    val confirmMetadata: () -> Unit,
    /** 작업 공간을 벗어날 때 유휴 프리뷰 Camera2 session을 놓는다. */
    val releaseIdlePreview: () -> Unit,
    val captureSettings: CaptureSettingsControls,
)

/**
 * 수집 파이프라인을 세우고 그 조작을 [CaptureDriver]로 돌려준다.
 *
 * 여기에는 composition에 묶인 것만 둔다. 권한 `rememberLauncherForActivityResult`는 composition에서만
 * 만들 수 있고, tracking 폴링과 `ON_STOP` 처리도 composition의 effect다. 일은 셋이 나눠 맡는다.
 * 유휴 프리뷰는 [rememberIdlePreview], 촬영 조건(해상도·수동 설정)은 [rememberCaptureSettings], Session의 시작·진행·
 * 마감은 [CaptureSessionActions]다. 돌려주는 [CaptureDriver]는 매 composition 새로 만들어 그때의
 * [state]를 조작에 넘긴다.
 */
@Composable
internal fun rememberCaptureDriver(
    state: CaptureUiState,
    onIntent: (CaptureIntent) -> Unit,
    repository: SessionRepository,
    resolutionStore: RecordingResolutionStore,
    onCompleted: (sessionId: String, startUpload: Boolean) -> Unit,
): CaptureDriver {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val preview = rememberIdlePreview(state, onIntent)
    val captureSettings = rememberCaptureSettings(state, onIntent, preview, resolutionStore)
    val session = rememberCaptureSessionActions(scope, repository, preview, onIntent, onCompleted)
    val cameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            session.onCameraPermissionResult(granted, state)
        }
    // onTracking은 호출 시점 기준으로 경과 시간을 판정하므로, 값이 변하지 않아도 계속 호출되어야
    // 안정화(1초)와 유실(0.5초) 마감이 발화한다. 값 변화 구독만으로는 유실 마감이 오지 않는다.
    LaunchedEffect(state.phase != CaptureWorkspaceControlState.Idle) {
        if (state.phase == CaptureWorkspaceControlState.Idle) return@LaunchedEffect
        while (true) {
            session.sampleTracking()
            delay(TRACKING_TICK)
        }
    }
    LaunchedEffect(state.notice, state.open) {
        if (state.open && state.notice.isNotBlank()) {
            val notice = state.notice
            onIntent(CaptureIntent.NoticeShown)
            scope.launch { snackbarHostState.showSnackbar(notice) }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        session.interrupt(state)
    }

    return CaptureDriver(
        snackbarHostState = snackbarHostState,
        onSurfaceAvailable = { surface, texture -> preview.attach(surface, texture) },
        onSurfaceDestroyed = {
            preview.detach()
            onIntent(CaptureIntent.PreviewReleased)
        },
        play = { session.play(state) { cameraPermission.launch(Manifest.permission.CAMERA) } },
        pause = { session.pause(state) },
        requestExit = { session.requestExit(state) },
        confirmStop = { session.confirmStop(state) },
        confirmMetadata = { onIntent(CaptureIntent.ConfirmMetadata) },
        releaseIdlePreview = preview::release,
        captureSettings = captureSettings,
    )
}

/** Tracking 판정 주기. 안정화(1초)와 유실(0.5초) 임계값보다 충분히 촘촘해야 마감 시점이 제때 발화한다. */
private val TRACKING_TICK = 100.milliseconds
