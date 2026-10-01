package com.ssafy.s15p21a206.tiger.core.database

data class SessionSummaryEntity(
    val sessionId: String,
    val displayNumber: Int,
    val uploadState: String,
    val recordedAtEpochMs: Long,
    val recordingStartNs: Long,
    val recordingEndNs: Long?,
    val bundlePath: String,
    val completedEpisodeCount: Int,
    val taskName: String = "",
    val objectName: String = "",
)
