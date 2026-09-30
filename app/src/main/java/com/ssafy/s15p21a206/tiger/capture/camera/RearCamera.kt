package com.ssafy.s15p21a206.tiger.capture.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/**
 * 첫 후면 카메라의 id. 없으면 null이다.
 *
 * 카메라 서비스가 응답하지 않으면 목록 조회도 던진다. 그때도 후면 카메라를 찾지 못한 것으로 다룬다.
 */
internal fun CameraManager.firstRearCameraId(): String? =
    runCatching {
        cameraIdList.firstOrNull { id ->
            getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()
