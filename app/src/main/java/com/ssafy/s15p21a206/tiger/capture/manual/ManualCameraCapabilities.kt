package com.ssafy.s15p21a206.tiger.capture.manual

/**
 * 실제 녹화에 쓰일 카메라가 무엇을 지원하는지.
 *
 * 임의의 후면 카메라가 아니라 ARCore가 고른 `cameraId`에서 읽어야 한다. 논리 카메라가 여럿인
 * 기기에서는 ISO·노출 범위가 카메라마다 다르고, 프리뷰에서 고른 값이 녹화 카메라의 범위를
 * 벗어나면 조용히 다른 값으로 찍힌다.
 */
data class ManualCameraCapabilities(
    val cameraId: String,
    val manualSensor: Boolean,
    val aeOffSupported: Boolean,
    val afOffSupported: Boolean,
    val awbOffSupported: Boolean,
    val awbLockSupported: Boolean,
    /** `LENS_INFO_MINIMUM_FOCUS_DISTANCE`. 0이면 고정 초점 렌즈라 초점을 옮길 수 없다. */
    val maxFocusDiopter: Float,
    val isoRange: IntRange?,
    val exposureRangeNs: LongRange?,
    val maxFrameDurationNs: Long?,
) {
    val focusSupported: Boolean get() = afOffSupported && maxFocusDiopter > 0f

    val isoSupported: Boolean get() = manualSensor && aeOffSupported && isoRange != null

    val exposureSupported: Boolean get() = manualSensor && aeOffSupported && exposureRangeNs != null

    /** gain을 직접 거는 방식이라 `AWB_MODE_OFF`가 있어야 한다. lock 가능 여부는 참고로만 든다. */
    val whiteBalanceSupported: Boolean get() = awbOffSupported

    /** 수동 설정을 쓸 수 없는 이유. null이면 쓸 수 있다. 화면은 이 문구를 그대로 보여 준다. */
    val unsupportedReason: String?
        get() =
            when {
                !manualSensor -> "이 카메라는 MANUAL_SENSOR를 지원하지 않습니다"
                !aeOffSupported -> "이 카메라는 노출 수동 제어를 지원하지 않습니다"
                isoRange == null -> "ISO 범위를 읽을 수 없습니다"
                exposureRangeNs == null -> "셔터 범위를 읽을 수 없습니다"
                else -> null
            }

    /** 이 기기에서 고를 수 있는 preset만. 하나도 없으면 빈 목록이다. */
    fun availableShutterPresets(): List<ShutterPreset> = ShutterPreset.entries.filter(::allows)

    /** 이 preset을 고를 수 있는지. 기기 범위 밖이거나 30 fps 프레임 간격을 넘으면 고를 수 없다. */
    fun allows(preset: ShutterPreset): Boolean {
        val range = exposureRangeNs ?: return false
        return preset.exposureTimeNs in range && preset.exposureTimeNs <= frameDurationNs()
    }

    /**
     * 실제로 걸 프레임 간격.
     *
     * 30 fps를 목표로 하되 기기가 허용하는 최대를 넘지 않는다. `SENSOR_INFO_MAX_FRAME_DURATION`은
     * 보통 30 fps보다 훨씬 길어 그대로 통과한다.
     */
    fun frameDurationNs(): Long {
        val max = maxFrameDurationNs ?: return ManualCameraConfig.TARGET_FRAME_DURATION_NS
        return minOf(ManualCameraConfig.TARGET_FRAME_DURATION_NS, max)
    }

    /** 처음 열었을 때 보여 줄 값. 저장된 설정이 없을 때 쓴다. */
    fun defaultConfig(): ManualCameraConfig =
        coerce(
            ManualCameraConfig(
                focusDistanceDiopter = DEFAULT_FOCUS_DIOPTER,
                iso = DEFAULT_ISO,
                exposureTimeNs = ShutterPreset.ONE_SIXTIETH.exposureTimeNs,
            ),
        )

    /**
     * 설정을 이 기기가 받는 범위 안으로 좁힌다.
     *
     * 노출은 프레임 간격까지 함께 넘지 않게 자른다. 두 제약을 한곳에서 거는 것은, 화면과 저장소와
     * 카메라 요청이 각자 자르면 어디서 잘렸는지 알 수 없기 때문이다.
     */
    fun coerce(config: ManualCameraConfig): ManualCameraConfig {
        val frameDuration = frameDurationNs()
        val exposureCeiling = minOf(frameDuration, exposureRangeNs?.last ?: frameDuration)
        val exposureFloor = exposureRangeNs?.first ?: 1L
        return config.copy(
            focusDistanceDiopter = config.focusDistanceDiopter.coerceIn(0f, maxFocusDiopter),
            iso = isoRange?.let { config.iso.coerceIn(it.first, it.last) } ?: config.iso,
            exposureTimeNs = config.exposureTimeNs.coerceIn(minOf(exposureFloor, exposureCeiling), exposureCeiling),
            frameDurationNs = frameDuration,
            whiteBalance = config.whiteBalance.takeIf { whiteBalanceSupported },
        )
    }

    private companion object {
        /** 25 cm. manipulation 작업 영역이 대개 이 근처다. 기기 최대보다 멀면 그쪽으로 잘린다. */
        const val DEFAULT_FOCUS_DIOPTER = 4f

        const val DEFAULT_ISO = 100
    }
}
