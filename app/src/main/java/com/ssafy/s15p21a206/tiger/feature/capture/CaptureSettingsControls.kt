package com.ssafy.s15p21a206.tiger.feature.capture

import android.hardware.camera2.CameraManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.capture.camera.RecordingResolutionStore
import com.ssafy.s15p21a206.tiger.core.capture.manual.ManualCameraConfigStore
import com.ssafy.s15p21a206.tiger.core.capture.manual.ManualCameraProfile
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraConfig
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 카메라 설정 시트가 촬영 조건을 바꾸는 조작. 녹화 해상도와 수동 설정(초점·ISO·셔터·화이트 밸런스)이다.
 *
 * 어느 조건이든 고르는 즉시 유휴 프리뷰에 걸고 다음 실행을 위해 기억한다. Session이 시작되면 잠긴다.
 */
internal class CaptureSettingsControls(
    val selectResolution: (RecordingResolution) -> Unit,
    val editManualCamera: (ManualCameraConfig) -> Unit,
    /** 지금 프리뷰가 수렴시킨 화이트 밸런스를 붙잡는다. */
    val fixWhiteBalance: () -> Unit,
    val releaseWhiteBalance: () -> Unit,
)

/**
 * 촬영 조건을 세운다. 녹화 카메라의 능력을 읽고, 바꾼 값을 유휴 프리뷰에 걸고 기억한다.
 *
 * 능력은 작업 공간을 열 때 아직 읽지 못했으면 읽고, 읽은 뒤에는 다시 읽지 않는다. ARCore에게 어느 카메라인지 물으려면 Session을 잠깐
 * 만들어야 해 수백 ms가 걸리므로 배경에서 한다. 읽지 못하면 패널이 사유를 보여 주고, 수집은 기존 자동
 * 동작 그대로 돈다.
 */
@Composable
internal fun rememberCaptureSettings(
    state: CaptureUiState,
    onIntent: (CaptureIntent) -> Unit,
    preview: IdlePreview,
    resolutionStore: RecordingResolutionStore,
): CaptureSettingsControls {
    val context = LocalContext.current
    val whiteBalancePendingMessage = stringResource(R.string.capture_camera_white_balance_pending)
    val store = remember { ManualCameraConfigStore(context.applicationContext) }
    val profile =
        remember {
            ManualCameraProfile(context.applicationContext, context.getSystemService(CameraManager::class.java))
        }

    LaunchedEffect(state.open) {
        if (!state.open || state.manualCamera.capabilities != null) return@LaunchedEffect
        val capabilities = withContext(Dispatchers.IO) { profile.read() } ?: return@LaunchedEffect
        val config = capabilities.coerce(store.load() ?: capabilities.defaultConfig())
        onIntent(CaptureIntent.ManualCameraProfiled(capabilities, config))
        preview.camera.apply(config)
    }

    fun applyManualCamera(config: ManualCameraConfig) {
        onIntent(CaptureIntent.EditManualCamera(config))
        preview.camera.apply(config)
        store.save(config)
    }

    return CaptureSettingsControls(
        selectResolution = select@{ chosen ->
            if (!state.captureSettingsEditable) return@select
            onIntent(CaptureIntent.SelectResolution(chosen))
            resolutionStore.save(chosen)
            if (state.idlePreviewSize != chosen) {
                onIntent(CaptureIntent.IdlePreviewResized(chosen))
                preview.reopenAt(chosen)
            }
        },
        editManualCamera = { requested ->
            val coerced = state.manualCamera.capabilities?.coerce(requested) ?: requested
            applyManualCamera(coerced)
        },
        fixWhiteBalance = fix@{
            val current = state.manualCamera.config ?: return@fix
            // 프리뷰가 아직 수렴시키지 못했으면 붙잡을 값이 없다. 중립값으로 채우면 색이 틀어진 채
            // 고정돼, 고정하지 않은 것보다 나쁘다.
            val converged =
                preview.camera.convergedWhiteBalance ?: run {
                    onIntent(CaptureIntent.Notify(whiteBalancePendingMessage))
                    return@fix
                }
            applyManualCamera(current.copy(whiteBalance = converged))
        },
        releaseWhiteBalance = release@{
            val current = state.manualCamera.config ?: return@release
            applyManualCamera(current.copy(whiteBalance = null))
        },
    )
}
