package com.ssafy.s15p21a206.tiger.feature.session.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ssafy.s15p21a206.tiger.core.session.readMainVideoResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 수집 영상의 해상도를 읽는 과정과 결과다. 읽기가 실패해도 상세 화면의 나머지는 그대로 보여야 한다. */
internal sealed interface VideoResolutionState {
    data object Loading : VideoResolutionState

    data class Available(
        val width: Int,
        val height: Int,
    ) : VideoResolutionState

    data object Unavailable : VideoResolutionState
}

/**
 * 수집분의 영상 해상도를 읽는다([readMainVideoResolution]).
 *
 * 파일 I/O이므로 composition이 아니라 IO dispatcher에서 읽고, 읽는 동안에는
 * [VideoResolutionState.Loading]을 돌려준다.
 */
@Composable
internal fun rememberVideoResolution(bundlePath: String): VideoResolutionState {
    var state by remember(bundlePath) { mutableStateOf<VideoResolutionState>(VideoResolutionState.Loading) }
    LaunchedEffect(bundlePath) {
        val resolution = withContext(Dispatchers.IO) { readMainVideoResolution(bundlePath) }
        state = resolution?.let { VideoResolutionState.Available(it.width, it.height) } ?: VideoResolutionState.Unavailable
    }
    return state
}
