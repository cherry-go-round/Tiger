package com.ssafy.s15p21a206.tiger.feature.capture

import android.Manifest
import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.capture.camera.PreviewCameraSession
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution

/**
 * Session을 시작하기 전의 유휴 프리뷰. 작업 공간이 열려 있는 동안 Camera2로 그린다.
 *
 * `TextureView`가 넘겨준 Surface와 SurfaceTexture를 들고 그 위에 [PreviewCameraSession]을 연다.
 * Surface를 여기서 드는 것은 Camera2 session을 여는 쪽이 여기이기 때문이다. 화면은 Surface가 생기고
 * 사라졌다는 사실만 알린다. Session이 시작되면 ARCore가 같은 Surface에 그리므로 이 프리뷰는 닫는다.
 */
internal class IdlePreview(
    context: Context,
    onFailure: () -> Unit,
) {
    /** 프리뷰를 그리는 Camera2 session. 수동 촬영 조건을 걸고 수렴한 화이트 밸런스를 읽는 데도 쓴다. */
    val camera = PreviewCameraSession(context) { _ -> onFailure() }

    /** ARCore도 Session을 시작할 때 이 Surface에 그린다. */
    var surface by mutableStateOf<Surface?>(null)
        private set

    /** 수집 시작 때 ARCore가 고른 해상도로 버퍼를 다시 맞추려면 들고 있어야 한다. */
    private var texture by mutableStateOf<SurfaceTexture?>(null)

    fun attach(
        surface: Surface,
        texture: SurfaceTexture,
    ) {
        this.surface = surface
        this.texture = texture
        camera.prepare(surface)
    }

    /**
     * 유휴 Camera2 프리뷰를 (다시) 연다.
     *
     * prepare()는 이미 열려 있으면 즉시 반환하므로 먼저 닫아야 실제로 다시 연다. Surface가 없으면 열
     * 곳이 없다.
     */
    fun restore() {
        val surface = surface ?: return
        camera.release()
        camera.prepare(surface)
    }

    fun resizeBuffer(
        width: Int,
        height: Int,
    ) {
        texture?.setDefaultBufferSize(width, height)
    }

    /**
     * 유휴 프리뷰를 [resolution] 크기로 다시 연다.
     *
     * Camera2는 session을 만들 때 stream 크기를 정하므로, 버퍼 크기만 바꾸면 이미 열린 session에는 반영되지 않는다.
     */
    fun reopenAt(resolution: RecordingResolution) {
        resizeBuffer(resolution.width, resolution.height)
        restore()
    }

    fun release() {
        camera.release()
    }

    fun detach() {
        camera.release()
        surface?.release()
        surface = null
        texture = null
    }
}

/**
 * 유휴 프리뷰를 세운다. 작업 공간이 열리면 카메라 권한을 받아 열고, 백그라운드에서 돌아오면 되살린다.
 */
@Composable
internal fun rememberIdlePreview(
    state: CaptureUiState,
    onIntent: (CaptureIntent) -> Unit,
): IdlePreview {
    val context = LocalContext.current
    val failureMessage = stringResource(R.string.capture_preview_failed)
    val preview =
        remember {
            IdlePreview(context.applicationContext) { onIntent(CaptureIntent.PreviewFailed(failureMessage)) }
        }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) preview.restore() else onIntent(CaptureIntent.PreviewFailed(failureMessage))
        }
    LaunchedEffect(state.open) {
        if (state.open) {
            permission.launch(Manifest.permission.CAMERA)
        }
    }
    // 백그라운드 전환으로 카메라를 놓고 돌아온 경우 TextureView는 살아 있지만 프레임이 끊긴 상태다.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (state.open && state.phase == CaptureWorkspaceControlState.Idle) preview.restore()
    }
    return preview
}
