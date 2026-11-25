package com.realityai.companion.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.realityai.companion.domain.model.AppSettings
import com.realityai.companion.domain.model.CaptureResolution
import com.realityai.companion.domain.model.GraphicsProfileType
import com.realityai.companion.domain.model.ImageAdjustmentParams
import com.realityai.companion.domain.model.ProcessingQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reality_ai_prefs")

data class PersistedState(
    val selectedGamePackage: String? = null,
    val selectedGameName: String? = null,
    val activeProfile: GraphicsProfileType = GraphicsProfileType.BALANCED,
    val gameProfiles: Map<String, GraphicsProfileType> = emptyMap(),
    val gameCustomAdjustments: Map<String, ImageAdjustmentParams> = emptyMap(),
    val settings: AppSettings = AppSettings()
)

class AppPreferencesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val SELECTED_GAME_PKG = stringPreferencesKey("selected_game_pkg")
        val SELECTED_GAME_NAME = stringPreferencesKey("selected_game_name")
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val GAME_PROFILES_JSON = stringPreferencesKey("game_profiles_json")
        val GAME_ADJUSTMENTS_JSON = stringPreferencesKey("game_adjustments_json")
        val CAPTURE_RESOLUTION = stringPreferencesKey("capture_resolution")
        val PROCESSING_QUALITY = stringPreferencesKey("processing_quality")
        val BATTERY_SAVING = booleanPreferencesKey("battery_saving_mode")
        val AUTO_DETECT_GAMES = booleanPreferencesKey("auto_detect_games")
        val THERMAL_DOWNSCALE = booleanPreferencesKey("thermal_auto_downscale")
    }

    val stateFlow: Flow<PersistedState> = context.dataStore.data.map { prefs ->
        val profileMap: Map<String, GraphicsProfileType> = prefs[Keys.GAME_PROFILES_JSON]?.let {
            runCatching { json.decodeFromString<Map<String, GraphicsProfileType>>(it) }.getOrNull()
        } ?: emptyMap()

        val adjustmentsMap: Map<String, ImageAdjustmentParams> = prefs[Keys.GAME_ADJUSTMENTS_JSON]?.let {
            runCatching { json.decodeFromString<Map<String, ImageAdjustmentParams>>(it) }.getOrNull()
        } ?: emptyMap()

        val activeProfile = prefs[Keys.ACTIVE_PROFILE]?.let { name ->
            runCatching { GraphicsProfileType.valueOf(name) }.getOrNull()
        } ?: GraphicsProfileType.BALANCED

        val resolution = prefs[Keys.CAPTURE_RESOLUTION]?.let { name ->
            runCatching { CaptureResolution.valueOf(name) }.getOrNull()
        } ?: CaptureResolution.P720

        val quality = prefs[Keys.PROCESSING_QUALITY]?.let { name ->
            runCatching { ProcessingQuality.valueOf(name) }.getOrNull()
        } ?: ProcessingQuality.MEDIUM

        PersistedState(
            selectedGamePackage = prefs[Keys.SELECTED_GAME_PKG],
            selectedGameName = prefs[Keys.SELECTED_GAME_NAME],
            activeProfile = activeProfile,
            gameProfiles = profileMap,
            gameCustomAdjustments = adjustmentsMap,
            settings = AppSettings(
                captureResolution = resolution,
                processingQuality = quality,
                batterySavingMode = prefs[Keys.BATTERY_SAVING] ?: false,
                autoDetectGames = prefs[Keys.AUTO_DETECT_GAMES] ?: false,
                thermalAutoDownscale = prefs[Keys.THERMAL_DOWNSCALE] ?: true,
                targetPreviewFps = 30
            )
        )
    }

    suspend fun selectGame(packageName: String, displayName: String, profileOverride: GraphicsProfileType? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_GAME_PKG] = packageName
            prefs[Keys.SELECTED_GAME_NAME] = displayName
            val existingMap = prefs[Keys.GAME_PROFILES_JSON]?.let {
                runCatching { json.decodeFromString<MutableMap<String, GraphicsProfileType>>(it) }.getOrNull()
            } ?: mutableMapOf()
            val targetProfile = profileOverride ?: existingMap[packageName] ?: GraphicsProfileType.BALANCED
            existingMap[packageName] = targetProfile
            prefs[Keys.GAME_PROFILES_JSON] = json.encodeToString(existingMap)
            prefs[Keys.ACTIVE_PROFILE] = targetProfile.name
        }
    }

    suspend fun saveProfileForGame(packageName: String, profile: GraphicsProfileType) {
        context.dataStore.edit { prefs ->
            val existingMap = prefs[Keys.GAME_PROFILES_JSON]?.let {
                runCatching { json.decodeFromString<MutableMap<String, GraphicsProfileType>>(it) }.getOrNull()
            } ?: mutableMapOf()
            existingMap[packageName] = profile
            prefs[Keys.GAME_PROFILES_JSON] = json.encodeToString(existingMap)
            if (prefs[Keys.SELECTED_GAME_PKG] == packageName) {
                prefs[Keys.ACTIVE_PROFILE] = profile.name
            }
        }
    }

    suspend fun setActiveProfile(profile: GraphicsProfileType) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROFILE] = profile.name
            val selectedPkg = prefs[Keys.SELECTED_GAME_PKG]
            if (selectedPkg != null) {
                val existingMap = prefs[Keys.GAME_PROFILES_JSON]?.let {
                    runCatching { json.decodeFromString<MutableMap<String, GraphicsProfileType>>(it) }.getOrNull()
                } ?: mutableMapOf()
                existingMap[selectedPkg] = profile
                prefs[Keys.GAME_PROFILES_JSON] = json.encodeToString(existingMap)
            }
        }
    }

    suspend fun saveGameAdjustments(packageName: String, params: ImageAdjustmentParams) {
        context.dataStore.edit { prefs ->
            val existingMap = prefs[Keys.GAME_ADJUSTMENTS_JSON]?.let {
                runCatching { json.decodeFromString<MutableMap<String, ImageAdjustmentParams>>(it) }.getOrNull()
            } ?: mutableMapOf()
            existingMap[packageName] = params.clamped()
            prefs[Keys.GAME_ADJUSTMENTS_JSON] = json.encodeToString(existingMap)
        }
    }

    suspend fun removeGameProfile(packageName: String) {
        context.dataStore.edit { prefs ->
            val profileMap = prefs[Keys.GAME_PROFILES_JSON]?.let {
                runCatching { json.decodeFromString<MutableMap<String, GraphicsProfileType>>(it) }.getOrNull()
            } ?: mutableMapOf()
            profileMap.remove(packageName)
            prefs[Keys.GAME_PROFILES_JSON] = json.encodeToString(profileMap)

            val adjMap = prefs[Keys.GAME_ADJUSTMENTS_JSON]?.let {
                runCatching { json.decodeFromString<MutableMap<String, ImageAdjustmentParams>>(it) }.getOrNull()
            } ?: mutableMapOf()
            adjMap.remove(packageName)
            prefs[Keys.GAME_ADJUSTMENTS_JSON] = json.encodeToString(adjMap)
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                captureResolution = prefs[Keys.CAPTURE_RESOLUTION]?.let {
                    runCatching { CaptureResolution.valueOf(it) }.getOrNull()
                } ?: CaptureResolution.P720,
                processingQuality = prefs[Keys.PROCESSING_QUALITY]?.let {
                    runCatching { ProcessingQuality.valueOf(it) }.getOrNull()
                } ?: ProcessingQuality.MEDIUM,
                batterySavingMode = prefs[Keys.BATTERY_SAVING] ?: false,
                autoDetectGames = prefs[Keys.AUTO_DETECT_GAMES] ?: false,
                thermalAutoDownscale = prefs[Keys.THERMAL_DOWNSCALE] ?: true
            )
            val updated = transform(current)
            prefs[Keys.CAPTURE_RESOLUTION] = updated.captureResolution.name
            prefs[Keys.PROCESSING_QUALITY] = updated.processingQuality.name
            prefs[Keys.BATTERY_SAVING] = updated.batterySavingMode
            prefs[Keys.AUTO_DETECT_GAMES] = updated.autoDetectGames
            prefs[Keys.THERMAL_DOWNSCALE] = updated.thermalAutoDownscale
        }
    }
}
