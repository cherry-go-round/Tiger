package com.ssafy.s15p21a206.tiger.capture.arcore

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.view.Surface

/**
 * ARCore가 Camera 텍스처를 채우기 위해 요구하는 GL context를 만든다.
 *
 * [window]가 주어지면 그 Surface에 직접 그릴 수 있는 window surface를, 없으면 화면에 보이지 않는
 * 1×1 pbuffer를 쓴다. 후자는 pose만 수집하면 되는 경우다.
 *
 * 네 프로퍼티는 선언 순서대로 만들어진다. 디스플레이를 열고, config를 고르고, 그 config로 context와
 * surface를 만든다. 어느 단계든 실패하면 예외를 던져 만들어지지 않는다.
 */
internal class CaptureEgl(
    private val window: Surface?,
) : AutoCloseable {
    private val display: EGLDisplay = openDisplay()
    private val config: EGLConfig = chooseConfig()
    private val context: EGLContext = createContext()
    private val surface: EGLSurface = createSurface()

    val hasWindow: Boolean get() = window != null

    /** 기본 디스플레이를 얻어 EGL을 초기화한다. */
    private fun openDisplay(): EGLDisplay {
        val opened = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(opened != EGL14.EGL_NO_DISPLAY) { "EGL display is unavailable" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(opened, version, 0, version, 1)) { "Could not initialize EGL" }
        return opened
    }

    /** ES2로 그릴 수 있고, [window]가 있으면 window surface를, 없으면 pbuffer를 받는 config. */
    private fun chooseConfig(): EGLConfig {
        val surfaceType = if (window != null) EGL14.EGL_WINDOW_BIT else EGL14.EGL_PBUFFER_BIT
        // (키, 값) 쌍의 나열이고 EGL_NONE으로 끝난다.
        val attributes =
            intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,
                surfaceType,
                EGL14.EGL_NONE,
            )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        val chosen = EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0)
        check(chosen && count[0] > 0) { "No EGL ES2 config is available" }
        return requireNotNull(configs[0])
    }

    /** [config]로 ES2 context를 만든다. 다른 context와 공유하지 않는다. */
    private fun createContext(): EGLContext {
        val attributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val created = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attributes, 0)
        check(created != EGL14.EGL_NO_CONTEXT) { "Could not create EGL context" }
        return created
    }

    /** [window]가 있으면 그 Surface에 그리는 window surface를, 없으면 보이지 않는 1×1 pbuffer를 만든다. */
    private fun createSurface(): EGLSurface {
        val created =
            if (window != null) {
                EGL14.eglCreateWindowSurface(display, config, window, intArrayOf(EGL14.EGL_NONE), 0)
            } else {
                val size = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
                EGL14.eglCreatePbufferSurface(display, config, size, 0)
            }
        check(created != EGL14.EGL_NO_SURFACE) { "Could not create the EGL surface" }
        return created
    }

    fun makeCurrent() {
        check(EGL14.eglMakeCurrent(display, surface, surface, context)) { "Could not make EGL context current" }
    }

    fun swapBuffers(): Boolean = EGL14.eglSwapBuffers(display, surface)

    override fun close() {
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, surface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
    }
}
