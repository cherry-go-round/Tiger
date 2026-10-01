package com.ssafy.s15p21a206.tiger

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ssafy.s15p21a206.tiger.core.capture.camera.RecordingResolutionStore
import com.ssafy.s15p21a206.tiger.core.model.session.SessionSummary
import com.ssafy.s15p21a206.tiger.core.session.SessionRepository
import com.ssafy.s15p21a206.tiger.feature.capture.state.CaptureIntent
import com.ssafy.s15p21a206.tiger.feature.capture.state.CaptureUiState
import com.ssafy.s15p21a206.tiger.feature.capture.state.reduce
import com.ssafy.s15p21a206.tiger.feature.session.SessionOperations
import com.ssafy.s15p21a206.tiger.navigation.SessionDetailRoute
import kotlinx.coroutines.flow.Flow

/**
 * 앱 루트의 상태와, 화면 이동이 얽힌 동작. [TigerApp]이 만들고 NavHost와 수집 작업 공간이 함께 쓴다.
 *
 * 수집 상태도 여기가 소유한다. 작업 공간을 여는 것이 조회 화면의 동작이고, 마감된 세션이 어디로 갈지도
 * 여기가 정하기 때문이다. 작업 공간은 목적지를 모른다.
 */
@Stable
internal class TigerAppState(
    val navController: NavHostController,
    val repository: SessionRepository,
    /** 세션 하나에 할 수 있는 일들. 목적지는 모르고 결과 상태만 든다. */
    val operations: SessionOperations,
    val resolutionStore: RecordingResolutionStore,
    private val uploadEndpointMissingMessage: String,
) {
    val completedSummaries: Flow<List<SessionSummary>> = repository.observeCompletedSummaries()

    /**
     * 수집 작업 공간의 상태.
     *
     * 프로세스가 재생성되면 복원하지 않는다. `ON_STOP`에서 진행 중인 Session을 `INTERRUPTED`로
     * 마감하므로, 되살려도 수집 상태가 없는 빈 작업 공간이 된다. 조회 흐름의 백스택은 NavHost가
     * 저장 상태로 복원한다. 화면 회전은 manifest의 `configChanges`가 받으므로 이 상태도 유지된다.
     */
    var capture by mutableStateOf(resolutionStore.load().let { CaptureUiState(resolution = it, idlePreviewSize = it) })
        private set

    private val onSessionDetail: Boolean
        get() = navController.currentDestination?.hasRoute<SessionDetailRoute>() == true

    fun onCapture(intent: CaptureIntent) {
        capture = capture.reduce(intent)
    }

    /**
     * 수집 정보 입력부터 시작한다.
     *
     * [initialTask]는 이미 알고 있는 Task 이름이다. Task의 세션 목록에서 시작하면 그 이름이
     * 채워진 채로 열려 같은 이름을 다시 입력하지 않는다. 채워진 뒤에도 고칠 수 있게 두므로,
     * 잘못 들어왔더라도 나갔다 올 필요는 없다.
     *
     * `Open`이 상태를 새로 만든다. 새 TextureView가 이 크기로 버퍼를 잡으므로 직전 Session이
     * 남긴 크기를 물려받지 않는다.
     */
    fun startCapture(initialTask: String) {
        onCapture(CaptureIntent.Open(initialTask, resolutionStore.load()))
    }

    /** 마감으로 세션이 저장됐다. 전송할 수 있으면 전송을 걸고, 아니면 작업 공간을 닫고 상세를 띄운다. */
    fun onCaptureCompleted(
        sessionId: String,
        startUpload: Boolean,
    ) {
        if (startUpload) {
            startUpload(sessionId)
        } else {
            onCapture(CaptureIntent.Close)
            navController.navigate(SessionDetailRoute(sessionId))
        }
    }

    /**
     * 전송을 걸고 그 세션의 상세를 띄운다.
     *
     * 마감이 끝나면 세션은 저장되어 존재한다. 존재하는 것에 무슨 일이 일어나는지는 그것의 화면에서
     * 보여 주면 되므로 전송만을 위한 목적지를 따로 두지 않는다. 전송은 네트워크에 매여 길어질 수
     * 있어 사용자를 붙잡아 둘 수도 없다.
     */
    fun startUpload(sessionId: String) {
        if (!operations.canUpload) {
            // 갈 곳이 없으므로 작업 공간을 닫지 않는다. 그 자리에서 이유를 보여 준다.
            onCapture(CaptureIntent.Notify(uploadEndpointMissingMessage))
            return
        }
        // 상세로 넘어가므로 작업 공간을 닫는다. 남겨 두면 NavHost를 계속 덮는다.
        onCapture(CaptureIntent.Close)
        if (!onSessionDetail) {
            navController.navigate(SessionDetailRoute(sessionId))
        }
        operations.upload(sessionId)
    }

    /**
     * Session을 기기에서 지운다.
     *
     * 상세에서 지웠으면 그 화면은 이제 "찾을 수 없음"이 되므로 목록으로 나간다. 목록에서 지웠으면 목록에
     * 머무른다. 카드는 Flow가 갱신하면서 스스로 사라진다. 업로드가 진행 중이라 거절당하면 상세에
     * 머무르고 이유만 본문에 남긴다.
     */
    fun deleteSession(sessionId: String) {
        operations.delete(sessionId) {
            if (onSessionDetail) {
                navController.popBackStack()
            }
        }
    }
}

/**
 * [TigerAppState]를 만든다.
 *
 * 프로세스 수명을 갖는 것들(저장소, 전송 서비스)은 `TigerApplication`이 소유하고 여기서는 받아만 쓴다.
 * remember로 만들면 Activity가 재생성될 때마다 인스턴스가 하나씩 더 생긴다.
 */
@Composable
internal fun rememberTigerAppState(): TigerAppState {
    val context = LocalContext.current
    val application = context.tigerApplication
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val uploadEndpointMissing = stringResource(R.string.upload_endpoint_missing)
    return remember(navController, application) {
        TigerAppState(
            navController = navController,
            repository = application.repository,
            operations = SessionOperations(application.repository, application.uploadService, scope),
            resolutionStore = RecordingResolutionStore(context.applicationContext),
            uploadEndpointMissingMessage = uploadEndpointMissing,
        )
    }
}
