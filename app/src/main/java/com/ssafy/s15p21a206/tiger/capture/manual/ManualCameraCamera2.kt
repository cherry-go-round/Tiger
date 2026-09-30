package com.ssafy.s15p21a206.tiger.capture.manual

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.util.Log
import com.ssafy.s15p21a206.tiger.capture.CAPTURE_LOG_TAG
import com.ssafy.s15p21a206.tiger.core.model.capture.ActualCaptureSettings
import com.ssafy.s15p21a206.tiger.core.model.capture.FixedWhiteBalance
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraCapabilities
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraConfig
import kotlin.math.abs
import android.hardware.camera2.CameraMetadata as Camera2Metadata

/*
 * ManualCameraConfig와 Camera2 사이의 경계.
 *
 * 순수 값으로 든 설정을 여기서만 Camera2 key로 바꾼다. 나머지 코드가 Camera2 타입을 모르게 하려는
 * 것이며, 그래야 설정 계산과 화면을 단위 테스트로 덮을 수 있다.
 */

/** 실제 녹화에 쓰일 카메라의 수동 제어 능력을 읽는다. 읽지 못한 항목은 null·false로 둔다. */
fun CameraManager.readManualCameraCapabilities(cameraId: String): ManualCameraCapabilities {
    val characteristics = getCameraCharacteristics(cameraId)
    return ManualCameraCapabilities(
        cameraId = cameraId,
        manualSensor =
            characteristics.has(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,
                Camera2Metadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR,
            ),
        aeOffSupported = characteristics.has(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, Camera2Metadata.CONTROL_AE_MODE_OFF),
        afOffSupported = characteristics.has(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, Camera2Metadata.CONTROL_AF_MODE_OFF),
        awbOffSupported = characteristics.has(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES, Camera2Metadata.CONTROL_AWB_MODE_OFF),
        awbLockSupported = characteristics.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE) == true,
        maxFocusDiopter = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f,
        isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let { it.lower..it.upper },
        exposureRangeNs = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let { it.lower..it.upper },
        maxFrameDurationNs = characteristics.get(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION),
    )
}

/** [key]가 가리키는 목록에 [value]가 있는지. 기기가 목록을 주지 않으면 없는 것으로 본다. */
private fun CameraCharacteristics.has(
    key: CameraCharacteristics.Key<IntArray>,
    value: Int,
): Boolean = get(key)?.contains(value) == true

/**
 * 수동 설정을 요청에 건다. [config]가 null이면 아무것도 걸지 않아 template의 auto가 그대로 남는다.
 *
 * 촬영 도중 값이 흔들리면 안 되므로 auto를 하나도 남기지 않는다.
 */
fun CaptureRequest.Builder.applyManualCamera(config: ManualCameraConfig?) {
    if (config == null) return
    fixFocus(config)
    fixExposure(config)
    disableStabilization()
    applyWhiteBalance(config.whiteBalance)
}

/** AF를 끄고 렌즈를 고른 거리에 둔다. */
private fun CaptureRequest.Builder.fixFocus(config: ManualCameraConfig) {
    set(CaptureRequest.CONTROL_AF_MODE, Camera2Metadata.CONTROL_AF_MODE_OFF)
    set(CaptureRequest.LENS_FOCUS_DISTANCE, config.focusDistanceDiopter)
}

/** AE를 끄고 감도·노출·프레임 간격을 직접 건다. */
private fun CaptureRequest.Builder.fixExposure(config: ManualCameraConfig) {
    set(CaptureRequest.CONTROL_AE_MODE, Camera2Metadata.CONTROL_AE_MODE_OFF)
    set(CaptureRequest.SENSOR_SENSITIVITY, config.iso)
    set(CaptureRequest.SENSOR_EXPOSURE_TIME, config.exposureTimeNs)
    set(CaptureRequest.SENSOR_FRAME_DURATION, config.frameDurationNs)
}

/** OIS와 EIS를 끈다. 둘 다 프레임마다 intrinsic을 움직여, 같은 조건으로 찍었다는 전제를 깬다. */
private fun CaptureRequest.Builder.disableStabilization() {
    set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, Camera2Metadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
    set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, Camera2Metadata.LENS_OPTICAL_STABILIZATION_MODE_OFF)
}

/** 고정한 값이 있으면 그 gain·transform으로 고정하고, 없으면 AUTO로 둔다. */
private fun CaptureRequest.Builder.applyWhiteBalance(whiteBalance: FixedWhiteBalance?) {
    if (whiteBalance == null) {
        // 아직 고정하지 않았다. 프리뷰가 수렴해야 고정할 값이 생기므로 AUTO로 둔다.
        set(CaptureRequest.CONTROL_AWB_MODE, Camera2Metadata.CONTROL_AWB_MODE_AUTO)
    } else {
        set(CaptureRequest.CONTROL_AWB_MODE, Camera2Metadata.CONTROL_AWB_MODE_OFF)
        set(CaptureRequest.COLOR_CORRECTION_MODE, Camera2Metadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
        set(CaptureRequest.COLOR_CORRECTION_GAINS, whiteBalance.toGains())
        set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, ColorSpaceTransform(whiteBalance.transform.toIntArray()))
    }
}

private fun FixedWhiteBalance.toGains() = RggbChannelVector(redGain, greenEvenGain, greenOddGain, blueGain)

/** 이 요청이 우리가 건 수동 요청인지. ARCore가 제 요청으로 갈아 끼웠는지 가리는 데 쓴다. */
fun CaptureRequest.carriesManualCamera(): Boolean = get(CaptureRequest.CONTROL_AE_MODE) == Camera2Metadata.CONTROL_AE_MODE_OFF

