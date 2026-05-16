package com.opclient.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.ui.components.PrimaryButton
import com.opclient.ui.components.SearchInput
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    var usernameInput by remember(uiState.username) { mutableStateOf(uiState.username) }
    var goalInput by remember(uiState.goalTarget) {
        mutableStateOf(if (uiState.goalTarget > 0) "${uiState.goalTarget}" else "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        SecondaryButton(
            text = "← BACK",
            onClick = onBack,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        SectionLabel(
            text = "OPENLIBRARY PROFILE",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchInput(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                onSearch = { viewModel.onIntent(ProfileIntent.SetUsername(usernameInput)) },
                placeholder = "USERNAME",
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = "SAVE",
                onClick = { viewModel.onIntent(ProfileIntent.SetUsername(usernameInput)) },
            )
        }
        if (uiState.profilePhotoUrl != null || uiState.profileName.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = uiState.profilePhotoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(colors.surface2),
                    error = ColorPainter(colors.surface2),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                )
                if (uiState.profileName.isNotEmpty()) {
                    BasicText(
                        text = uiState.profileName,
                        style = typography.bookTitle.copy(color = colors.textPrimary),
                    )
                }
            }
        }

        SectionLabel(
            text = "SYNC READING LOG",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        PrimaryButton(
            text = if (uiState.isSyncing) "SYNCING…" else "SYNC NOW",
            enabled = !uiState.isSyncing && uiState.username.isNotEmpty(),
            onClick = { viewModel.onIntent(ProfileIntent.Sync) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (uiState.lastSyncEntriesCount != null) {
            BasicText(
                text = "Last sync: ${uiState.lastSyncEntriesCount} entries imported",
                style = typography.bookAuthor.copy(color = colors.textSecondary),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        uiState.syncError?.let { syncError ->
            BasicText(
                text = syncError,
                style = typography.bookAuthor.copy(color = colors.textSecondary),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        SectionLabel(
            text = "READING GOAL",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shape = RoundedCornerShape(AppShapes.radius)
            BasicTextField(
                value = goalInput,
                onValueChange = { goalInput = it.filter { c -> c.isDigit() } },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = typography.body.copy(color = colors.textPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .background(color = colors.surface, shape = shape)
                            .border(width = 1.dp, color = colors.border, shape = shape)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (goalInput.isEmpty()) {
                            BasicText(
                                text = "BOOKS THIS YEAR",
                                style = typography.body.copy(color = colors.textSecondary),
                            )
                        }
                        innerTextField()
                    }
                },
            )
            SecondaryButton(
                text = "SET",
                onClick = {
                    val target = goalInput.toIntOrNull() ?: 0
                    if (target > 0) viewModel.onIntent(ProfileIntent.SetGoal(target))
                },
            )
            SecondaryButton(
                text = "CLEAR",
                onClick = { viewModel.onIntent(ProfileIntent.ClearGoal) },
            )
        }
        if (uiState.goalTarget > 0) {
            BasicText(
                text = "${uiState.progressCount} / ${uiState.goalTarget} books read this year",
                style = typography.bookAuthor.copy(color = colors.textPrimary),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}
