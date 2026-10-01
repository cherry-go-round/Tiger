package com.ssafy.s15p21a206.tiger.feature.capture

import androidx.compose.ui.unit.dp

/**
 * 프리뷰 위 오버레이의 크기와 여백. 상단 줄과 우측 제어가 서로 맞물려 있어 한곳에 둔다.
 */
internal object CaptureOverlayDimens {
    /** 상단 줄이 화면 위와 좌우에서 떨어지는 거리. */
    val edgeInset = 20.dp

    /** 상단 글리프 버튼(닫기, 카메라 설정)의 크기. */
    val glyphButtonSize = 48.dp

    /** 우측 제어 버튼(재생·일시 정지·정지)의 크기. 거치한 채 누르므로 상단 버튼보다 크다. */
    val controlButtonSize = 56.dp

    /** 우측 제어의 끝 여백. 제어 버튼의 중심을 상단 닫기 버튼의 중심과 같은 세로축에 둔다. */
    val controlEndInset = edgeInset + (glyphButtonSize - controlButtonSize) / 2
}