/** 프리뷰가 수렴시킨 화이트 밸런스를 읽는다. 둘 중 하나라도 없으면 null이다. */
fun CaptureResult.readConvergedWhiteBalance(): FixedWhiteBalance? {
    val gains = get(CaptureResult.COLOR_CORRECTION_GAINS) ?: return null
    val transform = get(CaptureResult.COLOR_CORRECTION_TRANSFORM) ?: return null
    val elements = IntArray(FixedWhiteBalance.TRANSFORM_SIZE)
    transform.copyElements(elements, 0)
    return FixedWhiteBalance(
        redGain = gains.red,
        greenEvenGain = gains.greenEven,
        greenOddGain = gains.greenOdd,
        blueGain = gains.blue,
        transform = elements.toList(),
    )
}

/**
 * 센서가 실제로 사용한 값.
 *
 * 요청에 넣었다고 그 값으로 찍혔다고 보지 않는다. 그 차이를 metadata에 남기는 것이 §7·§8의 요구다.
 */
fun CaptureResult.readAppliedCameraSettings(): ActualCaptureSettings =
    ActualCaptureSettings(
        focusDistanceDiopter = get(CaptureResult.LENS_FOCUS_DISTANCE),
        iso = get(CaptureResult.SENSOR_SENSITIVITY),
        exposureTimeNs = get(CaptureResult.SENSOR_EXPOSURE_TIME),
        frameDurationNs = get(CaptureResult.SENSOR_FRAME_DURATION),
        afMode = afModeName(get(CaptureResult.CONTROL_AF_MODE)),
        aeMode = aeModeName(get(CaptureResult.CONTROL_AE_MODE)),
        awbMode = awbModeName(get(CaptureResult.CONTROL_AWB_MODE)),
        awbLocked = get(CaptureResult.CONTROL_AWB_LOCK),
    )

private fun afModeName(mode: Int?) =
    when (mode) {
        null -> null
        Camera2Metadata.CONTROL_AF_MODE_OFF -> MODE_OFF
        Camera2Metadata.CONTROL_AF_MODE_AUTO -> "AUTO"
        Camera2Metadata.CONTROL_AF_MODE_MACRO -> "MACRO"
        Camera2Metadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "CONTINUOUS_VIDEO"
        Camera2Metadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "CONTINUOUS_PICTURE"
        Camera2Metadata.CONTROL_AF_MODE_EDOF -> "EDOF"
        else -> "UNKNOWN($mode)"
    }

private fun aeModeName(mode: Int?) =
    when (mode) {
        null -> null
        Camera2Metadata.CONTROL_AE_MODE_OFF -> MODE_OFF
        Camera2Metadata.CONTROL_AE_MODE_ON -> "ON"
        else -> "ON_VARIANT($mode)"
    }

private fun awbModeName(mode: Int?) =
    when (mode) {
        null -> null
        Camera2Metadata.CONTROL_AWB_MODE_OFF -> MODE_OFF
        Camera2Metadata.CONTROL_AWB_MODE_AUTO -> "AUTO"
        else -> "PRESET($mode)"
    }

/**
 * 요청값과 실제값이 어긋났으면 남긴다.
 *
 * 노출과 프레임 간격은 센서가 µs로 반올림해 돌려주므로 그만큼은 어긋난 것으로 세지 않는다.
 */
fun logCameraSettingMismatch(
    requested: ManualCameraConfig,
    actual: ActualCaptureSettings,
) {
    val mismatches =
        buildList {
            if (actual.afMode != MODE_OFF) add("af_mode=${actual.afMode}")
            if (actual.aeMode != MODE_OFF) add("ae_mode=${actual.aeMode}")
            if (requested.awbFixed && actual.awbMode != MODE_OFF) add("awb_mode=${actual.awbMode}")
            addIfDiffers("focus", requested.focusDistanceDiopter, actual.focusDistanceDiopter, FOCUS_TOLERANCE_DIOPTER)
            addIfDiffers("iso", requested.iso, actual.iso, tolerance = 0)
            addIfDiffers("exposure", requested.exposureTimeNs, actual.exposureTimeNs, NS_QUANTIZATION_TOLERANCE)
            addIfDiffers("frame_duration", requested.frameDurationNs, actual.frameDurationNs, NS_QUANTIZATION_TOLERANCE)
        }
    if (mismatches.isEmpty()) {
        Log.i(CAPTURE_LOG_TAG, "Manual camera settings applied as requested: $requested")
    } else {
        Log.w(CAPTURE_LOG_TAG, "Manual camera settings differ from the request: ${mismatches.joinToString("; ")}")
    }
}

/** 센서가 값을 돌려줬고 요청과 [tolerance]보다 크게 다르면 `name=실제 (requested 요청)`을 더한다. */
private fun MutableList<String>.addIfDiffers(
    name: String,
    requested: Number,
    actual: Number?,
    tolerance: Number,
) {
    if (actual == null) return
    if (abs(actual.toDouble() - requested.toDouble()) > tolerance.toDouble()) add("$name=$actual (requested $requested)")
}

private const val MODE_OFF = "OFF"

private const val FOCUS_TOLERANCE_DIOPTER = 0.05f

/** 센서가 µs 단위로 반올림해 돌려주는 만큼은 일치로 본다. */
private const val NS_QUANTIZATION_TOLERANCE = 2_000L
