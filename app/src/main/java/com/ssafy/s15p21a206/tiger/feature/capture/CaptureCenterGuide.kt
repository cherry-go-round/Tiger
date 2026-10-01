package com.ssafy.s15p21a206.tiger.feature.capture

import android.graphics.Point
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureCenterGuide
import kotlin.math.roundToInt

/**
 * 폰을 거치대에 물릴 때 가운데를 맞추는 기준선.
 *
 * 폰 몸체의 가운데와 일치해야 하므로 앱 창이 아니라 디스플레이의 가운데에 둔다. 창의 가운데는
 * 믿을 수 없다. edge-to-edge가 강제되지 않는 기기에서는 창이 카메라 구멍과 내비게이션 바만큼
 * 비대칭으로 잘려, SM-G973N 가로 화면에서 창 가운데가 디스플레이 가운데보다 7px 왼쪽에 있었다.
 *
 * [modifier]가 차지하는 영역 안에서 세로로 걸치며, 영역의 화면 좌표를 재 디스플레이 가운데로
 * 옮긴다. 화면에만 그리므로 저장되는 영상에는 들어가지 않는다. 장식이라 접근성 트리에 내놓지 않는다.
 */
@Composable
internal fun CaptureCenterGuide(modifier: Modifier = Modifier) {
    val view = LocalView.current
    var shift by remember { mutableIntStateOf(0) }
    Box(
        modifier =
            modifier.fillMaxSize().onGloballyPositioned { area ->
                val areaCenter = area.localToScreen(Offset(area.size.width / 2f, 0f)).x
                shift = (view.displayWidth() / 2f - areaCenter).roundToInt()
            },
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(shift, 0) }
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(CaptureCenterGuide)
                    .testTag(CAPTURE_CENTER_GUIDE_TAG),
        )
    }
}

/** 창이 아니라 디스플레이 전체의 너비. 카메라 구멍과 시스템 바 자리를 포함한다. */
private fun View.displayWidth(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context
            .getSystemService(WindowManager::class.java)
            .maximumWindowMetrics.bounds
            .width()
    } else {
        @Suppress("DEPRECATION")
        Point().also { display.getRealSize(it) }.x
    }

/** 기준선은 글자도 라벨도 없어 테스트가 찾을 이름이 따로 필요하다. */
internal const val CAPTURE_CENTER_GUIDE_TAG = "capture_center_guide"
