package com.opclient.subject.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.opclient.platform.PlatformConfig
import com.opclient.ui.components.FilterChip
import com.opclient.ui.components.SearchInput
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectBrowseScreen(
    onSubjectClick: (String) -> Unit,
    viewModel: SubjectBrowseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val typography = AppThemeTokens.typography
    val colors = AppThemeTokens.colors

    if (PlatformConfig.useLazyPagination) {
        Box(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SectionLabel(
                    text = "DISCOVER",
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                BasicText(
                    text = "Explore books by subject.\nTap a category to browse its collection.",
                    style = typography.body.copy(
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                SearchInput(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = "SEARCH SUBJECTS…",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
                when {
                    uiState.searchQuery.isEmpty() && uiState.subjects.isEmpty() -> BasicText(
                        text = "No subjects available.",
                        style = typography.body.copy(color = colors.textSecondary),
                    )
                    uiState.searchQuery.isEmpty() -> FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.subjects.forEach { subject ->
                            FilterChip(
                                label = subject,
                                selected = false,
                                onToggle = { onSubjectClick(subject) },
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                    uiState.searchStatus == SubjectSearchStatus.Loading -> Box(
                        modifier = Modifier.height(64.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingState()
                    }
                    uiState.searchStatus == SubjectSearchStatus.Error -> ErrorState(
                        message = "Couldn't load subjects.",
                    )
                    uiState.searchResults.isEmpty() -> BasicText(
                        text = "No subjects found.",
                        style = typography.body.copy(color = colors.textSecondary),
                    )
                    else -> FlowRow(
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.searchResults.forEach { subject ->
                            FilterChip(
                                label = subject,
                                selected = false,
                                onToggle = { onSubjectClick(subject) },
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            SectionLabel(
                text = "DISCOVER",
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            )
            BasicText(
                text = "Explore books by subject. Tap a category to browse its collection.",
                style = typography.body.copy(color = colors.textSecondary),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            SearchInput(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onQueryChanged(it) },
                placeholder = "SEARCH SUBJECTS…",
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
            when {
                uiState.searchQuery.isEmpty() && uiState.subjects.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "No subjects available.",
                        style = typography.body.copy(color = colors.textSecondary),
                    )
                }
                uiState.searchQuery.isEmpty() -> LazyVerticalGrid(
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
                uiState.searchStatus == SubjectSearchStatus.Loading -> LoadingState()
                uiState.searchStatus == SubjectSearchStatus.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorState(message = "Couldn't load subjects.")
                }
                uiState.searchResults.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "No subjects found.",
                        style = typography.body.copy(color = colors.textSecondary),
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.searchResults) { subject ->
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
}
