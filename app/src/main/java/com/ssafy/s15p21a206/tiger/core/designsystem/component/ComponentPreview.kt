package com.ssafy.s15p21a206.tiger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerSurface
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerTheme

/**
 * 컴포넌트 Preview가 그 위에 놓이는 판. 앱 화면과 같은 테마와 바닥을 깐다.
 *
 * 배경화면 색을 끈다. 켜 두면 Android 12 이상에서 Preview를 그리는 환경의 배경화면 색을 받아, 같은
 * 컴포넌트가 볼 때마다 다른 강조색으로 그려질 수 있다. 바닥은 조회 화면과 같은
 * [TigerSurface.listBackground]다. 흰 카드가 그 위에 놓여야 카드의 경계가 보인다.
 */
@Composable
@Suppress("FunctionName")
internal fun ComponentPreview(content: @Composable ColumnScope.() -> Unit) {
    TigerTheme(dynamicColor = false) {
        Column(
            modifier = Modifier.background(TigerSurface.listBackground).padding(PREVIEW_PADDING),
            verticalArrangement = Arrangement.spacedBy(PREVIEW_GAP),
            content = content,
        )
    }
}

/** 판의 가장자리와 컴포넌트 사이. */
private val PREVIEW_PADDING = 16.dp

/** 한 Preview 안에서 경우를 나란히 보일 때 그 사이. */
private val PREVIEW_GAP = 16.dp
