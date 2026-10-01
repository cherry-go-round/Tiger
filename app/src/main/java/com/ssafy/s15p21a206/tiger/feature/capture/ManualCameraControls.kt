package com.ssafy.s15p21a206.tiger.feature.capture

import android.hardware.camera2.CameraManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.capture.camera.PreviewCameraSession
import com.ssafy.s15p21a206.tiger.core.capture.manual.ManualCameraConfigStore
import com.ssafy.s15p21a206.tiger.core.capture.manual.ManualCameraProfile
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 카메라 설정 시트가 촬영 조건을 바꾸는 조작. */
internal class ManualCameraControls(
    /** 초점·ISO·셔터를 바꾼다. 바뀐 값은 곧바로 프리뷰에 걸린다. */
    val edit: (ManualCameraConfig) -> Unit,
    /** 지금 프리뷰가 수렴시킨 화이트 밸런스를 붙잡는다. */
    val fixWhiteBalance: () -> Unit,
    val releaseWhiteBalance: () -> Unit,
)

/**
 * 수동 촬영 조건을 세운다. 녹화 카메라의 능력을 읽고, 바꾼 값을 프리뷰에 걸고 기억한다.
 *
 * @param preview 값을 걸 유휴 프리뷰. Session이 시작되면 값은 녹화에 그대로 실리고 더 바뀌지 않는다.
 */
@Composable
internal fun rememberManualCamera(
    state: CaptureUiState,
    onIntent: (CaptureIntent) -> Unit,
    preview: PreviewCameraSession,
): ManualCameraControls {
    val context = LocalContext.current
    val whiteBalancePendingMessage = stringResource(R.string.capture_camera_white_balance_pending)
    val store = remember { ManualCameraConfigStore(context.applicationContext) }
    val profile =
        remember {
            ManualCameraProfile(context.applicationContext, context.getSystemService(CameraManager::class.java))
        }

    // 녹화에 쓰일 카메라의 능력은 한 번만 읽는다. ARCore에게 어느 카메라인지 물으려면 Session을
    // 잠깐 만들어야 해 수백 ms가 걸리므로 배경에서 한다. 읽지 못하면 패널이 사유를 보여 주고,
    // 수집은 기존 자동 동작 그대로 돈다.
    LaunchedEffect(state.open) {
        if (!state.open || state.manualCamera.capabilities != null) return@LaunchedEffect
        val capabilities = withContext(Dispatchers.IO) { profile.read() } ?: return@LaunchedEffect
        val config = capabilities.coerce(store.load() ?: capabilities.defaultConfig())
        onIntent(CaptureIntent.ManualCameraProfiled(capabilities, config))
        preview.apply(config)
    }

    // 촬영 조건을 바꾼다. 상태에 올리고, 곧바로 프리뷰에 걸고, 다음 실행을 위해 기억한다.
    fun apply(config: ManualCameraConfig) {
        onIntent(CaptureIntent.EditManualCamera(config))
        preview.apply(config)
        store.save(config)
    }

    return ManualCameraControls(
        // 값이 바뀌면 곧바로 프리뷰에 건다. 초점을 화면으로 보고 고르는 것이 이 기능의 목적이다.
        // 기억까지 여기서 하는 것은, 다음에 앱을 켰을 때도 같은 조건으로 찍어야 하기 때문이다.
        edit = { requested ->
            val coerced = state.manualCamera.capabilities?.coerce(requested) ?: requested
            apply(coerced)
        },
        fixWhiteBalance = fix@{
            val current = state.manualCamera.config ?: return@fix
            // 프리뷰가 아직 수렴시키지 못했으면 붙잡을 값이 없다. 중립값으로 채우면 색이 틀어진 채
            // 고정돼, 고정하지 않은 것보다 나쁘다.
            val converged =
                preview.convergedWhiteBalance ?: run {
                    onIntent(CaptureIntent.Notify(whiteBalancePendingMessage))
                    return@fix
                }
            apply(current.copy(whiteBalance = converged))
        },
        releaseWhiteBalance = release@{
            val current = state.manualCamera.config ?: return@release
            apply(current.copy(whiteBalance = null))
        },
    )
}
