package com.ssafy.s15p21a206.tiger.episode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RecordingState {
    IDLE,
    INITIALIZING,
    READY,
    FINALIZING,
    COMPLETED,
    INTERRUPTED,
}

@Serializable
enum class EpisodeState {
    NONE,
    ACTIVE,
    COMPLETED,
    INVALID_TRACKING,
}

@Serializable enum class TrackingState { INITIALIZING, READY, PAUSED, STOPPED }

@Serializable enum class UploadState { LOCAL_ONLY, UPLOADING, UPLOADED, FAILED }

@Serializable
data class CaptureSession(
    @SerialName("session_id") val sessionId: String,
    @SerialName("display_number") val displayNumber: Int,
    val recordingState: RecordingState,
    val uploadState: UploadState,
    @SerialName("recording_start_monotonic_timestamp_ns") val recordingStartMonotonicTimestampNs: Long,
    @SerialName("recording_end_monotonic_timestamp_ns") val recordingEndMonotonicTimestampNs: Long? = null,
    @SerialName("bundle_path") val bundlePath: String,
    @SerialName("recording_start_epoch_ms") val recordingStartEpochMs: Long = 0L,
    /**
     * 수집 정보 입력에서 받은 두 이름. 기본값을 두지 않는다.
     *
     * 이 Session을 저장하는 모든 경로가 두 값을 명시하게 만드는 것이 이 필드의 목적이다. 빈
     * 문자열을 기본값으로 두면, 넘기는 것을 잊은 저장 경로가 조용히 이름 없는 Session을 남긴다.
     * 그것이 애초에 이 필드를 만든 사고였다. 과거 Session은 이관에서 채우지 못하면 빈 값을 갖지만,
     * 그것은 읽는 쪽의 사실이지 쓰는 쪽의 기본값이 아니다.
     */
    val task: String,
    @SerialName("object") val objectName: String,
)

data class SessionSummary(
    val sessionId: String,
    val displayNumber: Int,
    val uploadState: UploadState,
    val recordingStartEpochMs: Long,
    val recordingStartMonotonicTimestampNs: Long,
    val recordingEndMonotonicTimestampNs: Long?,
    val bundlePath: String,
    val completedEpisodeCount: Int,
    val taskName: String = "",
    val objectName: String = "",
)

@Serializable
data class EpisodeMarker(
    @SerialName("episode_id") val episodeId: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("start_timestamp_ns") val startTimestampNs: Long,
    @SerialName("end_timestamp_ns") val endTimestampNs: Long? = null,
    val task: String,
    @SerialName("object") val objectName: String,
    val outcome: EpisodeState,
)

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

@Serializable data class RecordingResolution(
    val width: Int,
    val height: Int,
)

@Serializable data class RemoteReceipt(
    @SerialName("session_id") val sessionId: String,
    val result: String,
)
