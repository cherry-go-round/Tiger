package com.ssafy.s15p21a206.tiger.feature.session.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.component.NavigationHeaderHeight
import com.ssafy.s15p21a206.tiger.core.designsystem.component.TigerCard
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerSpacing
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText
import com.ssafy.s15p21a206.tiger.core.model.session.SessionSummary

/**
 * 홈. 완료된 Session을 Task별로 묶어 보인다.
 *
 * 이 화면에는 헤더가 없지만 헤더만큼 비우고 본문을 시작한다. Task 화면은 헤더 아래에서 본문을
 * 시작하므로, 그래야 화면을 오갈 때 이름표가 제자리에 머무르는 것으로 보인다.
 */
@Suppress("FunctionName")
@Composable
internal fun SessionListScreen(
    sessions: List<SessionSummary>,
    onStartCapture: () -> Unit,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unknownTask = stringResource(R.string.task_name_unknown)
    val taskGroups = sessions.groupBy { it.taskName.trim() }.toSortedMap()
    Box(modifier = modifier.fillMaxSize()) {
        SectionedList(
            title = stringResource(R.string.session_list_title),
            count = stringResource(R.string.task_list_count, taskGroups.size),
            emptyMessage = stringResource(R.string.session_list_empty),
            sectionKey = HOME_SECTION_KEY,
            topPadding = NavigationHeaderHeight + LIST_CONTENT_TOP_PADDING,
            isEmpty = taskGroups.isEmpty(),
        ) {
            items(taskGroups.entries.toList(), key = { it.key }) { (taskName, taskSessions) ->
                val label = taskName.ifBlank { unknownTask }
                TaskSummaryItem(label, taskSessions.size) { onOpenTask(taskName) }
            }
        }
        NewSessionFab(
            onClick = onStartCapture,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/** 홈의 Task 카드. 섹션 이름표 아래에 놓이므로 이름은 제목보다 한 단계 작다. */
@Composable
@Suppress("FunctionName")
private fun TaskSummaryItem(
    taskName: String,
    sessionCount: Int,
    onOpenTask: () -> Unit,
) {
    TigerCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = taskName }
                .clickable(role = Role.Button, onClick = onOpenTask),
    ) {
        Column(modifier = Modifier.padding(TigerSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(CARD_LINE_GAP)) {
            Text(text = taskName, style = TigerText.itemName)
            Text(text = stringResource(R.string.task_list_session_count, sessionCount), style = TigerText.supporting)
        }
    }
}

private const val HOME_SECTION_KEY = "home-section"
