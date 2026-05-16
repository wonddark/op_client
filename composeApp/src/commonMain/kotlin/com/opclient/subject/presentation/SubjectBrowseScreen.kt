package com.opclient.subject.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.components.FilterChip
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubjectBrowseScreen(
    onSubjectClick: (String) -> Unit,
    viewModel: SubjectBrowseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val typography = AppThemeTokens.typography
    val colors = AppThemeTokens.colors

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        SectionLabel(
            text = "DISCOVER",
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
        )
        BasicText(
            text = "Explore books by subject. Tap a category to browse its collection.",
            style = typography.body.copy(color = colors.textSecondary),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
        if (uiState.subjects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "No subjects available.",
                    style = typography.body.copy(color = colors.textSecondary),
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(uiState.subjects) { subject ->
                    FilterChip(
                        label = subject,
                        selected = false,
                        onToggle = { onSubjectClick(subject) },
                    )
                }
            }
        }
    }
}
