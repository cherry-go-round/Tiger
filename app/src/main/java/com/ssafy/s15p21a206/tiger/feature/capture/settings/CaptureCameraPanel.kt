package com.ssafy.s15p21a206.tiger.feature.capture.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.component.ScreenPreview
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureControlDisabled
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureOverlaySupporting
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraCapabilities
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraConfig
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingFormat
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution
import com.ssafy.s15p21a206.tiger.core.model.capture.ShutterPreset
import com.ssafy.s15p21a206.tiger.feature.capture.state.ManualCameraUiState

/**
 * 수집 전에 촬영 조건을 맞추는 패널.
 *
 * 값은 바꾸는 즉시 프리뷰에 걸린다. 초점을 화면으로 보고 고르는 것이 이 패널의 목적이라, 확인
 * 버튼을 눌러야 반영되는 구조로 두면 맞출 수가 없다. [enabled]가 `false`인 동안은 Session이
 * 돌고 있다는 뜻이고, 그때는 어떤 값도 받지 않는다.
 *
 * 기기가 지원하지 않는 항목은 감추지 않고 끈 채로 둔다. 자리가 사라지면 왜 못 쓰는지 물을 데가
 * 없어진다. 해상도는 카메라가 수동 제어를 지원하지 않아도 고를 수 있어 사유 문구보다 위에 둔다.
 * 가로 화면에서 항목이 한 번에 들어가지 않는 기기가 있어 잘라내는 대신 굴린다.
 */
@Composable
internal fun CaptureCameraPanel(
    state: ManualCameraUiState,
    resolution: RecordingResolution,
    enabled: Boolean,
    onChange: (ManualCameraConfig) -> Unit,
    onResolutionChange: (RecordingResolution) -> Unit,
    onFixWhiteBalance: () -> Unit,
    onClearWhiteBalance: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PanelHeader(onClose)
        ResolutionRow(resolution, enabled, onResolutionChange)
        val capabilities = state.capabilities
        val config = state.config
        val reason = state.unsupportedReason
        if (capabilities == null || config == null || reason != null) {
            PanelNote(stringResource(reason?.labelRes ?: R.string.capture_camera_reading))
        } else {
            FocusRow(capabilities, config, enabled, onChange)
            IsoRow(capabilities, config, enabled, onChange)
            ShutterRow(capabilities, config, enabled, onChange)
            PanelNote(stringResource(R.string.capture_camera_fps_fixed, RecordingFormat.TARGET_FPS))
            WhiteBalanceRow(state, enabled, onFixWhiteBalance, onClearWhiteBalance)
        }
    }
}

/**
 * 패널 제목과 닫기. 값은 바꾸는 즉시 걸리므로 확정할 것이 없다. "적용"이라 부르면 누르기 전에는 안
 * 걸린 것처럼 읽혀서 닫기만 둔다.
 */
@Composable
private fun PanelHeader(onClose: () -> Unit) {
    val closeDescription = stringResource(R.string.capture_camera_close)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.capture_camera_title), style = TigerText.overlayBadge)
        IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = closeDescription }) {
            Text(text = stringResource(R.string.control_close), style = TigerText.overlayGlyph, color = Color.White)
        }
    }
}

/**
 * 이번 Session으로 녹화할 해상도.
 *
 * 후보는 실기기에서 확인한 ARCore Camera config의 `textureSize`이며, 순서는
 * [RecordingFormat.supportedResolutions]를 따른다.
 */
@Composable
private fun ResolutionRow(
    resolution: RecordingResolution,
    enabled: Boolean,
    onChange: (RecordingResolution) -> Unit,
) {
    PanelLabel(label = stringResource(R.string.capture_camera_resolution), value = resolutionLabel(resolution))
    PanelChoices {
        RecordingFormat.supportedResolutions.forEach { option ->
            PanelChoice(
                label = resolutionLabel(option),
                selected = option == resolution,
                enabled = enabled,
                onClick = { onChange(option) },
            )
        }
    }
}

@Composable
private fun resolutionLabel(resolution: RecordingResolution): String =
    stringResource(R.string.capture_resolution_option, resolution.width, resolution.height)

/** 초점 거리. 0 D가 무한대, 최대 diopter가 최단 거리라 슬라이더를 오른쪽으로 밀수록 가까워진다. */
@Composable
private fun FocusRow(
    capabilities: ManualCameraCapabilities,
    config: ManualCameraConfig,
    enabled: Boolean,
    onChange: (ManualCameraConfig) -> Unit,
) {
    PanelLabel(
        label = stringResource(R.string.capture_camera_focus),
        value = stringResource(R.string.capture_camera_focus_value, config.focusDistanceDiopter),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        PanelNote(stringResource(R.string.capture_camera_focus_far))
        Slider(
            value = config.focusDistanceDiopter,
            onValueChange = { onChange(config.copy(focusDistanceDiopter = it)) },
            valueRange = 0f..capabilities.maxFocusDiopter.coerceAtLeast(MIN_SLIDER_SPAN),
            enabled = enabled && capabilities.focusSupported,
            modifier = Modifier.weight(1f),
        )
        PanelNote(stringResource(R.string.capture_camera_focus_near))
    }
    if (!capabilities.focusSupported) {
        Text(
            text = stringResource(R.string.capture_camera_focus_unsupported),
            style = TigerText.overlaySupporting,
            color = CaptureControlDisabled,
        )
    }
}

