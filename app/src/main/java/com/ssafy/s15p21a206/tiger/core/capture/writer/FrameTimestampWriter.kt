package com.ssafy.s15p21a206.tiger.core.capture.writer

import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import java.io.File

class FrameTimestampWriter(
    file: File,
) : CsvWriter(file, SessionBundle.FRAME_TIMESTAMPS_HEADER) {
    // 영상 녹화가 실제로 진행 중인 구간만 기록한다. 카메라 스레드가 읽고 수집 수명주기가 쓴다.
    @Volatile var recording: Boolean = false

    private var nextFrameNumber = 0L
    private var lastTimestampNs: Long? = null

    /**
     * 프레임 하나를 기록하고 실제로 기록했는지 돌려준다.
     * 같은 timestamp가 연속으로 도착하면 중복으로 보고 버린다. ARCore SharedCamera 구성에서
     * 동일한 CaptureCallback이 두 번 등록되어 한 프레임이 두 번 전달될 수 있기 때문이다.
     */
    fun record(timestampNs: Long): Boolean {
        if (!recording || timestampNs == lastTimestampNs) return false
        lastTimestampNs = timestampNs
        append("${nextFrameNumber++},$timestampNs,SENSOR_TIMESTAMP")
        return true
    }
}
