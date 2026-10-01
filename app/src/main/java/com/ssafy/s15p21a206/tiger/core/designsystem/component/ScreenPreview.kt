package com.ssafy.s15p21a206.tiger.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerSurface
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerTheme

/**
 * 화면 하나를 그리는 Preview 판. 앱이 목적지마다 까는 판과 같은 테마와 바닥이다.
 *
 * 배경화면 색을 끄는 이유는 [ComponentPreview]와 같다. 수집 화면은 프리뷰 뒤가 검으므로 [background]를
 * 검게 준다. 크기는 [PortraitScreenPreview]와 [LandscapeScreenPreview]가 정한다.
 */
@Composable
@Suppress("FunctionName")
internal fun ScreenPreview(
    background: Color = TigerSurface.listBackground,
    content: @Composable () -> Unit,
) {
    TigerTheme(dynamicColor = false) {
        Surface(modifier = Modifier.fillMaxSize(), color = background, content = content)
    }
}

/** 세로 화면 하나. 조회 화면은 manifest가 세로로 묶는다. */
@Preview(device = Devices.PHONE)
internal annotation class PortraitScreenPreview

/** 가로 화면 하나. 수집 작업 공간은 열려 있는 동안 가로로 고정된다. */
@Preview(device = "${Devices.PHONE},orientation=landscape")
internal annotation class LandscapeScreenPreview
