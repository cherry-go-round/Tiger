package com.ssafy.s15p21a206.tiger.core.model.session

import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState

data class SessionSummary(
    val sessionId: String,
    val displayNumber: Int,
    val uploadState: UploadState,
    val recordedAtEpochMs: Long,
    val recordingStartMonotonicTimestampNs: Long,
    val recordingEndMonotonicTimestampNs: Long?,
    val bundlePath: String,
    val completedEpisodeCount: Int,
    val taskName: String = "",
    val objectName: String = "",
)
