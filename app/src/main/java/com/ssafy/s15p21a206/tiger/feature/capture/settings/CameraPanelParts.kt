package com.ssafy.s15p21a206.tiger.feature.capture.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureChoiceSelected
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureChoiceSelectedInk
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureControlDisabled
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.CaptureOverlaySupporting
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText

@Suppress("FunctionName")
@Composable
internal fun PanelLabel(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PanelNote(label)
        Text(text = value, style = TigerText.overlayBadge)
    }
}

/** 이름표와 안내처럼 판 위에서 한 단계 물러난 글자. */
@Suppress("FunctionName")
@Composable
internal fun PanelNote(text: String) {
    Text(text = text, style = TigerText.overlaySupporting, color = CaptureOverlaySupporting)
}

/** 여럿 중 하나를 고르는 단추들. 한 줄에 들어가지 않으면 다음 줄로 내린다. 글자를 줄여 넣으면 장갑 낀 손으로 누르기 어려워진다. */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("FunctionName")
@Composable
internal fun PanelChoices(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

/**
 * 여럿 중 하나를 고르는 단추. 고른 것은 흰 판으로 채우고 나머지는 윤곽만 남긴다.
 *
 * 강조색(초록)으로 채우지 않는다. 그 색은 재생 버튼의 "시작"이라는 뜻을 이미 갖고 있고, 어두운
 * 판 위에서 흰 글자와의 대비도 낮아 무엇을 골랐는지가 한눈에 들어오지 않았다.
 */
@Suppress("FunctionName")
@Composable
internal fun PanelChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            enabled = enabled,
            contentPadding = CHOICE_PADDING,
            colors = ButtonDefaults.buttonColors(containerColor = CaptureChoiceSelected),
        ) {
            Text(text = label, style = TigerText.overlayBadge, color = CaptureChoiceSelectedInk)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, contentPadding = CHOICE_PADDING) {
            Text(
                text = label,
                style = TigerText.overlayBadge,
                color = if (enabled) CaptureOverlaySupporting else CaptureControlDisabled,
            )
        }
    }
}

/** 고르는 단추의 안쪽 여백. 좌우만 10dp로 두고 위아래 여백은 없앤다. */
private val CHOICE_PADDING = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
