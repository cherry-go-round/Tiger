package com.ssafy.s15p21a206.tiger.feature.session.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.designsystem.component.LabelledGroup
import com.ssafy.s15p21a206.tiger.core.designsystem.component.LabelledValue
import com.ssafy.s15p21a206.tiger.core.designsystem.component.NavigationHeader
import com.ssafy.s15p21a206.tiger.core.designsystem.component.PortraitScreenPreview
import com.ssafy.s15p21a206.tiger.core.designsystem.component.ScreenPreview
import com.ssafy.s15p21a206.tiger.core.designsystem.component.TigerMenuItem
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerSpacing
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText
import com.ssafy.s15p21a206.tiger.core.model.session.SessionSummary
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState
import com.ssafy.s15p21a206.tiger.feature.session.SessionDeleteAction
import com.ssafy.s15p21a206.tiger.feature.session.SessionDeleteConfirmation
import com.ssafy.s15p21a206.tiger.feature.session.SessionPreviewSamples
import com.ssafy.s15p21a206.tiger.feature.session.formatCaptureTime
import com.ssafy.s15p21a206.tiger.feature.session.labelRes
import com.ssafy.s15p21a206.tiger.feature.session.video.SharedVideoPlayer
import com.ssafy.s15p21a206.tiger.feature.session.video.VideoPlayer
import com.ssafy.s15p21a206.tiger.feature.session.video.playableMainVideo
import com.ssafy.s15p21a206.tiger.feature.session.video.rememberSharedVideoPlayer
import com.ssafy.s15p21a206.tiger.feature.session.video.rememberVideoAspectRatio

/**
 * 한 Session의 상세. 영상 아래에 이름(수집 일시)과 이름표를 단 묶음 카드를 쌓는다.
 *
 * - 어느 세션인지는 본문의 이름표가 말하므로 헤더에는 뒤로 가기와 메뉴만 남긴다.
 * - Task와 Object를 본문에 둔다. 수집을 마감하면 목록을 지나지 않고 이 화면으로 바로 오므로, 방금
 *   찍은 것이 맞는지 확인할 자리가 여기뿐이다. 목록 카드와 달리 Task도 적는다. 이 화면은 Task 이름을
 *   단 화면 안에 있지 않아 스스로 말하지 않으면 알 길이 없다.
 * - 상세는 훑는 목록이 아니라 줄을 아낄 이유가 없다. 이름표와 값을 쌓는다. 상세 정보 시트도 같은 형태다.
 * - 묶음마다 이름표를 단다. 이 수집이 무엇인지 말하는 값(Task·Object·ID)과 지금 어떤지 말하는
 *   줄(전송 상태)은 성격이 다르다. 이름표는 읽을 대상이 아니라 표지라 묶음 안 글보다 작고 옅다.
 * - 이름과 각 묶음은 같은 간격([DETAIL_GROUP_GAP])으로 띄운다. 가르는 일은 카드가 한다.
 * - 재생 영역 높이와 전송 상태에 따라 내용이 화면을 넘으므로 굴린다.
 */
@Suppress("FunctionName")
@Composable
internal fun SessionDetailScreen(
    summary: SessionSummary?,
    onBack: () -> Unit,
    onUpload: () -> Unit,
    onDelete: () -> Unit,
    deleteFailureReason: String?,
    uploadFailureReason: String?,
    sharedPlayer: SharedVideoPlayer,
    onOpenFullscreenVideo: () -> Unit,
) {
    if (summary == null) {
        MissingSessionDetail(onBack)
        return
    }
    SessionDetail(
        summary = summary,
        onBack = onBack,
        onUpload = onUpload,
        onDelete = onDelete,
        deleteFailureReason = deleteFailureReason,
        uploadFailureReason = uploadFailureReason,
        sharedPlayer = sharedPlayer,
        onOpenFullscreenVideo = onOpenFullscreenVideo,
    )
}

