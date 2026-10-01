package com.ssafy.s15p21a206.tiger.core.session

import com.ssafy.s15p21a206.tiger.core.model.session.BundleValidationResult
import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SessionBundleValidatorTest {
    @Test
    fun `main only bundle with metadata commit marker is valid`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")

        assertTrue(SessionBundleValidator.validate(directory).isValid)
        directory.deleteRecursively()
    }

    @Test
    fun `bundle without metadata commit marker is invalid`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        File(directory, SessionBundle.METADATA_FILE).delete()

        assertInvalid("metadata commit marker is missing", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `ultrawide declaration requires both ultrawide files`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1", includesUltraWide = true)
        File(directory, SessionBundle.ULTRAWIDE_VIDEO_FILE).delete()

        assertInvalid("invalid ultra-wide stream", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `metadata stream declaration must match ultrawide document set`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        File(directory, SessionBundle.ULTRAWIDE_VIDEO_FILE).writeBytes(byteArrayOf(1))
        File(directory, SessionBundle.ULTRAWIDE_FRAME_TIMESTAMPS_FILE).writeText("frame_number,timestamp_ns,timestamp_source\n")

        assertInvalid("ultra-wide document set does not match metadata", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `manifest size and sha256 must match raw documents`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        File(directory, SessionBundle.MAIN_VIDEO_FILE).writeBytes(byteArrayOf(9, 9))

        assertInvalid("metadata manifest does not match bundle", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `unexpected document is rejected even when required files are present`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        File(directory, "unexpected.txt").writeText("unexpected")

        assertInvalid("metadata manifest does not match bundle", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `missing directory is reported first`() {
        val directory = File(Files.createTempDirectory("session").toFile(), "absent")

        assertInvalid("bundle directory is missing", directory)
        directory.parentFile?.deleteRecursively()
    }

    @Test
    fun `empty main video is reported before the CSV files`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        File(directory, SessionBundle.MAIN_VIDEO_FILE).writeBytes(byteArrayOf())
        File(directory, SessionBundle.GYROSCOPE_FILE).delete()

        assertInvalid("main video is missing", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `CSV with a wrong header names the file`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        File(directory, SessionBundle.GYROSCOPE_FILE).writeText("t,x,y,z\n")

        assertInvalid("invalid ${SessionBundle.GYROSCOPE_FILE}", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `metadata without an ultrawide declaration is invalid`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        rewriteMetadata(directory) { it.replace(""","ultrawide":false""", "") }

        assertInvalid("metadata ultra-wide declaration is missing", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `metadata must declare the main stream`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        rewriteMetadata(directory) { it.replace(""""main":true""", """"main":false""") }

        assertInvalid("main stream declaration is invalid", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `metadata session id must not be blank`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, " ")

        assertInvalid("metadata session id is missing", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `manifest entry with a malformed sha256 does not match the bundle`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        val mainVideoHash = sha256(File(directory, SessionBundle.MAIN_VIDEO_FILE))
        rewriteMetadata(directory) { it.replace(mainVideoHash, mainVideoHash.uppercase()) }

        assertInvalid("metadata manifest does not match bundle", directory)
        directory.deleteRecursively()
    }

    @Test
    fun `without metadata the ultrawide stream is checked from the caller declaration`() {
        val directory = Files.createTempDirectory("session").toFile()
        writeCompleteBundle(directory, "session-1")
        File(directory, SessionBundle.METADATA_FILE).delete()

        assertTrue(SessionBundleValidator.validate(directory, requireMetadata = false).isValid)
        assertInvalid("invalid ultra-wide stream", directory, includesUltraWide = true, requireMetadata = false)
        directory.deleteRecursively()
    }

    @Test
    fun `display name uses fixed four digit sequence`() {
        val root = Files.createTempDirectory("sessions").toFile()
        val bundle = SessionBundleStore(root).createStagingBundle(12)

        assertTrue(bundle.displayName == "session_0012")
        root.deleteRecursively()
    }

    /** 사유까지 본다. 마감은 이 사유를 실패 이유로 그대로 남긴다(`FinalizeResult.Failed`). */
    private fun assertInvalid(
        reason: String,
        directory: File,
        includesUltraWide: Boolean = false,
        requireMetadata: Boolean = true,
    ) {
        assertEquals(
            BundleValidationResult.Invalid(reason),
            SessionBundleValidator.validate(directory, includesUltraWide, requireMetadata),
        )
    }

    private fun rewriteMetadata(
        directory: File,
        edit: (String) -> String,
    ) {
        val metadata = File(directory, SessionBundle.METADATA_FILE)
        metadata.writeText(edit(metadata.readText()))
    }

    private fun writeCompleteBundle(
        directory: File,
        sessionId: String,
        includesUltraWide: Boolean = false,
    ) {
        File(directory, SessionBundle.MAIN_VIDEO_FILE).writeBytes(byteArrayOf(1))
        File(directory, SessionBundle.MAIN_FRAME_TIMESTAMPS_FILE).writeText("frame_number,timestamp_ns,timestamp_source\n")
        File(directory, SessionBundle.ACCELEROMETER_FILE).writeText("timestamp_ns,x,y,z,accuracy\n")
        File(directory, SessionBundle.GYROSCOPE_FILE).writeText("timestamp_ns,x,y,z,accuracy\n")
        File(directory, SessionBundle.ROTATION_VECTOR_FILE).writeText("timestamp_ns,x,y,z,scalar_component,heading_accuracy_rad,accuracy\n")
        File(
            directory,
            SessionBundle.ARCORE_POSES_FILE,
        ).writeText("android_camera_timestamp_ns,tx,ty,tz,qx,qy,qz,qw,tracking_state,tracking_failure_reason\n")
        File(directory, SessionBundle.EPISODES_FILE).writeText("episode_id,start_timestamp_ns,end_timestamp_ns,task,object,outcome\n")
        if (includesUltraWide) {
            File(directory, SessionBundle.ULTRAWIDE_VIDEO_FILE).writeBytes(byteArrayOf(2))
            File(directory, SessionBundle.ULTRAWIDE_FRAME_TIMESTAMPS_FILE).writeText("frame_number,timestamp_ns,timestamp_source\n")
        }
        File(directory, SessionBundle.METADATA_FILE).writeText(metadataJson(directory, sessionId, includesUltraWide))
    }

    private fun metadataJson(
        directory: File,
        sessionId: String,
        includesUltraWide: Boolean,
    ): String {
        val files =
            directory
                .listFiles()
                .orEmpty()
                .filter { it.isFile && it.name != SessionBundle.METADATA_FILE }
                .sortedBy(File::getName)
                .joinToString(",") { file -> """{"path":"${file.name}","sizeBytes":${file.length()},"sha256":"${sha256(file)}"}""" }
        return """{"session_id":"$sessionId","camera_streams":{"main":true,"ultrawide":$includesUltraWide},"files":[$files]}"""
    }

    private fun sha256(file: File): String =
        java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
}
