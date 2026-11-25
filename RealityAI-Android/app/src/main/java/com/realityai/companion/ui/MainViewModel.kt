package com.realityai.companion.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.realityai.companion.RealityAiApp
import com.realityai.companion.data.CaptureFrameRepository
import com.realityai.companion.data.CaptureRuntimeState
import com.realityai.companion.data.CapturedFrame
import com.realityai.companion.data.PersistedState
import com.realityai.companion.domain.model.CaptureResolution
import com.realityai.companion.domain.model.DetectedForegroundGame
import com.realityai.companion.domain.model.DeviceTelemetry
import com.realityai.companion.domain.model.GameInfo
import com.realityai.companion.domain.model.GraphicsProfileType
import com.realityai.companion.domain.model.ImageAdjustmentParams
import com.realityai.companion.domain.model.ProcessingQuality
import com.realityai.companion.domain.model.SceneAnalysisReport
import com.realityai.companion.processing.EffectiveProcessingBudget
import com.realityai.companion.processing.ImageEnhancer
import com.realityai.companion.processing.SceneAnalyzer
import com.realityai.companion.processing.ThermalPolicy
import com.realityai.companion.system.DeviceTelemetryMonitor
import com.realityai.companion.system.ForegroundGameDetector
import com.realityai.companion.system.GameDiscoveryManager
import com.realityai.companion.system.RealityCaptureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val persisted: PersistedState = PersistedState(),
    val telemetry: DeviceTelemetry = DeviceTelemetry(),
    val capture: CaptureRuntimeState = CaptureRuntimeState(),
    val effectiveBudget: EffectiveProcessingBudget = EffectiveProcessingBudget(
        targetFpsCap = 30,
        processingQuality = ProcessingQuality.MEDIUM,
        isDownscaled = false,
        reason = null
    ),
    val installedGames: List<GameInfo> = emptyList(),
    val gamesLoading: Boolean = true,
    val liveAdjustments: ImageAdjustmentParams = GraphicsProfileType.BALANCED.defaultAdjustments,
    val enhancedPreviewBitmap: Bitmap? = null,
    val isProcessingPreview: Boolean = false,
    val latestSceneReport: SceneAnalysisReport? = null,
    val isAnalyzingScene: Boolean = false,
    val hasUsageAccessPermission: Boolean = false,
    val detectedForegroundGame: DetectedForegroundGame? = null,
    val uiNotice: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RealityAiApp
    private val prefsRepo = app.preferencesRepository
    private val telemetryMonitor = DeviceTelemetryMonitor(application)
    private val gameDiscovery = GameDiscoveryManager(application)
    private val foregroundDetector = ForegroundGameDetector(application)

    private val _installedGames = MutableStateFlow<List<GameInfo>>(emptyList())
    private val _gamesLoading = MutableStateFlow(true)
    private val _liveAdjustments = MutableStateFlow(GraphicsProfileType.BALANCED.defaultAdjustments)
    private val _enhancedBitmap = MutableStateFlow<Bitmap?>(null)
    private val _isProcessingPreview = MutableStateFlow(false)
    private val _sceneReport = MutableStateFlow<SceneAnalysisReport?>(null)
    private val _isAnalyzingScene = MutableStateFlow(false)
    private val _usagePermission = MutableStateFlow(foregroundDetector.hasUsageAccessPermission())
    private val _detectedGame = MutableStateFlow<DetectedForegroundGame?>(null)
    private val _uiNotice = MutableStateFlow<String?>(null)

    private var lastPromptedPkg: String? = null

    val uiState: StateFlow<MainUiState> = combine(
        prefsRepo.stateFlow,
        telemetryMonitor.telemetry,
        CaptureFrameRepository.state,
        _installedGames,
        _gamesLoading,
        _liveAdjustments,
        _enhancedBitmap,
        _isProcessingPreview,
        _sceneReport,
        _isAnalyzingScene,
        _usagePermission,
        _detectedGame,
        _uiNotice
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val persisted = values[0] as PersistedState
        val rawTelemetry = values[1] as DeviceTelemetry
        val capture = values[2] as CaptureRuntimeState
        val games = values[3] as List<GameInfo>
        val gamesLoading = values[4] as Boolean
        val adjustments = values[5] as ImageAdjustmentParams
        val enhanced = values[6] as Bitmap?
        val processing = values[7] as Boolean
        val report = values[8] as SceneAnalysisReport?
        val analyzing = values[9] as Boolean
        val usagePerm = values[10] as Boolean
        val detected = values[11] as DetectedForegroundGame?
        val notice = values[12] as String?

        val budget = ThermalPolicy.computeEffectiveBudget(
            settings = persisted.settings,
            thermalLevel = rawTelemetry.thermalLevel,
            batteryTempCelsius = rawTelemetry.batteryTempCelsius
        )

        val enrichedTelemetry = rawTelemetry.copy(
            measuredPreviewFps = if (capture.isCapturing) capture.measuredPreviewFps else null,
            targetPreviewFps = persisted.settings.targetPreviewFps,
            effectivePreviewFpsCap = budget.targetFpsCap,
            thermalDownscaleReason = budget.reason
        )

        MainUiState(
            persisted = persisted,
            telemetry = enrichedTelemetry,
            capture = capture,
            effectiveBudget = budget,
            installedGames = games.map { game ->
                game.copy(assignedProfile = persisted.gameProfiles[game.packageName] ?: game.assignedProfile)
            },
            gamesLoading = gamesLoading,
            liveAdjustments = adjustments,
            enhancedPreviewBitmap = enhanced,
            isProcessingPreview = processing,
            latestSceneReport = report,
            isAnalyzingScene = analyzing,
            hasUsageAccessPermission = usagePerm,
            detectedForegroundGame = detected,
            uiNotice = notice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        telemetryMonitor.start()
        refreshInstalledGames()
        startTelemetryAndDetectionLoops()
        observeCaptureFramesForPreview()
    }

    private fun startTelemetryAndDetectionLoops() {
        viewModelScope.launch {
            while (isActive) {
                telemetryMonitor.refreshMemoryAndThermal()
                val hasPerm = foregroundDetector.hasUsageAccessPermission()
                _usagePermission.value = hasPerm

                val currentState = uiState.value
                if (currentState.persisted.settings.autoDetectGames && hasPerm) {
                    val fgPkg = withContext(Dispatchers.IO) {
                        foregroundDetector.getCurrentForegroundPackage()
                    }
                    if (fgPkg != null && fgPkg != app.packageName) {
                        val matchedGame = currentState.installedGames.firstOrNull { it.packageName == fgPkg }
                        if (matchedGame != null && fgPkg != lastPromptedPkg &&
                            fgPkg != currentState.persisted.selectedGamePackage
                        ) {
                            lastPromptedPkg = fgPkg
                            _detectedGame.value = DetectedForegroundGame(
                                packageName = matchedGame.packageName,
                                displayName = matchedGame.displayName,
                                savedProfile = matchedGame.assignedProfile,
                                detectedAtMs = System.currentTimeMillis()
                            )
                        }
                    }
                }
                delay(3_000L)
            }
        }
    }

    private fun observeCaptureFramesForPreview() {
        viewModelScope.launch {
            combine(
                CaptureFrameRepository.state,
                _liveAdjustments,
                prefsRepo.stateFlow
            ) { capture, adjustments, persisted ->
                Triple(capture.latestFrame, adjustments, persisted.settings)
            }.collect { (frame, adjustments, settings) ->
                if (frame != null) {
                    val budget = ThermalPolicy.computeEffectiveBudget(
                        settings = settings,
                        thermalLevel = telemetryMonitor.telemetry.value.thermalLevel,
                        batteryTempCelsius = telemetryMonitor.telemetry.value.batteryTempCelsius
                    )
                    recomputeEnhancedPreview(frame.bitmap, adjustments, budget.processingQuality)
                }
            }
        }
    }

    private suspend fun recomputeEnhancedPreview(
        source: Bitmap,
        adjustments: ImageAdjustmentParams,
        quality: ProcessingQuality
    ) {
        _isProcessingPreview.value = true
        val processed = withContext(Dispatchers.Default) {
            runCatching { ImageEnhancer.processPreview(source, adjustments, quality) }.getOrNull()
        }
        _enhancedBitmap.value = processed
        _isProcessingPreview.value = false
    }

    fun refreshInstalledGames() {
        viewModelScope.launch {
            _gamesLoading.value = true
            val savedMap = uiState.value.persisted.gameProfiles
            val games = withContext(Dispatchers.IO) {
                gameDiscovery.loadInstalledGames(savedMap)
            }
            _installedGames.value = games
            _gamesLoading.value = false
        }
    }

    fun selectGame(game: GameInfo) {
        viewModelScope.launch {
            prefsRepo.selectGame(game.packageName, game.displayName)
            val savedCustom = uiState.value.persisted.gameCustomAdjustments[game.packageName]
            _liveAdjustments.value = savedCustom ?: game.assignedProfile.defaultAdjustments
            _detectedGame.value = null
            _uiNotice.value = "Selected ${game.displayName} (${game.assignedProfile.displayName})"
        }
    }

    fun acceptDetectedGame(detected: DetectedForegroundGame) {
        viewModelScope.launch {
            prefsRepo.selectGame(detected.packageName, detected.displayName, detected.savedProfile)
            val savedCustom = uiState.value.persisted.gameCustomAdjustments[detected.packageName]
            _liveAdjustments.value = savedCustom ?: detected.savedProfile.defaultAdjustments
            _detectedGame.value = null
            _uiNotice.value = "Applied ${detected.savedProfile.displayName} for ${detected.displayName}"
        }
    }

    fun dismissDetectedGame() {
        _detectedGame.value = null
    }

    fun setProfileForGame(packageName: String, profile: GraphicsProfileType) {
        viewModelScope.launch {
            prefsRepo.saveProfileForGame(packageName, profile)
            if (uiState.value.persisted.selectedGamePackage == packageName) {
                _liveAdjustments.value = profile.defaultAdjustments
            }
        }
    }

    fun selectActiveProfile(profile: GraphicsProfileType) {
        viewModelScope.launch {
            prefsRepo.setActiveProfile(profile)
            _liveAdjustments.value = profile.defaultAdjustments
            _uiNotice.value = "Profile set to ${profile.displayName}"
        }
    }

    fun updateAdjustments(params: ImageAdjustmentParams) {
        val clamped = params.clamped()
        _liveAdjustments.value = clamped
        val pkg = uiState.value.persisted.selectedGamePackage
        if (pkg != null) {
            viewModelScope.launch {
                prefsRepo.saveGameAdjustments(pkg, clamped)
            }
        }
    }

    fun resetAdjustmentsToActiveProfile() {
        val active = uiState.value.persisted.activeProfile
        updateAdjustments(active.defaultAdjustments)
    }

    fun startMediaProjectionCapture(resultCode: Int, data: Intent) {
        val state = uiState.value
        val shortEdge = state.persisted.settings.captureResolution.shortEdgePx
        val fpsCap = state.effectiveBudget.targetFpsCap
        val intent = RealityCaptureService.createStartIntent(
            context = app,
            resultCode = resultCode,
            projectionData = data,
            shortEdgePx = shortEdge,
            fpsCap = fpsCap
        )
        app.startForegroundService(intent)
    }

    fun stopMediaProjectionCapture() {
        app.startService(RealityCaptureService.createStopIntent(app))
    }

    fun importImageFromUri(uri: Uri) {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }
            if (loaded != null) {
                CaptureFrameRepository.setImportedFrame(
                    CapturedFrame(
                        bitmap = loaded,
                        width = loaded.width,
                        height = loaded.height,
                        capturedAtMs = System.currentTimeMillis(),
                        isFromScreenCapture = false
                    )
                )
                _uiNotice.value = "Loaded image (${loaded.width}x${loaded.height}) for offline analysis"
            } else {
                _uiNotice.value = "Could not decode the selected image."
            }
        }
    }

    /**
     * Strictly user-triggered on-demand analysis. Never runs automatically per frame.
     */
    fun analyzeCurrentFrameOnDemand() {
        val frame = CaptureFrameRepository.state.value.latestFrame
        if (frame == null) {
            _uiNotice.value = "Capture a screen frame or import an image first."
            return
        }
        viewModelScope.launch {
            _isAnalyzingScene.value = true
            val report = withContext(Dispatchers.Default) {
                SceneAnalyzer.analyzeBitmap(frame.bitmap)
            }
            _sceneReport.value = report
            _isAnalyzingScene.value = false
        }
    }

    fun applyAssistantRecommendation() {
        val report = _sceneReport.value ?: return
        viewModelScope.launch {
            prefsRepo.setActiveProfile(report.recommendedProfile)
            updateAdjustments(report.recommendedAdjustments)
            _uiNotice.value = "Applied ${report.recommendedProfile.displayName} & AI preview tuning"
        }
    }

    fun setCaptureResolution(resolution: CaptureResolution) {
        viewModelScope.launch {
            prefsRepo.updateSettings { it.copy(captureResolution = resolution) }
        }
    }

    fun setProcessingQuality(quality: ProcessingQuality) {
        viewModelScope.launch {
            prefsRepo.updateSettings { it.copy(processingQuality = quality) }
        }
    }

    fun setBatterySavingMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateSettings { it.copy(batterySavingMode = enabled) }
        }
    }

    fun setAutoDetectGames(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateSettings { it.copy(autoDetectGames = enabled) }
        }
    }

    fun setThermalAutoDownscale(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateSettings { it.copy(thermalAutoDownscale = enabled) }
        }
    }

    fun removeSavedGameProfile(packageName: String) {
        viewModelScope.launch {
            prefsRepo.removeGameProfile(packageName)
        }
    }

    fun clearNotice() {
        _uiNotice.value = null
    }

    override fun onCleared() {
        telemetryMonitor.stop()
        super.onCleared()
    }
}
