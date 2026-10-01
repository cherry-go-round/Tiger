package com.ssafy.s15p21a206.tiger.feature.capture

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.Surface
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.android.findActivity
import com.ssafy.s15p21a206.tiger.core.capture.AndroidCaptureRuntime
import com.ssafy.s15p21a206.tiger.core.capture.CaptureSessionCoordinator
import com.ssafy.s15p21a206.tiger.core.capture.MonotonicClock
import com.ssafy.s15p21a206.tiger.core.capture.PreviewSurfaceProvider
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 수집 Session의 시작·진행·마감.
 *
 * 화면 로직이 Composable 안에 두기에는 커서 plain class state holder로 뺀 것이다. 의존(ARCore
 * 런타임, Coordinator, 저장소, 유휴 프리뷰)은 [rememberCaptureSessionActions]가 한 번 만들어 들고,
 * 작업 공간 상태는 부모가 소유해 매 composition 바뀌므로 조작을 부를 때마다 그때의 값을 인자로 받는다.
 */
internal class CaptureSessionActions(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val scope: CoroutineScope,
    private val runtime: AndroidCaptureRuntime,
    private val repository: SessionRepository,
    private val preview: IdlePreview,
    private val onIntent: (CaptureIntent) -> Unit,
    private val onCompleted: (sessionId: String, startUpload: Boolean) -> Unit,
) {
    private val coordinator =
        CaptureSessionCoordinator(
            clock = MonotonicClock(SystemClock::elapsedRealtimeNanos),
            onEpisodeClosed = { marker ->
                // 사용자 종료와 Tracking 유실 자동 마감이 같은 경로로 기록된다.
                if (marker.outcome == EpisodeState.INVALID_TRACKING) {
                    notify(R.string.capture_episode_invalid_tracking)
                }
                scope.launch {
                    runCatching {
                        repository.save(marker)
                        runtime.appendEpisode(marker)
                    }.onFailure { notify(R.string.capture_operation_failed) }
                }
            },
        )

    private var arCoreInstallRequested = false

    /** Idle이면 Session을, Ready면 Episode를 시작한다. */
    fun play(
        state: CaptureUiState,
        requestCameraPermission: () -> Unit,
    ) {
        if (!state.policy.canPlay) return
        if (state.phase == CaptureWorkspaceControlState.Idle) {
            onIntent(CaptureIntent.SessionRequested)
            preview.release()
            requestCameraPermission()
        } else {
            // Episode 시작은 Coordinator가 Tracking 안정화 여부를 확인한 뒤에만 허용한다.
            runCatching { coordinator.startEpisode(state.task, state.objectName) }
                .onSuccess { onIntent(CaptureIntent.EpisodeStarted) }
                .onFailure { notify(R.string.capture_tracking_not_ready) }
        }
    }

    fun onCameraPermissionResult(
        granted: Boolean,
        state: CaptureUiState,
    ) {
        if (!granted) {
            declineStart(R.string.recording_camera_unavailable)
        } else if (arCoreReady()) {
            startSession(state)
        }
    }

    /** ARCore가 있는지 확인하고, 없으면 설치를 요청한다. 이번에 Session을 시작해도 되면 true다. */
    private fun arCoreReady(): Boolean {
        val activity = context.findActivity()
        val installStatus =
            runCatching {
                requireNotNull(activity) { "Activity is required to install ARCore." }
                ArCoreApk.getInstance().requestInstall(activity, !arCoreInstallRequested)
            }.getOrElse {
                declineStart(R.string.arcore_unavailable)
                return false
            }
        if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
            arCoreInstallRequested = true
            declineStart(R.string.arcore_install_requested)
            return false
        }
        return true
    }

    /** 이번에는 Session을 시작하지 않는다. 사유를 알리고 제어 잠금을 푼다. */
    private fun declineStart(
        @StringRes reason: Int,
    ) {
        notify(reason)
        onIntent(CaptureIntent.BusyReleased)
    }

    private fun startSession(state: CaptureUiState) {
        scope.launch {
            try {
                val bundle = openRecording(state) ?: return@launch
                recordSessionStart(state, bundle)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                notify(R.string.capture_operation_failed)
            } finally {
                onIntent(CaptureIntent.BusyReleased)
            }
        }
    }

    /** ARCore로 녹화를 연다. 열지 못하면 되돌리고 사유를 알린 뒤 null을 돌려준다. */
    private suspend fun openRecording(state: CaptureUiState): SessionBundle? {
        val displayNumber = repository.nextDisplayNumber()
        return runCatching {
            runtime.start(
                displayNumber = displayNumber,
                resolution = state.resolution,
                previewSurfaces = PreviewSurfaceProvider(::followArCoreResolution),
                manual = state.manualCamera.appliedConfig,
            )
        }.onFailure(::recoverFromStartFailure).getOrNull()
    }

    /**
     * ARCore가 실제로 고른 녹화 크기로 표시와 유휴 프리뷰 버퍼를 맞추고, 녹화 중 프리뷰를 그릴 Surface를 내준다.
     *
     * 후보 config가 없어 기본값으로 물러났으면 고른 크기와 다를 수 있다. Main dispatcher에서 불리고 유휴
     * 프리뷰 Camera2 session은 이미 닫혔으므로, 상태와 버퍼 크기를 바로 바꿔도 된다.
     */
    private fun followArCoreResolution(
        width: Int,
        height: Int,
    ): Surface? {
        val actual = RecordingResolution(width, height)
        onIntent(CaptureIntent.SelectResolution(actual))
        onIntent(CaptureIntent.IdlePreviewResized(actual))
        preview.resizeBuffer(width, height)
        return preview.surface
    }

    private fun recoverFromStartFailure(error: Throwable) {
        preview.restore()
        onIntent(CaptureIntent.Notify("${message(R.string.arcore_session_start_failed)}: ${error.message.orEmpty()}"))
        if (error is UnavailableArcoreNotInstalledException) {
            context.openArCoreStore()
        }
    }

    private suspend fun recordSessionStart(
        state: CaptureUiState,
        bundle: SessionBundle,
    ) {
        val startedAtNs = SystemClock.elapsedRealtimeNanos()
        coordinator.start(bundle.sessionId, bundle.displayNumber, bundle.directory.absolutePath, state.task, state.objectName)
        onIntent(CaptureIntent.SessionStarted(bundle, startedAtNs, state.resolution))
        repository.save(state.sessionRow(bundle, RecordingState.INITIALIZING, startNs = startedAtNs))
    }

    /** Tracking 표본 하나를 Coordinator에 넣고 화면 상태를 맞춘다. 작업 공간이 주기적으로 부른다. */
    fun sampleTracking() {
        val sample = runtime.tracking.value
        coordinator.onTracking(sample.isTracking, sample.observedAtNs.takeIf { it > 0L })
        // Tracking 유실 자동 마감은 사용자 조작 없이 일어나므로 화면 상태를 여기서 맞춘다.
        onIntent(
            CaptureIntent.TrackingSampled(
                ready = coordinator.trackingState == TrackingState.READY,
                episodeActive = coordinator.activeEpisode != null,
            ),
        )
    }

    fun pause(state: CaptureUiState) {
        if (!state.policy.canPause) return
        // 기록은 coordinator의 onEpisodeClosed가 담당한다.
        runCatching { coordinator.endEpisode() }
            .onSuccess { onIntent(CaptureIntent.EpisodeEnded) }
            .onFailure { notify(R.string.capture_operation_failed) }
    }

    /** 상태에 따라 무시하거나, 확인을 묻거나, 그냥 닫는다. */
    fun requestExit(state: CaptureUiState) {
        when (state.policy.exitAction) {
            CaptureExitAction.Ignore -> Unit
            CaptureExitAction.Confirm -> onIntent(CaptureIntent.StopRequested)
            // 녹화 전에는 Session을 만들지 않고 작업 공간만 닫는다. 아래에 목록이 그대로 남아 있다.
            CaptureExitAction.Leave -> onIntent(CaptureIntent.Close)
        }
    }

    fun confirmStop(state: CaptureUiState) {
        onIntent(CaptureIntent.StopDismissed)
        finalizeCapture(state)
    }

    private fun finalizeCapture(state: CaptureUiState) {
        if (!state.policy.canStop) return
        // 진행 중인 Episode가 있으면 Session을 마감하지 않고 먼저 종료하도록 안내한다.
        if (coordinator.activeEpisode != null) {
            notify(R.string.active_episode_end_required)
            return
        }
        val bundle = state.activeBundle
        val startedAtNs = state.recordingStartNs
        onIntent(CaptureIntent.FinalizeStarted)
        scope.launch {
            try {
                val result =
                    runCatching { withContext(Dispatchers.IO) { runtime.stop() } }
                        .getOrElse { FinalizeResult.Failed(it.message.orEmpty()) }
                when (result) {
                    is FinalizeResult.Completed -> {
                        bundle?.let {
                            // 백그라운드에서 마감됐으면 전송을 걸지 않고 실패로 남긴다.
                            val startUpload = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                            repository.save(
                                state.sessionRow(
                                    bundle,
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
                    is FinalizeResult.Failed ->
                        onIntent(CaptureIntent.FinalizeFailed("${message(R.string.capture_finalize_failed)}: ${result.reason}"))
                }
                // 다음 Session을 시작할 수 있도록 Coordinator의 Session·Tracking 상태를 닫는다.
                coordinator.release()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                onIntent(CaptureIntent.FinalizeFailed(message(R.string.capture_finalize_failed)))
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

    /** 앱이 밀려나면 진행 중인 Session을 `INTERRUPTED`로 남긴다. 다음 실행이 복구한다. */
    fun interrupt(state: CaptureUiState) {
        if (state.phase == CaptureWorkspaceControlState.Finalizing) return
        val interruptedBundle = state.activeBundle ?: return
        val startedAtNs = state.recordingStartNs
        runtime.interrupt()
        scope.launch {
            repository.save(
                state.sessionRow(
                    interruptedBundle,
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

    private fun notify(
        @StringRes id: Int,
    ) {
        onIntent(CaptureIntent.Notify(message(id)))
    }

    private fun message(
        @StringRes id: Int,
    ): String = context.getString(id)
}

/**
 * [CaptureSessionActions]를 한 번 만든다.
 *
 * 부모가 넘기는 콜백은 composition마다 새로 만들어지므로 최신 것을 부르게 감싼다.
 */
@Composable
internal fun rememberCaptureSessionActions(
    scope: CoroutineScope,
    repository: SessionRepository,
    preview: IdlePreview,
    onIntent: (CaptureIntent) -> Unit,
    onCompleted: (sessionId: String, startUpload: Boolean) -> Unit,
): CaptureSessionActions {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnIntent by rememberUpdatedState(onIntent)
    val currentOnCompleted by rememberUpdatedState(onCompleted)
    return remember {
        CaptureSessionActions(
            context = context,
            lifecycle = lifecycle,
            scope = scope,
            runtime = AndroidCaptureRuntime(context.applicationContext, SessionBundleStore(context.applicationContext)),
            repository = repository,
            preview = preview,
            onIntent = { currentOnIntent(it) },
            onCompleted = { sessionId, startUpload -> currentOnCompleted(sessionId, startUpload) },
        )
    }
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
 * 복구하게 하기 위해서다. Task·Object와 표시 번호는 번들에 없고 이 행에만 있다. Task·Object는 작업 공간
 * 상태에서 채운다.
 *
 * [recordedAtEpochMs]는 영상이 만들어진 벽시계 시각이라 녹화가 멈춘 저장(마감·중단)에서만 넘긴다.
 * 시작 행은 아직 영상이 없어 0으로 두며, 끝에서 저장하지 못하고 복구된 Session은 복구가 영상 파일의
 * 수정 시각으로 채운다. 수집 길이는 [startNs]와 [endNs](부팅 이후 경과 시간)로 따로 잰다.
 */
private fun CaptureUiState.sessionRow(
    bundle: SessionBundle,
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
