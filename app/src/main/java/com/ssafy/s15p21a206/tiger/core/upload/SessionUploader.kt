package com.ssafy.s15p21a206.tiger.core.upload

import com.ssafy.s15p21a206.tiger.core.model.session.SessionBundle
import com.ssafy.s15p21a206.tiger.core.model.upload.RemoteReceipt
import com.ssafy.s15p21a206.tiger.core.model.upload.UploadResult
import com.ssafy.s15p21a206.tiger.core.session.SessionBundleValidator
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import kotlin.coroutines.resume

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
            runCatching {
                uploadCall(client.newCall(factory.create(bundle)), bundle)
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                UploadResult.Failed("Network upload failed")
            }
        }

    private suspend fun uploadCall(
        call: Call,
        bundle: SessionBundle,
    ): UploadResult =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: java.io.IOException,
                    ) {
                        continuation.resumeIfActive(UploadResult.Failed("Network upload failed"))
                    }

                    override fun onResponse(
                        call: Call,
                        response: okhttp3.Response,
                    ) {
                        completeResponse(continuation, response, bundle)
                    }
                },
            )
        }

    private fun CancellableContinuation<UploadResult>.resumeIfActive(result: UploadResult) {
        if (isActive) resume(result)
    }

    private fun completeResponse(
        continuation: CancellableContinuation<UploadResult>,
        response: okhttp3.Response,
        bundle: SessionBundle,
    ) {
        response.use { continuation.resumeIfActive(responseResult(response, bundle)) }
    }

    private fun responseResult(
        response: okhttp3.Response,
        bundle: SessionBundle,
    ): UploadResult {
        if (response.code !in setOf(200, 201) || response.isRedirect) {
            return UploadResult.Failed("Upload was rejected (${response.code})")
        }
        val contentType = response.body.contentType()
        if (contentType?.type != "application" || contentType.subtype != "json") {
            return UploadResult.Failed("Upload receipt is not JSON")
        }
        val receipt = json.decodeFromString<RemoteReceipt>(response.body.string())
        return if (receipt.sessionId == bundle.sessionId && receipt.result in setOf("created", "duplicate")) {
            UploadResult.Uploaded
        } else {
            UploadResult.Failed("Upload receipt does not match the session")
        }
    }
}
