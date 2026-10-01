package com.ssafy.s15p21a206.tiger.feature.session.video

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import java.io.File

/**
 * 영상 재생기 화면. 전체화면 버튼과 그 콜백만 Media3가 주고, 화면 전환은 앱이 한다.
 *
 * 리스너는 [PlayerView]를 만들 때 한 번만 걸므로, 콜백은 최신 값을 따라가게 감싸 넘긴다.
 */
@Composable
@Suppress("FunctionName")
internal fun VideoPlayer(
    player: ExoPlayer,
    fullscreen: Boolean,
    onFullscreenClick: () -> Unit,
    modifier: Modifier,
    onControlsVisibilityChanged: (Boolean) -> Unit = {},
) {
    val currentFullscreenClick by rememberUpdatedState(onFullscreenClick)
    val currentControlsVisibilityChanged by rememberUpdatedState(onControlsVisibilityChanged)
    AndroidView(
        factory = { viewContext ->
            createPlayerView(
                context = viewContext,
                fullscreen = fullscreen,
                onFullscreenClick = { currentFullscreenClick() },
                onControlsVisibilityChanged = { currentControlsVisibilityChanged(it) },
            )
        },
        update = { view -> view.player = player },
        onRelease = { view -> view.player = null },
        modifier = modifier,
    )
}

/**
 * 재생기 뷰를 만들고 전체화면 버튼과 컨트롤 표시 리스너를 건다.
 *
 * 버튼 상태를 먼저 맞추고 리스너를 건다. `setFullscreenButtonState`는 상태만 바꾸지 않고 리스너까지
 * 부르므로, 순서를 바꾸면 전체화면에 들어가자마자 콜백이 불려 곧바로 상세 화면으로 되돌아간다.
 *
 * [PlayerView]는 Media3의 unstable API다. 앱이 직접 쓰는 유일한 지점이라 여기서만 opt-in한다.
 */
@androidx.annotation.OptIn(UnstableApi::class)
private fun createPlayerView(
    context: Context,
    fullscreen: Boolean,
    onFullscreenClick: () -> Unit,
    onControlsVisibilityChanged: (Boolean) -> Unit,
): PlayerView =
    PlayerView(context).apply {
        setFullscreenButtonState(fullscreen)
        setFullscreenButtonClickListener { onFullscreenClick() }
        setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                onControlsVisibilityChanged(visibility == View.VISIBLE)
            },
        )
    }

/**
 * 영상의 가로세로 비율을 따라간다. 회전 metadata가 적용된 크기를 쓰므로, `video_rotation_degrees`가
 * 90인 기존 수집분은 세로 비율로 나온다.
 */
@Composable
internal fun rememberVideoAspectRatio(player: ExoPlayer): Float {
    var aspectRatio by remember(player) { mutableFloatStateOf(DEFAULT_VIDEO_ASPECT_RATIO) }
    DisposableEffect(player) {
        fun apply(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                aspectRatio = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
            }
        }
        apply(player.videoSize)
        val listener =
            object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) = apply(videoSize)
            }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return aspectRatio
}

/**
 * 재생할 수 있는 수집 영상. 파일이 없거나 비어 있으면 null이다.
 *
 * 상세의 재생 영역, 전체화면, 해상도 읽기가 같은 기준으로 판단해야 한쪽은 재생하고 다른 쪽은
 * 없다고 말하는 일이 생기지 않는다.
 */
internal fun playableMainVideo(bundlePath: String): File? =
    File(bundlePath, SessionBundle.MAIN_VIDEO_FILE).takeIf { it.isFile && it.length() > 0L }

/** 영상 크기를 아직 모를 때 쓰는 비율. 크기를 알게 되면 즉시 교체된다. */
private const val DEFAULT_VIDEO_ASPECT_RATIO = 16f / 9f
