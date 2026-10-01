package com.ssafy.s15p21a206.tiger.feature.capture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.os.SystemClock
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.android.findActivity
import com.ssafy.s15p21a206.tiger.core.capture.AndroidCaptureRuntime
import com.ssafy.s15p21a206.tiger.core.capture.CaptureSessionCoordinator
import com.ssafy.s15p21a206.tiger.core.capture.MonotonicClock
import com.ssafy.s15p21a206.tiger.core.capture.camera.RecordingResolutionStore
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraConfig
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution
import com.ssafy.s15p21a206.tiger.core.model.capture.TrackingState
import com.ssafy.s15p21a206.tiger.core.model.session.CaptureSession
import com.ssafy.s15p21a206.tiger.core.model.session.EpisodeState
import com.ssafy.s15p21a206.tiger.core.model.session.FinalizeResult
import com.ssafy.s15p21a206.tiger.core.model.session.RecordingState
import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState
import com.ssafy.s15p21a206.tiger.core.session.SessionBundleStore
import com.ssafy.s15p21a206.tiger.core.session.SessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * 평범한 클래스로 두지 못하는 이유가 있다. 권한 `rememberLauncherForActivityResult`는 composition
 * 에서만 만들 수 있고, tracking 폴링과 `ON_STOP`·`ON_START` 처리도 composition의 effect다.
 * 대신 무거운 것들은 [remember]로 한 번만 만들고, 돌려주는 [CaptureDriver]는 매 composition
 * 새로 만든다 — 그래야 콜백이 최신 [state]를 본다.
 *
 * 유휴 프리뷰와 수동 촬영 조건은 [rememberIdlePreview]와 [rememberManualCamera]가 세운다. 여기는
 * 그 둘을 받아 Session의 수명(시작·tracking·중단·마감)을 맡는다.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun rememberCaptureDriver(
    state: CaptureUiState,
    onIntent: (CaptureIntent) -> Unit,
    repository: SessionRepository,
    resolutionStore: RecordingResolutionStore,
    onCompleted: (sessionId: String, startUpload: Boolean) -> Unit,
): CaptureDriver {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    // 화면 문구. 콜백이 composition 밖에서 불리므로 여기서 미리 읽어 둔다.
    val operationFailureMessage = stringResource(R.string.capture_operation_failed)
    val episodeInvalidatedMessage = stringResource(R.string.capture_episode_invalid_tracking)
    val recordingCameraUnavailable = stringResource(R.string.recording_camera_unavailable)
    val activeEpisodeEndRequired = stringResource(R.string.active_episode_end_required)
    val trackingNotReadyMessage = stringResource(R.string.capture_tracking_not_ready)
    val captureFinalizeFailed = stringResource(R.string.capture_finalize_failed)
    val arCoreUnavailable = stringResource(R.string.arcore_unavailable)
    val arCoreInstallMessage = stringResource(R.string.arcore_install_requested)
    val arCoreSessionStartFailed = stringResource(R.string.arcore_session_start_failed)

    val captureRuntime = remember { AndroidCaptureRuntime(context.applicationContext, SessionBundleStore(context.applicationContext)) }
    val snackbarHostState = remember { SnackbarHostState() }
    val preview = rememberIdlePreview(state, onIntent)
    val manualCamera = rememberManualCamera(state, onIntent, preview.camera)
    var arCoreInstallRequested by remember { mutableStateOf(false) }
    val coordinator =
        remember {
            CaptureSessionCoordinator(
                clock = MonotonicClock(SystemClock::elapsedRealtimeNanos),
                onEpisodeClosed = { marker ->
                    // 사용자 종료와 Tracking 유실 자동 마감이 같은 경로로 기록된다.
                    if (marker.outcome == EpisodeState.INVALID_TRACKING) onIntent(CaptureIntent.Notify(episodeInvalidatedMessage))
                    scope.launch {
                        runCatching {
                            repository.save(marker)
                            captureRuntime.appendEpisode(marker)
                        }.onFailure { onIntent(CaptureIntent.Notify(operationFailureMessage)) }
                    }
                },
            )
        }

    // ARCore가 있는지 확인하고, 없으면 설치를 요청한다. 이번 요청으로 Session을 시작해도 되면 true다.
    // 시작하지 않을 때는 사유를 알리고 제어 잠금을 푼다.
    fun arCoreReady(): Boolean {
        val activity = context.findActivity()
        val installStatus =
            runCatching {
                requireNotNull(activity) { "Activity is required to install ARCore." }
                ArCoreApk.getInstance().requestInstall(activity, !arCoreInstallRequested)
            }.getOrElse {
                onIntent(CaptureIntent.Notify(arCoreUnavailable))
                onIntent(CaptureIntent.BusyReleased)
                return false
            }
        if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
            arCoreInstallRequested = true
            onIntent(CaptureIntent.Notify(arCoreInstallMessage))
            onIntent(CaptureIntent.BusyReleased)
            return false
        }
        return true
    }

    // ARCore로 녹화를 시작하고 시작 행을 남긴다. 성공하든 실패하든 끝나면 제어 잠금을 푼다.
    fun startSession() {
        scope.launch {
            try {
                val displayNumber = repository.nextDisplayNumber()
                runCatching {
                    // ARCore가 고른 해상도로 버퍼를 맞춰 프리뷰 화각을 저장 영상과 일치시킨다.
                    // 이 람다는 Main dispatcher에서 실행되므로 Compose 상태를 직접 갱신해도 된다.
                    // 프리뷰용 Camera2 세션은 play에서 이미 닫혔으므로 버퍼 크기를 바꿔도 안전하다.
                    captureRuntime.start(
                        displayNumber = displayNumber,
                        resolution = state.resolution,
                        previewSurfaces = { width, height ->
                            // ARCore가 실제로 고른 크기다. 후보 config가 없어 기본값으로 물러났으면
                            // 고른 값과 다를 수 있으므로, 표시와 프리뷰를 여기에 맞춘다.
                            val actual = RecordingResolution(width, height)
                            onIntent(CaptureIntent.SelectResolution(actual))
                            onIntent(CaptureIntent.IdlePreviewResized(actual))
                            preview.resizeBuffer(width, height)
                            preview.surface
                        },
                        // 프리뷰에서 맞춘 값을 그대로 녹화에 건다. Session이 시작된 뒤에는 바뀌지 않는다.
                        manual = state.manualCamera.appliedConfig,
                    )
                }.onSuccess { bundle ->
                    val startedAtNs = SystemClock.elapsedRealtimeNanos()
                    // Session 시작은 Episode를 만들지 않는다. Tracking 안정화 뒤 사용자가 따로 시작한다.
                    coordinator.start(
                        bundle.sessionId,
                        bundle.displayNumber,
                        bundle.directory.absolutePath,
                        state.task,
                        state.objectName,
                    )
                    onIntent(CaptureIntent.SessionStarted(bundle, startedAtNs, state.resolution))
                    repository.save(
                        sessionRow(bundle, state.task, state.objectName, RecordingState.INITIALIZING, startNs = startedAtNs),
                    )
                }.onFailure { error ->
                    // ARCore가 카메라를 잡지 못했으므로 유휴 프리뷰를 되살린다.
                    preview.restore()
                    onIntent(CaptureIntent.Notify("$arCoreSessionStartFailed: ${error.message.orEmpty()}"))
                    if (error is UnavailableArcoreNotInstalledException) {
                        context.openArCoreStore()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onIntent(CaptureIntent.Notify(operationFailureMessage))
            } finally {
                onIntent(CaptureIntent.BusyReleased)
            }
        }
    }
    val cameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                onIntent(CaptureIntent.Notify(recordingCameraUnavailable))
                onIntent(CaptureIntent.BusyReleased)
            } else if (arCoreReady()) {
                startSession()
            }
        }
    // onTracking은 호출 시점 기준으로 경과 시간을 판정하므로, 값이 변하지 않아도 계속 호출되어야
    // 안정화(1초)와 유실(0.5초) 마감이 발화한다. 값 변화 구독만으로는 유실 마감이 오지 않는다.
    LaunchedEffect(state.phase != CaptureWorkspaceControlState.Idle) {
        if (state.phase == CaptureWorkspaceControlState.Idle) return@LaunchedEffect
        while (true) {
            val sample = captureRuntime.tracking.value
            coordinator.onTracking(sample.isTracking, sample.observedAtNs.takeIf { it > 0L })
            // Tracking 유실 자동 마감은 사용자 조작 없이 일어나므로 화면 상태를 여기서 맞춘다.
            onIntent(
                CaptureIntent.TrackingSampled(
                    ready = coordinator.trackingState == TrackingState.READY,
                    episodeActive = coordinator.activeEpisode != null,
                ),
            )
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
        if (state.phase == CaptureWorkspaceControlState.Finalizing) return@LifecycleEventEffect
        val interruptedBundle = state.activeBundle ?: return@LifecycleEventEffect
        val startedAtNs = state.recordingStartNs
        captureRuntime.interrupt()
        scope.launch {
            repository.save(
                sessionRow(
                    interruptedBundle,
                    state.task,
                    state.objectName,
                    RecordingState.INTERRUPTED,
                    startNs = startedAtNs,
                    endNs = SystemClock.elapsedRealtimeNanos(),
                    recordedAtEpochMs = System.currentTimeMillis(),
                ),
            )
        }
        onIntent(CaptureIntent.Interrupted)
        coordinator.release()
    }

    fun finalizeCapture() {
        if (!state.policy.canStop) return
        // 진행 중인 Episode가 있으면 Session을 마감하지 않고 먼저 종료하도록 안내한다.
        if (coordinator.activeEpisode != null) {
            onIntent(CaptureIntent.Notify(activeEpisodeEndRequired))
            return
        }
        val bundle = state.activeBundle
        val startedAtNs = state.recordingStartNs
        onIntent(CaptureIntent.FinalizeStarted)
        scope.launch {
            try {
                when (
                    val result =
                        runCatching { withContext(Dispatchers.IO) { captureRuntime.stop() } }
                            .getOrElse { FinalizeResult.Failed(it.message.orEmpty()) }
                ) {
                    is FinalizeResult.Completed -> {
                        bundle?.let {
                            // 백그라운드에서 마감됐으면 전송을 걸지 않고 실패로 남긴다.
                            val startUpload = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                            repository.save(
                                sessionRow(
                                    bundle,
                                    state.task,
                                    state.objectName,
                                    RecordingState.COMPLETED,
                                    startNs = startedAtNs,
                                    endNs = SystemClock.elapsedRealtimeNanos(),
                                    recordedAtEpochMs = System.currentTimeMillis(),
                                    bundlePath = result.directory.absolutePath,
                                    uploadState = if (startUpload) UploadState.LOCAL_ONLY else UploadState.FAILED,
                                ),
                            )
                            // 어디로 갈지는 부모가 정한다. 수집은 목적지를 모른다.
                            onCompleted(bundle.sessionId, startUpload)
                        }
                    }
                    // 마감 실패는 지나가는 알림이 아니다. 저장된 세션이 없으므로 넘어갈 곳도 없다.
                    // 작업 공간을 덮은 판에 세워 두고, 확인을 받은 뒤에야 다시 찍을 수 있게 한다.
                    is FinalizeResult.Failed -> onIntent(CaptureIntent.FinalizeFailed("$captureFinalizeFailed: ${result.reason}"))
                }
                // 다음 Session을 시작할 수 있도록 Coordinator의 Session·Tracking 상태를 닫는다.
                coordinator.release()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onIntent(CaptureIntent.FinalizeFailed(captureFinalizeFailed))
            } finally {
                // 예외로 빠져나가도 Finalizing에 머무르지 않게 여기서 닫는다.
                onIntent(CaptureIntent.Finalized)
                // 작업 공간에 남는 경로(마감 실패, 업로드 서버 주소 없음)를 위해 유휴 프리뷰를 다시 연다.
                // 의도는 그 경로에서만 여는 것이지만 `state`는 마감을 시작한 시점의 값이라 늘 open이다.
                // 상세로 넘어가는 경로에서도 Surface가 아직 남아 있어 잠깐 열렸다가, 작업 공간을 벗어날 때
                // 닫힌다.
                if (state.open) preview.restore()
            }
        }
    }

    // 매 composition 새로 만든다. 콜백이 remember에 갇히면 옛 state를 보게 된다.
    return CaptureDriver(
        snackbarHostState = snackbarHostState,
        onSurfaceAvailable = { surface, texture -> preview.attach(surface, texture) },
        onSurfaceDestroyed = {
            preview.detach()
            onIntent(CaptureIntent.PreviewReleased)
        },
        play = play@{
            if (!state.policy.canPlay) return@play
            if (state.phase == CaptureWorkspaceControlState.Idle) {
                onIntent(CaptureIntent.SessionRequested)
                preview.release()
                cameraPermission.launch(Manifest.permission.CAMERA)
            } else {
                // Episode 시작은 Coordinator가 Tracking 안정화 여부를 확인한 뒤에만 허용한다.
                runCatching { coordinator.startEpisode(state.task, state.objectName) }
                    .onSuccess { onIntent(CaptureIntent.EpisodeStarted) }
                    .onFailure { onIntent(CaptureIntent.Notify(trackingNotReadyMessage)) }
            }
        },
        pause = pause@{
            if (!state.policy.canPause) return@pause
            // 기록은 coordinator의 onEpisodeClosed가 담당한다.
            runCatching { coordinator.endEpisode() }
                .onSuccess { onIntent(CaptureIntent.EpisodeEnded) }
                .onFailure { onIntent(CaptureIntent.Notify(operationFailureMessage)) }
        },
        requestExit = {
            when (state.policy.exitAction) {
                CaptureExitAction.Ignore -> Unit
                CaptureExitAction.Confirm -> onIntent(CaptureIntent.StopRequested)
                // 녹화 전에는 Session을 만들지 않고 작업 공간만 닫는다. 아래에 목록이 그대로 남아 있다.
                CaptureExitAction.Leave -> onIntent(CaptureIntent.Close)
            }
        },
        confirmStop = {
            onIntent(CaptureIntent.StopDismissed)
            finalizeCapture()
        },
        confirmMetadata = { onIntent(CaptureIntent.ConfirmMetadata) },
        // 다른 촬영 조건처럼 고르는 즉시 기억하고 프리뷰에 건다. Session이 시작되면 상태 전이가 막는다.
        selectResolution = select@{ chosen ->
            if (state.phase != CaptureWorkspaceControlState.Idle || state.busy) return@select
            onIntent(CaptureIntent.SelectResolution(chosen))
            resolutionStore.save(chosen)
            // 유휴 프리뷰도 고른 해상도로 다시 연다. Camera2는 session을 만들 때 stream 크기를
            // 정하므로, 버퍼 크기만 바꾸면 이미 열린 session에는 반영되지 않는다.
            if (state.idlePreviewSize != chosen) {
                onIntent(CaptureIntent.IdlePreviewResized(chosen))
                preview.resizeBuffer(chosen.width, chosen.height)
                preview.restore()
            }
        },
        releaseIdlePreview = preview::release,
        editManualCamera = manualCamera.edit,
        fixWhiteBalance = manualCamera.fixWhiteBalance,
        releaseWhiteBalance = manualCamera.releaseWhiteBalance,
    )
}

