package com.ssafy.s15p21a206.tiger.feature.session.list

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerSpacing

/**
 * 새 세션을 시작하는 FAB다.
 *
 * 새 세션은 목록 화면의 유일한 주 동작이다. Material 3은 그런 동작을 헤더의 아이콘이 아니라 FAB에
 * 두라고 하며, 아이콘 버튼은 부수 동작 자리로 본다. 글자를 함께 둬서 카메라 글리프가 촬영인지
 * 세션 시작인지 헷갈리지 않게 한다.
 *
 * 기본값인 primaryContainer는 연한 판이라 목록 위에서 흐릿하게 떠, 짙은 primary로 주 동작임을
 * 보인다. 그림자도 기본 6dp는 과해서 떠 있는 정도만 남긴다. 오른쪽 끝은 카드와 같은 [TigerSpacing.screenEdge]에 맞춘다.
 */
@Composable
@Suppress("FunctionName")
internal fun NewSessionFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.session_list_new_capture)
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_new_session),
                contentDescription = null,
            )
        },
        text = { Text(label) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation =
            FloatingActionButtonDefaults.elevation(
                defaultElevation = 2.dp,
                pressedElevation = 2.dp,
                focusedElevation = 3.dp,
                hoveredElevation = 3.dp,
            ),
        modifier = modifier.padding(TigerSpacing.screenEdge).extendedFabName(label),
    )
}

/**
 * Extended FAB의 접근성 이름을 FAB 노드 자신에 둔다.
 *
 * M3의 Extended FAB는 글자를 접었다 펴는 칸으로 다루고 그 칸을 `clearAndSetSemantics`로 감싼다.
 * 그래서 글자의 semantics는 merged 트리에서 지워지고, 글자만 두면 보조 기술에는 이름 없는 버튼으로
 * 읽힌다. 눈에 보이는 문구를 그대로 써야 음성 조작이 들은 대로 부를 수 있다.
 */
private fun Modifier.extendedFabName(label: String): Modifier = semantics { text = AnnotatedString(label) }
