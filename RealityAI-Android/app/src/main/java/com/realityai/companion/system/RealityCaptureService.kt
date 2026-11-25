package com.realityai.companion.system

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.realityai.companion.MainActivity
import com.realityai.companion.R
import com.realityai.companion.data.CaptureFrameRepository
import com.realityai.companion.data.CapturedFrame
import kotlin.math.roundToInt

class RealityCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null

    @Volatile
    private var minFrameIntervalNs: Long = 33_333_333L // 30 FPS default cap
    private var lastAcceptedFrameNs: Long = 0L
    private var fpsWindowStartNs: Long = 0L
    private var fpsFrameCount: Int = 0
    private var currentMeasuredFps: Float? = null

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopCaptureInternal("System revoked MediaProjection session")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_CAPTURE -> {
                stopCaptureInternal("Stopped by user")
                return START_NOT_STICKY
            }
            ACTION_UPDATE_FPS_CAP -> {
                val cap = intent.getIntExtra(EXTRA_FPS_CAP, 30).coerceIn(15, 30)
                minFrameIntervalNs = 1_000_000_000L / cap
                return START_NOT_STICKY
            }
            ACTION_START_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val projectionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_PROJECTION_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_PROJECTION_DATA)
                }
                val shortEdge = intent.getIntExtra(EXTRA_SHORT_EDGE_PX, 720).coerceIn(360, 1080)
                val fpsCap = intent.getIntExtra(EXTRA_FPS_CAP, 30).coerceIn(15, 30)
                minFrameIntervalNs = 1_000_000_000L / fpsCap

                if (resultCode != Activity.RESULT_OK || projectionData == null) {
                    CaptureFrameRepository.setError("MediaProjection consent was not granted.")
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForegroundServiceWithNotification()
                startProjection(resultCode, projectionData, shortEdge, fpsCap)
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProjection(
        resultCode: Int,
        projectionData: Intent,
        shortEdgePx: Int,
        fpsCap: Int
    ) {
        stopProjectionResourcesOnly()

        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        val projection = projectionManager?.getMediaProjection(resultCode, projectionData)
        if (projection == null) {
            CaptureFrameRepository.setError("Unable to acquire MediaProjection handle on this device.")
            stopSelf()
            return
        }

        val metrics = resources.displayMetrics
        val screenW = metrics.widthPixels.coerceAtLeast(1)
        val screenH = metrics.heightPixels.coerceAtLeast(1)
        val aspect = screenW.toFloat() / screenH.toFloat()
        val (targetW, targetH) = if (screenW <= screenH) {
            val w = shortEdgePx
            val h = (w / aspect).roundToInt().coerceAtLeast(2)
            w to h
        } else {
            val h = shortEdgePx
            val w = (h * aspect).roundToInt().coerceAtLeast(2)
            w to h
        }

        val thread = HandlerThread("RealityCaptureThread").apply { start() }
        val handler = Handler(thread.looper)
        captureThread = thread
        captureHandler = handler

        projection.registerCallback(projectionCallback, handler)
        mediaProjection = projection

        val reader = ImageReader.newInstance(targetW, targetH, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        fpsWindowStartNs = System.nanoTime()
        fpsFrameCount = 0
        lastAcceptedFrameNs = 0L
        currentMeasuredFps = null

        reader.setOnImageAvailableListener({ r ->
            val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            try {
                val nowNs = System.nanoTime()
                if (nowNs - lastAcceptedFrameNs < minFrameIntervalNs) {
                    return@setOnImageAvailableListener
                }
                lastAcceptedFrameNs = nowNs

                val bitmap = imageToBitmap(image, targetW, targetH) ?: return@setOnImageAvailableListener
                fpsFrameCount++
                val elapsedNs = nowNs - fpsWindowStartNs
                if (elapsedNs >= 1_000_000_000L) {
                    currentMeasuredFps = (fpsFrameCount * 1_000_000_000f) / elapsedNs.toFloat()
                    fpsFrameCount = 0
                    fpsWindowStartNs = nowNs
                }

                CaptureFrameRepository.updateCapturedFrame(
                    frame = CapturedFrame(
                        bitmap = bitmap,
                        width = targetW,
                        height = targetH,
                        capturedAtMs = System.currentTimeMillis(),
                        isFromScreenCapture = true
                    ),
                    measuredFps = currentMeasuredFps
                )
            } finally {
                image.close()
            }
        }, handler)

        virtualDisplay = projection.createVirtualDisplay(
            "RealityAiCaptureDisplay",
            targetW,
            targetH,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )

        CaptureFrameRepository.setCapturingStarted(targetW, targetH, fpsCap)
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        if (pixelStride <= 0) return null

        val rowPadding = rowStride - pixelStride * width
        val paddedWidth = width + rowPadding / pixelStride
        val tempBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        tempBitmap.copyPixelsFromBuffer(buffer)

        return if (paddedWidth != width) {
            val cropped = Bitmap.createBitmap(tempBitmap, 0, 0, width, height)
            tempBitmap.recycle()
            cropped
        } else {
            tempBitmap
        }
    }

    private fun stopCaptureInternal(reason: String) {
        stopProjectionResourcesOnly()
        CaptureFrameRepository.setStopped()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopProjectionResourcesOnly() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null

        runCatching { imageReader?.close() }
        imageReader = null

        runCatching {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        }
        mediaProjection = null

        runCatching { captureThread?.quitSafely() }
        captureThread = null
        captureHandler = null
    }

    override fun onDestroy() {
        stopProjectionResourcesOnly()
        CaptureFrameRepository.setStopped()
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.capture_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.capture_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            100,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            101,
            Intent(this, RealityCaptureService::class.java).apply { action = ACTION_STOP_CAPTURE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.action_stop_capture), stopIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "reality_ai_capture_channel"
        const val NOTIFICATION_ID = 4102

        const val ACTION_START_CAPTURE = "com.realityai.companion.action.START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "com.realityai.companion.action.STOP_CAPTURE"
        const val ACTION_UPDATE_FPS_CAP = "com.realityai.companion.action.UPDATE_FPS_CAP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_PROJECTION_DATA = "extra_projection_data"
        const val EXTRA_SHORT_EDGE_PX = "extra_short_edge_px"
        const val EXTRA_FPS_CAP = "extra_fps_cap"

        fun createStartIntent(
            context: Context,
            resultCode: Int,
            projectionData: Intent,
            shortEdgePx: Int,
            fpsCap: Int
        ): Intent = Intent(context, RealityCaptureService::class.java).apply {
            action = ACTION_START_CAPTURE
            putExtra(EXTRA_RESULT_CODE, resultCode)
            putExtra(EXTRA_PROJECTION_DATA, projectionData)
            putExtra(EXTRA_SHORT_EDGE_PX, shortEdgePx)
            putExtra(EXTRA_FPS_CAP, fpsCap)
        }

        fun createStopIntent(context: Context): Intent =
            Intent(context, RealityCaptureService::class.java).apply {
                action = ACTION_STOP_CAPTURE
            }
    }
}
