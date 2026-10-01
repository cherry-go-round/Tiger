package com.ssafy.s15p21a206.tiger.core.upload

import com.ssafy.s15p21a206.tiger.core.model.session.CaptureSession
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState

interface UploadSessionStore {
    suspend fun completedSource(sessionId: String): CaptureSession?

    suspend fun updateUploadState(
        sessionId: String,
        state: UploadState,
    )
}
