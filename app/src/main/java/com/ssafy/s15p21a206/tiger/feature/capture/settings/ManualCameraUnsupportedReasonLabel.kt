package com.ssafy.s15p21a206.tiger.feature.capture.settings

import androidx.annotation.StringRes
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.model.capture.ManualCameraUnsupportedReason

/** 수동 설정을 쓸 수 없는 이유를 카메라 설정 시트의 문구로 옮긴다. */
@get:StringRes
internal val ManualCameraUnsupportedReason.labelRes: Int
    get() =
        when (this) {
            ManualCameraUnsupportedReason.NO_MANUAL_SENSOR -> R.string.capture_camera_unsupported_manual_sensor
            ManualCameraUnsupportedReason.NO_MANUAL_EXPOSURE -> R.string.capture_camera_unsupported_manual_exposure
            ManualCameraUnsupportedReason.ISO_RANGE_UNKNOWN -> R.string.capture_camera_unsupported_iso_range
            ManualCameraUnsupportedReason.EXPOSURE_RANGE_UNKNOWN -> R.string.capture_camera_unsupported_shutter_range
        }
