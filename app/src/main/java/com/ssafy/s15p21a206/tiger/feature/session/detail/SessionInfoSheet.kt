package com.ssafy.s15p21a206.tiger.feature.session.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.component.ComponentPreview
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerSurface
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText
import com.ssafy.s15p21a206.tiger.core.model.session.SessionSummary
import com.ssafy.s15p21a206.tiger.feature.session.SessionPreviewSamples
import com.ssafy.s15p21a206.tiger.feature.session.formatCaptureTime
import com.ssafy.s15p21a206.tiger.feature.session.video.VideoResolutionState
import com.ssafy.s15p21a206.tiger.feature.session.video.rememberVideoResolution

/**
 * 세션을 특정해 주지 않는 값들을 담는 시트다.
 *
 * Episode 수·길이·해상도·전체 ID는 세션을 고를 때가 아니라 확인하러 들어왔을 때만 필요하다.
 * 사진 앱이 ⓘ 뒤에 두는 것과 같은 성격이라 상세 본문에서 빼고 여기로 옮겼다.
 *
 * 행 사이는 12dp다. 값이 제목보다 한 단계 작아 사이도 좁혀야 이름표-값 간격과의 대비가 유지된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionInfoSheet(
    summary: SessionSummary,
    durationSeconds: Long,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = TigerSurface.content) {
        SessionInfoContent(summary, durationSeconds, rememberVideoResolution(summary.bundlePath))
    }
}

/** 시트 안의 내용. 해상도는 파일에서 읽어야 하므로 읽은 결과([resolution])만 받는다. */
@Composable
private fun SessionInfoContent(
    summary: SessionSummary,
    durationSeconds: Long,
    resolution: VideoResolutionState,
) {
    Column(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.session_info_title),
            style = TigerText.itemName,
        )
        SessionInfoRow(stringResource(R.string.session_info_id), summary.sessionId)
        SessionInfoRow(stringResource(R.string.session_info_captured_at), formatCaptureTime(summary.recordedAtEpochMs))
        SessionInfoRow(
            label = stringResource(R.string.session_info_episodes),
            value = stringResource(R.string.session_info_episode_count, summary.completedEpisodeCount),
        )
        SessionInfoRow(
            label = stringResource(R.string.session_info_duration),
            value = stringResource(R.string.session_detail_duration, durationSeconds),
        )
        SessionInfoRow(
            label = stringResource(R.string.session_info_resolution),
            value = resolutionText(resolution),
        )
    }
}

@Composable
private fun resolutionText(resolution: VideoResolutionState): String =
    when (resolution) {
        is VideoResolutionState.Available ->
            stringResource(R.string.session_detail_resolution, resolution.width, resolution.height)
        VideoResolutionState.Loading -> stringResource(R.string.session_detail_resolution_loading)
        VideoResolutionState.Unavailable -> stringResource(R.string.session_detail_resolution_unavailable)
    }

/**
 * 시트의 한 행. 이름표 아래 값을 둔다.
 *
 * 값은 시트 제목보다 한 단계 작다. 그래야 제목이 이 시트의 유일한 최상위가 되고, 큰 글자가 사다리처럼
 * 쌓이지 않는다. 이름표와는 크기가 같고 잉크로만 갈려 두 줄이 한 묶음으로 붙는다. 36자 식별자도 한 줄에
 * 들어가 행 높이가 고르게 된다.
 */
@Composable
private fun SessionInfoRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = TigerText.supporting)
        Text(text = value, style = TigerText.value)
    }
}

/** 시트는 창을 따로 띄워 Preview에 그려지지 않으므로 내용만 시트와 같은 바닥에 올린다. */
@Preview
@Composable
private fun SessionInfoContentPreview() {
    val summary = SessionPreviewSamples.sessions.first()
    ComponentPreview {
        Box(Modifier.background(TigerSurface.content).padding(top = 16.dp)) {
            SessionInfoContent(
                summary = summary,
                durationSeconds = SessionDetailPresentation.from(summary).durationSeconds,
                resolution = VideoResolutionState.Available(width = 1920, height = 1080),
            )
        }
    }
}
