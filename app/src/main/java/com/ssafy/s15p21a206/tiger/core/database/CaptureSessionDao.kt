package com.ssafy.s15p21a206.tiger.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureSessionDao {
    /**
     * 목록 정렬은 절대 시각인 [CaptureSessionEntity.recordedAtEpochMs]를 기준으로 한다.
     * `recordingStartNs`는 부팅 이후 경과 시간이라 재부팅하면 0으로 돌아가므로,
     * 재부팅 경계를 걸친 Session끼리 순서가 뒤섞인다. 화면에 표시하는 수집 시각과 같은 값이어야 한다.
     *
     * Task와 Object는 `sessions` 행에서 그대로 읽는다. 전에는 `episode_markers`의 최솟값으로
     * 역산했는데, 그러면 Episode가 하나도 없는 Session에서 두 이름이 빈 문자열로 사라졌다.
     * Episode 조인은 이제 개수를 세기 위해서만 남는다.
     */
    @Query(
        """
        SELECT sessions.sessionId, sessions.displayNumber, sessions.uploadState,
               sessions.recordedAtEpochMs, sessions.recordingStartNs, sessions.recordingEndNs,
               sessions.bundlePath,
               COUNT(CASE WHEN episode_markers.outcome = 'COMPLETED' THEN 1 END) AS completedEpisodeCount,
               sessions.task AS taskName, sessions.objectName AS objectName
        FROM sessions
        LEFT JOIN episode_markers ON episode_markers.sessionId = sessions.sessionId
        WHERE sessions.recordingState = 'COMPLETED'
        GROUP BY sessions.sessionId
        ORDER BY sessions.recordedAtEpochMs DESC
        """,
    )
    fun observeCompletedSummaries(): Flow<List<SessionSummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: CaptureSessionEntity)

    @Query("UPDATE sessions SET uploadState = :uploadState WHERE sessionId = :sessionId")
    suspend fun updateUploadState(
        sessionId: String,
        uploadState: String,
    )

    @Query("UPDATE sessions SET uploadState = 'FAILED' WHERE uploadState = 'UPLOADING'")
    suspend fun failInterruptedUploads()

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId AND recordingState = 'COMPLETED' LIMIT 1")
    suspend fun completedSession(sessionId: String): CaptureSessionEntity?

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun session(sessionId: String): CaptureSessionEntity?

    /**
     * 마감되지 않은 Session. 구제 대상이므로 이전 실행에서 `INTERRUPTED`로 남은 것도 포함한다.
     * 한 번 중단으로 표시됐더라도 번들이 온전하면 다음 실행에서 정상 마감할 수 있다.
     */
    @Query("SELECT * FROM sessions WHERE recordingState IN ('INITIALIZING', 'READY', 'FINALIZING', 'INTERRUPTED')")
    suspend fun recoverableSessions(): List<CaptureSessionEntity>

    @Query("SELECT * FROM sessions ORDER BY recordedAtEpochMs ASC, recordingStartNs ASC, sessionId ASC")
    suspend fun sessionsInCaptureOrder(): List<CaptureSessionEntity>

    @Query("UPDATE sessions SET displayNumber = :displayNumber WHERE sessionId = :sessionId")
    suspend fun updateDisplayNumber(
        sessionId: String,
        displayNumber: Int,
    )

    @Query("SELECT COALESCE(MAX(displayNumber), 0) + 1 FROM sessions")
    suspend fun nextDisplayNumber(): Int

    @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)

    /**
     * 색인이 아는 모든 Session의 id. 마감 여부를 가리지 않는다.
     *
     * 고아 번들 회수에 쓴다. 아직 마감되지 않은 Session의 디렉터리를 고아로 오인해 지우면 수집
     * 중인 데이터가 사라지므로, `COMPLETED`만 세어서는 안 된다.
     */
    @Query("SELECT sessionId FROM sessions")
    suspend fun allSessionIds(): List<String>
}
