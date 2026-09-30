package com.ssafy.s15p21a206.tiger

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.ssafy.s15p21a206.tiger.core.database.MIGRATION_7_8
import com.ssafy.s15p21a206.tiger.core.database.TigerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `recordingStartEpochMs`를 `recordedAtEpochMs`로 바꾸는 이관.
 *
 * 열 이름이 엔티티와 맞는지가 핵심이라 SQL만 돌리지 않고 [TigerDatabase]를 Room으로 연다. Room은 이관을
 * 돌린 뒤 엔티티에서 만든 스키마와 실제 스키마를 견주고, 다르면 여는 순간 예외를 던진다. 값이 그대로
 * 옮겨졌는지와 목록 조회가 새 열로 정렬하는지도 함께 본다.
 */
class RecordedAtMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migrationKeepsTheWallClockValueUnderTheNewName() {
        createVersionSevenDatabase()

        val database =
            Room
                .databaseBuilder(context, TigerDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_7_8)
                .build()
        runBlocking {
            val dao = database.captureSessionDao()
            val older = requireNotNull(dao.session("older"))
            assertEquals(1_000L, older.recordedAtEpochMs)
            assertEquals("open", older.task)
            assertEquals("cup", older.objectName)
            assertEquals(20L, older.recordingEndNs)

            val summaries = dao.observeCompletedSummaries().first()
            assertEquals(listOf("newer", "older"), summaries.map { it.sessionId })
            assertEquals(listOf(2_000L, 1_000L), summaries.map { it.recordedAtEpochMs })
        }
        database.close()
    }

    /** 버전 7의 `sessions`(5→6 이관이 만든 모양)와 `episode_markers`에 Session 둘을 둔다. */
    private fun createVersionSevenDatabase() {
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(DATABASE_NAME)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(7) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    "CREATE TABLE sessions (sessionId TEXT NOT NULL PRIMARY KEY, displayNumber INTEGER NOT NULL, " +
                                        "recordingState TEXT NOT NULL, uploadState TEXT NOT NULL, recordingStartNs INTEGER NOT NULL, " +
                                        "recordingEndNs INTEGER, bundlePath TEXT NOT NULL, recordingStartEpochMs INTEGER NOT NULL, " +
                                        "task TEXT NOT NULL, objectName TEXT NOT NULL)",
                                )
                                db.execSQL(
                                    "CREATE TABLE episode_markers (episodeId TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, " +
                                        "startTimestampNs INTEGER NOT NULL, endTimestampNs INTEGER, task TEXT NOT NULL, " +
                                        "objectName TEXT NOT NULL, outcome TEXT NOT NULL)",
                                )
                                db.execSQL(
                                    "INSERT INTO sessions VALUES ('older', 1, 'COMPLETED', 'LOCAL_ONLY', 10, 20, " +
                                        "'/bundles/older', 1000, 'open', 'cup')",
                                )
                                db.execSQL(
                                    "INSERT INTO sessions VALUES ('newer', 2, 'COMPLETED', 'UPLOADED', 30, 40, " +
                                        "'/bundles/newer', 2000, 'pick', 'block')",
                                )
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    ).build(),
            )
        helper.writableDatabase
        helper.close()
    }

    private companion object {
        const val DATABASE_NAME = "recorded-at-migration-test.db"
    }
}
