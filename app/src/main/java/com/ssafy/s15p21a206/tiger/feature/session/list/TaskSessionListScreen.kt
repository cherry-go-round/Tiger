package com.ssafy.s15p21a206.tiger.feature.session.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.component.NavigationHeader
import com.ssafy.s15p21a206.tiger.core.designsystem.component.TigerCard
import com.ssafy.s15p21a206.tiger.core.designsystem.component.TigerMenuItem
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerSpacing
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText
import com.ssafy.s15p21a206.tiger.core.model.session.SessionSummary
import com.ssafy.s15p21a206.tiger.feature.session.SessionDeleteAction
import com.ssafy.s15p21a206.tiger.feature.session.SessionDeleteConfirmation
import com.ssafy.s15p21a206.tiger.feature.session.formatCaptureTime
import com.ssafy.s15p21a206.tiger.feature.session.labelRes

/**
 * 한 Task의 Session 목록.
 *
 * 이름은 본문의 이름표가 말하므로 헤더에는 뒤로 가기만 남긴다. 같은 Task를 한 번 더 찍으려고 홈까지
 * 나갔다 올 이유가 없어 여기에도 새 세션 FAB를 둔다. 여기서 시작하면 Task 이름은 이미 알고 있어 다시
 * 입력하지 않는다. 마지막 세션을 지우면 빈 채로 남으므로 홈과 같은 짜임으로 안내를 가운데에 둔다.
 */
@Composable
@Suppress("FunctionName")
internal fun TaskSessionListScreen(
    taskName: String,
    sessions: List<SessionSummary>,
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onStartCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = taskName.ifBlank { stringResource(R.string.task_name_unknown) }
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NavigationHeader(title = "", onBack = onBack)
            SectionedList(
                title = title,
                count = stringResource(R.string.session_list_count, sessions.size),
                emptyMessage = stringResource(R.string.task_session_list_empty),
                sectionKey = TASK_SESSIONS_SECTION_KEY,
                topPadding = LIST_CONTENT_TOP_PADDING,
                isEmpty = sessions.isEmpty(),
            ) {
                items(sessions, key = SessionSummary::sessionId) { summary ->
                    SessionSummaryItem(summary, onOpenSession, onDeleteSession)
                }
            }
        }
        NewSessionFab(
            onClick = onStartCapture,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/**
 * 목록의 Session 카드다. 열기는 탭, 삭제는 길게 누르기에 둔다.
 *
 * 카드마다 삭제 표를 상주시키지 않는다. 이 목록의 주 동작은 여는 것이고, 모든 행에 부수 동작이
 * 붙으면 목록이 점으로 얼룩진다. 길게 누르기는 안드로이드에서 항목별 동작을 여는 관용구다.
 *
 * 밀어서 삭제는 쓰지 않는다. 그 제스처는 실행취소가 따라온다는 약속을 달고 다니는데, 여기서는
 * 번들을 실제로 지우므로 되돌릴 수 없다. 확인 판을 붙이면 제스처의 약속을 깨고, 붙이지 않으면
 * 스크롤 오조작이 그대로 영구 삭제가 된다.
 *
 * 발견성은 상세 화면의 메뉴가 담당한다. 여기 제스처는 아는 사람을 위한 지름길이다.
 */
@Composable
@Suppress("FunctionName")
private fun SessionSummaryItem(
    summary: SessionSummary,
    onOpenSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
) {
    val deleteAction = SessionDeleteAction.from(summary.uploadState)
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SessionDeleteAction?>(null) }
    var pressPosition by remember { mutableStateOf(IntOffset.Zero) }
    Box {
        SessionSummaryCard(
            summary = summary,
            onOpen = { onOpenSession(summary.sessionId) },
            onLongPress = { menuExpanded = true },
            onPress = { pressPosition = it },
        )
        SessionCardMenu(
            expanded = menuExpanded,
            anchor = pressPosition,
            deleteEnabled = deleteAction != null,
            onDismiss = { menuExpanded = false },
            onDelete = {
                menuExpanded = false
                pendingDelete = deleteAction
            },
        )
    }
    pendingDelete?.let { action ->
        SessionDeleteConfirmation(
            action = action,
            onConfirm = {
                pendingDelete = null
                onDeleteSession(summary.sessionId)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/** 탭하면 열고 길게 누르면 메뉴를 부르는 카드. 누른 자리를 [onPress]로 알려 메뉴가 그 자리에서 열리게 한다. */
@Composable
@Suppress("FunctionName")
private fun SessionSummaryCard(
    summary: SessionSummary,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onPress: (IntOffset) -> Unit,
) {
    val captureTime = formatCaptureTime(summary.recordedAtEpochMs)
    val sessionLabel = stringResource(R.string.session_list_item_content_description, captureTime)
    TigerCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = sessionLabel }
                .recordPressPosition(onPress)
                .combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = stringResource(R.string.session_delete),
                    onLongClick = onLongPress,
                    onClick = onOpen,
                ),
    ) {
        SessionSummaryCardContent(summary, captureTime)
    }
}

/**
 * 누른 자리를 [onPress]로 알린다. 기록만 하고 소비하지 않는다.
 *
 * Initial pass에서 보기만 하므로 뒤에 붙는 `combinedClickable`의 탭·길게 누르기와 ripple이 그대로 동작한다.
 */
private fun Modifier.recordPressPosition(onPress: (IntOffset) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) {
                    onPress(
                        event.changes
                            .first()
                            .position
                            .round(),
                    )
                }
            }
        }
    }

