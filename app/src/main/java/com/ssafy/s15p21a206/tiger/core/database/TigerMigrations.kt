package com.ssafy.s15p21a206.tiger.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            EXPORT_MIGRATION_SQL.forEach(db::execSQL)
        }
    }

val EXPORT_MIGRATION_SQL =
    listOf(
        "ALTER TABLE sessions ADD COLUMN exportState TEXT NOT NULL DEFAULT 'NOT_EXPORTED'",
        "ALTER TABLE sessions ADD COLUMN exportTreeUri TEXT",
        "ALTER TABLE sessions ADD COLUMN exportFailureReason TEXT",
    )

val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(RECORDING_START_EPOCH_MIGRATION_SQL)
        }
    }

const val RECORDING_START_EPOCH_MIGRATION_SQL =
    "ALTER TABLE sessions ADD COLUMN recordingStartEpochMs INTEGER NOT NULL DEFAULT 0"

val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            SESSION_METADATA_MIGRATION_SQL.forEach(db::execSQL)
        }
    }

/**
 * Task와 Object를 Session 행으로 옮긴다.
 *
 * 컬럼을 더하는 것만으로는 안 된다. 조회가 `episode_markers` 집계를 떠나 `sessions.task`를 읽으므로,
 * 채워 넣지 않으면 이미 쌓인 Session이 모두 이름 없는 Task로 떨어진다. 이 이관이 고치려는 증상을
 * 과거 데이터 전체에 되풀이하는 셈이다. 그래서 같은 이관 안에서 Episode 행의 값으로 채운다.
 *
 * 채우는 식은 떠나는 집계와 같다. Episode가 없는 Session은 채울 값이 없어 빈 문자열로 남는다.
 * 그 행의 두 이름은 애초에 어디에도 저장된 적이 없다.
 */
val SESSION_METADATA_MIGRATION_SQL =
    listOf(
        "ALTER TABLE sessions ADD COLUMN task TEXT NOT NULL DEFAULT ''",
        "ALTER TABLE sessions ADD COLUMN objectName TEXT NOT NULL DEFAULT ''",
        """
        UPDATE sessions SET
            task = COALESCE((
                SELECT MIN(NULLIF(episode_markers.task, ''))
                FROM episode_markers WHERE episode_markers.sessionId = sessions.sessionId
            ), ''),
            objectName = COALESCE((
                SELECT MIN(NULLIF(episode_markers.objectName, ''))
                FROM episode_markers WHERE episode_markers.sessionId = sessions.sessionId
            ), '')
        """,
    )

val MIGRATION_5_6 =
    object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            EXPORT_REMOVAL_MIGRATION_SQL.forEach(db::execSQL)
        }
    }

/**
 * SAF 내보내기를 걷어 내면서 그 세 컬럼을 지운다.
 *
 * `DROP COLUMN`을 쓰지 않는다. SQLite가 그것을 받는 것은 3.35부터이고 minSdk 28의 기기에는 더 낮은
 * 버전이 실린다. 테이블을 다시 만들어 옮긴다.
 *
 * 컬럼을 남겨 두는 쪽이 싸 보이지만 그렇지 않다. Room은 엔티티에서 만든 스키마와 실제 스키마를
 * 견주므로, 엔티티에서만 빼면 불일치로 잡힌다. 남기려면 쓰지 않는 필드를 엔티티에 영구히 들고
 * 있어야 한다.
 *
 * 2→3이 이 컬럼들을 더하는 이관은 그대로 둔다. 버전 2에서 올라오는 기기는 여전히 그 길을 지나야
 * 하고, 이관 이력은 지난 일이라 고쳐 쓰는 것이 아니다.
 */
val EXPORT_REMOVAL_MIGRATION_SQL =
    listOf(
        """
        CREATE TABLE sessions_without_export (
            sessionId TEXT NOT NULL PRIMARY KEY,
            displayNumber INTEGER NOT NULL,
            recordingState TEXT NOT NULL,
            uploadState TEXT NOT NULL,
            recordingStartNs INTEGER NOT NULL,
            recordingEndNs INTEGER,
            bundlePath TEXT NOT NULL,
            recordingStartEpochMs INTEGER NOT NULL,
            task TEXT NOT NULL,
            objectName TEXT NOT NULL
        )
        """,
        """
        INSERT INTO sessions_without_export
        SELECT sessionId, displayNumber, recordingState, uploadState, recordingStartNs, recordingEndNs,
               bundlePath, recordingStartEpochMs, task, objectName
        FROM sessions
        """,
        "DROP TABLE sessions",
        "ALTER TABLE sessions_without_export RENAME TO sessions",
    )

val MIGRATION_6_7 =
    object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            CAPTURE_LOG_REMOVAL_MIGRATION_SQL.forEach(db::execSQL)
        }
    }

/**
 * 수집 로그 테이블을 지운다.
 *
 * `capture_logs`에 쓰는 코드는 만들어진 뒤 한 번도 배선되지 않았다. 읽는 곳도 없어 어느 기기에도
 * 행이 쌓여 있지 않다. 그래서 옮길 데이터가 없고 테이블을 그대로 떨어뜨린다.
 *
 * 엔티티에서만 빼고 테이블을 남기는 선택지는 없다. Room이 엔티티에서 만든 스키마와 실제 스키마를
 * 견주므로 불일치로 잡힌다. 5→6에서 내보내기 컬럼을 걷어낼 때와 같은 이유다.
 */
val CAPTURE_LOG_REMOVAL_MIGRATION_SQL = listOf("DROP TABLE IF EXISTS capture_logs")

val MIGRATION_7_8 =
    object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            RECORDED_AT_MIGRATION_SQL.forEach(db::execSQL)
        }
    }

/**
 * `recordingStartEpochMs`를 `recordedAtEpochMs`로 바꾼다. 값은 그대로 옮긴다.
 *
 * 이름은 "수집 시작 시각"이었지만 앱은 마감과 중단 때 그 순간의 벽시계 시각을 넣어 왔다. 그 순간은
 * MP4가 완성되는 때라 영상이 만들어진 시각이다. 저장된 값의 뜻은 이미 그것이므로 바꾸지 않고
 * 이름만 맞춘다.
 *
 * `RENAME COLUMN`을 쓰지 않는다. SQLite가 그것을 받는 것은 3.25부터이고 minSdk 28의 기기에는 더 낮은
 * 버전이 실린다. 5→6과 같이 테이블을 다시 만들어 옮긴다.
 */
val RECORDED_AT_MIGRATION_SQL =
    listOf(
        """
        CREATE TABLE sessions_recorded_at (
            sessionId TEXT NOT NULL PRIMARY KEY,
            displayNumber INTEGER NOT NULL,
            recordingState TEXT NOT NULL,
            uploadState TEXT NOT NULL,
            recordingStartNs INTEGER NOT NULL,
            recordingEndNs INTEGER,
            bundlePath TEXT NOT NULL,
            recordedAtEpochMs INTEGER NOT NULL,
            task TEXT NOT NULL,
            objectName TEXT NOT NULL
        )
        """,
        """
        INSERT INTO sessions_recorded_at
        SELECT sessionId, displayNumber, recordingState, uploadState, recordingStartNs, recordingEndNs,
               bundlePath, recordingStartEpochMs, task, objectName
        FROM sessions
        """,
        "DROP TABLE sessions",
        "ALTER TABLE sessions_recorded_at RENAME TO sessions",
    )
