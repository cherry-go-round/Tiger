package com.ssafy.s15p21a206.tiger.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureFullScreenScrim
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText

/**
 * 마감과 그 실패를 알리는 판이다.
 *
 * 마감 중에는 어떤 제어도 받지 않는다([CaptureControlPolicy]가 `canPlay`·`canStop`을 모두 막고
 * 이탈도 무시한다). 그런데 진행 표시를 제어 아이콘과 같은 줄, 같은 크기로 두면 누를 수 있는 것처럼
 * 보인다. 화면을 덮어 아무것도 받지 않는 상태임을 그대로 드러낸다.
 *
 * 진행률은 쓰지 않는다. 오래 걸리는 구간이 둘인데 `MediaRecorder.stop()`은 진행을 알려 주지 않아,
 * 막대를 쓰면 한참 0%에 멈춰 있다가 뛴다. 멈춘 막대는 "잴 수 없다"가 아니라 "멈췄다"로 읽힌다.
 *
 * [failure]가 있으면 실패를 알리고 [onDismissFailure]까지 남는다. 마감에 실패하면 저장된 세션이
 * 없으므로 넘어갈 곳이 없다. 작업 공간에 머물러야 프리뷰가 살아 있는 채로 다시 찍을 수 있다.
 */
@Composable
@Suppress("FunctionName")
internal fun CaptureFinalizingOverlay(
    failure: String?,
    onDismissFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(CaptureFullScreenScrim)
                .blockTouchesBelow()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        if (failure == null) FinalizingProgress() else FinalizeFailure(failure, onDismissFailure)
    }
}

/** 덮은 아래의 제어가 눌리지 않게 입력을 여기서 삼킨다. */
private fun Modifier.blockTouchesBelow(): Modifier = clickable(enabled = false, onClick = {})

@Composable
@Suppress("FunctionName")
private fun FinalizingProgress() {
    val finalizing = stringResource(R.string.capture_finalizing)
    CircularProgressIndicator(
        modifier = Modifier.size(48.dp).semantics { contentDescription = finalizing },
        color = Color.White,
    )
    Text(text = finalizing, style = TigerText.overlayTitle)
    Text(
        text = stringResource(R.string.capture_finalizing_warning),
        style = TigerText.overlaySupporting,
    )
}

@Composable
@Suppress("FunctionName")
private fun FinalizeFailure(
    failure: String,
    onDismiss: () -> Unit,
) {
    Text(text = failure, style = TigerText.overlayTitle)
    Button(onClick = onDismiss) { Text(stringResource(R.string.action_confirm)) }
}
