package com.ssafy.s15p21a206.tiger.core.model.capture

import kotlinx.serialization.Serializable

@Serializable data class RecordingResolution(
    val width: Int,
    val height: Int,
)

object RecordingFormat {
    /** 카메라 설정 시트에 보여 주는 순서와 같다. 첫 항목이 기본값이다. */
    val supportedResolutions = listOf(RecordingResolution(1920, 1080), RecordingResolution(1280, 720))
    val DEFAULT_RESOLUTION = supportedResolutions.first()

    /**
     * 유일하게 지원하는 촬영 frame rate.
     *
     * 대상 기기의 상한이기도 하다. 후면 Camera가 알리는 AE target FPS 범위의 최댓값이 30이고,
     * ARCore가 내놓는 Camera config 후보도 모두 30이다.
     */
    const val TARGET_FPS = 30
}
