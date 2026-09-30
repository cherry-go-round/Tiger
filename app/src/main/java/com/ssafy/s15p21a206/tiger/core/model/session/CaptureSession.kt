package com.ssafy.s15p21a206.tiger.core.model.session

import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RecordingState {
    INITIALIZING,
    READY,
    FINALIZING,
    COMPLETED,
    INTERRUPTED,
}

@Serializable
data class CaptureSession(
    @SerialName("session_id") val sessionId: String,
    @SerialName("display_number") val displayNumber: Int,
    val recordingState: RecordingState,
    val uploadState: UploadState,
    @SerialName("recording_start_monotonic_timestamp_ns") val recordingStartMonotonicTimestampNs: Long,
    @SerialName("recording_end_monotonic_timestamp_ns") val recordingEndMonotonicTimestampNs: Long? = null,
    @SerialName("bundle_path") val bundlePath: String,
    @SerialName("recorded_at_epoch_ms") val recordedAtEpochMs: Long = 0L,
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
