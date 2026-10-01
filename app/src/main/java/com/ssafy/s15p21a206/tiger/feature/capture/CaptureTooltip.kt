package com.ssafy.s15p21a206.tiger.feature.capture

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/** 길게 누르면 [label]을 보여 준다. 수집 화면의 글리프 버튼들이 이름을 드러내는 방법이다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaptureTooltip(
    @StringRes label: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(stringResource(label)) } },
        state = rememberTooltipState(),
        modifier = modifier,
        content = content,
    )
}
