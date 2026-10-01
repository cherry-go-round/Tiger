package com.ssafy.s15p21a206.tiger.core.session

import com.ssafy.s15p21a206.tiger.core.model.session.BundleValidationResult
import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.security.MessageDigest

object SessionBundleValidator {
    private val requiredCsvHeaders =
        mapOf(
            SessionBundle.MAIN_FRAME_TIMESTAMPS_FILE to SessionBundle.FRAME_TIMESTAMPS_HEADER,
            SessionBundle.ACCELEROMETER_FILE to SessionBundle.ACCELEROMETER_HEADER,
            SessionBundle.GYROSCOPE_FILE to SessionBundle.GYROSCOPE_HEADER,
            SessionBundle.ROTATION_VECTOR_FILE to SessionBundle.ROTATION_VECTOR_HEADER,
            SessionBundle.ARCORE_POSES_FILE to SessionBundle.ARCORE_POSES_HEADER,
            SessionBundle.EPISODES_FILE to SessionBundle.EPISODES_HEADER,
        )

    /**
     * 번들이 온전한지 본다. 처음 걸린 문제 하나를 사유로 돌려준다.
     *
     * [requireMetadata]가 `false`면 `metadata.json`을 쓰기 전(마감 중)이라 기록 파일만 보고, 초광각을 함께
     * 찍었는지는 [includesUltraWide]로 받는다. `true`면 그것을 `metadata.json`의 선언에서 읽으므로
     * [includesUltraWide]는 보지 않는다.
     */
    fun validate(
        directory: File,
        includesUltraWide: Boolean = false,
        requireMetadata: Boolean = true,
    ): BundleValidationResult {
        val recording = recordingProblem(directory)
        val problem =
            when {
                recording != null -> recording
                requireMetadata -> metadataProblem(directory)
                else -> ultraWideProblem(directory, includesUltraWide)
            }
        return problem?.let(BundleValidationResult::Invalid) ?: BundleValidationResult.Valid
    }

    /** 수집이 남기는 기록 파일(주 영상과 CSV들)의 문제. */
    private fun recordingProblem(directory: File): String? {
        if (!directory.isDirectory) return "bundle directory is missing"
        if (!File(directory, SessionBundle.MAIN_VIDEO_FILE).hasContent()) return "main video is missing"
        val invalidCsv = requiredCsvHeaders.entries.firstOrNull { (fileName, header) -> !File(directory, fileName).hasHeader(header) }
        return invalidCsv?.let { "invalid ${it.key}" }
    }

    /** `metadata.json`이 번들과 맞는지. 선언(세션 ID, 카메라 스트림)과 파일 manifest를 본다. */
    private fun metadataProblem(directory: File): String? {
        val metadata = parseMetadata(File(directory, SessionBundle.METADATA_FILE)) ?: return "metadata commit marker is missing"
        val declaresUltraWide = metadata.declaresStream("ultrawide") ?: return "metadata ultra-wide declaration is missing"
        return when {
            !metadata.declaresMainStream -> "main stream declaration is invalid"
            metadata.sessionId.isBlank() -> "metadata session id is missing"
            !hasUltraWideDocumentsAsDeclared(directory, declaresUltraWide) -> "ultra-wide document set does not match metadata"
            declaresUltraWide && !hasValidUltraWideFiles(directory) -> "invalid ultra-wide stream"
            !hasExactManifest(directory, metadata.manifest) -> "metadata manifest does not match bundle"
            else -> null
        }
    }

    /** 초광각 파일이 있는지가 선언과 같은지. 선언했으면 둘 중 하나라도 있어야 하고, 하지 않았으면 둘 다 없어야 한다. */
    private fun hasUltraWideDocumentsAsDeclared(
        directory: File,
        declared: Boolean,
    ): Boolean {
        val present =
            File(directory, SessionBundle.ULTRAWIDE_VIDEO_FILE).exists() ||
                File(directory, SessionBundle.ULTRAWIDE_FRAME_TIMESTAMPS_FILE).exists()
        return present == declared
    }

