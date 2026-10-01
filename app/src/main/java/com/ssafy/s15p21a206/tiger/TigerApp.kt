package com.ssafy.s15p21a206.tiger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ssafy.s15p21a206.tiger.feature.capture.CaptureWorkspace
import com.ssafy.s15p21a206.tiger.navigation.TigerNavHost

/**
 * 앱의 뿌리. 조회 흐름의 NavHost와 그 위에 얹히는 수집 작업 공간을 담는다.
 *
 * 상태와 화면 이동이 얽힌 동작은 [TigerAppState]가, 목적지는 [TigerNavHost]가, 수집은
 * [CaptureWorkspace]가 맡는다. 여기는 셋을 잇고 앱 수명에 걸린 일만 건다.
 */
@Composable
fun TigerApp() {
    val appState = rememberTigerAppState()
    LaunchedEffect(appState.repository) {
        appState.repository.recoverOnLaunch()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // 백그라운드 업로드는 하지 않는다. 어느 화면에 있든 전송 중이면 끊고 FAILED로 남긴다.
        if (appState.operations.uploadInFlight) {
            appState.operations.cancelUpload()
        }
    }
    // 작업 공간이 NavHost 바깥에 있으므로 수집 상태가 백스택 조작과 무관하게 남고, ARCore·프리뷰
    // Surface·업로드 Job의 수명주기를 건드리지 않는다.
    Box(modifier = Modifier.fillMaxSize()) {
        TigerNavHost(appState, Modifier.fillMaxSize())
        CaptureWorkspace(
            state = appState.capture,
            onIntent = appState::onCapture,
            repository = appState.repository,
            resolutionStore = appState.resolutionStore,
            onCompleted = appState::onCaptureCompleted,
        )
    }
}
