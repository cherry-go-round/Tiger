package com.ssafy.s15p21a206.tiger.core.model.capture

/**
 * 한 Session 내내 고정할 촬영 조건.
 *
 * 프리뷰와 녹화가 같은 값을 쓰게 하려고 하나의 객체로 든다. 화면이 따로, 카메라 요청이 따로
 * 값을 들면 둘이 어긋나도 아무도 모른다. calibration과 dataset이 같은 광학 조건이어야 한다는
 * 것이 이 기능의 전부이므로, 그 어긋남은 조용한 실패가 된다.
 *
 * 값은 전부 기기가 보고한 범위 안으로 좁힌 뒤에만 들어온다. [ManualCameraCapabilities.coerce]가
 * 그 일을 하며, 이 클래스는 좁히기를 스스로 하지 않는다.
 */
data class ManualCameraConfig(
    /** 0이 무한대, 클수록 가깝다. 단위는 diopter(1/m). */
    val focusDistanceDiopter: Float,
    val iso: Int,
    val exposureTimeNs: Long,
    val frameDurationNs: Long = TARGET_FRAME_DURATION_NS,
    /** null이면 AWB AUTO로 둔다. 값이 있으면 그 gain으로 고정한다. */
    val whiteBalance: FixedWhiteBalance? = null,
) {
    /**
     * 노출이 프레임 간격을 넘지 않는지.
     *
     * 넘으면 센서가 프레임 간격을 노출에 맞춰 늘려 30 fps가 깨진다. UI에서 고를 수 없게 막지만,
     * 저장된 값을 불러오는 경로도 있으므로 여기서도 물어볼 수 있게 둔다.
     */
    val exposureFitsFrame: Boolean get() = exposureTimeNs <= frameDurationNs

    val awbFixed: Boolean get() = whiteBalance != null

    companion object {
        /** `RecordingInputValidator.TARGET_FPS`(30 fps)의 프레임 간격. dataset contract가 고정한 값이다. */
        const val TARGET_FRAME_DURATION_NS = 33_333_333L
    }
}

/**
 * 프리뷰에서 수렴시킨 화이트 밸런스를 그대로 옮기기 위한 값.
 *
 * `CONTROL_AWB_LOCK`을 쓰지 않는 이유는 lock이 절대값이 아니라 "지금 수렴한 값을 유지"라는 상대
 * 상태이기 때문이다. 프리뷰의 `CameraDevice`를 닫고 ARCore가 새 session을 열면 AWB는 처음부터
 * 다시 수렴하므로, 프리뷰에서 잠근 색이 녹화본으로 넘어가지 않는다. gain과 transform을 숫자로
 * 들고 건너가는 것만이 두 session에서 같은 색을 보장한다.
 *
 * Camera2 타입(`RggbChannelVector`, `ColorSpaceTransform`) 대신 순수 값으로 두어 단위 테스트에서
 * 다룰 수 있게 한다. Camera2 타입으로의 변환은 [applyManualCamera][com.ssafy.s15p21a206.tiger.core.capture.manual.applyManualCamera]가 한다.
 */
data class FixedWhiteBalance(
    val redGain: Float,
    val greenEvenGain: Float,
    val greenOddGain: Float,
    val blueGain: Float,
    /** `COLOR_CORRECTION_TRANSFORM`의 유리수 9개를 분자·분모 쌍 18개로 편 것. */
    val transform: List<Int>,
) {
    init {
        require(transform.size == TRANSFORM_SIZE) { "Color correction transform must hold $TRANSFORM_SIZE values" }
    }

    companion object {
        const val TRANSFORM_SIZE = 18
    }
}

/**
 * md가 정한 shutter preset. 자유 입력 대신 이 다섯만 제공한다.
 *
 * 값은 1초를 분모로 나눈 나노초다. 1/500만 2,000,000 ns로 떨어지고 나머지는 반올림이 들어간다.
 */
enum class ShutterPreset(
    val label: String,
    val exposureTimeNs: Long,
) {
    ONE_THIRTIETH("1/30", 33_333_333L),
    ONE_SIXTIETH("1/60", 16_666_667L),
    ONE_TWENTIETH_HUNDRED("1/120", 8_333_333L),
    ONE_TWO_FORTIETH("1/240", 4_166_667L),
    ONE_FIVE_HUNDREDTH("1/500", 2_000_000L),
}
