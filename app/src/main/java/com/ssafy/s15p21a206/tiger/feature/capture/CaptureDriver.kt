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
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraConfig
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution
import com.ssafy.s15p21a206.tiger.core.session.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * 수집 작업 공간이 사용자 조작에 대해 실제로 하는 일들.
 *
 * 화면은 무엇을 그릴지만 알고, 카메라 세션을 열고 ARCore를 띄우고 Session을 마감하는 것은
 * 여기가 한다. [rememberCaptureDriver]가 만든다.
 */
internal class CaptureDriver(
    /** 작업 공간의 알림을 띄우는 자리. 문구는 상태의 `notice`에서 온다. */
    val snackbarHostState: SnackbarHostState,
    val onSurfaceAvailable: (Surface, SurfaceTexture) -> Unit,
    val onSurfaceDestroyed: () -> Unit,
    /** Idle이면 Session을, Ready면 Episode를 시작한다. */
    val play: () -> Unit,
    val pause: () -> Unit,
    /** 상태에 따라 무시하거나, 확인을 묻거나, 그냥 닫는다. */
    val requestExit: () -> Unit,
    val confirmStop: () -> Unit,
    val confirmMetadata: () -> Unit,
    /** 녹화 해상도를 바꾼다. 유휴 프리뷰를 그 크기로 다시 연다. */
    val selectResolution: (RecordingResolution) -> Unit,
    /** 작업 공간을 벗어날 때 유휴 프리뷰 Camera2 session을 놓는다. */
    val releaseIdlePreview: () -> Unit,
    /** 초점·ISO·셔터를 바꾼다. 바뀐 값은 곧바로 프리뷰에 걸린다. */
    val editManualCamera: (ManualCameraConfig) -> Unit,
    /** 지금 프리뷰가 수렴시킨 화이트 밸런스를 붙잡는다. */
    val fixWhiteBalance: () -> Unit,
    val releaseWhiteBalance: () -> Unit,
)

/**
 * 수집 파이프라인을 세우고 그 조작을 [CaptureDriver]로 돌려준다.
 *
 * 여기에는 composition에 묶인 것만 둔다. 권한 `rememberLauncherForActivityResult`는 composition에서만
 * 만들 수 있고, tracking 폴링과 `ON_STOP` 처리도 composition의 effect다. 일은 셋이 나눠 맡는다.
 * 유휴 프리뷰는 [rememberIdlePreview], 수동 촬영 조건은 [rememberManualCamera], Session의 시작·진행·
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
    val manualCamera = rememberManualCamera(state, onIntent, preview.camera)
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

    // 다른 촬영 조건처럼 고르는 즉시 기억하고 프리뷰에 건다. Session이 시작되면 상태 전이가 막는다.
    fun selectResolution(chosen: RecordingResolution) {
        if (state.phase != CaptureWorkspaceControlState.Idle || state.busy) return
        onIntent(CaptureIntent.SelectResolution(chosen))
        resolutionStore.save(chosen)
        // 유휴 프리뷰도 고른 해상도로 다시 연다. Camera2는 session을 만들 때 stream 크기를
        // 정하므로, 버퍼 크기만 바꾸면 이미 열린 session에는 반영되지 않는다.
        if (state.idlePreviewSize != chosen) {
            onIntent(CaptureIntent.IdlePreviewResized(chosen))
            preview.resizeBuffer(chosen.width, chosen.height)
            preview.restore()
        }
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
        selectResolution = ::selectResolution,
        releaseIdlePreview = preview::release,
        editManualCamera = manualCamera.edit,
        fixWhiteBalance = manualCamera.fixWhiteBalance,
        releaseWhiteBalance = manualCamera.releaseWhiteBalance,
    )
}

// Tracking 판정 주기. 안정화(1초)와 유실(0.5초) 임계값보다 충분히 촘촘해야 마감 시점이 제때 발화한다.
private val TRACKING_TICK = 100.milliseconds
