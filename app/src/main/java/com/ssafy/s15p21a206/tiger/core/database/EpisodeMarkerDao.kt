package com.ssafy.s15p21a206.tiger.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EpisodeMarkerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(marker: EpisodeMarkerEntity)

    @Query("DELETE FROM episode_markers WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
