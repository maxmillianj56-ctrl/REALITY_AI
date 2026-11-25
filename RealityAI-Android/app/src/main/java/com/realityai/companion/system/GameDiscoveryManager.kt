package com.realityai.companion.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.realityai.companion.domain.model.GameInfo
import com.realityai.companion.domain.model.GraphicsProfileType

class GameDiscoveryManager(private val context: Context) {

    private val knownGamePackageHints = setOf(
        "com.tencent.ig",
        "com.pubg.imobile",
        "com.activision.callofduty.shooter",
        "com.miHoYo.GenshinImpact",
        "com.HoYoverse.hkrpgoversea",
        "com.epicgames.fortnite",
        "com.dts.freefireth",
        "com.mobile.legends",
        "com.riotgames.league.wildrift",
        "com.supercell.brawlstars",
        "com.gameloft.android.ANMP.GloftA9HM",
        "com.roblox.client",
        "com.mojang.minecraftpe",
        "com.ea.gp.fifamobile",
        "com.blizzard.diablo.immortal",
        "com.kurogame.wutheringwaves.global",
        "com.innersloth.spacemafia"
    )

    fun loadInstalledGames(savedProfiles: Map<String, GraphicsProfileType>): List<GameInfo> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        val selfPkg = context.packageName
        val discovered = mutableMapOf<String, GameInfo>()

        for (info in resolveInfos) {
            val appInfo = info.activityInfo?.applicationInfo ?: continue
            val pkg = appInfo.packageName
            if (pkg == selfPkg || discovered.containsKey(pkg)) continue

            val isDeclaredGame = appInfo.category == ApplicationInfo.CATEGORY_GAME
            @Suppress("DEPRECATION")
            val isLegacyGameFlag = (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            val isKnownHint = knownGamePackageHints.contains(pkg)

            if (isDeclaredGame || isLegacyGameFlag || isKnownHint) {
                val label = runCatching { pm.getApplicationLabel(appInfo).toString() }
                    .getOrDefault(pkg)
                val icon = runCatching { pm.getApplicationIcon(appInfo) }.getOrNull()
                val categoryText = if (isDeclaredGame) "Android Game Category" else "Detected Game Package"
                discovered[pkg] = GameInfo(
                    packageName = pkg,
                    displayName = label,
                    categoryLabel = categoryText,
                    icon = icon,
                    assignedProfile = savedProfiles[pkg] ?: GraphicsProfileType.BALANCED
                )
            }
        }

        return discovered.values.sortedBy { it.displayName.lowercase() }
    }
}
