package com.ssafy.s15p21a206.tiger.core.model.upload

import kotlinx.serialization.Serializable

@Serializable enum class UploadState { LOCAL_ONLY, UPLOADING, UPLOADED, FAILED }