/** 목록에서 사라진 세션을 연 경우. 보일 것도 할 일도 없어 헤더에는 뒤로 가기만 둔다. */
@Composable
@Suppress("FunctionName")
private fun MissingSessionDetail(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        NavigationHeader(title = "", onBack = onBack)
        Text(
            text = stringResource(R.string.session_detail_unavailable),
            style = TigerText.bodyMuted,
            modifier = Modifier.padding(horizontal = TigerSpacing.screenEdge),
        )
    }
}

@Composable
@Suppress("FunctionName")
private fun SessionDetail(
    summary: SessionSummary,
    onBack: () -> Unit,
    onUpload: () -> Unit,
    onDelete: () -> Unit,
    deleteFailureReason: String?,
    uploadFailureReason: String?,
    sharedPlayer: SharedVideoPlayer,
    onOpenFullscreenVideo: () -> Unit,
) {
    var showSessionInfo by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SessionDeleteAction?>(null) }
    val presentation = SessionDetailPresentation.from(summary)
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        NavigationHeader(title = "", onBack = onBack) {
            SessionDetailMenu(
                deleteAction = presentation.deleteAction,
                onOpenSessionInfo = { showSessionInfo = true },
                onRequestDelete = { pendingDelete = it },
            )
        }
        SessionVideoPreview(summary.bundlePath, sharedPlayer, onOpenFullscreenVideo)
        SessionDetailBody(summary, presentation, deleteFailureReason, uploadFailureReason, onUpload)
    }
    if (showSessionInfo) {
        SessionInfoSheet(
            summary = summary,
            durationSeconds = presentation.durationSeconds,
            onDismiss = { showSessionInfo = false },
        )
    }
    PausePlaybackWhileConfirmingDelete(pendingDelete, sharedPlayer)
    pendingDelete?.let { action ->
        SessionDeleteConfirmation(
            action = action,
            onConfirm = {
                pendingDelete = null
                onDelete()
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

/**
 * 상세 정보 시트와 삭제를 담는 헤더 메뉴다.
 *
 * 둘 다 이 화면의 주 동작이 아니다. 본문의 전송 버튼이 주 동작을 맡고 있고, 이쪽은 확인하거나
 * 정리하러 들어왔을 때만 찾는다. 제목이 없는 헤더에 흐린 글리프를 나란히 세우면 둘 다 무엇인지
 * 추측해야 하는 표가 된다. 메뉴로 접으면 글자로 이름이 붙고 헤더에는 뒤로 가기만 남는다. 메뉴
 * 글리프는 뒤로 가기와 같은 급으로 보이지 않도록 작고 옅게 둔다.
 *
 * 삭제는 되돌릴 수 없으므로 메뉴를 여는 한 단계가 더 있는 편이 낫다. 업로드가 번들을 읽고 있는
 * 동안은 누를 수 없으며, 흐린 아이콘과 달리 흐린 글자는 무엇이 막혔는지를 스스로 말한다.
 */
@Composable
@Suppress("FunctionName")
private fun SessionDetailMenu(
    deleteAction: SessionDeleteAction?,
    onOpenSessionInfo: () -> Unit,
    onRequestDelete: (SessionDeleteAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_actions),
                contentDescription = stringResource(R.string.session_detail_more_actions),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(HEADER_MENU_ICON_SIZE),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TigerMenuItem(
                label = stringResource(R.string.session_info_title),
                icon = R.drawable.ic_session_info,
                onClick = {
                    expanded = false
                    onOpenSessionInfo()
                },
            )
            TigerMenuItem(
                label = stringResource(R.string.session_delete),
                icon = R.drawable.ic_session_delete,
                enabled = deleteAction != null,
                destructive = true,
                onClick = {
                    expanded = false
                    deleteAction?.let(onRequestDelete)
                },
            )
        }
    }
}

/**
 * 영상. 좌우 여백 없이 화면 폭을 다 쓴다. 16:9 안에 컨트롤이 오버레이로 놓이므로 여백을 주면 재생
 * 영역만 줄고 얻는 것이 없다. 영상이 없으면 그렇다고 알리는 문구는 본문이라 본문 여백을 둔다.
 */
@Composable
@Suppress("FunctionName")
private fun SessionVideoPreview(
    bundlePath: String,
    sharedPlayer: SharedVideoPlayer,
    onOpenFullscreenVideo: () -> Unit,
) {
    val videoFile = playableMainVideo(bundlePath)
    val videoDescription = stringResource(R.string.session_detail_video_content_description)
    if (videoFile == null) {
        Text(
            text = stringResource(R.string.session_detail_video_unavailable),
            style = TigerText.bodyMuted,
            modifier = Modifier.padding(horizontal = TigerSpacing.screenEdge),
        )
        return
    }
    val player = sharedPlayer.playerFor(videoFile)
    VideoPlayer(
        player = player,
        fullscreen = false,
        onFullscreenClick = onOpenFullscreenVideo,
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(rememberVideoAspectRatio(player))
                .semantics { contentDescription = videoDescription },
    )
}

/** 영상 아래 본문. 이름, 세션 정보, 전송 상태, 삭제 실패 사유, 전송 버튼 순이다. */
@Composable
@Suppress("FunctionName")
private fun SessionDetailBody(
    summary: SessionSummary,
    presentation: SessionDetailPresentation,
    deleteFailureReason: String?,
    uploadFailureReason: String?,
    onUpload: () -> Unit,
) {
    Column(
        modifier =
            Modifier.padding(
                start = TigerSpacing.screenEdge,
                end = TigerSpacing.screenEdge,
                top = DETAIL_VIDEO_GAP,
                bottom = TigerSpacing.screenEdge,
            ),
        verticalArrangement = Arrangement.spacedBy(DETAIL_GROUP_GAP),
    ) {
        Text(
            text = formatCaptureTime(summary.recordedAtEpochMs),
            style = TigerText.itemTitle,
        )
        SessionInfoGroup(summary)
        UploadStatusGroup(summary.uploadState, uploadFailureReason)
        // 지우지 못했으면 화면이 그대로 남는다. 아무 말이 없으면 눌리지 않은 것처럼 보인다.
        if (deleteFailureReason != null) {
            Text(text = deleteFailureReason, style = TigerText.supporting)
        }
        presentation.uploadAction?.let { UploadButton(it, onUpload) }
    }
}

@Composable
@Suppress("FunctionName")
private fun SessionInfoGroup(summary: SessionSummary) {
    LabelledGroup(stringResource(R.string.session_group_info)) {
        if (summary.taskName.isNotBlank()) {
            LabelledValue(stringResource(R.string.session_label_task), summary.taskName)
        }
        if (summary.objectName.isNotBlank()) {
            LabelledValue(stringResource(R.string.session_label_object), summary.objectName)
        }
        LabelledValue(stringResource(R.string.session_label_id), summary.sessionId.take(8))
    }
}

/**
 * 전송 상태 묶음. 아직 올리지 않았으면(`LOCAL_ONLY`) 그 상태에서만 나오는 업로드 버튼이 이미 같은 말을
 * 하므로 묶음째 뺀다.
 *
 * 실패했으면 그 사유를 같은 카드에 둔다. 무엇이 막았는지 알아야 다시 걸어 볼지 판단할 수 있고, 따로 선
 * 값이 아니라 상태를 설명하는 줄이다. 사유는 앱을 다시 켜면 남지 않는다. 기록하는 컬럼이 없다.
 */
@Composable
@Suppress("FunctionName")
private fun UploadStatusGroup(
    uploadState: UploadState,
    failureReason: String?,
) {
    if (uploadState == UploadState.LOCAL_ONLY) return
    LabelledGroup(stringResource(R.string.session_group_upload)) {
        Text(text = stringResource(uploadState.labelRes), style = TigerText.value)
        if (uploadState == UploadState.FAILED && failureReason != null) {
            Text(text = failureReason, style = TigerText.supporting)
        }
    }
}

@Composable
@Suppress("FunctionName")
private fun UploadButton(
    action: SessionDetailPresentation.UploadAction,
    onUpload: () -> Unit,
) {
    val label =
        when (action) {
            SessionDetailPresentation.UploadAction.Upload -> R.string.upload_session
            SessionDetailPresentation.UploadAction.Retry -> R.string.upload_retry
        }
    Button(onClick = onUpload) { Text(stringResource(label)) }
}

/**
 * 삭제를 묻는 동안은 영상을 멈춘다. 되돌릴 수 없는 확인을 받는데 뒤에서 소리가 계속 나면 무엇을 묻고
 * 있는지 흐려진다.
 *
 * 확인을 누르면 재생기가 연 그 파일이 곧 사라진다. unlink 자체는 열린 파일에도 안전하지만, 멈춰 두면
 * 재생기가 파일을 다시 열 일이 없어 사라진 뒤에 읽으려 드는 경우가 생기지 않는다.
 */
@Composable
@Suppress("FunctionName")
private fun PausePlaybackWhileConfirmingDelete(
    pendingDelete: SessionDeleteAction?,
    sharedPlayer: SharedVideoPlayer,
) {
    LaunchedEffect(pendingDelete) {
        if (pendingDelete != null) sharedPlayer.pause()
    }
}

/** 아직 올리지 않은 세션. 전송 상태 묶음 대신 업로드 버튼이 선다. */
@PortraitScreenPreview
@Composable
@Suppress("FunctionName")
private fun LocalSessionDetailScreenPreview() {
    SessionDetailSample(SessionPreviewSamples.withState(UploadState.LOCAL_ONLY))
}

/** 전송과 삭제가 모두 실패한 세션. 두 사유가 다 보인다. */
@PortraitScreenPreview
@Composable
@Suppress("FunctionName")
private fun FailedSessionDetailScreenPreview() {
    SessionDetailSample(
        summary = SessionPreviewSamples.withState(UploadState.FAILED),
        deleteFailureReason = stringResource(R.string.session_delete_failed),
        uploadFailureReason = "Network upload failed",
    )
}

/** 목록에서 사라진 세션을 연 경우. */
@PortraitScreenPreview
@Composable
@Suppress("FunctionName")
private fun MissingSessionDetailScreenPreview() {
    SessionDetailSample(summary = null)
}

@Composable
@Suppress("FunctionName")
private fun SessionDetailSample(
    summary: SessionSummary?,
    deleteFailureReason: String? = null,
    uploadFailureReason: String? = null,
) {
    ScreenPreview {
        SessionDetailScreen(
            summary = summary,
            onBack = {},
            onUpload = {},
            onDelete = {},
            deleteFailureReason = deleteFailureReason,
            uploadFailureReason = uploadFailureReason,
            sharedPlayer = rememberSharedVideoPlayer(),
            onOpenFullscreenVideo = {},
        )
    }
}

/** 영상과 본문 사이 간격. 좌우 여백보다 넓어야 영상이 끝나고 설명이 시작되는 것으로 읽힌다. */
private val DETAIL_VIDEO_GAP = 24.dp

/**
 * 묶음과 묶음 사이. 이름과 첫 묶음 사이도 같다.
 *
 * 묶음 안 줄 사이(6dp)와 이름표가 제 묶음에 붙는 간격(4dp)보다 뚜렷하게 넓어야 한다. 셋이 엇비슷하면
 * 이름표가 어느 묶음의 것인지 간격이 말해 주지 못한다.
 */
private val DETAIL_GROUP_GAP = 20.dp

/**
 * 헤더 메뉴 버튼의 글리프 크기.
 *
 * 헤더에서 뒤로 가기와 나란히 서지만 같은 급의 동작은 아니다. 기본 24dp보다 작게 두어 있는 줄만
 * 알면 되는 단추로 남긴다. 터치 영역은 `IconButton`의 48dp를 그대로 둔다.
 */
private val HEADER_MENU_ICON_SIZE = 20.dp
