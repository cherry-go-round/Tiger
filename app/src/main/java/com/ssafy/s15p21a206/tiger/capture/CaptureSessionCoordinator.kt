package com.ssafy.s15p21a206.tiger.capture

import com.ssafy.s15p21a206.tiger.core.model.capture.TrackingState
import com.ssafy.s15p21a206.tiger.core.model.session.CaptureSession
import com.ssafy.s15p21a206.tiger.core.model.session.EpisodeMarker
import com.ssafy.s15p21a206.tiger.core.model.session.EpisodeState
import com.ssafy.s15p21a206.tiger.core.model.session.RecordingState
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState
import java.util.UUID

fun interface MonotonicClock {
    fun nowNs(): Long
}

class CaptureSessionCoordinator(
    private val clock: MonotonicClock,
    // 사용자 종료와 Tracking 유실 자동 마감이 같은 출구를 쓰도록 한다.
    private val onEpisodeClosed: (EpisodeMarker) -> Unit = {},
) {
    var session: CaptureSession? = null
        private set
    var activeEpisode: EpisodeMarker? = null
        private set
    var trackingState: TrackingState = TrackingState.INITIALIZING
        private set
    private var readySinceNs: Long? = null
    private var trackingLoss: TrackingLoss? = null

    /**
     * Session을 연다. [task]와 [objectName]은 이 Session 전체에 적용되는 두 이름이다.
     *
     * Episode 시작 때 다시 받는 것과 같은 값이지만 여기서도 받는다. Episode를 하나도 시작하지
     * 않고 끝나는 Session이 있고, 그때 두 이름을 아는 곳은 이 호출뿐이다.
     */
    fun start(
        sessionId: String = UUID.randomUUID().toString(),
        displayNumber: Int,
        bundlePath: String,
        task: String,
        objectName: String,
    ): CaptureSession {
        check(session == null) { "Session already exists" }
        check(task.isNotBlank() && objectName.isNotBlank()) { "Task and object are required" }
        return CaptureSession(
            sessionId,
            displayNumber,
            RecordingState.INITIALIZING,
            UploadState.LOCAL_ONLY,
            clock.nowNs(),
            bundlePath = bundlePath,
            task = task,
            objectName = objectName,
        ).also { session = it }
    }

    /**
     * Tracking 신호를 전달한다. 값이 변하지 않아도 반복 호출되어야 시간 기반 판정이 발화한다.
     *
     * [observedAtNs]는 이 신호를 만든 ARCore 프레임의 카메라 시각이다. 주어지면 유실 구간의
     * **기록용** 시작 시각으로 쓰여 `arcore_poses.csv`의 첫 유실 행과 정확히 맞는다.
     * 판정용 경과 시간은 항상 [clock]으로만 재므로, pose 처리 지연이 게이트를 앞당기지 않는다.
     */
    fun onTracking(
        isTracking: Boolean,
        observedAtNs: Long? = null,
    ) {
        val now = clock.nowNs()
        if (isTracking) holdTracking(now) else loseTracking(now, observedAtNs)
    }

    /** Tracking이 [READY_GATE_NS] 동안 이어지면 READY로 올리고, 시작을 기다리는 Session도 함께 올린다. */
    private fun holdTracking(now: Long) {
        trackingLoss = null
        val readySince = readySinceNs ?: now.also { readySinceNs = it }
        if (now - readySince < READY_GATE_NS) return
        trackingState = TrackingState.READY
        val current = session ?: return
        if (current.recordingState == RecordingState.INITIALIZING) session = current.copy(recordingState = RecordingState.READY)
    }

    /** Tracking을 잃은 지 [TRACKING_LOSS_NS]가 지나면 진행 중 Episode를 유실이 시작된 시각 기준으로 무효 마감한다. */
    private fun loseTracking(
        now: Long,
        observedAtNs: Long?,
    ) {
        readySinceNs = null
        trackingState = TrackingState.PAUSED
        val loss =
            trackingLoss
                ?: TrackingLoss(detectedAtNs = now, startedAtNs = trustedObservation(observedAtNs, now) ?: now)
                    .also { trackingLoss = it }
        val episode = activeEpisode ?: return
        if (now - loss.detectedAtNs < TRACKING_LOSS_NS) return
        closeEpisode(episode.copy(endTimestampNs = loss.startedAtNs + TRACKING_LOSS_NS, outcome = EpisodeState.INVALID_TRACKING))
    }

    /**
     * 카메라 시각을 기록용으로 쓸 수 있을 때만 돌려준다.
     *
     * 카메라 timestamp 소스가 `REALTIME`이 아닌 기기에서는 [observedAtNs]가 단조 시계와 다른
     * 시간축이라 그대로 쓰면 엉뚱한 값이 기록된다. 진행 중 Episode의 시작보다 이르거나 현재보다
     * 미래인 값은 다른 시간축으로 보고 버린다.
     */
    private fun trustedObservation(
        observedAtNs: Long?,
        now: Long,
    ): Long? {
        val candidate = observedAtNs ?: return null
        if (candidate > now) return null
        activeEpisode?.let { if (candidate < it.startTimestampNs) return null }
        return candidate
    }

    /** 판정은 [detectedAtNs](단조 시계)로, 기록은 [startedAtNs](가능하면 카메라 시각)로 한다. */
    private data class TrackingLoss(
        val detectedAtNs: Long,
        val startedAtNs: Long,
    )

    fun startEpisode(
        task: String,
        objectName: String,
    ): EpisodeMarker {
        check(session?.recordingState == RecordingState.READY && trackingState == TrackingState.READY) { "Tracking is not ready" }
        check(task.isNotBlank() && objectName.isNotBlank()) { "Task and object are required" }
        check(activeEpisode == null) { "Episode is already active" }
        return EpisodeMarker(
            UUID.randomUUID().toString(),
            session!!.sessionId,
            clock.nowNs(),
            task = task,
            objectName = objectName,
            outcome = EpisodeState.ACTIVE,
        ).also { activeEpisode = it }
    }

    fun endEpisode(): EpisodeMarker {
        val completed =
            requireNotNull(activeEpisode) {
                "No active episode"
            }.copy(endTimestampNs = clock.nowNs(), outcome = EpisodeState.COMPLETED)
        closeEpisode(completed)
        return completed
    }

    /** 사용자 종료와 Tracking 유실 자동 마감이 함께 지나는 출구. */
    private fun closeEpisode(closed: EpisodeMarker) {
        activeEpisode = null
        onEpisodeClosed(closed)
    }

    /**
     * Session 경계를 닫아 다음 Session을 시작할 수 있게 한다.
     *
     * Tracking 판정 상태까지 함께 되돌린다. 이월되면 새 Session이 이전 Session의 안정화 결과를
     * 물려받아 READY gate 없이 Episode를 시작할 수 있게 된다.
     */
    fun release() {
        session = null
        activeEpisode = null
        trackingState = TrackingState.INITIALIZING
        readySinceNs = null
        trackingLoss = null
    }

    companion object {
        const val READY_GATE_NS = 1_000_000_000L
        const val TRACKING_LOSS_NS = 500_000_000L
    }
}
