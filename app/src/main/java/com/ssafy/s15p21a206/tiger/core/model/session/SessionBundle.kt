package com.ssafy.s15p21a206.tiger.core.model.session

import java.io.File

data class SessionBundle(
    val sessionId: String,
    val displayNumber: Int,
    val displayName: String,
    val directory: File,
) {
    val mainVideo = File(directory, MAIN_VIDEO_FILE)
    val mainFrameTimestamps = File(directory, MAIN_FRAME_TIMESTAMPS_FILE)
    val accelerometer = File(directory, ACCELEROMETER_FILE)
    val gyroscope = File(directory, GYROSCOPE_FILE)
    val rotationVector = File(directory, ROTATION_VECTOR_FILE)
    val arcorePoses = File(directory, ARCORE_POSES_FILE)
    val episodes = File(directory, EPISODES_FILE)
    val metadata = File(directory, METADATA_FILE)

    companion object {
        const val MAIN_VIDEO_FILE = "main_rgb.mp4"
        const val MAIN_FRAME_TIMESTAMPS_FILE = "main_frame_timestamps.csv"
        const val ACCELEROMETER_FILE = "accelerometer.csv"
        const val GYROSCOPE_FILE = "gyroscope.csv"
        const val ROTATION_VECTOR_FILE = "rotation_vector.csv"
        const val ARCORE_POSES_FILE = "arcore_poses.csv"
        const val EPISODES_FILE = "episodes.csv"
        const val ULTRAWIDE_VIDEO_FILE = "ultrawide_rgb.mp4"
        const val ULTRAWIDE_FRAME_TIMESTAMPS_FILE = "ultrawide_frame_timestamps.csv"
        const val METADATA_FILE = "metadata.json"

        // CSV 헤더. 기록하는 쪽이 적고 마감할 때 SessionBundleValidator가 같은 값으로 검사한다.
        // 수신 측과의 계약이므로 specs/001-episode-recorder/contracts/episode-bundle.md와 함께 바꾼다.
        const val FRAME_TIMESTAMPS_HEADER = "frame_number,timestamp_ns,timestamp_source"
        const val ACCELEROMETER_HEADER = "timestamp_ns,x,y,z,accuracy"
        const val GYROSCOPE_HEADER = "timestamp_ns,x,y,z,accuracy"
        const val ROTATION_VECTOR_HEADER = "timestamp_ns,x,y,z,scalar_component,heading_accuracy_rad,accuracy"
        const val ARCORE_POSES_HEADER =
            "android_camera_timestamp_ns,tx,ty,tz,qx,qy,qz,qw,tracking_state,tracking_failure_reason"
        const val EPISODES_HEADER = "episode_id,start_timestamp_ns,end_timestamp_ns,task,object,outcome"
    }
}

sealed interface BundleValidationResult {
    val isValid: Boolean

    data object Valid : BundleValidationResult {
        override val isValid = true
    }

    data class Invalid(
        val reason: String,
    ) : BundleValidationResult {
        override val isValid = false
    }
}

sealed interface FinalizeResult {
    data class Completed(
        val directory: File,
        val checksums: Map<String, String>,
    ) : FinalizeResult

    data class Failed(
        val reason: String,
    ) : FinalizeResult
}
