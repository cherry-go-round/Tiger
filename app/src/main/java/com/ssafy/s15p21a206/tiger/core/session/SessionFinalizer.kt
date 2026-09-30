package com.ssafy.s15p21a206.tiger.core.session

import com.ssafy.s15p21a206.tiger.core.model.capture.CameraMetadata
import com.ssafy.s15p21a206.tiger.core.model.capture.CaptureSettingsMetadata
import com.ssafy.s15p21a206.tiger.core.model.session.BundleValidationResult
import com.ssafy.s15p21a206.tiger.core.model.session.FinalizeResult
import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.io.File
import java.security.MessageDigest

class SessionFinalizer(
    private val bundleStore: SessionBundleStore,
) {
    /**
     * staging 번들을 검증하고 `metadata.json`을 쓴 뒤 completed로 공개한다.
     *
     * `metadata.json`은 commit marker라 나머지 파일이 모두 검증된 뒤에만 쓴다. 검증에 실패하면 아무것도
     * 쓰지 않고 [FinalizeResult.Failed]를 돌려준다.
     *
     * @param camera 확보하지 못했으면 null. 메타데이터 부재가 Session 마감을 실패시키지 않는다.
     * @param captureSettings 어떤 촬영 조건으로 찍었는지. 수동 설정을 쓰지 않았으면 null이다.
     */
    fun finalize(
        bundle: SessionBundle,
        includesUltraWide: Boolean = false,
        camera: CameraMetadata? = null,
        captureSettings: CaptureSettingsMetadata? = null,
    ): FinalizeResult {
        val validation = SessionBundleValidator.validate(bundle.directory, includesUltraWide, requireMetadata = false)
        if (validation is BundleValidationResult.Invalid) return FinalizeResult.Failed(validation.reason)
        val manifest = manifestOf(bundle)
        val document = metadataJson(bundle, includesUltraWide, camera, captureSettings, manifest)
        bundle.metadata.writeText(Json.encodeToString(document))
        val published = bundleStore.publish(bundle, includesUltraWide)
        return FinalizeResult.Completed(published, checksums = manifest.mapValues { it.value.sha256 })
    }

    /** `metadata.json`을 뺀 번들 파일마다의 크기와 SHA-256. */
    private fun manifestOf(bundle: SessionBundle): Map<String, ManifestEntry> =
        bundle.directory
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.name != SessionBundle.METADATA_FILE }
            .associate { file -> file.name to ManifestEntry(file.length(), sha256(file)) }

    /** `metadata.json`의 내용. 키와 형식은 Session metadata 계약을 따른다. */
    private fun metadataJson(
        bundle: SessionBundle,
        includesUltraWide: Boolean,
        camera: CameraMetadata?,
        captureSettings: CaptureSettingsMetadata?,
        manifest: Map<String, ManifestEntry>,
    ): JsonObject =
        buildJsonObject {
            put("session_id", bundle.sessionId)
            put(
                "camera_streams",
                buildJsonObject {
                    put("main", true)
                    put("ultrawide", includesUltraWide)
                },
            )
            if (camera != null) put("camera", Json.encodeToJsonElement(camera))
            if (captureSettings != null) put("capture_settings", Json.encodeToJsonElement(captureSettings))
            put("files", filesJson(manifest))
        }

    /** manifest를 경로 순으로 정렬한 `files` 배열. */
    private fun filesJson(manifest: Map<String, ManifestEntry>): JsonArray =
        buildJsonArray {
            manifest.toSortedMap().forEach { (path, entry) ->
                add(
                    buildJsonObject {
                        put("path", path)
                        put("sizeBytes", entry.sizeBytes)
                        put("sha256", entry.sha256)
                    },
                )
            }
        }

    private fun sha256(file: File): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
}

private data class ManifestEntry(
    val sizeBytes: Long,
    val sha256: String,
)