/**
 * 카드 안의 배치. 행으로 종류를 가른다. 묶고 싶은 것은 붙여 두고, 다른 종류는 떼어 둔다. 붙어 있는
 * 것이 곧 한 묶음으로 읽힌다.
 *
 * - 첫 행은 이 수집이 무엇이고 지금 어떤가다. 수집 일시를 이름으로 두고, 전송 상태를 오른쪽 끝
 *   (Material 3 리스트의 trailing 자리)에 둔다. 상태는 이 행에서 변하는 유일한 값이고, 목록을 훑는
 *   이유가 무엇이 올라갔는지 보는 것이다. 왼쪽 더미에 끼우면 한글 줄(`업로드`의 ㅇ)만 둥근 획 때문에
 *   들여쓴 것처럼 보이기도 한다. 두 글자는 크기가 달라 baseline을 맞춘다. 위를 맞추면 작은 글자가 떠
 *   보인다.
 * - 그 아래 줄들은 변하지 않는 메타(Object, 짧은 ID)다. 왼쪽에 세로로 붙여 쌓아 한 묶음으로 둔다.
 *   값을 부호로 이어 붙이지 않고 값마다 제 줄을 준다.
 * - 메타에는 `Object`·`ID` 같은 이름표 낱말을 적지 않는다. 카드마다 되풀이되는 비계이고, 값이 제
 *   성격을 말한다. Object는 한 단계, ID는 두 단계 옅은 잉크로 두고 ID 앞의 `#`가 번호임을 말한다.
 *   상세에서는 한 번만 나오므로 이름표를 적는다.
 * - Object가 비어 있으면(두 이름을 Session 행에 저장하기 전에 마감된 세션) 빈 이름표를 세우지 않고
 *   줄째로 뺀다.
 * - 스타일은 셋만 쓴다. 이름([TigerText.itemName]), 딸린 값([TigerText.supporting], Object와 전송
 *   상태가 함께 쓴다), 식별자([TigerText.identifier]). 요소마다 새 조합을 만들면 위계가 아니라
 *   잡음으로 읽힌다.
 */
@Composable
@Suppress("FunctionName")
private fun SessionSummaryCardContent(
    summary: SessionSummary,
    captureTime: String,
) {
    Column(modifier = Modifier.padding(TigerSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(CARD_LINE_GAP)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = captureTime,
                style = TigerText.itemName,
                modifier =
                    Modifier
                        .weight(1f)
                        .alignByBaseline(),
            )
            Text(
                text = stringResource(summary.uploadState.labelRes),
                style = TigerText.supporting,
                modifier = Modifier.alignByBaseline(),
            )
        }
        if (summary.objectName.isNotBlank()) {
            Text(text = summary.objectName, style = TigerText.supporting)
        }
        Text(
            text = stringResource(R.string.session_short_id_tag, summary.sessionId.take(8)),
            style = TigerText.identifier,
        )
    }
}

/**
 * 카드를 길게 누르면 여는 메뉴. 누른 손가락 자리([anchor])에서 열린다.
 *
 * 카드를 앵커로 쓰면 폭이 화면을 꽉 채우므로 어디를 눌렀든 카드 왼쪽 아래 구석에서 열려, 방금 건드린
 * 곳과 메뉴가 멀리 떨어진다. 그래서 크기 0짜리 앵커를 누른 지점에 두고 거기에 건다. 업로드가 번들을
 * 읽고 있는 동안은 삭제를 끈다. 이유는 카드의 전송 상태가 말한다.
 */
@Composable
@Suppress("FunctionName")
private fun SessionCardMenu(
    expanded: Boolean,
    anchor: IntOffset,
    deleteEnabled: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(modifier = Modifier.offset { anchor }) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            TigerMenuItem(
                label = stringResource(R.string.session_delete),
                icon = R.drawable.ic_session_delete,
                enabled = deleteEnabled,
                destructive = true,
                onClick = onDelete,
            )
        }
    }
}

private const val TASK_SESSIONS_SECTION_KEY = "task-sessions-section"
