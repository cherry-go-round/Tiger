package com.ssafy.s15p21a206.tiger.feature.capture.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.component.LandscapeScreenPreview
import com.ssafy.s15p21a206.tiger.core.designsystem.component.ScreenPreview

/**
 * 수집을 시작하기 전에 Task·Object를 받는다.
 *
 * 무엇을 찍는지만 묻는다. 해상도처럼 어떻게 찍는지는 초점·ISO와 함께 카메라 설정 시트에서
 * 고른다. 셋 다 Session 내내 고정되는 촬영 조건이라 한곳에서 정한다.
 *
 * 프리뷰나 카메라를 알지 못한다. 값과 콜백만 받고, 확정이 무엇을 여는지는 부모가 정한다.
 *
 * 키보드 입력이 필요한 다이얼로그는 Material 가이드라인상 전체화면으로 띄우고 확인·취소를 상단
 * 앱바에 둔다. 가운데 띄우는 다이얼로그는 가로 화면에서 키보드가 올라오면 아래쪽 버튼이 가려져
 * 닿을 방법이 없다. 앱바는 키보드와 겹치지 않으므로 방향과 무관하게 항상 누를 수 있다.
 */
@Composable
internal fun CaptureMetadataDialog(
    task: String,
    objectName: String,
    onTaskChange: (String) -> Unit,
    onObjectNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val ready = task.isNotBlank() && objectName.isNotBlank()
    val confirmFromKeyboard = {
        focusManager.clearFocus()
        onConfirm()
    }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        MetadataForm(
            task = task,
            objectName = objectName,
            ready = ready,
            onTaskChange = onTaskChange,
            onObjectNameChange = onObjectNameChange,
            onConfirm = onConfirm,
            onCancel = onCancel,
            onKeyboardDone = confirmFromKeyboard.takeIf { ready },
        )
    }
}

/**
 * 다이얼로그 창 안을 채우는 전체화면 판. 상단 앱바와 입력 칸이다.
 *
 * @param onKeyboardDone 키보드의 완료가 할 일. 두 칸이 다 채워지지 않았으면 null이다.
 */
@Composable
private fun MetadataForm(
    task: String,
    objectName: String,
    ready: Boolean,
    onTaskChange: (String) -> Unit,
    onObjectNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onKeyboardDone: (() -> Unit)?,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            MetadataTopBar(ready = ready, onConfirm = onConfirm, onCancel = onCancel)
            MetadataFields(
                task = task,
                objectName = objectName,
                onTaskChange = onTaskChange,
                onObjectNameChange = onObjectNameChange,
                onDone = onKeyboardDone,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataTopBar(
    ready: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.capture_metadata_title)) },
        navigationIcon = {
            IconButton(onClick = onCancel) {
                Icon(
                    painter = painterResource(R.drawable.ic_navigation_back),
                    contentDescription = stringResource(R.string.action_cancel),
                )
            }
        },
        actions = {
            TextButton(enabled = ready, onClick = onConfirm) { Text(stringResource(R.string.capture_metadata_confirm)) }
        },
    )
}

/**
 * Task와 Object 입력 칸.
 *
 * Task가 채워진 채로 열렸으면 손댈 곳은 다음 칸이라 Object에 커서를 둔다. 이미 적혀 있는 칸에 커서를
 * 두면 지우고 다시 쓰라는 신호로 읽힌다.
 *
 * @param onDone 키보드의 완료가 할 일. 두 칸이 다 채워지지 않았으면 null이고 완료는 아무것도 하지 않는다.
 */
@Composable
private fun MetadataFields(
    task: String,
    objectName: String,
    onTaskChange: (String) -> Unit,
    onObjectNameChange: (String) -> Unit,
    onDone: (() -> Unit)?,
) {
    val objectFieldFocus = remember { FocusRequester() }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LaunchedEffect(Unit) {
            if (task.isNotBlank()) objectFieldFocus.requestFocus()
        }
        OutlinedTextField(
            value = task,
            onValueChange = onTaskChange,
            label = { Text(stringResource(R.string.capture_metadata_task)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { objectFieldFocus.requestFocus() }),
        )
        OutlinedTextField(
            value = objectName,
            onValueChange = onObjectNameChange,
            label = { Text(stringResource(R.string.capture_metadata_object)) },
            modifier = Modifier.focusRequester(objectFieldFocus),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(onDone = { onDone?.invoke() }),
        )
    }
}

/**
 * Task 화면에서 시작해 Task가 채워진 채로 연 경우. Object가 비어 확인을 아직 누를 수 없다.
 *
 * 다이얼로그는 창을 따로 띄워 Preview에 그려지지 않으므로 그 안의 판을 그린다.
 */
@LandscapeScreenPreview
@Composable
private fun MetadataFormPreview() {
    ScreenPreview {
        MetadataForm(
            task = "컵 집기",
            objectName = "",
            ready = false,
            onTaskChange = {},
            onObjectNameChange = {},
            onConfirm = {},
            onCancel = {},
            onKeyboardDone = null,
        )
    }
}
