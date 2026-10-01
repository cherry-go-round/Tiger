package com.ssafy.s15p21a206.tiger.core.upload

import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID

class SessionUploadRequestFactory(
    private val baseUrl: String,
) {
    fun create(bundle: SessionBundle): Request {
        require(bundle.sessionId.isUuid()) { "Session ID must be a UUID" }
        val body =
            MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .apply {
                    add("metadata", bundle.metadata, "application/json")
                    add("main_video", bundle.mainVideo, "video/mp4")
                    add("main_frame_timestamps", bundle.mainFrameTimestamps, "text/csv")
                    add("accelerometer", bundle.accelerometer, "text/csv")
                    add("gyroscope", bundle.gyroscope, "text/csv")
                    add("rotation_vector", bundle.rotationVector, "text/csv")
                    add("arcore_poses", bundle.arcorePoses, "text/csv")
                    add("episodes", bundle.episodes, "text/csv")
                    if (metadataIncludesUltraWide(bundle)) {
                        add("ultrawide_video", File(bundle.directory, SessionBundle.ULTRAWIDE_VIDEO_FILE), "video/mp4")
                        add("ultrawide_frame_timestamps", File(bundle.directory, SessionBundle.ULTRAWIDE_FRAME_TIMESTAMPS_FILE), "text/csv")
                    }
                }.build()
        return Request
            .Builder()
            .url("${baseUrl.trimEnd('/')}/sessions")
            .header("Idempotency-Key", bundle.sessionId)
            .post(body)
            .build()
    }

    private fun metadataIncludesUltraWide(bundle: SessionBundle): Boolean {
        val metadata = Json.parseToJsonElement(bundle.metadata.readText()).jsonObject
        require(metadata["session_id"]?.jsonPrimitive?.content == bundle.sessionId) {
            "Metadata session ID must match the idempotency key"
        }
        return metadata["camera_streams"]
            ?.jsonObject
            ?.get("ultrawide")
            ?.jsonPrimitive
            ?.boolean
            ?: error("Metadata ultra-wide declaration is missing")
    }

    private fun MultipartBody.Builder.add(
        name: String,
        file: File,
        type: String,
    ) {
        addFormDataPart(name, file.name, file.asRequestBody(type.toMediaType()))
    }

    private fun String.isUuid(): Boolean = runCatching { UUID.fromString(this) }.isSuccess
}
