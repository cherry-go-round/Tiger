package com.ssafy.s15p21a206.tiger.navigation

import kotlinx.serialization.Serializable

// 조회 흐름의 목적지다. 인자는 Navigation Compose의 type-safe route로 전달한다. Task 이름은
// 사용자가 자유롭게 입력하는 문자열이라 `/`나 공백이 들어올 수 있는데, route 문자열을 직접
// 조립하면 그 값이 경로를 깨뜨린다.
//
// 수집 작업 공간은 목적지가 아니다. 진입 경로가 하나뿐이고, 뒤로 가기가 "이전 화면으로"가 아니라
// "종료할까요?"이며, 안에 또 모달을 품는다. 백스택 pop이 아닌 해제 가드이므로 상태로 두고
// NavHost 위에 모달로 얹는다.

@Serializable
internal data object SessionListRoute

@Serializable
internal data class TaskSessionsRoute(
    val taskName: String,
)

@Serializable
internal data class SessionDetailRoute(
    val sessionId: String,
)

@Serializable
internal data class SessionVideoRoute(
    val sessionId: String,
)
