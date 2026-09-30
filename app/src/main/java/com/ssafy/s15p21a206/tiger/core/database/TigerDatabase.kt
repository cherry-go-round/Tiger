package com.ssafy.s15p21a206.tiger.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CaptureSessionEntity::class, EpisodeMarkerEntity::class], version = 8, exportSchema = false)
abstract class TigerDatabase : RoomDatabase() {
    abstract fun captureSessionDao(): CaptureSessionDao

    abstract fun episodeMarkerDao(): EpisodeMarkerDao
}
