package com.ssafy.s15p21a206.tiger.feature.capture

/**
 * 수집 작업 공간이 표시하는 상태.
 *
 * 이름은 명세 및 수신 측 어휘를 따른다. [Ready]는 Session이 이미 수집 중이고 ARCore Tracking이
 * 안정화되어 Episode를 시작할 수 있는 상태를 뜻한다. Session을 아직 시작하지 않은 상태는 [Idle]이다.
 */
internal enum class CaptureWorkspaceControlState {
    Idle,
    Initializing,
    Ready,
    EpisodeActive,
    Finalizing,
}

/** 작업 공간을 나가려 할 때 할 일. 닫기 버튼, 정지 버튼, 시스템 뒤로 가기가 같은 판단을 쓴다. */
internal enum class CaptureExitAction {
    /** Session 전이라 그냥 닫는다. */
    Leave,

    /** Session이 돌고 있어 종료를 확인받는다. */
    Confirm,

    /** 마감 중이거나 다른 작업이 걸려 있어 받지 않는다. */
    Ignore,
}

/** 수집 단계에서 파생되는 허용 동작. */
internal data class CaptureControlPolicy(
    val state: CaptureWorkspaceControlState,
    val ready: Boolean = true,
    val busy: Boolean = false,
) {
    /** Idle에서는 Session을, Ready에서는 Episode를 시작한다. Initializing은 Tracking이 안정화될 때까지 막는다. */
    val canPlay: Boolean
        get() =
            !busy &&
                when (state) {
                    CaptureWorkspaceControlState.Idle -> ready
                    CaptureWorkspaceControlState.Ready -> true
                    else -> false
                }

    val canPause: Boolean
        get() = !busy && state == CaptureWorkspaceControlState.EpisodeActive

    val canStop: Boolean
        get() = !busy && sessionRunning

    val exitAction: CaptureExitAction
        get() =
            when {
                busy || state == CaptureWorkspaceControlState.Finalizing -> CaptureExitAction.Ignore
                state == CaptureWorkspaceControlState.Idle -> CaptureExitAction.Leave
                else -> CaptureExitAction.Confirm
            }

    private val sessionRunning: Boolean
        get() =
            when (state) {
                CaptureWorkspaceControlState.Initializing,
                CaptureWorkspaceControlState.Ready,
                CaptureWorkspaceControlState.EpisodeActive,
                -> true
                CaptureWorkspaceControlState.Idle,
                CaptureWorkspaceControlState.Finalizing,
                -> false
            }
}
