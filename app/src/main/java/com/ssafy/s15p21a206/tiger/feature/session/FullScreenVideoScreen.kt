package com.ssafy.s15p21a206.tiger.feature.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerText

@Composable
@Suppress("FunctionName")
internal fun FullScreenVideoScreen(
    bundlePath: String?,
    sharedPlayer: SharedVideoPlayer,
    onBack: () -> Unit,
) {
    val videoFile = bundlePath?.let(::playableMainVideo)
    var landscapeLocked by remember { mutableStateOf(false) }
    // 재생할 영상이 없으면 컨트롤이 뜨지 않으므로, 뒤로 가기가 사라지지 않게 처음부터 보이게 둔다.
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
            // 검은 배경은 화면 끝까지 채우되 컨트롤만 시스템 바를 피한다. 가로로 눕히면 컷아웃이
            // 좌우로 오므로 상단 여백만으로는 모자란다.
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
            if (videoFile != null) {
                LandscapeLockButton(
                    landscapeLocked = landscapeLocked,
                    onToggle = { landscapeLocked = !landscapeLocked },
                    modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding(),
                )
            }
        }
    }
}

/**
 * 가로 고정을 켜고 끄는 플레이어 컨트롤이다. Media3는 회전 버튼을 제공하지 않아 직접 만든다.
 */
@Composable
@Suppress("FunctionName")
private fun LandscapeLockButton(
    landscapeLocked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier,
) {
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
            contentDescription =
                stringResource(
                    if (landscapeLocked) {
                        R.string.session_video_unlock_landscape
                    } else {
                        R.string.session_video_lock_landscape
                    },
                ),
            tint = Color.White,
        )
    }
}
