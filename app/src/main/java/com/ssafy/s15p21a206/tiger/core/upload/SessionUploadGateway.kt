package com.ssafy.s15p21a206.tiger.core.upload

import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadResult

interface SessionUploadGateway {
    suspend fun upload(bundle: SessionBundle): UploadResult
}
