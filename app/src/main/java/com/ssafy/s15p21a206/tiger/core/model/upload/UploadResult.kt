package com.ssafy.s15p21a206.tiger.core.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface UploadResult {
    data object Uploaded : UploadResult

    data class Failed(
        val reason: String,
    ) : UploadResult
}

@Serializable data class RemoteReceipt(
    @SerialName("session_id") val sessionId: String,
    val result: String,
)
