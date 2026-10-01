package com.ssafy.s15p21a206.tiger.feature.session.video

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * 상세 화면과 전체화면이 같은 ExoPlayer를 쓰게 한다.
 *
 * 두 화면은 서로 다른 destination이라 Composable이 새로 만들어지지만, 재생기는 이 객체가 들고
 * 있으므로 화면을 오갈 때도 재생 위치가 유지된다.
 */
internal class SharedVideoPlayer(
    private val context: Context,
) {
    private var player: ExoPlayer? = null
    private var preparedPath: String? = null

    /** 같은 파일이면 쓰던 재생기를 그대로 준다. 다른 파일이면 그 파일로 다시 적재한다. */
    fun playerFor(videoFile: File): ExoPlayer {
        val current = player ?: ExoPlayer.Builder(context).build().also { player = it }
        val path = videoFile.absolutePath
        if (preparedPath != path) {
            current.setMediaItem(MediaItem.fromUri(Uri.fromFile(videoFile)))
            current.prepare()
            preparedPath = path
        }
        return current
    }

    /**
     * 재생을 멈춘다. 적재한 파일은 그대로 두므로 다시 누르면 이어서 재생한다.
     *
     * 화면을 덮는 판이 뜨는 동안 쓴다. 놓지 않고 멈추기만 하는 이유는, 놓으면 판 뒤의 재생 영역이
     * 빈 화면이 되어 무엇을 덮고 있는지 알 수 없게 되기 때문이다.
     */
    fun pause() {
        player?.pause()
    }

    fun release() {
        player?.release()
        player = null
        preparedPath = null
    }
}

@Composable
internal fun rememberSharedVideoPlayer(): SharedVideoPlayer {
    val context = LocalContext.current.applicationContext
    val sharedPlayer = remember(context) { SharedVideoPlayer(context) }
    DisposableEffect(sharedPlayer) { onDispose(sharedPlayer::release) }
    return sharedPlayer
}
