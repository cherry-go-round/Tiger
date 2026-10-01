package com.ssafy.s15p21a206.tiger.core.session

import android.media.MediaMetadataRetriever
import com.ssafy.s15p21a206.tiger.core.model.capture.RecordingResolution
import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import java.io.File

/**
 * 재생할 수 있는 수집 영상. 파일이 없거나 비어 있으면 null이다.
 *
 * 상세의 재생 영역, 전체화면, 해상도 읽기가 같은 기준으로 판단해야 한쪽은 재생하고 다른 쪽은
 * 없다고 말하는 일이 생기지 않는다.
 */
internal fun playableMainVideo(bundlePath: String): File? =
    File(bundlePath, SessionBundle.MAIN_VIDEO_FILE).takeIf { it.isFile && it.length() > 0L }

/**
 * 수집 영상의 해상도를 재생기가 보여 주는 크기로 읽는다. 영상이 없거나 읽지 못하면 null이다.
 *
 * 값은 `metadata.json`이 아니라 `main_rgb.mp4`에서 직접 읽는다. Room에 컬럼을 두지 않아도 과거
 * 수집분까지 실제 값이 나온다. 파일을 여므로 main 스레드에서 부르지 않는다.
 */
internal fun readMainVideoResolution(bundlePath: String): RecordingResolution? {
    val videoFile = playableMainVideo(bundlePath) ?: return null
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(videoFile.absolutePath)
        displayResolution(
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
            rotationDegrees = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull(),
        )
        // 손상된 파일과 지원하지 않는 컨테이너 모두 RuntimeException으로 나온다. 해상도 한 줄
        // 때문에 상세 화면 전체가 무너지면 안 되므로 여기서 멈춘다.
    } catch (_: RuntimeException) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

/**
 * 재생기가 보여 주는 크기로 맞춘다. 크기를 모르거나 0 이하면 null이다.
 *
 * `MediaMetadataRetriever`는 회전을 반영하지 않은 저장 크기를 돌려준다. 재생기는 회전 metadata를
 * 적용해 그리므로, 90도와 270도에서는 가로세로를 바꿔야 화면에 보이는 것과 같은 값이 된다.
 * `video_rotation_degrees`가 90인 기존 수집분이 세로로 나오는 것은 이 때문이며, 회전 없이
 * 저장되는 이후 수집분은 읽은 크기가 그대로 쓰인다.
 */
internal fun displayResolution(
    width: Int?,
    height: Int?,
    rotationDegrees: Int?,
): RecordingResolution? {
    if (width == null || height == null || width <= 0 || height <= 0) return null
    val normalized = (rotationDegrees ?: 0).mod(360)
    return if (normalized == 90 || normalized == 270) {
        RecordingResolution(width = height, height = width)
    } else {
        RecordingResolution(width = width, height = height)
    }
}
