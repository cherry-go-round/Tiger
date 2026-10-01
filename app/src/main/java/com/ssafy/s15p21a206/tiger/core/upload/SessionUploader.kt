package com.ssafy.s15p21a206.tiger.core.upload

import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import com.ssafy.s15p21a206.tiger.core.model.upload.RemoteReceipt
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadResult
import com.ssafy.s15p21a206.tiger.core.session.SessionBundleValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Response

class SessionUploader(
    private val client: OkHttpClient,
    private val factory: SessionUploadRequestFactory,
) : SessionUploadGateway {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun upload(bundle: SessionBundle): UploadResult =
        withContext(Dispatchers.IO) {
            if (!SessionBundleValidator.validate(bundle.directory).isValid) {
                return@withContext UploadResult.Failed("Completed bundle is invalid")
            }
            try {
                client.newCall(factory.create(bundle)).await().use { responseResult(it, bundle) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // 연결 실패, receipt를 읽는 도중 끊긴 경우, 요청을 만들다 난 오류다.
                UploadResult.Failed("Network upload failed")
            }
        }

    private fun responseResult(
        response: Response,
        bundle: SessionBundle,
    ): UploadResult {
        if (response.code !in setOf(200, 201) || response.isRedirect) {
            return UploadResult.Failed("Upload was rejected (${response.code})")
        }
        val contentType = response.body.contentType()
        if (contentType?.type != "application" || contentType.subtype != "json") {
            return UploadResult.Failed("Upload receipt is not JSON")
        }
        val receipt =
            try {
                json.decodeFromString<RemoteReceipt>(response.body.string())
            } catch (_: SerializationException) {
                return UploadResult.Failed("Upload receipt cannot be decoded")
            }
        return if (receipt.sessionId == bundle.sessionId && receipt.result in setOf("created", "duplicate")) {
            UploadResult.Uploaded
        } else {
            UploadResult.Failed("Upload receipt does not match the session")
        }
    }
}
