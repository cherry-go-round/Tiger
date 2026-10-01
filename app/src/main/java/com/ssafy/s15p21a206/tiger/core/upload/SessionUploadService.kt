package com.ssafy.s15p21a206.tiger.core.upload

import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadResult
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState
import kotlinx.coroutines.CancellationException
import java.io.File

class SessionUploadService(
    private val store: UploadSessionStore,
    private val uploader: SessionUploadGateway,
) {
    suspend fun upload(sessionId: String): UploadResult {
        val session =
            store.completedSource(sessionId)
                ?: return UploadResult.Failed("Completed session is unavailable")
        store.updateUploadState(sessionId, UploadState.UPLOADING)
        val result =
            try {
                uploader.upload(
                    SessionBundle(
                        sessionId = session.sessionId,
                        displayNumber = session.displayNumber,
                        displayName = "session_%04d".format(session.displayNumber),
                        directory = File(session.bundlePath),
                    ),
                )
            } catch (error: CancellationException) {
                store.updateUploadState(sessionId, UploadState.FAILED)
                throw error
            }
        store.updateUploadState(
            sessionId,
            if (result == UploadResult.Uploaded) UploadState.UPLOADED else UploadState.FAILED,
        )
        return result
    }
}
