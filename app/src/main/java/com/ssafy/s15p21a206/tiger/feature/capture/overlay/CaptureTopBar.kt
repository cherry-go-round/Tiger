package com.ssafy.s15p21a206.tiger.feature.capture.overlay
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureControlDisabled
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureOverlayScrim
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText
import com.ssafy.s15p21a206.tiger.feature.capture.settings.CaptureCameraSettingsButton
import com.ssafy.s15p21a206.tiger.feature.capture.state.CaptureControlPolicy
import com.ssafy.s15p21a206.tiger.feature.capture.state.CaptureExitAction
import com.ssafy.s15p21a206.tiger.feature.capture.state.CaptureUiState
import com.ssafy.s15p21a206.tiger.feature.capture.state.CaptureWorkspaceControlState

/**
 * 프리뷰 위 상단 줄. 왼쪽 자리와 닫기 버튼을 한 Row에 담는다.
 *
 * 서로 다른 align으로 두면 배지가 길어질 때 닫기 버튼 아래로 파고든다. 그래서 왼쪽 자리는
 * `weight(fill = false)`로 제 너비만 쓴다. 기준은 프리뷰가 아니라 화면이다. 우측 제어와 좌표계를
 * 맞추고, 레터박스가 생기는 기기에서는 검은 띠 위에 얹혀 영상을 가리지 않는다.
 *
 * 왼쪽 자리는 Session 전에는 촬영 조건을 여는 톱니바퀴, 시작한 뒤에는 tracking 상태 배지가 쓴다.
 * 시작하면 조건이 잠기고, 그 전에는 상태가 늘 IDLE이라 배지가 알려 줄 것이 없다.
 */
@Composable
internal fun CaptureTopBar(
    state: CaptureUiState,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(
                    top = CaptureOverlayDimens.edgeInset,
                    start = CaptureOverlayDimens.edgeInset,
                    end = CaptureOverlayDimens.edgeInset,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.phase == CaptureWorkspaceControlState.Idle) {
                CaptureCameraSettingsButton(enabled = state.captureSettingsEditable, onClick = onOpenSettings)
            } else {
                CaptureWorkspaceStatus(state = state.phase)
            }
        }
        CaptureWorkspaceExitControls(policy = state.policy, onExit = onExit)
    }
}

/** 현재 수집 상태를 프리뷰 위에 표시한다. Tracking 안정화 여부를 사용자가 바로 알 수 있어야 한다. */
@Composable
internal fun CaptureWorkspaceStatus(
    state: CaptureWorkspaceControlState,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(state.statusLabelRes)
    Text(
        text = label,
        style = TigerText.overlayBadge,
        modifier =
            modifier
                .background(CaptureOverlayScrim, CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { contentDescription = label },
    )
}

@get:StringRes
private val CaptureWorkspaceControlState.statusLabelRes: Int
    get() =
        when (this) {
            CaptureWorkspaceControlState.Idle -> R.string.capture_status_idle
            CaptureWorkspaceControlState.Initializing -> R.string.capture_status_initializing
            CaptureWorkspaceControlState.Ready -> R.string.capture_status_ready
            CaptureWorkspaceControlState.EpisodeActive -> R.string.capture_status_episode_active
            CaptureWorkspaceControlState.Finalizing -> R.string.capture_status_finalizing
        }

/**
 * 작업 공간을 닫는 버튼과 시스템 뒤로 가기. 둘 다 [CaptureControlPolicy.exitAction]을 따른다.
 *
 * 배경 없는 글리프는 영상과 레터박스 경계에 걸쳐 떠 보이므로 다른 오버레이와 같은 판에 올린다.
 */
@Composable
internal fun CaptureWorkspaceExitControls(
    policy: CaptureControlPolicy,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = policy.exitAction != CaptureExitAction.Ignore
    BackHandler { if (enabled) onExit() }
    val description = stringResource(R.string.capture_close_content_description)
    CaptureTooltip(R.string.capture_close_content_description, modifier) {
        IconButton(
            onClick = onExit,
            enabled = enabled,
            modifier =
                Modifier
                    .size(CaptureOverlayDimens.glyphButtonSize)
                    .background(CaptureOverlayScrim, CircleShape)
                    .semantics { contentDescription = description },
        ) {
            Text(
                text = stringResource(R.string.control_close),
                style = TigerText.overlayGlyph,
                color = if (enabled) Color.White else CaptureControlDisabled,
            )
        }
    }
}
