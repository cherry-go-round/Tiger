package com.ssafy.s15p21a206.tiger.ui.capture

import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ssafy.s15p21a206.tiger.session.RecordingResolution

/**
 * 카메라 프레임이 올라오는 판.
 *
 * `TextureView`와 그 `SurfaceTexture`의 수명만 맡는다. Camera2 session을 열고 닫는 것은 부모다 —
 * [onSurfaceAvailable]과 [onSurfaceDestroyed]로 알리기만 한다. 이전에는 리스너 안에서 세션을 직접
 * 열었고, 그 자리가 이 화면에서 "렌더링과 사용자 이벤트 처리로 한정"이 가장 크게 깨지는 곳이었다.
 *
 * [applyTransform]은 유휴 프리뷰에서만 켠다. 수집이 시작되면 ARCore가 같은 Surface에 표시 기하를
 * 반영해 직접 그리므로, `TextureView` 변환이 남아 있으면 그 위에 한 번 더 돌아간다.
 */
@Suppress("FunctionName")
@Composable
internal fun CapturePreviewSurface(
    bufferSize: RecordingResolution,
    applyTransform: Boolean,
    onSurfaceAvailable: (Surface, SurfaceTexture) -> Unit,
    onSurfaceDestroyed: () -> Unit,
    onFrame: () -> Unit,
    modifier: Modifier,
) {
    AndroidView(
        factory = { viewContext ->
            TextureView(viewContext).apply {
                surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            surfaceTexture.setDefaultBufferSize(bufferSize.width, bufferSize.height)
                            applyIdlePreviewTransform(this@apply, width, height, bufferSize.width, bufferSize.height)
                            onSurfaceAvailable(Surface(surfaceTexture), surfaceTexture)
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surfaceTexture: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) = applyIdlePreviewTransform(this@apply, width, height, bufferSize.width, bufferSize.height)

                        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                            onSurfaceDestroyed()
                            return true
                        }

                        // 실패한 프리뷰는 늦게 온 프레임으로 되살아나지 않는다. reduce가 막는다.
                        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = onFrame()
                    }
            }
        },
        update = { view ->
            if (applyTransform) {
                applyIdlePreviewTransform(view, view.width, view.height, bufferSize.width, bufferSize.height)
            } else {
                view.setTransform(Matrix())
            }
        },
        modifier = modifier,
    )
}

/**
 * 유휴 프리뷰의 회전을 맞춘다.
 *
 * TextureView는 화면 회전을 반영하지 않는다. 수집 중 프리뷰는 ARCore가 처리하지만 이 경로는 앱이
 * 직접 걸어야 한다. 창에 붙기 전에는 화면을 알 수 없으므로 건너뛰고, Surface가 생길 때 다시 건다.
 */
private fun applyIdlePreviewTransform(
    view: TextureView,
    viewWidth: Int,
    viewHeight: Int,
    bufferWidth: Int,
    bufferHeight: Int,
) {
    val displayRotation = view.display?.rotation ?: return
    val rotation = counterRotation(displayRotation.surfaceRotationDegrees())
    view.setTransform(previewMatrix(viewWidth, viewHeight, bufferWidth, bufferHeight, rotation))
}

/** `Surface.ROTATION_*`(0~3)를 각도로 바꾼다. */
private fun Int.surfaceRotationDegrees(): Int = this * 90

/**
 * [degrees]만큼 돌아간 것을 되돌리는 시계 방향 회전.
 *
 * 프리뷰는 화면이 돌아간 만큼만 되돌리고 센서 방향은 보지 않는다. Camera2가 SurfaceTexture 버퍼에
 * 센서 방향 보정을 이미 실어 보내고 TextureView가 그것을 반영하므로, 버퍼는 기기의 자연 방향으로
 * 서 있다. `(센서 방향 - 화면 회전)`으로 계산하면 보정이 두 번 걸려 영상이 90도 눕는다. Google
 * `Camera2Basic` 샘플도 화면 회전만 보고 `ROTATION_90`에서 `-90`을, `ROTATION_270`에서 `+90`을 건다.
 */
private fun counterRotation(degrees: Int): Int = (-degrees).mod(360)

/**
 * 뷰 가운데를 축으로 [rotation]만큼 돌리는 행렬.
 *
 * TextureView는 버퍼를 뷰 크기에 늘여 그린 뒤 이 행렬을 건다. 90·270도에서는 돌린 영상의 가로세로가
 * 뒤바뀌므로 회전만 걸면 비율이 반대로 보인다. 늘인 것을 가로세로를 바꾼 버퍼 사각형으로 되돌리고,
 * 뷰를 채우도록 키운 뒤 돌린다. `Camera2Basic` 샘플의 `configureTransform`과 같은 방식이다.
 */
private fun previewMatrix(
    viewWidth: Int,
    viewHeight: Int,
    bufferWidth: Int,
    bufferHeight: Int,
    rotation: Int,
): Matrix {
    val matrix = Matrix()
    if (viewWidth <= 0 || viewHeight <= 0 || bufferWidth <= 0 || bufferHeight <= 0) return matrix
    if (rotation != 90 && rotation != 270) {
        if (rotation == 180) matrix.postRotate(180f, viewWidth / 2f, viewHeight / 2f)
        return matrix
    }
    val centerX = viewWidth / 2f
    val centerY = viewHeight / 2f
    val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
    // 회전 뒤 버퍼가 놓일 모양이므로 가로세로를 바꿔 잡는다.
    val bufferRect = RectF(0f, 0f, bufferHeight.toFloat(), bufferWidth.toFloat())
    bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
    matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
    val scale = maxOf(viewHeight.toFloat() / bufferHeight, viewWidth.toFloat() / bufferWidth)
    matrix.postScale(scale, scale, centerX, centerY)
    matrix.postRotate(rotation.toFloat(), centerX, centerY)
    return matrix
}
