package com.opclient.search.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.core.ApiError
import com.opclient.searchinside.presentation.SearchInsideIntent
import com.opclient.searchinside.presentation.SearchInsideViewModel
import com.opclient.ui.components.BookRow
import com.opclient.ui.components.FilterChip
import com.opclient.ui.components.PrimaryButton
import com.opclient.ui.components.SearchInput
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.EmptyState
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

private enum class SearchMode { BOOKS, FULL_TEXT }

@Composable
fun SearchScreen(
    onBookClick: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
    insideVm: SearchInsideViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val insideUiState by insideVm.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchMode by remember { mutableStateOf(SearchMode.BOOKS) }
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    val placeholderPainter = remember(colors.surface2) { ColorPainter(colors.surface2) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SearchEffect.SearchError -> {
                    errorMessage = when (val err = effect.error) {
                        is ApiError.NetworkError -> "Network error. Check your connection."
                        is ApiError.HttpError -> "Server error (${err.code})."
                        is ApiError.ParseError -> "Failed to parse response."
                        ApiError.Unknown -> "Unknown error occurred."
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SearchInput(
            value = uiState.query,
            onValueChange = { viewModel.onIntent(SearchIntent.QueryChanged(it)) },
            onSearch = {
                errorMessage = null
                when (searchMode) {
                    SearchMode.BOOKS -> viewModel.onIntent(SearchIntent.Search)
                    SearchMode.FULL_TEXT -> insideVm.onIntent(SearchInsideIntent.Search(uiState.query))
                }
            },
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            FilterChip(
                label = "Books",
                selected = searchMode == SearchMode.BOOKS,
                onToggle = { searchMode = SearchMode.BOOKS },
            )
            FilterChip(
                label = "Full Text",
                selected = searchMode == SearchMode.FULL_TEXT,
                onToggle = { searchMode = SearchMode.FULL_TEXT },
            )
        }

        when (searchMode) {
            SearchMode.BOOKS -> {
                when (uiState.status) {
                    SearchStatus.Idle -> {
                        SectionLabel(text = "RECENT", modifier = Modifier.padding(bottom = 8.dp))
                    }
                    SearchStatus.Loading -> {
                        LoadingState()
                    }
                    SearchStatus.Success, SearchStatus.LoadingMore -> {
                        SectionLabel(
                            text = "RESULTS · ${uiState.totalFound} BOOKS",
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(uiState.books, key = { it.key }) { book ->
                                BookRow(
                                    title = book.title,
                                    author = book.author,
                                    subject = book.primarySubject,
                                    coverContent = {
                                        if (book.coverUrl != null) {
                                            AsyncImage(
                                                model = book.coverUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                placeholder = placeholderPainter,
                                                error = placeholderPainter,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        } else {
                                            Box(modifier = Modifier.fillMaxSize())
                                        }
                                    },
                                    onClick = { onBookClick(book.key) },
                                )
                            }
                            if (uiState.canLoadMore && uiState.status != SearchStatus.LoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        PrimaryButton(
                                            text = "LOAD MORE",
                                            onClick = { viewModel.onIntent(SearchIntent.LoadMore) },
                                        )
                                    }
                                }
                            }
                            if (uiState.status == SearchStatus.LoadingMore) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                                        LoadingState(modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }
                    }
                    SearchStatus.Empty -> {
                        EmptyState(message = "No books found for \"${uiState.query}\"")
                    }
                    SearchStatus.Error -> {
                        ErrorState(
                            message = errorMessage ?: "Something went wrong.",
                            onRetry = { viewModel.onIntent(SearchIntent.Search) },
                        )
                    }
                }
            }
            SearchMode.FULL_TEXT -> {
                if (insideUiState.isLoading) {
                    LoadingState()
                } else if (insideUiState.error != null) {
                    ErrorState(
                        message = insideUiState.error ?: "Something went wrong.",
                        onRetry = { insideVm.onIntent(SearchInsideIntent.Search(uiState.query)) },
                    )
                } else if (!insideUiState.hasSearched) {
                    SectionLabel(text = "SEARCH INSIDE BOOKS")
                } else if (insideUiState.results.isEmpty()) {
                    EmptyState(message = "No passages found for \"${uiState.query}\"")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(insideUiState.results, key = { it.workKey + it.passage }) { result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBookClick(result.workKey) }
                                    .padding(vertical = 12.dp),
                            ) {
                                BasicText(
                                    text = result.title,
                                    style = typography.bookTitle.copy(color = colors.textPrimary),
                                )
                                if (result.authorName != null) {
                                    BasicText(
                                        text = result.authorName,
                                        style = typography.bookAuthor.copy(color = colors.textSecondary),
                                    )
                                }
                                BasicText(
                                    text = "\"${result.passage}\"",
                                    style = typography.body.copy(color = colors.textSecondary),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
