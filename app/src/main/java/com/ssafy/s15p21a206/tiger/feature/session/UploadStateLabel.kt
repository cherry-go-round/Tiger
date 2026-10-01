package com.ssafy.s15p21a206.tiger.feature.session

import androidx.annotation.StringRes
import com.ssafy.s15p21a206.tiger.R
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadState

/**
 * 전송 상태를 사람이 읽는 문구로 옮긴다.
 *
 * 목록 카드와 세션 상세가 같은 문구를 쓰도록 한곳에 둔다.
 *
 * [UploadState] 자체에 붙이지 않는 것은 그것이 번들 JSON으로 직렬화되는 데이터 계약이기 때문이다.
 * 저장 형식이 앱 리소스를 알 이유가 없다.
 */
@get:StringRes
internal val UploadState.labelRes: Int
    get() =
        when (this) {
            UploadState.LOCAL_ONLY -> R.string.upload_local_only
            UploadState.UPLOADING -> R.string.upload_in_progress
            UploadState.UPLOADED -> R.string.upload_completed
            UploadState.FAILED -> R.string.upload_failed
        }
