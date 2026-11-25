package com.realityai.companion

import android.app.Application
import com.realityai.companion.data.AppPreferencesRepository
import com.realityai.companion.data.CaptureFrameRepository

class RealityAiApp : Application() {
    lateinit var preferencesRepository: AppPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = AppPreferencesRepository(this)
        CaptureFrameRepository.reset()
    }
}
