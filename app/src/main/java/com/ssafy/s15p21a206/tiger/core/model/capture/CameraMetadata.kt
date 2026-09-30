package com.ssafy.s15p21a206.tiger.core.model.capture

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 실제 촬영에 사용된 Camera의 식별자·해상도·Intrinsic.
 *
 * 필수 값은 녹화와 같은 ARCore GPU 텍스처 스트림(`textureIntrinsics`)에서 얻으므로 촬영 해상도와 대응이
 * 보장된다.
 * nullable 필드는 기기가 제공할 때만 채운다. 값을 억지로 계산해 채우지 않는다.
 */
@Serializable
data class CameraMetadata(
    @SerialName("camera_id") val cameraId: String,
    @SerialName("image_width") val imageWidth: Int,
    @SerialName("image_height") val imageHeight: Int,
    val fx: Float,
    val fy: Float,
    val cx: Float,
    val cy: Float,
    @SerialName("focal_length_mm") val focalLengthMm: Float? = null,
    @SerialName("sensor_width_mm") val sensorWidthMm: Float? = null,
    @SerialName("sensor_height_mm") val sensorHeightMm: Float? = null,
    @SerialName("distortion_coefficients") val distortionCoefficients: List<Float>? = null,
    /** `main_rgb.mp4`에 적용된 시계 방향 회전. 이 값을 반영한 뒤의 기하가 위 필드에 담긴다. */
    @SerialName("video_rotation_degrees") val videoRotationDegrees: Int = 0,
)

data class CameraOptics(
    val focalLengthMm: Float? = null,
    val sensorWidthMm: Float? = null,
    val sensorHeightMm: Float? = null,
    val distortionCoefficients: List<Float>? = null,
)
