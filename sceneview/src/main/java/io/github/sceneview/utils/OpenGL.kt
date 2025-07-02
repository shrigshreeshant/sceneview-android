package io.github.sceneview.utils

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.util.Log

/**
 * Convenience class to perform common GL operations.
 */
object OpenGL {
    private val TAG = "EGLContextCreator"
    private val EGL_OPENGL_ES3_BIT = 0x40

    fun createEglContext(): EGLContext {
        Log.d(TAG, "Creating EGL context without shared context")
        return createEglContext(EGL14.EGL_NO_CONTEXT)!!
    }

    fun createEglContext(shareContext: EGLContext?): EGLContext? {
        Log.d(TAG, "Starting EGL context creation process")
        Log.d(TAG, "Share context: ${if (shareContext != null && shareContext != EGL14.EGL_NO_CONTEXT) "Provided" else "None"}")

        try {
            // Get EGL display
            Log.d(TAG, "Getting EGL display...")
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

            if (display == EGL14.EGL_NO_DISPLAY) {
                Log.e(TAG, "❌ Failed to get EGL display")
                return null
            }
            Log.d(TAG, "✅ EGL display obtained successfully")

            // Initialize EGL
            Log.d(TAG, "Initializing EGL...")
            val initResult = EGL14.eglInitialize(display, null, 0, null, 0)
            if (!initResult) {
                Log.e(TAG, "❌ Failed to initialize EGL. Error: ${getEglErrorString()}")
                return null
            }
            Log.d(TAG, "✅ EGL initialized successfully")

            // Choose EGL configuration
            Log.d(TAG, "Choosing EGL configuration...")
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfig = intArrayOf(0)
            val attribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16,
                EGL14.EGL_NONE
            )

            val configResult = EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfig, 0)
            if (!configResult || numConfig[0] <= 0 || configs[0] == null) {
                Log.e(TAG, "❌ Failed to choose EGL config. Available configs: ${numConfig[0]}, Error: ${getEglErrorString()}")
                return null
            }
            Log.d(TAG, "✅ EGL configuration chosen successfully. Available configs: ${numConfig[0]}")

            // Create EGL context
            Log.d(TAG, "Creating EGL context...")
            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )

            val context = EGL14.eglCreateContext(
                display,
                configs[0],
                shareContext ?: EGL14.EGL_NO_CONTEXT,
                contextAttribs,
                0
            )

            if (context == EGL14.EGL_NO_CONTEXT) {
                Log.e(TAG, "❌ Failed to create EGL context. Error: ${getEglErrorString()}")
                return null
            }
            Log.d(TAG, "✅ EGL context created successfully: $context")

            // Create PBuffer surface for context validation
            Log.d(TAG, "Creating PBuffer surface for context validation...")
            val surfaceAttribs = intArrayOf(
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
            )

            val surface = EGL14.eglCreatePbufferSurface(
                display,
                configs[0],
                surfaceAttribs,
                0
            )

            if (surface == EGL14.EGL_NO_SURFACE) {
                Log.e(TAG, "❌ Failed to create PBuffer surface. Error: ${getEglErrorString()}")
                // Clean up context
                EGL14.eglDestroyContext(display, context)
                return null
            }
            Log.d(TAG, "✅ PBuffer surface created successfully")

            // Make context current
            Log.d(TAG, "Making EGL context current...")
            val makeCurrentResult = EGL14.eglMakeCurrent(display, surface, surface, context)

            if (!makeCurrentResult) {
                Log.e(TAG, "❌ Error making GL context current. Error: ${getEglErrorString()}")
                // Clean up
                EGL14.eglDestroySurface(display, surface)
                EGL14.eglDestroyContext(display, context)
                return null
            }

            Log.d(TAG, "✅ EGL context made current successfully")

            // Verify OpenGL ES context
            Log.d(TAG, "Verifying OpenGL ES context...")
            try {
                val vendor = GLES30.glGetString(GLES30.GL_VENDOR)
                val renderer = GLES30.glGetString(GLES30.GL_RENDERER)
                val version = GLES30.glGetString(GLES30.GL_VERSION)
                val glslVersion = GLES30.glGetString(GLES30.GL_SHADING_LANGUAGE_VERSION)

                Log.i(TAG, "📊 OpenGL ES Context Info:")
                Log.i(TAG, "   Vendor: $vendor")
                Log.i(TAG, "   Renderer: $renderer")
                Log.i(TAG, "   Version: $version")
                Log.i(TAG, "   GLSL Version: $glslVersion")

                val glError = GLES30.glGetError()
                if (glError != GLES30.GL_NO_ERROR) {
                    Log.w(TAG, "⚠️ GL Error after context verification: $glError")
                } else {
                    Log.d(TAG, "✅ OpenGL ES context verified successfully")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error verifying OpenGL ES context", e)
            }

            Log.i(TAG, "🎉 EGL Context creation completed successfully!")
            Log.i(TAG, "Context details - Display: $display, Surface: $surface, Context: $context")

            return context

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception during EGL context creation", e)
            return null
        }
    }

    private fun getEglErrorString(): String {
        return when (val error = EGL14.eglGetError()) {
            EGL14.EGL_SUCCESS -> "EGL_SUCCESS"
            EGL14.EGL_NOT_INITIALIZED -> "EGL_NOT_INITIALIZED"
            EGL14.EGL_BAD_ACCESS -> "EGL_BAD_ACCESS"
            EGL14.EGL_BAD_ALLOC -> "EGL_BAD_ALLOC"
            EGL14.EGL_BAD_ATTRIBUTE -> "EGL_BAD_ATTRIBUTE"
            EGL14.EGL_BAD_CONTEXT -> "EGL_BAD_CONTEXT"
            EGL14.EGL_BAD_CONFIG -> "EGL_BAD_CONFIG"
            EGL14.EGL_BAD_CURRENT_SURFACE -> "EGL_BAD_CURRENT_SURFACE"
            EGL14.EGL_BAD_DISPLAY -> "EGL_BAD_DISPLAY"
            EGL14.EGL_BAD_SURFACE -> "EGL_BAD_SURFACE"
            EGL14.EGL_BAD_MATCH -> "EGL_BAD_MATCH"
            EGL14.EGL_BAD_PARAMETER -> "EGL_BAD_PARAMETER"
            EGL14.EGL_BAD_NATIVE_PIXMAP -> "EGL_BAD_NATIVE_PIXMAP"
            EGL14.EGL_BAD_NATIVE_WINDOW -> "EGL_BAD_NATIVE_WINDOW"
            EGL14.EGL_CONTEXT_LOST -> "EGL_CONTEXT_LOST"
            else -> "Unknown EGL error: $error"
        }
    }

    // Additional utility function for context validation
    fun validateEglContext(context: EGLContext): Boolean {
        Log.d(TAG, "Validating EGL context: $context")

        if (context == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "❌ Context is EGL_NO_CONTEXT")
            return false
        }

        val currentContext = EGL14.eglGetCurrentContext()
        Log.d(TAG, "Current context: $currentContext")

        if (currentContext == context) {
            Log.d(TAG, "✅ Context is current and valid")
            return true
        } else {
            Log.w(TAG, "⚠️ Context exists but is not current")
            return false
        }
    }
    fun createExternalTextureId(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val result = textures[0]
        val textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        GLES30.glBindTexture(textureTarget, result)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        return result
    }

    fun destroyEglContext(context: EGLContext?) {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(EGL14.eglDestroyContext(display, context)) { "Error destroying GL context." }
    }
}

fun EGLContext.destroy() = OpenGL.destroyEglContext(this)