package com.ssafy.s15p21a206.tiger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerSpacing
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText

/**
 * 이름표를 첫 항목으로 두고 그 아래로 카드를 쌓는 목록. 목록 화면(홈, Task의 Session 목록)이 같은 짜임을 쓴다.
 *
 * 이름표를 목록 바깥에 두면 홀로 뜬 머리띠가 되고, FAB가 아래로 내려간 뒤로는 같은 줄에 짝이 될
 * 것도 없다. 목록의 첫 항목으로 넣어 카드와 같은 기둥 안에서 함께 흐르게 한다. 아래 여백은 FAB가
 * 마지막 카드를 가리지 않을 만큼 둔다.
 *
 * 비어 있으면 목록이 아니다. 이름표는 제자리에 두고 안내만 남은 공간 가운데로 보낸다. 목록 항목처럼
 * 왼쪽 위에 붙여 두면 채워질 자리를 기다리는 빈 행으로 읽힌다. 그때는 아래 안내가 같은 말을 하므로
 * 이름표 아래에 개수([count])를 달지 않는다.
 *
 * @param reservedTop 이름표 위에 더 비워 둘 높이. 헤더가 없는 화면은 헤더 높이만큼 비워, 헤더가 있는 화면과
 *   이름표 높이를 맞춘다.
 */
@Composable
internal fun SectionedList(
    title: String,
    count: String,
    emptyMessage: String,
    sectionKey: String,
    reservedTop: Dp = 0.dp,
    isEmpty: Boolean,
    content: LazyListScope.() -> Unit,
) {
    if (isEmpty) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = TigerSpacing.screenEdge,
                        end = TigerSpacing.screenEdge,
                        top = reservedTop + CONTENT_TOP_PADDING,
                        bottom = FAB_CLEARANCE,
                    ),
        ) {
            ListSectionHeader(
                modifier = Modifier.padding(bottom = SECTION_HEADER_BOTTOM_PADDING),
                title = title,
                supporting = null,
            )
            EmptyListMessage(emptyMessage, Modifier.weight(1f))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = TigerSpacing.screenEdge,
                    end = TigerSpacing.screenEdge,
                    top = reservedTop + CONTENT_TOP_PADDING,
                    bottom = FAB_CLEARANCE,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = sectionKey) {
                ListSectionHeader(
                    modifier = Modifier.padding(bottom = SECTION_HEADER_BOTTOM_PADDING),
                    title = title,
                    supporting = count,
                )
            }
            content()
        }
    }
}

/**
 * 목록이 비었을 때 남은 공간 가운데에 놓는 안내다.
 *
 * 목록 항목처럼 왼쪽 위에 붙여 두면 곧 채워질 자리를 기다리는 빈 행으로 읽힌다. 화면에 혼자 있는
 * 문장이므로 카드 안 메타 정보와 같은 크기일 이유도 없다.
 */
@Composable
private fun EmptyListMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = TigerText.bodyMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 목록 이름표가 첫 카드에 붙지 않도록 두는 여백.
 *
 * 목록 자체의 8dp 간격에 더해져 카드와 카드 사이보다 넓어야 한 묶음을 이끄는 줄로 읽힌다.
 */
private val SECTION_HEADER_BOTTOM_PADDING = 8.dp

/** 이름표가 화면 맨 위나 헤더에 붙지 않도록 본문 위에 두는 여백. */
private val CONTENT_TOP_PADDING = 8.dp

/** FAB가 마지막 카드나 안내를 가리지 않도록 목록 아래에 두는 여백. */
private val FAB_CLEARANCE = 88.dp
