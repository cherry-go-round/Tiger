package com.ssafy.s15p21a206.tiger.feature.session

import com.ssafy.s15p21a206.tiger.core.model.session.SessionSummary
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState

/**
 * 조회 화면 Preview가 그리는 Session. 전송 상태마다 하나씩 두고 Task 둘에 나눈다. 저장소가 주는 것처럼
 * 최근에 찍은 것이 먼저 오고, 나중에 찍을수록 번호가 크다.
 *
 * 번들 경로는 없는 곳이라 영상 자리에는 영상이 없다는 문구가 그려진다.
 */
internal object SessionPreviewSamples {
    val sessions: List<SessionSummary> =
        listOf(
            sample("9c0a11e8-local", 4, UploadState.LOCAL_ONLY, "컵 집기", "머그컵"),
            sample("4b7d02f1-uploading", 3, UploadState.UPLOADING, "컵 집기", "종이컵"),
            sample("e31f6c90-failed", 2, UploadState.FAILED, "서랍 열기", ""),
            sample("70ad5b2c-uploaded", 1, UploadState.UPLOADED, "서랍 열기", "책상 서랍"),
        )

    fun withState(uploadState: UploadState): SessionSummary = sessions.first { it.uploadState == uploadState }

    private fun sample(
        sessionId: String,
        displayNumber: Int,
        uploadState: UploadState,
        taskName: String,
        objectName: String,
    ) = SessionSummary(
        sessionId = sessionId,
        displayNumber = displayNumber,
        uploadState = uploadState,
        recordedAtEpochMs = FIRST_RECORDED_AT_EPOCH_MS + (displayNumber - 1) * HOUR_MS,
        recordingStartMonotonicTimestampNs = 0L,
        recordingEndMonotonicTimestampNs = DURATION_NS,
        bundlePath = "/preview/$sessionId",
        completedEpisodeCount = displayNumber,
        taskName = taskName,
        objectName = objectName,
    )

    /** 1번을 찍은 때. 2026-10-01 10:30 KST다. */
    private const val FIRST_RECORDED_AT_EPOCH_MS = 1_790_818_200_000L
    private const val HOUR_MS = 3_600_000L
    private const val DURATION_NS = 42_000_000_000L
}
