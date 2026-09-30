package com.ssafy.s15p21a206.tiger.core.model.session

/** [SessionRepository.delete][com.ssafy.s15p21a206.tiger.core.session.SessionRepository.delete]의 결과. */
enum class SessionDeleteResult {
    /** 색인과 번들이 모두 사라졌다. */
    DELETED,

    /** 색인은 지웠지만 디렉터리가 남았다. 목록에서는 사라지며, 다음 실행이 디렉터리를 회수한다. */
    BUNDLE_RETAINED,

    /** 업로드가 진행 중이라 지우지 않았다. */
    UPLOAD_IN_PROGRESS,
}
