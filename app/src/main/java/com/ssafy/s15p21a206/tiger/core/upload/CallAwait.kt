package com.ssafy.s15p21a206.tiger.core.upload

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

/**
 * 요청을 보내고 응답이 오기를 기다린다. 코루틴이 취소되면 요청도 끊는다. 연결 실패는 [IOException]으로
 * 던진다.
 *
 * 응답을 받은 뒤의 일은 이 함수를 부른 코루틴에서 한다. OkHttp 콜백 안에서 하면 거기서 난 예외가 부른
 * 쪽에 닿지 않는다. 해석 예외는 OkHttp 스레드 밖으로 던져지고, 본문을 읽다 난 [IOException]은 로그로만
 * 남아 기다리는 쪽이 깨어나지 못한다.
 *
 * 응답은 부른 쪽이 닫는다. 응답이 온 뒤에 취소됐으면 여기서 닫는다.
 */
internal suspend fun Call.await(): Response =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    continuation.resume(response) { _, value, _ -> value.close() }
                }
            },
        )
    }
