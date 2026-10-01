package com.ssafy.s15p21a206.tiger.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.ssafy.s15p21a206.tiger.TigerAppState
import com.ssafy.s15p21a206.tiger.core.designsystem.theme.TigerSurface
import com.ssafy.s15p21a206.tiger.core.model.session.SessionSummary
import com.ssafy.s15p21a206.tiger.feature.session.deleteFailureMessage
import com.ssafy.s15p21a206.tiger.feature.session.detail.SessionDetailScreen
import com.ssafy.s15p21a206.tiger.feature.session.list.SessionListScreen
import com.ssafy.s15p21a206.tiger.feature.session.list.TaskSessionListScreen
import com.ssafy.s15p21a206.tiger.feature.session.video.FullScreenVideoScreen
import com.ssafy.s15p21a206.tiger.feature.session.video.SharedVideoPlayer
import com.ssafy.s15p21a206.tiger.feature.session.video.rememberSharedVideoPlayer

/**
 * 조회 흐름의 목적지들. 홈, Task의 세션 목록, 세션 상세, 전체화면 재생이다.
 *
 * 목적지는 저장소의 완료 세션 목록에서 제 몫을 골라 화면에 넘기고, 화면 이동이 얽힌 동작은 [appState]에
 * 맡긴다. 상세와 전체화면은 재생기 하나를 함께 써서 오갈 때 재생 위치가 이어진다.
 */
@Composable
internal fun TigerNavHost(
    appState: TigerAppState,
    modifier: Modifier = Modifier,
) {
    val navController = appState.navController
    val summaries by appState.completedSummaries.collectAsState(emptyList())
    val sharedVideoPlayer = rememberSharedVideoPlayer()
    ReleasePlayerOffPlaybackRoutes(appState, sharedVideoPlayer)
    NavHost(
        navController = navController,
        startDestination = SessionListRoute,
        modifier = modifier,
        // NavHost의 기본 전환은 좌우 슬라이드다. 이관 전에는 화면이 즉시 바뀌었고, 이 앱에는
        // 전환 애니메이션을 도입할 이유가 없다. 업로드 상태 화면이 있던 동안에는 수집 마감 경로가
        // Detail과 그 화면을 한 프레임에 연달아 쌓아, 애니메이션이 있으면 슬라이드가 두 번 겹쳐 보였다.
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<SessionListRoute> {
            DestinationSurface {
                SessionListScreen(
                    sessions = summaries,
                    onStartCapture = { appState.startCapture("") },
                    onOpenTask = { taskName -> navController.navigate(TaskSessionsRoute(taskName)) },
                )
            }
        }
        composable<TaskSessionsRoute> { entry ->
            TaskSessionsDestination(entry.toRoute(), summaries, appState)
        }
        composable<SessionDetailRoute> { entry ->
            SessionDetailDestination(entry.toRoute(), summaries, appState, sharedVideoPlayer)
        }
        composable<SessionVideoRoute> { entry ->
            val route = entry.toRoute<SessionVideoRoute>()
            // 전체화면은 스스로 검은 배경을 꽉 채우므로 판을 따로 깔지 않는다.
            FullScreenVideoScreen(
                bundlePath = summaries.withId(route.sessionId)?.bundlePath,
                sharedPlayer = sharedVideoPlayer,
                onBack = navController::popBackStack,
            )
        }
    }
}

/** 재생 화면(상세·전체화면)을 벗어나면 decoder를 계속 물고 있지 않도록 재생기를 놓는다. */
@Composable
private fun ReleasePlayerOffPlaybackRoutes(
    appState: TigerAppState,
    sharedVideoPlayer: SharedVideoPlayer,
) {
    val currentEntry by appState.navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry) {
        val onPlaybackRoute =
            currentEntry?.destination?.let { it.hasRoute<SessionDetailRoute>() || it.hasRoute<SessionVideoRoute>() } == true
        if (!onPlaybackRoute) sharedVideoPlayer.release()
    }
}

@Composable
private fun TaskSessionsDestination(
    route: TaskSessionsRoute,
    summaries: List<SessionSummary>,
    appState: TigerAppState,
) {
    val navController = appState.navController
    DestinationSurface {
        TaskSessionListScreen(
            taskName = route.taskName,
            sessions = summaries.filter { it.taskName.trim() == route.taskName },
            onBack = navController::popBackStack,
            onOpenSession = { sessionId -> navController.navigate(SessionDetailRoute(sessionId)) },
            onDeleteSession = appState::deleteSession,
            // 이름 없는 Task 묶음은 이름이 빈 세션들이라 채울 것이 없다. 빈 채로 연다.
            onStartCapture = { appState.startCapture(route.taskName) },
        )
    }
}

@Composable
private fun SessionDetailDestination(
    route: SessionDetailRoute,
    summaries: List<SessionSummary>,
    appState: TigerAppState,
    sharedVideoPlayer: SharedVideoPlayer,
) {
    val navController = appState.navController
    val operations = appState.operations
    DestinationSurface {
        SessionDetailScreen(
            summary = summaries.withId(route.sessionId),
            onBack = navController::popBackStack,
            onUpload = { appState.startUpload(route.sessionId) },
            onDelete = { appState.deleteSession(route.sessionId) },
            // 무엇이 막았는지만 운용이 말하고, 문구는 화면 쪽이 붙인다.
            deleteFailureReason = deleteFailureMessage(operations.deleteFailure),
            sharedPlayer = sharedVideoPlayer,
            onOpenFullscreenVideo = { navController.navigate(SessionVideoRoute(route.sessionId)) },
            uploadFailureReason = operations.uploadFailureReason,
        )
    }
}

private fun List<SessionSummary>.withId(sessionId: String): SessionSummary? = firstOrNull { it.sessionId == sessionId }

/**
 * 목적지 내용을 불투명한 판 위에 올린다.
 *
 * `NavHost`는 `AnimatedContent`로 목적지를 교체한다. 전환 애니메이션을 껐더라도 나가는 목적지가
 * 한 프레임 더 composition에 남으므로, 배경을 그리지 않으면 그 프레임에 두 화면의 내용이 같은
 * 창 배경 위에 겹쳐 그려져 이전 화면이 비쳐 보인다. `TigerTheme`에는 `Surface`가 없고 각 화면도
 * 배경을 그리지 않아, 판은 목적지마다 깔아야 한다.
 */
@Composable
private fun DestinationSurface(content: @Composable () -> Unit) {
    // targetSdk 35부터 창이 시스템 바 아래까지 늘어난다. 목적지마다 막지 않으면 헤더가 상태 표시줄에,
    // 목록 끝이 제스처 바에 물린다. 판이 한 번 막으면 안에 놓이는 화면은 인셋을 몰라도 된다.
    //
    // 인셋은 판 자신이 아니라 판 안쪽에서 막는다. 판에 걸면 판이 인셋만큼 줄어들어 시스템 바 자리에는
    // 판 색이 칠해지지 않고 창 배경이 드러난다. 두 색이 같던 동안은 보이지 않았지만, 목록이 바닥을
    // 어둡게 깔고부터는 위아래에 다른 색 띠로 남는다.
    Surface(modifier = Modifier.fillMaxSize(), color = TigerSurface.listBackground) {
        Box(modifier = Modifier.safeDrawingPadding()) { content() }
    }
}