private fun Context.openArCoreStore() {
    val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=com.google.ar.core".toUri())
    val webIntent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=com.google.ar.core".toUri())
    startActivity(if (marketIntent.resolveActivity(packageManager) != null) marketIntent else webIntent)
}

/**
 * 이 수집의 Session 행. 저장할 때마다 이 함수로 만들어 행 전체를 덮어쓴다.
 *
 * 시작 때 먼저 저장해 두는 것은 수집 도중 프로세스가 죽어도 다음 실행이 이 행으로 staging 번들을 찾아
 * 복구하게 하기 위해서다. Task·Object와 표시 번호는 번들에 없고 이 행에만 있다.
 *
 * [recordedAtEpochMs]는 영상이 만들어진 벽시계 시각이라 녹화가 멈춘 저장(마감·중단)에서만 넘긴다.
 * 시작 행은 아직 영상이 없어 0으로 두며, 끝에서 저장하지 못하고 복구된 Session은 복구가 영상 파일의
 * 수정 시각으로 채운다. 수집 길이는 [startNs]와 [endNs](부팅 이후 경과 시간)로 따로 잰다.
 */
private fun sessionRow(
    bundle: SessionBundle,
    task: String,
    objectName: String,
    recordingState: RecordingState,
    startNs: Long,
    endNs: Long? = null,
    recordedAtEpochMs: Long = 0L,
    bundlePath: String = bundle.directory.absolutePath,
    uploadState: UploadState = UploadState.LOCAL_ONLY,
) = CaptureSession(
    sessionId = bundle.sessionId,
    displayNumber = bundle.displayNumber,
    recordingState = recordingState,
    uploadState = uploadState,
    recordingStartMonotonicTimestampNs = startNs,
    recordingEndMonotonicTimestampNs = endNs,
    bundlePath = bundlePath,
    recordedAtEpochMs = recordedAtEpochMs,
    task = task,
    objectName = objectName,
)

// Tracking 판정 주기. 안정화(1초)와 유실(0.5초) 임계값보다 충분히 촘촘해야 마감 시점이 제때 발화한다.
private val TRACKING_TICK = 100.milliseconds
