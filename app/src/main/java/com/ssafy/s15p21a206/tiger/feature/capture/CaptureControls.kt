package com.ssafy.s15p21a206.tiger.feature.capture

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.component.DestructiveConfirmationDialog
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureControlDisabled
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureDestructive
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureOverlayScrim
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureStart

/**
 * 재생·일시 정지·정지 제어. 단계에 따라 둘 또는 하나를 보인다.
 *
 * 거치대 집게가 폰의 가운데를 물어 하단 중앙은 가려지므로 우측 가장자리에 세로로 쌓는다. 끝 여백
 * 16dp는 56dp 아이콘의 중심을 상단 닫기(48dp, 끝 여백 20dp)와 같은 세로축에 둔다.
 */
@Composable
@Suppress("FunctionName")
internal fun CaptureWorkspaceControls(
    policy: CaptureControlPolicy,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.safeDrawingPadding().padding(end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (policy.state) {
            CaptureWorkspaceControlState.Idle -> PlayControl(R.string.capture_control_start, policy.canPlay, onPlay)
            CaptureWorkspaceControlState.EpisodeActive -> {
                PauseControl(policy.canPause, onPause)
                StopControl(policy.canStop, onStop)
            }
            CaptureWorkspaceControlState.Initializing,
            CaptureWorkspaceControlState.Ready,
            CaptureWorkspaceControlState.Finalizing,
            -> {
                PlayControl(R.string.capture_control_resume, policy.canPlay, onPlay)
                StopControl(policy.canStop, onStop)
            }
        }
    }
}

/** Session 전에는 Session을, 그 뒤에는 Episode를 시작한다. 이름([description])만 다르다. */
@Composable
@Suppress("FunctionName")
private fun PlayControl(
    @StringRes description: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    CaptureControlIcon(R.drawable.ic_capture_play, description, onClick, enabled, tint = CaptureStart)
}

@Composable
@Suppress("FunctionName")
private fun PauseControl(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    CaptureControlIcon(R.drawable.ic_capture_pause, R.string.capture_control_pause, onClick, enabled)
}

@Composable
@Suppress("FunctionName")
private fun StopControl(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    CaptureControlIcon(R.drawable.ic_capture_stop, R.string.capture_control_stop, onClick, enabled, tint = CaptureDestructive)
}

@Composable
@Suppress("FunctionName")
private fun CaptureControlIcon(
    @DrawableRes iconRes: Int,
    @StringRes contentDescriptionRes: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    tint: Color = Color.White,
) {
    CaptureTooltip(contentDescriptionRes) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(56.dp).background(CaptureOverlayScrim, CircleShape),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(contentDescriptionRes),
                tint = if (enabled) tint else CaptureControlDisabled,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
@Suppress("FunctionName")
internal fun CaptureStopConfirmation(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DestructiveConfirmationDialog(
        title = stringResource(R.string.capture_stop_title),
        message = stringResource(R.string.capture_stop_message),
        confirmLabel = stringResource(R.string.capture_stop_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
