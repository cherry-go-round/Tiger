package com.ssafy.s15p21a206.tiger.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * 조회 화면(목록·상세)이 함께 쓰는 여백.
 *
 * 화면마다 같은 숫자를 따로 적으면 한쪽만 바뀌어 가장자리가 어긋난다. 여기서 고르면 목록 카드, 상세
 * 묶음, FAB가 함께 움직인다. 한 화면에서만 쓰는 간격은 그 화면에 둔다.
 */
object TigerSpacing {
    /** 화면 좌우 가장자리. 목록, 상세 본문, FAB의 오른쪽 끝이 이 선에 선다. */
    val screenEdge = 16.dp

    /** 카드가 안쪽에 두는 여백. 목록 카드와 상세 묶음 카드가 같은 값이어야 같은 판으로 읽힌다. */
    val cardPadding = 16.dp
}
