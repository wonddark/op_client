package com.opclient.library.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.library.domain.Shelf
import com.opclient.ui.components.BookRow
import com.opclient.ui.components.FilterChip
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.feedback.EmptyState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryScreen(
    onBookClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: LibraryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedShelf = uiState.selectedShelf
    val entries = uiState.entries

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            SecondaryButton(text = "PROFILE & SYNC", onClick = onProfileClick)
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (shelf in Shelf.entries) {
                FilterChip(
                    label = shelf.label,
                    selected = selectedShelf == shelf,
                    onToggle = { viewModel.onIntent(LibraryIntent.SelectShelf(shelf)) },
                )
            }
        }

        if (entries.isEmpty()) {
            EmptyState(message = "No books here yet")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(entries, key = { it.workKey }) { entry ->
                    BookRow(
                        title = entry.title,
                        author = entry.authorName ?: "",
                        onClick = { onBookClick(entry.workKey) },
                    )
                }
            }
        }
    }
}
