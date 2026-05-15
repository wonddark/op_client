package com.opclient.recentchanges.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.recentchanges.domain.RecentChange
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.EmptyState
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RecentChangesScreen(
    onBookClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    viewModel: RecentChangesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(text = "RECENT CHANGES")
            SecondaryButton(
                text = "REFRESH",
                onClick = { viewModel.onIntent(RecentChangesIntent.Refresh) },
            )
        }

        when {
            uiState.isLoading -> LoadingState()
            uiState.error != null -> ErrorState(
                message = uiState.error ?: "",
                onRetry = { viewModel.onIntent(RecentChangesIntent.Refresh) },
            )
            uiState.changes.isEmpty() -> EmptyState(message = "No recent changes")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.changes, key = { it.id }) { change ->
                    ChangeRow(
                        change = change,
                        onClick = {
                            val key = change.targetKey ?: return@ChangeRow
                            when {
                                key.startsWith("/works/") || key.startsWith("/books/") ->
                                    onBookClick(key)
                                key.startsWith("/authors/") ->
                                    onAuthorClick(key)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeRow(change: RecentChange, onClick: () -> Unit) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = change.label,
            style = typography.bookTitle.copy(color = colors.textPrimary),
            modifier = Modifier.weight(1f),
        )
        BasicText(
            text = formatTimestamp(change.addedAt),
            style = typography.bookAuthor.copy(color = colors.textSecondary),
        )
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')}"
}