@Composable
private fun IsoRow(
    capabilities: ManualCameraCapabilities,
    config: ManualCameraConfig,
    enabled: Boolean,
    onChange: (ManualCameraConfig) -> Unit,
) {
    val range = capabilities.isoRange
    PanelLabel(
        label = stringResource(R.string.capture_camera_iso),
        value = stringResource(R.string.capture_camera_iso_value, config.iso),
    )
    Slider(
        value = config.iso.toFloat(),
        onValueChange = { onChange(config.copy(iso = it.toInt())) },
        valueRange = (range?.first?.toFloat() ?: 0f)..(range?.last?.toFloat() ?: 1f),
        enabled = enabled && capabilities.isoSupported,
    )
}

/**
 * 셔터 preset. 기기가 못 내는 셔터와 30 fps 프레임 간격을 넘는 셔터는 끈다. 노출이 프레임 간격보다
 * 길면 센서가 간격을 늘려 30 fps가 깨진다.
 */
@Composable
private fun ShutterRow(
    capabilities: ManualCameraCapabilities,
    config: ManualCameraConfig,
    enabled: Boolean,
    onChange: (ManualCameraConfig) -> Unit,
) {
    PanelLabel(
        label = stringResource(R.string.capture_camera_shutter),
        value = stringResource(R.string.capture_camera_shutter_value, config.exposureTimeNs / NANOS_PER_MICROSECOND),
    )
    PanelChoices {
        ShutterPreset.entries.forEach { preset ->
            PanelChoice(
                label = preset.label,
                selected = preset.exposureTimeNs == config.exposureTimeNs,
                enabled = enabled && capabilities.exposureSupported && capabilities.allows(preset),
                onClick = { onChange(config.copy(exposureTimeNs = preset.exposureTimeNs)) },
            )
        }
    }
}

/**
 * 화이트 밸런스는 AUTO로 수렴시킨 뒤 그 값을 붙잡는 것만 제공한다.
 *
 * Kelvin이나 RGB gain을 직접 고르는 화면은 만들지 않는다. 목적이 정확한 색을 지정하는 것이 아니라
 * 촬영 내내, 그리고 calibration 촬영과 dataset 수집 사이에 색이 변하지 않게 하는 것이기 때문이다.
 */
@Composable
private fun WhiteBalanceRow(
    state: ManualCameraUiState,
    enabled: Boolean,
    onFix: () -> Unit,
    onClear: () -> Unit,
) {
    val fixed = state.whiteBalanceFixed
    val actionable = enabled && state.capabilities?.whiteBalanceSupported == true
    PanelLabel(
        label = stringResource(R.string.capture_camera_white_balance),
        value =
            stringResource(
                if (fixed) R.string.capture_camera_white_balance_fixed else R.string.capture_camera_white_balance_auto,
            ),
    )
    OutlinedButton(onClick = if (fixed) onClear else onFix, enabled = actionable) {
        Text(
            text =
                stringResource(
                    if (fixed) R.string.capture_camera_white_balance_release else R.string.capture_camera_white_balance_hold,
                ),
            style = TigerText.overlayBadge,
            color = if (actionable) CaptureOverlaySupporting else CaptureControlDisabled,
        )
    }
}

/** 수동 설정을 다 쓸 수 있는 기기. */
@CameraSheetPreview
@Composable
private fun CaptureCameraPanelPreview() {
    val capabilities = sampleCapabilities(manualSensor = true)
    CaptureCameraPanelSample(ManualCameraUiState(capabilities, capabilities.defaultConfig(), panelOpen = true))
}

/** 수동 설정을 쓸 수 없는 기기. 조작 대신 사유가 서고 해상도만 고를 수 있다. */
@CameraSheetPreview
@Composable
private fun UnsupportedCaptureCameraPanelPreview() {
    CaptureCameraPanelSample(ManualCameraUiState(sampleCapabilities(manualSensor = false), panelOpen = true))
}

/** 기기 능력을 아직 읽는 중. */
@CameraSheetPreview
@Composable
private fun ReadingCaptureCameraPanelPreview() {
    CaptureCameraPanelSample(ManualCameraUiState(panelOpen = true))
}

@Composable
private fun CaptureCameraPanelSample(state: ManualCameraUiState) {
    ScreenPreview(background = colorResource(R.color.capture_camera_sheet)) {
        CaptureCameraPanel(
            state = state,
            resolution = RecordingFormat.DEFAULT_RESOLUTION,
            enabled = true,
            onChange = {},
            onResolutionChange = {},
            onFixWhiteBalance = {},
            onClearWhiteBalance = {},
            onClose = {},
        )
    }
}

private fun sampleCapabilities(manualSensor: Boolean) =
    ManualCameraCapabilities(
        cameraId = "0",
        manualSensor = manualSensor,
        aeOffSupported = true,
        afOffSupported = true,
        awbOffSupported = true,
        awbLockSupported = true,
        maxFocusDiopter = 10f,
        isoRange = 50..3200,
        exposureRangeNs = 100_000L..100_000_000L,
        maxFrameDurationNs = null,
    )

/** 시트의 크기. 폭은 `Widget.Tiger.CameraSheet`의 320dp, 높이는 시트가 가로 화면에서 받는 높이다. */
@Preview(widthDp = 320, heightDp = 411)
private annotation class CameraSheetPreview

/** 초점 슬라이더가 0 폭이 되지 않게 하는 최소 범위. 고정 초점 기기에서도 화면이 깨지지 않는다. */
private const val MIN_SLIDER_SPAN = 1f

private const val NANOS_PER_MICROSECOND = 1_000L
