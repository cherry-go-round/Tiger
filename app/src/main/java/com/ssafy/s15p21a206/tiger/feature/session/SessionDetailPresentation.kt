package com.ssafy.s15p21a206.tiger.feature.session

import com.ssafy.s15p21a206.tiger.core.model.session.SessionSummary
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState

/** 상세 화면이 [SessionSummary]에서 끌어내는 값. 수집 길이와 지금 걸 수 있는 전송·삭제 동작이다. */
internal data class SessionDetailPresentation(
    val durationSeconds: Long,
    val uploadAction: UploadAction?,
    val deleteAction: SessionDeleteAction?,
) {
    internal enum class UploadAction { Upload, Retry }

    companion object {
        fun from(summary: SessionSummary): SessionDetailPresentation {
            val endTimestamp = summary.recordingEndMonotonicTimestampNs ?: summary.recordingStartMonotonicTimestampNs
            return SessionDetailPresentation(
                durationSeconds = (endTimestamp - summary.recordingStartMonotonicTimestampNs) / NANOS_PER_SECOND,
                uploadAction =
                    when (summary.uploadState) {
                        UploadState.LOCAL_ONLY -> UploadAction.Upload
                        UploadState.FAILED -> UploadAction.Retry
                        UploadState.UPLOADING, UploadState.UPLOADED -> null
                    },
                deleteAction = SessionDeleteAction.from(summary.uploadState),
            )
        }

        private const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
