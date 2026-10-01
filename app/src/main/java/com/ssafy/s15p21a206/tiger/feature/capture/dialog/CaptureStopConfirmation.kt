package com.ssafy.s15p21a206.tiger.feature.capture.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.component.DestructiveConfirmationDialog

@Composable
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