    private fun hasValidUltraWideFiles(directory: File): Boolean =
        File(directory, SessionBundle.ULTRAWIDE_VIDEO_FILE).hasContent() &&
            File(directory, SessionBundle.ULTRAWIDE_FRAME_TIMESTAMPS_FILE).hasHeader(SessionBundle.FRAME_TIMESTAMPS_HEADER)

    /** metadata 없이 볼 때의 초광각 문제. 함께 찍었다면 영상과 프레임 시각이 온전해야 한다. */
    private fun ultraWideProblem(
        directory: File,
        includesUltraWide: Boolean,
    ): String? = "invalid ultra-wide stream".takeIf { includesUltraWide && !hasValidUltraWideFiles(directory) }

    private fun parseMetadata(metadataFile: File): BundleMetadata? =
        runCatching {
            if (!metadataFile.hasContent()) return null
            val root = Json.parseToJsonElement(metadataFile.readText()).jsonObject
            val entries = root["files"]?.jsonArray?.map(::parseManifestEntry) ?: return null
            BundleMetadata(
                sessionId = root["session_id"]?.jsonPrimitive?.contentOrNull ?: return null,
                cameraStreams = root["camera_streams"]?.jsonObject ?: return null,
                manifest = entries.takeIf { null !in it }?.filterNotNull(),
            )
        }.getOrNull()

    /** manifest의 한 항목. 필드가 빠졌거나 SHA-256이 소문자 16진 64자가 아니면 null이다. */
    private fun parseManifestEntry(element: JsonElement): ManifestEntry? {
        val entry = element as? JsonObject ?: return null
        return ManifestEntry(
            path = entry["path"]?.jsonPrimitive?.contentOrNull ?: return null,
            sizeBytes = entry["sizeBytes"]?.jsonPrimitive?.longOrNull ?: return null,
            sha256 = entry["sha256"]?.jsonPrimitive?.contentOrNull?.takeIf { it.matches(SHA_256) } ?: return null,
        )
    }

    /**
     * manifest가 번들의 raw 파일을 빠짐없이, 겹치지 않게 설명하는지.
     *
     * 디렉터리에는 파일만 있어야 하고, `metadata.json`을 뺀 파일 이름의 집합이 manifest 경로의 집합과
     * 같아야 한다. 그 위에서 항목마다 크기와 SHA-256이 실제 파일과 같아야 한다.
     * 형식이 틀린 항목이 있어 [manifest]가 null이면 맞지 않는 것으로 본다.
     */
    private fun hasExactManifest(
        directory: File,
        manifest: List<ManifestEntry>?,
    ): Boolean {
        if (manifest == null) return false
        val documents = directory.listFiles()?.toList().orEmpty()
        if (documents.any { !it.isFile }) return false
        val rawDocuments = documents.filter { it.name != SessionBundle.METADATA_FILE }.associateBy(File::getName)
        val paths = manifest.map(ManifestEntry::path)
        if (paths.distinct().size != paths.size || paths.toSet() != rawDocuments.keys) return false
        return manifest.all { it.describes(rawDocuments.getValue(it.path)) }
    }

    private fun ManifestEntry.describes(document: File): Boolean = sizeBytes == document.length() && sha256 == sha256(document)

    private fun File.hasContent() = isFile && length() > 0L

    private fun File.hasHeader(header: String) = hasContent() && bufferedReader().use { it.readLine() == header }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        file.inputStream().use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private data class BundleMetadata(
        val sessionId: String,
        val cameraStreams: JsonObject,
        /** 파일 manifest. 항목 중 하나라도 형식이 틀리면 null이다. */
        val manifest: List<ManifestEntry>?,
    ) {
        /** 주 스트림을 담았다고 선언했는지. 선언이 없거나 불리언이 아니면 담지 않은 것으로 본다. */
        val declaresMainStream: Boolean get() = declaresStream("main") == true

        /** `camera_streams`에서 [name] 스트림을 담았다고 선언했는지. 선언이 없거나 불리언이 아니면 null이다. */
        fun declaresStream(name: String): Boolean? = cameraStreams[name]?.jsonPrimitive?.booleanOrNull
    }

    private data class ManifestEntry(
        val path: String,
        val sizeBytes: Long,
        val sha256: String,
    )

    private val SHA_256 = Regex("[0-9a-f]{64}")
}
