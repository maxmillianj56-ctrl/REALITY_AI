package com.realityai.companion.data

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CapturedFrame(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val capturedAtMs: Long,
    val isFromScreenCapture: Boolean
)

data class CaptureRuntimeState(
    val isCapturing: Boolean = false,
    val latestFrame: CapturedFrame? = null,
    val measuredPreviewFps: Float? = null,
    val activeWidth: Int = 0,
    val activeHeight: Int = 0,
    val effectiveFpsCap: Int = 30,
    val statusMessage: String = "Idle",
    val lastError: String? = null
)

object CaptureFrameRepository {
    private val _state = MutableStateFlow(CaptureRuntimeState())
    val state: StateFlow<CaptureRuntimeState> = _state.asStateFlow()

    fun setCapturingStarted(width: Int, height: Int, fpsCap: Int) {
        _state.update {
            it.copy(
                isCapturing = true,
                activeWidth = width,
                activeHeight = height,
                effectiveFpsCap = fpsCap,
                statusMessage = "Capturing ${width}x${height} (cap ${fpsCap} FPS)",
                lastError = null
            )
        }
    }

    fun updateCapturedFrame(frame: CapturedFrame, measuredFps: Float?) {
        _state.update {
            it.copy(
                latestFrame = frame,
                measuredPreviewFps = measuredFps
            )
        }
    }

    fun setImportedFrame(frame: CapturedFrame) {
        _state.update {
            it.copy(
                latestFrame = frame,
                lastError = null
            )
        }
    }

    fun setError(message: String) {
        _state.update {
            it.copy(
                isCapturing = false,
                measuredPreviewFps = null,
                statusMessage = "Error",
                lastError = message
            )
        }
    }

    fun setStopped() {
        _state.update {
            it.copy(
                isCapturing = false,
                measuredPreviewFps = null,
                statusMessage = "Stopped"
            )
        }
    }

    fun reset() {
        _state.value = CaptureRuntimeState()
    }
}
