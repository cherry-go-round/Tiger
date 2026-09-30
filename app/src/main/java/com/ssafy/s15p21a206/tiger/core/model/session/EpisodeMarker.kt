package com.ssafy.s15p21a206.tiger.core.model.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EpisodeState {
    ACTIVE,
    COMPLETED,
    INVALID_TRACKING,
}

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
