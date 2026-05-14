package com.opclient.subject.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.components.FilterChip
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubjectBrowseScreen(
    onSubjectClick: (String) -> Unit,
    viewModel: SubjectBrowseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
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
