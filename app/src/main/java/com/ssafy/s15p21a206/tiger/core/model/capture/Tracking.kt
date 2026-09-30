package com.ssafy.s15p21a206.tiger.core.model.capture

import kotlinx.serialization.Serializable

@Serializable enum class TrackingState { INITIALIZING, READY, PAUSED }

/**
 * ARCore Tracking 관측 한 건.
 *
 * [observedAtNs]는 이 관측을 만든 프레임의 카메라 시각이며 `arcore_poses.csv`의
 * `android_camera_timestamp_ns`와 같은 값이다.
 */
data class TrackingSample(
    val isTracking: Boolean,
    val observedAtNs: Long,
)
