package com.ssafy.s15p21a206.tiger.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episode_markers")
data class EpisodeMarkerEntity(
    @PrimaryKey val episodeId: String,
    val sessionId: String,
    val startTimestampNs: Long,
    val endTimestampNs: Long?,
    val task: String,
    val objectName: String,
    val outcome: String,
)
