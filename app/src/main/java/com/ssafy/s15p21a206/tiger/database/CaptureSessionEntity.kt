package com.ssafy.s15p21a206.tiger.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class CaptureSessionEntity(
    @PrimaryKey val sessionId: String,
    val displayNumber: Int,
    val recordingState: String,
    val uploadState: String,
    val recordingStartNs: Long,
    val recordingEndNs: Long?,
    val bundlePath: String,
    /**
     * 영상이 만들어진 벽시계 시각. 녹화를 멈춰 MP4가 완성되는 순간(마감·중단)에 기록한다.
     *
     * 목록·상세의 수집 시각이자 정렬 기준이다. 수집 길이는 [recordingStartNs]와 [recordingEndNs]로 잰다.
     * 버전 7까지는 `recordingStartEpochMs`라는 이름이었고 [MIGRATION_7_8]이 값을 그대로 옮긴다.
     */
    val recordedAtEpochMs: Long = 0L,
    /**
     * 수집을 시작할 때 입력받은 Task와 Object.
     *
     * 한 Session의 모든 Episode가 같은 값을 갖는 Session 속성이므로 여기에 둔다. Episode 행에서
     * 역산하던 때에는 Episode가 0개인 Session에서 두 이름이 사라졌다. 입력받은 자리에 저장하면
     * Episode 유무와 무관해진다.
     */
    val task: String = "",
    val objectName: String = "",
)
