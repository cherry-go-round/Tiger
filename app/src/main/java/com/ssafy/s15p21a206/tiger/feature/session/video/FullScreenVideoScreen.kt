package com.ssafy.s15p21a206.tiger.feature.session.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.android.LockLandscapeWhilePlaying
import com.ssafy.s15p21a206.tiger.core.designsystem.component.PortraitScreenPreview
import com.ssafy.s15p21a206.tiger.core.designsystem.component.ScreenPreview
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText

/**
 * 전체화면 재생. 상세 화면과 같은 재생기([sharedPlayer])를 써서 재생 위치가 이어진다.
 *
 * 위쪽 컨트롤(뒤로 가기·가로 고정)은 재생기 컨트롤과 함께 보였다 숨는다. 재생할 영상이 없으면 재생기
 * 컨트롤이 뜨지 않으므로, 뒤로 가기가 사라지지 않게 처음부터 보이게 둔다.
 */
@Composable
internal fun FullScreenVideoScreen(
    bundlePath: String?,
    sharedPlayer: SharedVideoPlayer,
    onBack: () -> Unit,
) {
    val videoFile = bundlePath?.let(::playableMainVideo)
    var landscapeLocked by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    LockLandscapeWhilePlaying(landscapeLocked)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (videoFile != null) {
            VideoPlayer(
                player = sharedPlayer.playerFor(videoFile),
                fullscreen = true,
                onFullscreenClick = onBack,
                modifier = Modifier.fillMaxSize(),
                onControlsVisibilityChanged = { controlsVisible = it },
            )
        } else {
            Text(
                text = stringResource(R.string.session_detail_video_unavailable),
                style = TigerText.onVideoBody,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (controlsVisible) {
            FullScreenTopControls(
                playable = videoFile != null,
                landscapeLocked = landscapeLocked,
                onToggleLandscape = { landscapeLocked = !landscapeLocked },
                onBack = onBack,
            )
        }
    }
}

/**
 * 뒤로 가기와 가로 고정. 검은 배경은 화면 끝까지 채우되 컨트롤만 시스템 바를 피한다. 가로로 눕히면
 * 컷아웃이 좌우로 오므로 상단 여백만으로는 모자라 `safeDrawingPadding`을 쓴다.
 */
@Composable
private fun BoxScope.FullScreenTopControls(
    playable: Boolean,
    landscapeLocked: Boolean,
    onToggleLandscape: () -> Unit,
    onBack: () -> Unit,
) {
    IconButton(
        onClick = onBack,
        modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding(),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_navigation_back),
            contentDescription = stringResource(R.string.navigation_back),
            tint = Color.White,
        )
    }
    if (playable) {
        LandscapeLockButton(
            landscapeLocked = landscapeLocked,
            onToggle = onToggleLandscape,
            modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding(),
        )
    }
}

/**
 * 가로 고정을 켜고 끄는 플레이어 컨트롤이다. Media3는 회전 버튼을 제공하지 않아 직접 만든다.
 */
@Composable
private fun LandscapeLockButton(
    landscapeLocked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier,
) {
    val description =
        stringResource(if (landscapeLocked) R.string.session_video_unlock_landscape else R.string.session_video_lock_landscape)
    IconButton(
        onClick = onToggle,
        modifier =
            modifier.background(
                color = if (landscapeLocked) Color.White.copy(alpha = 0.24f) else Color.Transparent,
                shape = CircleShape,
            ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_screen_rotation),
            contentDescription = description,
            tint = Color.White,
        )
    }
}

/** 재생할 영상이 없는 경우. 재생기 대신 문구가 서고 뒤로 가기만 남는다. */
@PortraitScreenPreview
@Composable
private fun FullScreenVideoScreenPreview() {
    ScreenPreview(background = Color.Black) {
        FullScreenVideoScreen(bundlePath = null, sharedPlayer = rememberSharedVideoPlayer(), onBack = {})
    }
}
