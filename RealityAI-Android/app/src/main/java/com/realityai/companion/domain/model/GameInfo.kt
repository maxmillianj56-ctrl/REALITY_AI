package com.realityai.companion.domain.model

import android.graphics.drawable.Drawable

data class GameInfo(
    val packageName: String,
    val displayName: String,
    val categoryLabel: String,
    val icon: Drawable? = null,
    val assignedProfile: GraphicsProfileType = GraphicsProfileType.BALANCED
)

data class DetectedForegroundGame(
    val packageName: String,
    val displayName: String,
    val savedProfile: GraphicsProfileType,
    val detectedAtMs: Long
)
