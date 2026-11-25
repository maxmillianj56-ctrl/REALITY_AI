package com.realityai.companion.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.realityai.companion.domain.model.GameInfo
import com.realityai.companion.domain.model.GraphicsProfileType
import com.realityai.companion.ui.MainUiState
import com.realityai.companion.ui.components.DashboardPanel
import com.realityai.companion.ui.components.rememberDrawableImageBitmap
import com.realityai.companion.ui.theme.CyberCyan
import com.realityai.companion.ui.theme.ElevatedCharcoal
import com.realityai.companion.ui.theme.HairlineBorder
import com.realityai.companion.ui.theme.NeonGreen
import com.realityai.companion.ui.theme.TextPrimary
import com.realityai.companion.ui.theme.TextSecondary

@Composable
fun GameLibraryScreen(
    state: MainUiState,
    onSelectGame: (GameInfo) -> Unit,
    onAssignProfile: (String, GraphicsProfileType) -> Unit,
    onRefreshGames: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredGames = remember(state.installedGames, searchQuery) {
        if (searchQuery.isBlank()) {
            state.installedGames
        } else {
            val q = searchQuery.trim().lowercase()
            state.installedGames.filter {
                it.displayName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Installed Game Library",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Discovered via Android PackageManager launcher visibility",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onRefreshGames) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh installed games",
                        tint = CyberCyan
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Filter installed games") },
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (state.gamesLoading) {
            item {
                DashboardPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = NeonGreen,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "Scanning installed launcher packages...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else if (filteredGames.isEmpty()) {
            item {
                DashboardPanel {
                    Text(
                        text = "No matching Android games discovered",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Android only exposes apps declared with CATEGORY_GAME or matching launcher packages. Install a game or tap refresh to re-scan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedButton(onClick = onRefreshGames) {
                        Text("Rescan Device")
                    }
                }
            }
        } else {
            items(filteredGames, key = { it.packageName }) { game ->
                val isSelected = state.persisted.selectedGamePackage == game.packageName
                GameLibraryRow(
                    game = game,
                    isSelected = isSelected,
                    onSelect = { onSelectGame(game) },
                    onAssignProfile = { profile -> onAssignProfile(game.packageName, profile) }
                )
            }
        }
    }
}

@Composable
private fun GameLibraryRow(
    game: GameInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAssignProfile: (GraphicsProfileType) -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val iconBitmap = rememberDrawableImageBitmap(game.icon)

    DashboardPanel(
        modifier = Modifier.clickable(onClick = onSelect),
        borderColor = if (isSelected) NeonGreen else HairlineBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = game.displayName,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedCharcoal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = CyberCyan
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = game.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = game.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Box {
                OutlinedButton(
                    onClick = { menuExpanded = true },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = game.assignedProfile.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    GraphicsProfileType.entries.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.displayName) },
                            onClick = {
                                menuExpanded = false
                                onAssignProfile(profile)
                            }
                        )
                    }
                }
            }
        }
    }
}
