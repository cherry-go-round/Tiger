package com.ssafy.s15p21a206.tiger.feature.capture.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.google.android.material.sidesheet.SideSheetDialog
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureControlDisabled
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureOverlayScrim
import com.ssafy.s15p21a206.tiger.feature.capture.CaptureTooltip

/**
 * 카메라 설정 시트를 여는 단추. Session이 도는 동안은 받지 않는다.
 *
 * 다른 수집 오버레이와 같은 판에 올린다. 윤곽선만 있는 단추는 밝은 장면의 프리뷰 위에서 묻혔다.
 */
@Suppress("FunctionName")
@Composable
internal fun CaptureCameraSettingsButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CaptureTooltip(R.string.capture_camera_title, modifier) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp).background(CaptureOverlayScrim, CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_capture_settings),
                contentDescription = stringResource(R.string.capture_camera_title),
                tint = if (enabled) Color.White else CaptureControlDisabled,
            )
        }
    }
}

/**
 * 카메라 설정을 오른쪽 모달 사이드 시트로 띄운다. 이 Composable이 composition에 있는 동안 열려 있다.
 *
 * Compose Material3에는 사이드 시트가 없어 Material Components의 [SideSheetDialog]를 쓴다. 막 탭과
 * 시스템 뒤로 가기가 [onDismiss]로 온다. 모달이라 열려 있는 동안 뒤의 제어는 받지
 * 않는다. 막은 어둡게 깔지 않는다(`Theme.Tiger.CameraSheet`). 옆의 프리뷰를 보며 값을 맞춰야 한다.
 *
 * 시트의 내용은 이 자리의 composition을 부모로 삼아 그린다. 테마와 CompositionLocal이 그대로
 * 이어지고, [content]가 바뀌면 시트 안도 다시 그려진다.
 */
@Suppress("FunctionName")
@Composable
internal fun CaptureCameraSheet(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val parent = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    DisposableEffect(view) {
        val dialog = SideSheetDialog(view.context, R.style.Theme_Tiger_CameraSheet)
        val sheetContent =
            ComposeView(dialog.context).apply {
                setParentCompositionContext(parent)
                setContent { currentContent() }
            }
        dialog.setContentView(sheetContent)
        // 가로로 끌어 닫는 동작을 끈다. 초점·ISO 슬라이더를 미는 손이 시트를 끌어 버린다.
        // 닫는 길은 X, 바깥 탭, 시스템 뒤로 가기로 충분하다.
        dialog.behavior.isDraggable = false
        dialog.setOnDismissListener { currentOnDismiss() }
        dialog.show()
        onDispose {
            // 상태가 먼저 닫힌 경우(Session 시작 등)다. 사용자가 닫은 것처럼 다시 알리지 않는다.
            dialog.setOnDismissListener(null)
            dialog.dismiss()
        }
    }
}
